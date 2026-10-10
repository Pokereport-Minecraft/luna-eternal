import asyncio
from datetime import datetime
from io import BytesIO
import discord
from .config import GOLD
from .rendering import card


def navigation(config):
    view = discord.ui.View(timeout=None)
    links = [('Empezar', 'start' if config.channels.get('start') else 'welcome'), ('Normas', 'rules'), ('Intereses y roles', 'roles'),
             ('Guías', 'guides'), ('Ayuda privada', 'support_panel')]
    for label, key in links:
        identifier = config.channels.get(key)
        if identifier:
            view.add_item(discord.ui.Button(label=label, style=discord.ButtonStyle.link,
                url=f'https://discord.com/channels/{config.guild_id}/{identifier}', row=0 if key in ('welcome','rules','roles') else 1))
    return view


class Welcome:
    def __init__(self, client):
        self.client = client
        self.lock = asyncio.Lock()
        self.errors = 0

    def reserve(self, member):
        if member.bot or member.guild.id != self.client.config.guild_id or not member.joined_at:
            return None
        joined = member.joined_at.isoformat()
        db = self.client.store.db
        db.execute('INSERT OR IGNORE INTO welcome_events(guild_id,user_id,joined_at,created_at) VALUES(?,?,?,?)',
                   (member.guild.id, member.id, joined, self.client.store.now()))
        row = db.execute('SELECT * FROM welcome_events WHERE guild_id=? AND user_id=? AND joined_at=?',
                         (member.guild.id, member.id, joined)).fetchone()
        return row

    async def joined(self, member):
        row=self.reserve(member)
        if row is not None:
            await self.deliver(row, member)

    async def deliver(self, row, member):
        async with self.lock:
            db = self.client.store.db
            current = db.execute('SELECT * FROM welcome_events WHERE id=?', (row['id'],)).fetchone()
            if current['status'] != 'PENDING':
                return
            channel = await self.client.port.channel(int(self.client.config.channels['welcome']))
            timestamp = datetime.fromisoformat(row['joined_at'])
            # Recover an ambiguous send without posting a second welcome.
            async for message in channel.history(limit=100):
                if message.author.id == self.client.user.id and any(e.timestamp == timestamp and
                    e.footer.text == 'PokeReport · Bienvenida' and f"<@{member.id}>" in (e.description or '') for e in message.embeds):
                    db.execute("UPDATE welcome_events SET status='SENT',message_id=? WHERE id=?", (message.id,row['id']))
                    return
            avatar = None
            try:
                avatar = await asyncio.wait_for(member.display_avatar.with_size(256).with_format('png').read(), timeout=8)
            except (discord.HTTPException, asyncio.TimeoutError):
                pass
            embed = discord.Embed(title='¡Bienvenido a PokeReport!',
                description=f'<@{member.id}> · Lee las normas, elige tus intereses y encuentra tu próxima aventura.',
                color=GOLD, timestamp=timestamp)
            embed.set_footer(text='PokeReport · Bienvenida')
            file = None
            try:
                image = await asyncio.to_thread(card, 'welcome', member.display_name, avatar)
                file = discord.File(BytesIO(image), filename='bienvenida.png')
                embed.set_image(url='attachment://bienvenida.png')
            except Exception as exc:
                self.errors += 1
                self.client.port.log('welcome_renderer_fallback', error=type(exc).__name__)
            kwargs = {'embed':embed, 'view':navigation(self.client.config), 'allowed_mentions':discord.AllowedMentions.none()}
            if file:
                kwargs['file'] = file
            message = await channel.send(**kwargs)
            db.execute("UPDATE welcome_events SET status='SENT',message_id=? WHERE id=?", (message.id,row['id']))

    async def reconcile(self):
        db = self.client.store.db
        for row in list(db.execute("SELECT * FROM welcome_events WHERE status='PENDING' ORDER BY id LIMIT 10")):
            try:
                member = await self.client.get_guild(row['guild_id']).fetch_member(row['user_id'])
                if not member.joined_at or member.joined_at.isoformat() != row['joined_at']:
                    db.execute("UPDATE welcome_events SET status='SKIPPED' WHERE id=?", (row['id'],))
                    continue
                await self.deliver(row, member)
            except discord.NotFound:
                db.execute("UPDATE welcome_events SET status='SKIPPED' WHERE id=?", (row['id'],))
        db.execute('DELETE FROM welcome_events WHERE created_at<? AND status!=?', (self.client.store.now()-90*86400,'PENDING'))
