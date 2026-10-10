import io
import json
import logging
import discord
from .config import GOLD, TYPES
from .security import overwrite_spec


class DiscordPort:
    def __init__(self, client):
        self.client = client
        self.config = client.config
        self.guild = client.get_guild(self.config.guild_id)

    def log(self, event, **fields):
        logging.getLogger('pokereport').info(json.dumps({'event': event, **fields}))

    async def channel(self, channel_id):
        channel = self.guild.get_channel(channel_id)
        return channel or await self.client.fetch_channel(channel_id)

    def overwrites(self, ticket, closed=False):
        result = {}
        for identifier, kind, allow, deny in overwrite_spec(self.config, self.client.user.id, ticket, closed):
            subject = self.guild.get_role(identifier) if kind == 0 else discord.Object(identifier, type=discord.Member)
            if subject is None:
                raise RuntimeError('Configured role missing')
            result[subject] = discord.PermissionOverwrite.from_pair(discord.Permissions(allow), discord.Permissions(deny))
        return result

    async def find_channel(self, ticket_id):
        # Refresh on ambiguous HTTP errors, not only the Gateway cache.
        channels = await self.guild.fetch_channels()
        marker = f'pokereport-ticket:{ticket_id}'
        matches = [c for c in channels if getattr(c, 'topic', None) == marker]
        if len(matches) > 1:
            raise RuntimeError('Multiple channels share stable ticket identifier')
        return matches[0].id if matches else None

    async def create_channel(self, ticket):
        category = self.guild.get_channel(int(self.config.channels['tickets_open']))
        # Inherit the already-private category first. Discord rejects creation
        # overwrites containing permissions absent from the bot's guild role.
        # Scoped Manage Roles is inherited here, then used to set case ACLs.
        if category.overwrites_for(self.guild.default_role).view_channel is not False:
            raise RuntimeError('Ticket category must deny public access')
        allowed = {self.client.user.id, int(self.config.roles['founder']), int(self.config.roles['admin'])}
        if any(ow.view_channel is True and subject.id not in allowed for subject, ow in category.overwrites.items()):
            raise RuntimeError('Ticket category contains unexpected public access')
        channel = await self.guild.create_text_channel(f"ticket-{ticket['id']:06d}", category=category,
                    topic=f"pokereport-ticket:{ticket['id']}",
                    slowmode_delay=3, reason=f"PokeReport ticket {ticket['id']}")
        return channel.id

    async def exists(self, channel_id):
        try:
            await self.channel(channel_id)
            return True
        except discord.NotFound:
            return False

    async def permissions(self, ticket, closed=False):
        channel = await self.channel(ticket['channel_id'])
        await channel.edit(overwrites=self.overwrites(ticket, closed), reason='PokeReport ticket privacy')

    async def move_closed(self, ticket):
        channel = await self.channel(ticket['channel_id'])
        category = self.guild.get_channel(int(self.config.channels['tickets_closed']))
        if channel.category_id != category.id:
            await channel.edit(category=category, sync_permissions=False)

    async def move_open(self, ticket):
        channel = await self.channel(ticket['channel_id'])
        category = self.guild.get_channel(int(self.config.channels['tickets_open']))
        if channel.category_id != category.id:
            await channel.edit(category=category, sync_permissions=False)

    def embed(self, ticket):
        kind = TYPES[ticket['type']]
        state = {'OPEN': 'Abierto', 'CLAIMED': 'En atención', 'CLOSED': 'Cerrado', 'CLOSING': 'Cerrando', 'ARCHIVED': 'Archivado'}.get(ticket['status'], ticket['status'])
        embed = discord.Embed(title=f"{kind['emoji']} SOPORTE POKEREPORT · #{ticket['id']:06d}",
            description='Puedes añadir aquí información y evidencia relevante. No compartas contraseñas ni tokens.', color=GOLD)
        if self.guild.icon:
            embed.set_thumbnail(url=self.guild.icon.url)
        embed.add_field(name='Tipo', value=kind['label'])
        embed.add_field(name='Estado', value=state)
        embed.add_field(name='Responsable', value=f"<@{ticket['claimed_by']}>" if ticket['claimed_by'] else 'Sin asignar')
        for key, value in ticket['form'].items():
            embed.add_field(name=str(key)[:256], value=discord.utils.escape_mentions(str(value))[:1024] or '—', inline=False)
        if ticket['status'] == 'CLOSED':
            embed.add_field(name='Cierre', value=discord.utils.escape_mentions(ticket['close_reason'] or 'Cerrado')[:1024], inline=False)
        embed.set_footer(text=f"PokeReport Network · Ticket {ticket['id']} · v1")
        return embed

    async def controls(self, ticket):
        channel = await self.channel(ticket['channel_id'])
        from .interactions import TicketView
        embed = self.embed(ticket)
        view = TicketView(self.client, closed=ticket['status'] == 'CLOSED')
        message = None
        if ticket['control_message_id']:
            try:
                message = await channel.fetch_message(ticket['control_message_id'])
            except discord.NotFound:
                pass
        if message is None:
            # Covers crash after SEND succeeded but before message ID persisted.
            async for candidate in channel.history(limit=100, oldest_first=True):
                if candidate.author.id == self.client.user.id and any(e.footer.text == embed.footer.text for e in candidate.embeds):
                    message = candidate
                    break
        if message:
            await message.edit(embed=embed, view=view, allowed_mentions=discord.AllowedMentions.none())
        else:
            message = await channel.send(embed=embed, view=view, allowed_mentions=discord.AllowedMentions.none())
        return message.id

    async def messages(self, channel_id):
        channel = await self.channel(channel_id)
        result = []
        async for message in channel.history(limit=None, oldest_first=True):
            result.append({'id': message.id, 'author_id': message.author.id, 'author_name': str(message.author),
                'created_at': message.created_at.isoformat(), 'edited_at': message.edited_at.isoformat() if message.edited_at else None,
                'content': message.content, 'embeds': [e.to_dict() for e in message.embeds],
                'attachments': [{'id': a.id, 'filename': a.filename, 'size': a.size, 'url': a.url, 'content_type': a.content_type} for a in message.attachments]})
        return result

    async def delete_channel(self, channel_id):
        try:
            channel = await self.channel(channel_id)
            await channel.delete(reason='Approved ticket archive after 72h; transcript retained privately')
        except discord.NotFound:
            pass

    async def audit_events(self):
        channel = await self.channel(int(self.config.channels['ticket_logs']))
        rows = list(self.client.store.db.execute('SELECT id,ticket_id,actor_id,event_type,payload FROM ticket_events WHERE delivered_message_id IS NULL AND expires_at>? ORDER BY id LIMIT 100', (self.client.store.now(),)))
        if not rows:
            return
        known = {}
        async for message in channel.history(limit=100):
            if message.author.id == self.client.user.id:
                for embed in message.embeds:
                    if embed.footer.text and embed.footer.text.startswith('PokeReport · Audit '):
                        known[embed.footer.text] = message.id
        for row in rows:
            marker = f"PokeReport · Audit {row['id']}"
            message_id = known.get(marker)
            if not message_id:
                subject=f"Ticket #{row['ticket_id']:06d}" if row['ticket_id'] else 'Acción administrativa'
                contribution=json.loads(row['payload']).get('contribution_id')
                if contribution:
                    subject=f'Reconocimiento #{int(contribution)}'
                embed = discord.Embed(title=row['event_type'], description=f"{subject}\nActor ID: {row['actor_id'] or 'Sistema'}", color=GOLD)
                embed.set_footer(text=marker)
                message = await channel.send(embed=embed)
                message_id = message.id
            self.client.store.db.execute('UPDATE ticket_events SET delivered_message_id=? WHERE id=?', (message_id, row['id']))

    async def expire_audit(self):
        rows = list(self.client.store.db.execute('SELECT delivered_message_id FROM ticket_events WHERE expires_at<=? AND delivered_message_id IS NOT NULL', (self.client.store.now(),)))
        if not rows:
            return
        channel = await self.channel(int(self.config.channels['ticket_logs']))
        for row in rows:
            try:
                await channel.get_partial_message(row['delivered_message_id']).delete()
            except discord.NotFound:
                pass

    async def panel(self):
        from .interactions import SupportView
        channel = await self.channel(int(self.config.channels['support_panel']))
        embed = discord.Embed(title='🎫 CENTRO DE SOPORTE POKEREPORT',
            description='**Abre tu ticket con los botones de abajo.**\n1. Elige el tipo de consulta.\n2. Completa el formulario.\n3. Recibirás el enlace a tu canal privado.\n\nEl personal autorizado te atenderá allí. No necesitas escribir comandos.', color=GOLD)
        if self.guild.icon:
            embed.set_thumbnail(url=self.guild.icon.url)
        embed.add_field(name='Ayuda y errores', value='Ayuda general para consultas privadas. Reportar bug para errores técnicos reproducibles.', inline=False)
        embed.add_field(name='Casos sensibles', value='Denuncia privada para conductas o evidencias sensibles. Administración para asuntos que realmente requieren revisión administrativa.', inline=False)
        embed.add_field(name='Privacidad y conservación', value='Soporte y bugs: 90 días. Denuncias y administración: 180 días. Auditoría: 365 días. Canales cerrados: 72 horas. Historial sensible solo para Fundador y Administrador. No publiques credenciales.', inline=False)
        embed.set_footer(text='PokeReport Network · Panel tickets · v1')
        saved = self.client.store.db.execute("SELECT message_id FROM managed_panels WHERE panel_key='tickets'").fetchone()
        message = None
        if saved:
            try:
                message = await channel.fetch_message(saved[0])
            except discord.NotFound:
                pass
        if not message:
            async for candidate in channel.history(limit=100):
                if candidate.author.id == self.client.user.id and any(e.footer.text == embed.footer.text for e in candidate.embeds):
                    message = candidate
                    break
        if message:
            await message.edit(embed=embed, view=SupportView(self.client))
        else:
            message = await channel.send(embed=embed, view=SupportView(self.client))
        if not message.pinned:
            try:
                await message.pin(reason='PokeReport support panel')
            except discord.Forbidden:
                # Pin Messages is separate from Manage Messages. Missing this
                # optional privilege must not disable a working support panel.
                self.log('panel_pin_unavailable', channel_id=channel.id)
        self.client.store.db.execute('INSERT INTO managed_panels VALUES(?,?,?,?,?) ON CONFLICT(panel_key) DO UPDATE SET channel_id=excluded.channel_id,message_id=excluded.message_id,version=excluded.version,updated_at=excluded.updated_at',
            ('tickets', channel.id, message.id, 1, self.client.store.now()))
        return message.id
