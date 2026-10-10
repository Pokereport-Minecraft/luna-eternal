import hashlib
import json
from pathlib import Path
import discord
from discord import app_commands
from .interactions import identity, reply, safe, CloseModal
from .store import Rejected


class TicketCommands(app_commands.Group):
    def __init__(self, client):
        super().__init__(name='ticket', description='Soporte privado PokeReport', guild_only=True)
        self.client = client

    async def interaction_check(self, interaction):
        if interaction.guild_id != self.client.config.guild_id:
            await reply(interaction, 'Servidor incorrecto.')
            return False
        return True

    async def current(self, interaction, ticket_id=None, action='info'):
        ticket = self.client.store.ticket(ticket_id=ticket_id) if ticket_id else self.client.store.ticket(channel_id=interaction.channel_id)
        actor, roles = await identity(self.client, interaction)
        self.client.tickets.require(ticket, actor, roles, action)
        return ticket, actor, roles

    async def on_error(self, interaction, error):
        original = getattr(error, 'original', error)
        if isinstance(original, Rejected):
            await reply(interaction, str(original))
        else:
            self.client.port.log('command_error', error=type(original).__name__)
            await reply(interaction, 'No se completÃ³ la operaciÃ³n. Contacta con AdministraciÃ³n.')

    @app_commands.command(description='Muestra tus tickets activos')
    async def status(self, interaction: discord.Interaction):
        await identity(self.client, interaction)
        tickets = self.client.store.active(interaction.user.id)
        await reply(interaction, '\n'.join(f"#{t['id']:06d} Â· {t['status']} Â· <#{t['channel_id']}>" for t in tickets) or 'No tienes tickets activos.')

    @app_commands.command(description='Consulta un ticket autorizado')
    async def info(self, interaction: discord.Interaction, ticket_id: int):
        ticket, _, _ = await self.current(interaction, ticket_id)
        await reply(interaction, f"Ticket #{ticket['id']:06d} Â· {ticket['status']} Â· Responsable: {ticket['claimed_by'] or 'Sin asignar'}")

    @app_commands.command(description='Reclama el ticket actual')
    async def claim(self, interaction: discord.Interaction):
        ticket, actor, roles = await self.current(interaction, action='claim')
        await safe(interaction, self.client.tickets.claim(ticket['id'], actor, roles))

    @app_commands.command(description='Libera el ticket actual')
    async def release(self, interaction: discord.Interaction):
        ticket, actor, roles = await self.current(interaction, action='release')
        await safe(interaction, self.client.tickets.release(ticket['id'], actor, roles))

    @app_commands.command(description='Transfiere a otro miembro del staff autorizado')
    async def transfer(self, interaction: discord.Interaction, usuario: discord.Member):
        ticket, actor, roles = await self.current(interaction, action='transfer')
        usuario = await interaction.guild.fetch_member(usuario.id)
        await safe(interaction, self.client.tickets.transfer(ticket['id'], actor, roles, usuario.id, [r.id for r in usuario.roles]))

    @app_commands.command(description='Cierra el ticket mediante un formulario obligatorio')
    async def close(self, interaction: discord.Interaction):
        ticket, _, _ = await self.current(interaction, action='close')
        await interaction.response.send_modal(CloseModal(self.client, ticket['id']))

    @app_commands.command(description='Reabre un ticket o crea uno vinculado si ya se archivÃ³')
    async def reopen(self, interaction: discord.Interaction, ticket_id: int):
        ticket, actor, roles = await self.current(interaction, ticket_id, 'reopen')
        await safe(interaction, self.client.tickets.reopen(ticket['id'], actor, roles))

    @app_commands.command(description='AÃ±ade un participante de forma deliberada')
    async def add(self, interaction: discord.Interaction, usuario: discord.Member):
        ticket, actor, roles = await self.current(interaction, action='add')
        usuario = await interaction.guild.fetch_member(usuario.id)
        # A developer must never gain other users' report access via add.
        if ticket['type'] == 'report' and int(self.client.config.roles['developer']) in [r.id for r in usuario.roles] and not {int(self.client.config.roles['founder']), int(self.client.config.roles['admin'])} & {r.id for r in usuario.roles}:
            raise Rejected('Desarrollador no puede aÃ±adirse a una denuncia ajena.')
        await safe(interaction, self.client.tickets.participant(ticket['id'], actor, roles, usuario.id, True))

    @app_commands.command(description='Retira un participante adicional')
    async def remove(self, interaction: discord.Interaction, usuario: discord.Member):
        ticket, actor, roles = await self.current(interaction, action='remove')
        await safe(interaction, self.client.tickets.participant(ticket['id'], actor, roles, usuario.id, False))

    @app_commands.command(description='Entrega una transcripciÃ³n solo al staff autorizado')
    async def transcript(self, interaction: discord.Interaction, ticket_id: int):
        ticket, actor, roles = await self.current(interaction, ticket_id, 'transcript')
        await interaction.response.defer(ephemeral=True, thinking=True)
        row = self.client.store.db.execute('SELECT * FROM transcripts WHERE ticket_id=? AND expires_at>? ORDER BY generation DESC LIMIT 1', (ticket['id'], self.client.store.now())).fetchone()
        if not row:
            raise Rejected('No hay transcripciÃ³n vigente; se genera al cerrar.')
        path = self.client.config.data_dir / 'transcripts' / Path(row['storage_reference']).name
        data = path.read_bytes()
        if hashlib.sha256(data).hexdigest() != row['hash']:
            raise Rejected('La integridad de la transcripciÃ³n no coincide.')
        # Never upload a transcript file to Discord/CDN. A short-lived
        # ephemeral reader checks authorization on every page request.
        from .transcript_reader import TranscriptReader
        reader = TranscriptReader(self.client, interaction.user.id, ticket['id'], json.loads(data))
        actor,roles=await identity(self.client,interaction)
        self.client.tickets.require(self.client.store.ticket(ticket_id=ticket['id']),actor,roles,'transcript')
        reader.message = await interaction.followup.send(embed=reader.render(), view=reader,
            ephemeral=True, wait=True, allowed_mentions=discord.AllowedMentions.none())
        self.client.store.event(ticket['id'], actor, 'TRANSCRIPT_REQUESTED', {'hash': row['hash']})
