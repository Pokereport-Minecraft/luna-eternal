"""Staff-approved public recognition with preview, durable identity and edits."""
import asyncio
import hashlib
import json
from datetime import datetime, timezone
from io import BytesIO
from zoneinfo import ZoneInfo
import discord
from discord import app_commands
from .config import GOLD
from .interactions import identity, reply
from .rendering import card
from .store import Rejected

CATEGORIES = ('Diseño', 'Experiencia', 'Seguridad', 'Herramientas', 'Comunidad', 'Documentación')


def require_staff(config, roles):
    if not set(roles) & {int(config.roles['founder']), int(config.roles['admin'])}:
        raise Rejected('Solo Fundador o Administrador pueden aprobar reconocimientos.')


class Contributions:
    def __init__(self, client):
        self.client = client
        self.lock = asyncio.Lock()

    def get(self, identifier):
        row = self.client.store.db.execute('SELECT * FROM contributions WHERE id=?', (identifier,)).fetchone()
        if not row:
            raise Rejected('Reconocimiento no disponible.')
        return dict(row)

    def draft(self, target, actor, roles, title, description, category, identifier=None):
        require_staff(self.client.config, roles)
        title, description, category = title.strip(), description.strip(), category.strip()
        if not title or len(title)>100 or not description or len(description)>1000 or category not in CATEGORIES:
            raise Rejected('Completa título, descripción y una categoría válida.')
        db = self.client.store.db
        key = hashlib.sha256(json.dumps([target,title,description,category],ensure_ascii=False).encode()).hexdigest()
        now = self.client.store.now()
        with self.client.store.transaction():
            if identifier is not None:
                old = self.get(identifier)
                if old['state'] != 'DRAFT':
                    raise Rejected('Usa la vista previa de edición para un reconocimiento publicado.')
                db.execute('UPDATE contributions SET title=?,description=?,category=?,updated_at=?,dedupe_key=? WHERE id=?',
                           (title,description,category,now,key,identifier))
                return self.get(identifier)
            db.execute('INSERT OR IGNORE INTO contributions(user_id,authorized_by,title,description,category,created_at,updated_at,dedupe_key) VALUES(?,?,?,?,?,?,?,?)',
                       (target,actor,title,description,category,now,now,key))
            return dict(db.execute('SELECT * FROM contributions WHERE dedupe_key=?',(key,)).fetchone())

    async def artwork(self, entry, member):
        avatar = None
        try:
            avatar = await asyncio.wait_for(member.display_avatar.with_size(256).with_format('png').read(), timeout=8)
        except (discord.HTTPException, asyncio.TimeoutError):
            pass
        day = datetime.fromtimestamp(entry['updated_at'], ZoneInfo('America/Bogota')).strftime('%d/%m/%Y')
        return await asyncio.to_thread(card, 'contribution', member.display_name, avatar,
                                       entry['title'],entry['description'],entry['category'],day)

    async def publish(self, identifier, actor, roles, expected_revision, update=None):
        require_staff(self.client.config, roles)
        if not self.client.config.recognition_enabled:
            raise Rejected('Reconocimientos aún no activados.')
        async with self.lock:
            entry = self.get(identifier)
            if entry['revision'] != expected_revision:
                raise Rejected('La vista previa ha cambiado. Abre una nueva antes de confirmar.')
            if entry['state']=='PUBLISHED' and update is None:
                return entry['message_id']
            is_edit=bool(update)
            if entry['state']=='PUBLISHING':
                pending=json.loads(entry['pending_data'])
                update=pending['fields']
                is_edit=pending['edited']
            member = await self.client.get_guild(self.client.config.guild_id).fetch_member(entry['user_id'])
            if update:
                entry = dict(entry, **update, updated_at=self.client.store.now())
            channel = await self.client.port.channel(int(self.client.config.channels['recognition']))
            marker = f"PokeReport · Mejora {entry['id']}"
            message = None
            if entry['message_id']:
                message = await channel.fetch_message(entry['message_id'])
            else:
                async for candidate in channel.history(limit=100):
                    if candidate.author.id==self.client.user.id and any(e.footer.text==marker for e in candidate.embeds):
                        message = candidate
                        break
            image = await self.artwork(entry, member)
            embed = discord.Embed(title=entry['title'],description=discord.utils.escape_mentions(entry['description']),color=GOLD)
            embed.add_field(name='Colaboración',value=f'<@{member.id}> · {entry["category"]}')
            embed.set_image(url='attachment://mejora.png')
            embed.set_footer(text=marker)
            # Revalidate the approving member immediately before publication.
            approver = await channel.guild.fetch_member(actor)
            require_staff(self.client.config, [r.id for r in approver.roles])
            pending={'fields':{k:entry[k] for k in ('title','description','category','updated_at')},'edited':is_edit,'actor':actor}
            self.client.store.db.execute("UPDATE contributions SET state='PUBLISHING',pending_data=? WHERE id=?",(json.dumps(pending),identifier))
            file = discord.File(BytesIO(image),filename='mejora.png')
            if message:
                await message.edit(embed=embed,attachments=[file],allowed_mentions=discord.AllowedMentions.none())
            else:
                message = await channel.send(embed=embed,file=file,allowed_mentions=discord.AllowedMentions.none())
            self.finalize(identifier,message.id,channel.id,pending)
            return message.id

    def finalize(self, identifier, message_id, channel_id, pending):
        fields=pending['fields']
        with self.client.store.transaction():
            self.client.store.db.execute("UPDATE contributions SET state='PUBLISHED',message_id=?,channel_id=?,authorized_by=?,title=?,description=?,category=?,updated_at=?,revision=revision+1,pending_data='{}' WHERE id=?",
                (message_id,channel_id,pending['actor'],fields['title'],fields['description'],fields['category'],fields['updated_at'],identifier))
            self.client.store.event(None,pending['actor'],'CONTRIBUTION_EDITED' if pending['edited'] else 'CONTRIBUTION_PUBLISHED',{'contribution_id':identifier})

    async def reconcile(self):
        self.client.store.db.execute("DELETE FROM contributions WHERE state='DRAFT' AND updated_at<?",(self.client.store.now()-86400,))
        rows=list(self.client.store.db.execute("SELECT * FROM contributions WHERE state='PUBLISHING'"))
        if not rows:return
        channel=await self.client.port.channel(int(self.client.config.channels['recognition']))
        known={}
        async for message in channel.history(limit=100):
            if message.author.id==self.client.user.id:
                for embed in message.embeds:
                    known[embed.footer.text]=(message,embed)
        async with self.lock:
            for row in rows:
                pending=json.loads(row['pending_data'])
                candidate=known.get(f"PokeReport · Mejora {row['id']}")
                if candidate and candidate[1].title==pending['fields']['title'] and candidate[1].description==discord.utils.escape_mentions(pending['fields']['description']):
                    self.finalize(row['id'],candidate[0].id,channel.id,pending)
                # Never auto-publish an unconfirmed draft or retry an uncertain
                # edit with old text. Staff can explicitly resume with preview.


