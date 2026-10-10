import asyncio
import json
import logging
import os
import signal
from pathlib import Path
import discord
from discord import app_commands
from .config import Config
from .store import Store
from .service import Tickets
from .discord_port import DiscordPort
from .interactions import SupportView, TicketView
from .commands import TicketCommands
from .contributions import Contributions, ContributionCommands
from .welcome import Welcome
from .health import metrics
from .community import Community, PreferencesButton


class SupportBot(discord.Client):
    def __init__(self, config):
        intents = discord.Intents.none()
        intents.guilds = True
        # Needed for complete REST message history, not member enumeration.
        intents.message_content = config.message_content
        intents.members = config.welcome_enabled
        super().__init__(intents=intents, member_cache_flags=discord.MemberCacheFlags.none(),
                         chunk_guilds_at_startup=False, allowed_mentions=discord.AllowedMentions.none())
        self.config = config
        config.data_dir.mkdir(parents=True, exist_ok=True)
        self.store = Store(config.data_dir / 'tickets.sqlite', audit_days=config.audit_days)
        self.tree = app_commands.CommandTree(self)
        self.commands = TicketCommands(self)
        self.tree.add_command(self.commands, guild=discord.Object(id=config.guild_id))
        self.contribution_commands=ContributionCommands(self)
        self.tree.add_command(self.contribution_commands,guild=discord.Object(id=config.guild_id))
        self.contributions=Contributions(self)
        self.welcome=Welcome(self)
        self.community=Community(self) if config.community_enabled else None
        if self.community:
            for command in self.community.commands:
                self.tree.add_command(command,guild=discord.Object(id=config.guild_id))
        self.last_retention_at=None
        self.last_maintenance_error=None
        self.ready_for_tickets = False
        self.initialized = False
        self.startup_failed = False
        self.worker = None

    async def setup_hook(self):
        if self.community:self.add_view(PreferencesButton(self))
        self.add_view(SupportView(self))
        self.add_view(TicketView(self))
        self.add_view(TicketView(self, closed=True))

    async def on_ready(self):
        if self.initialized:
            self.ready_for_tickets = True
            return
        self.ready_for_tickets = False
        if self.user.id != self.config.application_id:
            raise RuntimeError('Unexpected bot identity')
        guild = self.get_guild(self.config.guild_id)
        if guild is None:
            raise RuntimeError('Configured guild unavailable')
        app = await self.application_info()
        if not self.config.message_content or not (app.flags.gateway_message_content or app.flags.gateway_message_content_limited):
            raise RuntimeError('Message Content must be explicitly enabled for complete transcripts')
        if self.config.welcome_enabled and not (app.flags.gateway_guild_members or app.flags.gateway_guild_members_limited):
            raise RuntimeError('Approved welcome requires Server Members Intent')
        self.port = DiscordPort(self)
        self.tickets = Tickets(self.config, self.store, self.port)
        await self.tickets.reconcile()
        if self.config.recognition_enabled:
            await self.contributions.reconcile()
        if self.config.welcome_enabled:
            await self.welcome.reconcile()
        # Upsert only our command; never bulk-delete other commands.
        await self.http.upsert_guild_command(self.application_id, self.config.guild_id, self.commands.to_dict(self.tree))
        if self.config.recognition_enabled:
            await self.http.upsert_guild_command(self.application_id,self.config.guild_id,self.contribution_commands.to_dict(self.tree))
        if self.community:
            for command in self.community.commands:
                await self.http.upsert_guild_command(self.application_id,self.config.guild_id,command.to_dict(self.tree))
            self.community.task=asyncio.create_task(self.community.run())
        if self.config.publish_panel:
            await self.port.panel()
        self.ready_for_tickets = True
        self.initialized = True
        self.port.log('startup_ready', guild_id=self.config.guild_id)
        self.worker = asyncio.create_task(self.maintenance())

    async def on_member_join(self, member):
        if self.config.welcome_enabled:
            try:
                if self.initialized:
                    await self.welcome.joined(member)
                else:
                    self.welcome.reserve(member)
            except Exception as exc:
                self.port.log('welcome_delivery_error',error=type(exc).__name__)

    async def on_disconnect(self):
        self.ready_for_tickets = False

    async def on_resumed(self):
        if self.initialized:
            self.ready_for_tickets = True

    async def on_error(self, event, *args, **kwargs):
        # discord.py default prints exception traceback, which may include
        # user content. Report event only; health remains false on startup error.
        logging.getLogger('pokereport').error('gateway_event_error event=%s', event)
        if event == 'on_ready' and not self.initialized:
            self.startup_failed = True
            self.ready_for_tickets = False
            self.write_health(error='StartupFailed')
            await self.close()

    async def maintenance(self):
        last_backup = 0
        while not self.is_closed():
            try:
                if self.is_ready():
                    await self.tickets.reconcile()
                    await self.port.audit_events()
                    await self.port.expire_audit()
                    self.tickets.expire()
                    self.last_retention_at=self.store.now()
                    if self.config.welcome_enabled:
                        await self.welcome.reconcile()
                    if self.config.recognition_enabled:
                        await self.contributions.reconcile()
                    self.ready_for_tickets = True
                    now = self.store.now()
                    if now - last_backup >= 86400:
                        root = self.config.data_dir / 'backups'
                        root.mkdir(exist_ok=True, mode=0o700)
                        path = root / f'{now}.sqlite'
                        self.tickets.backup(path)
                        os.chmod(path, 0o600)
                        backup_status=self.config.data_dir/'backup-status.json'
                        backup_status.write_text(json.dumps({'file':path.name,'created_at':now,'verified':True}),encoding='utf-8')
                        os.chmod(backup_status,0o600)
                        for old in root.glob('*.sqlite'):
                            if old.stat().st_mtime < now - 7 * 86400:
                                old.unlink()
                        last_backup = now
                    self.write_health()
                    self.last_maintenance_error=None
            except Exception as exc:
                self.port.log('maintenance_error', error=type(exc).__name__)
                self.ready_for_tickets = False
                self.last_maintenance_error=type(exc).__name__
                self.write_health(error=type(exc).__name__)
            await asyncio.sleep(60)

    def write_health(self, error=None):
        self.store.db.execute('SELECT 1').fetchone()
        data = {'process': True, 'discord': self.is_ready(), 'tickets_ready': self.ready_for_tickets,
                'database': True, 'updated_at': self.store.now(), 'error': error}
        data.update(metrics(self))
        if self.community:
            data['community']={'last_release':self.community.last_release,'last_check':self.community.last_check,'last_error':self.community.last_error,
                               'nickname_permission_missing':self.community.nickname_permission_missing,
                               'notice_permission_missing':self.community.notice_permission_missing}
        path = self.config.data_dir / 'health.json'
        temp = path.with_suffix('.tmp')
        temp.write_text(json.dumps(data), encoding='utf-8')
        os.chmod(temp, 0o600)
        os.replace(temp, path)

    async def close(self):
        self.ready_for_tickets = False
        if self.worker:
            self.worker.cancel()
        if self.community:await self.community.close()
        await super().close()
        self.store.close()


def main():
    os.umask(0o077)
    logging.basicConfig(level=logging.INFO, format='%(asctime)s %(name)s %(message)s')
    logging.getLogger('discord').setLevel(logging.WARNING)
    config = Config.load()
    client = SupportBot(config)
    client.run(os.environ['DISCORD_TOKEN'], log_handler=None)
    if client.startup_failed:
        raise SystemExit(1)


if __name__ == '__main__':
    main()
