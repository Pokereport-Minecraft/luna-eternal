import discord
from .config import GOLD
from .interactions import identity, reply
from .store import Rejected


class TranscriptReader(discord.ui.View):
    def __init__(self, client, user_id, ticket_id, payload):
        super().__init__(timeout=120)
        self.client, self.user_id, self.ticket_id = client, user_id, ticket_id
        self.message = None
        self.index = 0
        self.pages = []
        text = f"Ticket #{ticket_id:06d}\nMotivo de cierre: {payload['close_reason']}\n\n"
        for m in payload['messages']:
            text += f"{m['created_at']} Â· {m['author_name']} ({m['author_id']})\n{m['content']}\n"
            for e in m['embeds']:
                text += f"{e.get('title', '')}\n{e.get('description', '')}\n"
                text += '\n'.join(f"{f['name']}: {f['value']}" for f in e.get('fields', [])) + '\n'
            for a in m['attachments']:
                text += f"Adjunto: {a['filename']} Â· {a['size']} bytes Â· referencia Discord ID {a['id']}\n"
            text += '\n'
        # Exact stored export remains on disk. Reader exposes all text in
        # bounded pages; no remote attachment download or public share link.
        self.pages = [text[i:i+3500] for i in range(0, len(text), 3500)] or ['Sin mensajes.']

    def render(self):
        embed = discord.Embed(title=f'TranscripciÃ³n privada Â· #{self.ticket_id:06d}',
            description=discord.utils.escape_mentions(self.pages[self.index]), color=GOLD)
        embed.set_footer(text=f'PokeReport Â· {self.index+1}/{len(self.pages)} Â· Consulta efÃ­mera, expira en 2 minutos')
        return embed

    async def interaction_check(self, interaction):
        try:
            if interaction.user.id != self.user_id or interaction.guild_id != self.client.config.guild_id:
                raise Rejected('Consulta no autorizada.')
            ticket = self.client.store.ticket(ticket_id=self.ticket_id)
            actor, roles = await identity(self.client, interaction)
            self.client.tickets.require(ticket, actor, roles, 'transcript')
            row = self.client.store.db.execute('SELECT 1 FROM transcripts WHERE ticket_id=? AND expires_at>?', (self.ticket_id, self.client.store.now())).fetchone()
            if not row:
                raise Rejected('La transcripciÃ³n ha expirado.')
            return True
        except Rejected as exc:
            await reply(interaction, str(exc))
            return False

    @discord.ui.button(label='Anterior', style=discord.ButtonStyle.secondary)
    async def previous(self, interaction, button):
        self.index = max(0, self.index-1)
        await interaction.response.edit_message(embed=self.render())

    @discord.ui.button(label='Siguiente', style=discord.ButtonStyle.secondary)
    async def next_page(self, interaction, button):
        self.index = min(len(self.pages)-1, self.index+1)
        await interaction.response.edit_message(embed=self.render())

    async def on_timeout(self):
        self.pages.clear()
        if self.message:
            try:
                await self.message.delete()
            except discord.HTTPException:
                pass