class Approval(discord.ui.View):
    def __init__(self, client, entry, actor, update=None):
        super().__init__(timeout=300)
        self.client,self.entry,self.actor,self.update=client,entry,actor,update

    async def interaction_check(self, interaction):
        try:
            actor,roles=await identity(self.client,interaction)
            require_staff(self.client.config,roles)
            if actor!=self.actor:
                raise Rejected('Esta vista previa pertenece a otro administrador.')
            return True
        except Rejected as exc:
            await reply(interaction,str(exc));return False

    @discord.ui.button(label='Confirmar publicación',style=discord.ButtonStyle.success)
    async def confirm(self, interaction, button):
        await interaction.response.defer(ephemeral=True,thinking=True)
        try:
            actor,roles=await identity(self.client,interaction)
            message=await self.client.contributions.publish(self.entry['id'],actor,roles,self.entry['revision'],self.update)
            await reply(interaction,f'Reconocimiento publicado: https://discord.com/channels/{self.client.config.guild_id}/{self.client.config.channels["recognition"]}/{message}')
            self.stop()
        except Rejected as exc:
            await reply(interaction,str(exc))
        except Exception as exc:
            self.client.port.log('recognition_error',error=type(exc).__name__)
            await reply(interaction,'No se completó. La vista previa se conserva; revisa el estado antes de repetir.')

    @discord.ui.button(label='Cancelar',style=discord.ButtonStyle.secondary)
    async def cancel(self, interaction, button):
        self.stop()
        await interaction.response.edit_message(content='Publicación cancelada.',embed=None,attachments=[],view=None)


