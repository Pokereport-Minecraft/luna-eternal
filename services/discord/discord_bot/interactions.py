import logging
import discord
from .config import TYPES
from .store import Rejected


async def identity(client, interaction):
    if interaction.guild_id != client.config.guild_id:
        raise Rejected('Operación no autorizada.')
    guild = client.get_guild(client.config.guild_id)
    if guild is None:
        raise Rejected('Operación no autorizada.')
    try:
        member = await guild.fetch_member(interaction.user.id)
    except discord.NotFound:
        raise Rejected('Operación no autorizada.') from None
    if member.pending:
        raise Rejected('Completa primero las normas de entrada de Discord.')
    return member.id, [r.id for r in member.roles]


async def reply(interaction, text, **kwargs):
    kwargs.setdefault('ephemeral', True)
    kwargs.setdefault('allowed_mentions', discord.AllowedMentions.none())
    if interaction.response.is_done():
        await interaction.followup.send(text, **kwargs)
    else:
        await interaction.response.send_message(text, **kwargs)


async def safe(interaction, operation):
    await interaction.response.defer(ephemeral=True, thinking=True)
    try:
        result = await operation
        if isinstance(result, dict):
            result = f"Ticket #{result['id']:06d} · <#{result['channel_id']}>"
        await reply(interaction, result or 'Operación completada.')
    except Rejected as exc:
        await reply(interaction, str(exc))
    except Exception as exc:
        logging.getLogger('pokereport').error('interaction_error class=%s', type(exc).__name__)
        await reply(interaction, 'No se pudo completar. El estado se conserva para recuperación; no repitas la solicitud de inmediato. Contacta con Administración.')


class SafeView(discord.ui.View):
    async def interaction_check(self, interaction):
        if interaction.guild_id != self.client.config.guild_id or not isinstance(interaction.user, discord.Member):
            await reply(interaction, 'Esta herramienta solo funciona dentro de PokeReport.')
            return False
        return True

    async def on_error(self, interaction, error, item):
        logging.getLogger('pokereport').error('view_error class=%s', type(error).__name__)
        await reply(interaction, 'No se completó la acción. El equipo puede revisar el estado del ticket.')


FORM_FIELDS = {
    'help': [('Asunto', True, False), ('Describe lo que necesitas', True, True), ('Nick de Minecraft', False, False)],
    'bug': [('Resumen del problema', True, False), ('¿Qué ocurrió?', True, True), ('¿Cómo puede reproducirse?', True, True), ('Nick de Minecraft', False, False)],
    'report': [('Usuario involucrado', True, False), ('Qué ocurrió', True, True), ('Cuándo ocurrió aproximadamente', True, False), ('Contexto adicional', False, True)],
    'admin': [('Asunto administrativo', True, False), ('Por qué requiere Administración', True, True), ('Contexto adicional', False, True)],
}


class CreateModal(discord.ui.Modal):
    def __init__(self, client, kind):
        super().__init__(title=TYPES[kind]['label'], timeout=300)
        self.client, self.kind = client, kind
        self.fields = []
        for label, required, long in FORM_FIELDS[kind]:
            field = discord.ui.TextInput(label=label, required=required,
                style=discord.TextStyle.paragraph if long else discord.TextStyle.short,
                max_length=900 if long else 120)
            self.add_item(field)
            self.fields.append(field)

    async def on_submit(self, interaction):
        if interaction.guild_id != self.client.config.guild_id:
            return await reply(interaction, 'Servidor incorrecto.')
        async def action():
            await identity(self.client, interaction)
            ticket = await self.client.tickets.create(interaction.user.id, self.kind, {f.label: str(f.value).strip() for f in self.fields})
            return f"Ticket #{ticket['id']:06d} abierto: <#{ticket['channel_id']}>. Puedes adjuntar allí capturas, vídeos o fragmentos de logs revisados."
        await safe(interaction, action())

    async def on_error(self, interaction, error):
        await reply(interaction, 'No se completó la solicitud. Contacta con Administración sin publicar información privada.')


class SupportView(SafeView):
    def __init__(self, client):
        super().__init__(timeout=None)
        self.client = client
        for kind, spec in TYPES.items():
            button = discord.ui.Button(label=spec['label'], emoji=spec['emoji'], style=discord.ButtonStyle.secondary,
                                       custom_id=f'pr:support:{kind}', row=0 if kind in ('help', 'bug') else 1)
            async def callback(interaction, selected=kind):
                if not self.client.ready_for_tickets:
                    return await reply(interaction, 'Soporte temporalmente no disponible. Usa ayuda para consultas públicas o contacta directamente con el staff para casos privados.')
                await interaction.response.send_modal(CreateModal(self.client, selected))
            button.callback = callback
            self.add_item(button)


class CloseModal(discord.ui.Modal):
    def __init__(self, client, ticket_id):
        super().__init__(title='Cerrar ticket', timeout=300)
        self.client, self.ticket_id = client, ticket_id
        self.reason = discord.ui.TextInput(label='Motivo del cierre', style=discord.TextStyle.paragraph, required=True, max_length=1000)
        self.add_item(self.reason)

    async def on_submit(self, interaction):
        async def action():
            actor, roles = await identity(self.client, interaction)
            await self.client.tickets.close(self.ticket_id, actor, roles, self.reason.value)
            return 'Ticket cerrado. Transcripción guardada de forma privada.'
        await safe(interaction, action())


class TicketView(SafeView):
    def __init__(self, client, closed=False):
        super().__init__(timeout=None)
        self.client = client
        for action, label, disabled in [('claim', 'Reclamar', closed), ('release', 'Liberar', closed), ('close', 'Cerrar', closed), ('reopen', 'Reabrir', not closed)]:
            button = discord.ui.Button(label=label, style=discord.ButtonStyle.secondary, custom_id=f'pr:ticket:{action}', disabled=disabled)
            async def callback(interaction, operation=action):
                try:
                    ticket = self.client.store.ticket(channel_id=interaction.channel_id)
                    actor, roles = await identity(self.client, interaction)
                    self.client.tickets.require(ticket, actor, roles, operation)
                    if operation == 'close':
                        return await interaction.response.send_modal(CloseModal(self.client, ticket['id']))
                    async def execute():
                        await getattr(self.client.tickets, operation)(ticket['id'], actor, roles)
                        return 'Operación completada.'
                    await safe(interaction, execute())
                except Rejected as exc:
                    await reply(interaction, str(exc))
            button.callback = callback
            self.add_item(button)