class ContributionModal(discord.ui.Modal):
    def __init__(self, client, target, existing=None):
        super().__init__(title='Reconocer una mejora',timeout=300)
        self.client,self.target,self.existing=client,target,existing
        self.title_input=discord.ui.TextInput(label='Título',max_length=100,default=existing['title'] if existing else None)
        self.description_input=discord.ui.TextInput(label='Mejora y beneficio para la comunidad',style=discord.TextStyle.paragraph,max_length=1000,default=existing['description'] if existing else None)
        self.category_input=discord.ui.TextInput(label='Categoría: Diseño, Experiencia, Seguridad…',max_length=30,default=existing['category'] if existing else 'Comunidad')
        for item in (self.title_input,self.description_input,self.category_input):self.add_item(item)

    async def on_submit(self, interaction):
        await interaction.response.defer(ephemeral=True,thinking=True)
        try:
            actor,roles=await identity(self.client,interaction)
            require_staff(self.client.config,roles)
            member=await interaction.guild.fetch_member(self.target)
            data={'title':self.title_input.value.strip(),'description':self.description_input.value.strip(),'category':self.category_input.value.strip()}
            if data['category'] not in CATEGORIES:
                raise Rejected('Categorías: '+', '.join(CATEGORIES))
            if not data['title'] or not data['description']:
                raise Rejected('Título y descripción son obligatorios.')
            if self.existing and self.existing['state'] in ('PUBLISHED','PUBLISHING'):
                if self.existing['state']=='PUBLISHING' and any(data[k]!=self.existing[k] for k in data):
                    raise Rejected('La publicación anterior está pendiente. Confirma su texto antes de editarlo.')
                entry=self.existing;preview=dict(entry,**data);update=data if entry['state']=='PUBLISHED' else None
            else:
                entry=self.client.contributions.draft(member.id,actor,roles,**data,identifier=self.existing['id'] if self.existing else None)
                preview=entry;update=None
            image=await self.client.contributions.artwork(preview,member)
            embed=discord.Embed(title='Vista previa · requiere tu confirmación',description='Revisa el texto y el colaborador. Se publicará una sola tarjeta.',color=GOLD)
            embed.set_image(url='attachment://vista-previa.png')
            await interaction.followup.send(embed=embed,file=discord.File(BytesIO(image),filename='vista-previa.png'),
                view=Approval(self.client,entry,actor,update),ephemeral=True,allowed_mentions=discord.AllowedMentions.none())
        except (Rejected,discord.NotFound) as exc:
            await reply(interaction,str(exc) if isinstance(exc,Rejected) else 'El colaborador ya no está en el servidor.')
        except Exception as exc:
            self.client.port.log('recognition_preview_error',error=type(exc).__name__)
            await reply(interaction,'No se pudo preparar la tarjeta. No se publicó ningún reconocimiento.')


class ContributionCommands(app_commands.Group):
    def __init__(self, client):
        super().__init__(name='mejora',description='Reconocimientos aprobados de la comunidad',guild_only=True)
        self.client=client

    async def interaction_check(self, interaction):
        try:
            _,roles=await identity(self.client,interaction)
            require_staff(self.client.config,roles)
            return self.client.config.recognition_enabled
        except Rejected as exc:
            await reply(interaction,str(exc));return False

    async def on_error(self, interaction, error):
        original=getattr(error,'original',error)
        await reply(interaction,str(original) if isinstance(original,Rejected) else 'No se completó la operación de reconocimiento.')

    @app_commands.command(description='Prepara una tarjeta, con vista previa y aprobación')
    async def registrar(self, interaction:discord.Interaction, usuario:discord.Member):
        await interaction.response.send_modal(ContributionModal(self.client,usuario.id))

    @app_commands.command(description='Edita un reconocimiento con nueva confirmación')
    async def editar(self, interaction:discord.Interaction, contribution_id:int):
        entry=self.client.contributions.get(contribution_id)
        if entry['state']=='PUBLISHING':
            entry=dict(entry,**json.loads(entry['pending_data'])['fields'])
        await interaction.response.send_modal(ContributionModal(self.client,entry['user_id'],entry))
