"""Quiet community automation, separate from tickets and their database."""
import asyncio, hashlib, json, re, sqlite3, time
import unicodedata
from typing import Literal
from datetime import datetime, timedelta
from zoneinfo import ZoneInfo
import discord
from discord import app_commands
from discord.http import Route
from .config import GOLD
from .releases import discover

ALERT_ROLES={'Actualizaciones':'updates_alert','Eventos y comunidad':'community_alert','Comercio':'trade_alert'}
QUESTIONS=[
 '¿Cuál fue tu mejor captura de la semana? Comparte una foto o el nombre del Pokémon; también valen los que todavía estás buscando.',
 '¿Qué objetivo estás intentando completar? Cuenta uno y encuentra a alguien que quiera acompañarte.',
 '¿Qué consejo te habría ayudado cuando entraste por primera vez? Una frase puede ayudar al próximo entrenador.',
 '¿Con qué Pokémon te gustaría formar tu próximo equipo? Comparte tu favorito y el motivo.',
]

class Ledger:
    def __init__(self,path):
        self.db=sqlite3.connect(path,isolation_level=None)
        self.db.row_factory=sqlite3.Row
        self.db.execute('PRAGMA journal_mode=WAL')
        self.db.execute('CREATE TABLE IF NOT EXISTS deliveries(key TEXT PRIMARY KEY,digest TEXT NOT NULL,status TEXT NOT NULL DEFAULT \'PENDING\',message_id INTEGER,created_at INTEGER NOT NULL)')
        self.db.execute('CREATE TABLE IF NOT EXISTS notice_budget(key TEXT PRIMARY KEY,created_at INTEGER NOT NULL)')
        self.db.execute('CREATE TABLE IF NOT EXISTS profiles(user_id INTEGER PRIMARY KEY,character TEXT NOT NULL,updated_at INTEGER NOT NULL)')

    def reserve(self,key,digest):
        self.db.execute('INSERT OR IGNORE INTO deliveries(key,digest,created_at) VALUES(?,?,?)',(key,digest,int(time.time())))
        row=self.db.execute('SELECT * FROM deliveries WHERE key=?',(key,)).fetchone()
        if row['digest']!=digest:raise ValueError('Published identity changed; review required')
        return dict(row)

    def sent(self,key,identifier):
        self.db.execute("UPDATE deliveries SET status='SENT',message_id=? WHERE key=?",(identifier,key))

    def notice(self,key,now=None):
        now=int(time.time()) if now is None else now
        self.db.execute('BEGIN IMMEDIATE')
        try:
            if self.db.execute('SELECT 1 FROM notice_budget WHERE key=?',(key,)).fetchone():return True
            if self.db.execute('SELECT 1 FROM notice_budget WHERE created_at>?',(now-86400,)).fetchone():return False
            self.db.execute('INSERT INTO notice_budget VALUES(?,?)',(key,now));return True
        finally:self.db.execute('COMMIT')

def links(config,release=None):
    view=discord.ui.View(timeout=None)
    view.add_item(discord.ui.Button(label='Instalar y jugar',url='https://pokereport.online/jugar/'))
    view.add_item(discord.ui.Button(label='Modpack completo',url=release.mrpack if release else 'https://pokereport.online/client/PokeReport-Base-0.2.0.mrpack'))
    if release and release.manual:view.add_item(discord.ui.Button(label='Actualizar manualmente',url=release.manual))
    view.add_item(discord.ui.Button(label='Ayuda privada',url=f'https://discord.com/channels/{config.guild_id}/{config.channels["support_panel"]}'))
    return view

def character_name(value):
    value=unicodedata.normalize('NFC',value).strip()
    if not 1<=len(value)<=18 or any(unicodedata.category(c).startswith('C') or c in '@[]<>`' for c in value):
        raise ValueError('Usa entre 1 y 18 caracteres visibles, sin menciones, corchetes ni símbolos de formato.')
    return value

def profile_alias(member,config,character):
    for key,label in [('founder','Fundador'),('admin','Admin'),('moderator','Mod'),('developer','Dev'),
                      ('builder','Constructor'),('beta','Tester')]:
        identifier=config.roles.get(key)
        if identifier and any(r.id==int(identifier) for r in member.roles):return f'[{label}] {character}'[:32]
    return f'[Entrenador] {character}'[:32]

def start_embed(config):
    embed=discord.Embed(title='Tu aventura empieza aquí',color=GOLD,
       description='**1 · Instala.** Usa el launcher oficial o importa el MRPACK en tu launcher favorito.\n'
                   '**2 · Entra.** Minecraft 1.21.1, Fabric y Java 21. Servidor: `play.pokereport.online`.\n'
                   '**3 · Encuentra tu sitio.** Saluda con tu objetivo o busca compañeros; no hace falta escribir una presentación larga.')
    for title,key in [('Habla con la comunidad','general'),('Encuentra compañeros','team'),('Consulta cambios y descargas','updates'),('Pregunta o pide ayuda','help')]:
        if config.channels.get(key):embed.add_field(name=title,value=f'<#{config.channels[key]}>',inline=True)
    embed.add_field(name='Tú eliges los avisos',value='Usa `/avisos` o Canales y roles. Sin mensajes privados automáticos ni avisos a todo el servidor.',inline=False)
    embed.set_footer(text='PokeReport · Ruta de entrada v1')
    return embed

class Preferences(discord.ui.View):
    def __init__(self,client,user):
        super().__init__(timeout=180);self.client=client;self.owner=user.id
        options=[]
        for label,key in ALERT_ROLES.items():
            role=user.guild.get_role(int(client.config.roles[key]))
            options.append(discord.SelectOption(label=label,value=str(role.id),default=role in user.roles))
        options.append(discord.SelectOption(label='No recibir avisos',value='none'))
        select=discord.ui.Select(placeholder='Elige tus avisos',min_values=1,max_values=3,options=options)
        select.callback=self.selected;self.select=select;self.add_item(select)

    async def selected(self,interaction):
        if interaction.user.id!=self.owner:return await interaction.response.send_message('Este panel es personal. Abre `/avisos`.',ephemeral=True)
        await interaction.response.defer(ephemeral=True)
        values=set(self.select.values)
        if 'none' in values and len(values)>1:return await interaction.followup.send('Selecciona «No recibir avisos» por separado.',ephemeral=True)
        config=self.client.config;guild=interaction.guild;member=interaction.user
        roles=[guild.get_role(int(config.roles[key])) for key in ALERT_ROLES.values()]
        if any(r is None or r.permissions.value!=0 or r>=guild.me.top_role for r in roles):
            return await interaction.followup.send('Las preferencias necesitan revisión del staff. Puedes usar Canales y roles.',ephemeral=True)
        try:
            add=[r for r in roles if str(r.id) in values and r not in member.roles]
            remove=[r for r in roles if str(r.id) not in values and r in member.roles]
            if add:await member.add_roles(*add,reason='Preferencias voluntarias de avisos')
            if remove:await member.remove_roles(*remove,reason='Preferencias voluntarias de avisos')
            await interaction.followup.send('Preferencias guardadas. Puedes cambiarlas cuando quieras. No te enviaremos mensajes privados automáticos.',ephemeral=True)
        except discord.Forbidden:
            await interaction.followup.send('El bot necesita permiso para gestionar estos roles. Usa Canales y roles mientras el staff lo revisa.',ephemeral=True)

class PreferencesButton(discord.ui.View):
    def __init__(self,client):super().__init__(timeout=None);self.client=client

    @discord.ui.button(label='Elegir mis avisos',style=discord.ButtonStyle.secondary,custom_id='pokereport:community:preferences:v1')
    async def choose(self,interaction,button):
        if not interaction.guild or interaction.guild.id!=self.client.config.guild_id:return
        await interaction.response.send_message('Elige solo lo que te interesa.',view=Preferences(self.client,interaction.user),ephemeral=True)

class Community:
    def __init__(self,client):
        self.client=client;self.config=client.config
        self.ledger=Ledger(self.config.data_dir/'community.sqlite')
        self.task=None;self.last_release=None;self.last_error=None;self.last_check=None
        self.notice_permission_missing=False
        self.nickname_permission_missing=False
        self.commands=[app_commands.Command(name='empezar',description='Instala, entra y encuentra ayuda',callback=self.start),
                       app_commands.Command(name='avisos',description='Elige tus avisos voluntarios',callback=self.preferences),
                       app_commands.Command(name='estado-comunidad',description='Estado de automatizaciones (administración)',callback=self.status),
                       app_commands.Command(name='publicar-aviso',description='Comunicado oficial sin duplicados (administración)',callback=self.publish),
                       app_commands.Command(name='perfil',description='Define tu personaje y activa tu alias por rol',callback=self.profile)]

    async def update_alias(self,member,character):
        guild=member.guild
        if member.bot:return 'Las cuentas de bot no usan perfiles de jugador.'
        if not guild.me.guild_permissions.manage_nicknames:
            self.nickname_permission_missing=True
            return 'Personaje guardado. Falta que el staff active «Gestionar apodos» para el bot.'
        self.nickname_permission_missing=False
        if member.id==guild.owner_id or member.top_role>=guild.me.top_role:
            return 'Personaje guardado. Discord impide cambiar el apodo del propietario o de miembros por encima del bot.'
        alias=profile_alias(member,self.config,character)
        if member.nick!=alias:await member.edit(nick=alias,reason='Alias voluntario /perfil basado en roles existentes')
        return f'Alias actualizado: {alias}'

    async def profile(self,interaction:discord.Interaction,personaje:str):
        if not interaction.guild or interaction.guild.id!=self.config.guild_id:return
        try:name=character_name(personaje)
        except ValueError as exc:return await interaction.response.send_message(str(exc),ephemeral=True)
        await interaction.response.defer(ephemeral=True)
        self.ledger.db.execute('INSERT INTO profiles VALUES(?,?,?) ON CONFLICT(user_id) DO UPDATE SET character=excluded.character,updated_at=excluded.updated_at',
                               (interaction.user.id,name,int(time.time())))
        try:message=await self.update_alias(interaction.user,name)
        except discord.Forbidden:message='Personaje guardado. Discord rechazó el cambio de apodo; el staff debe revisar permisos y jerarquía.'
        await interaction.followup.send(message+' Este nombre es un perfil de Discord; no verifica tu cuenta de Minecraft.',ephemeral=True)

    async def aliases(self):
        guild=self.client.get_guild(self.config.guild_id)
        if not guild:return
        self.nickname_permission_missing=not guild.me.guild_permissions.manage_nicknames
        if self.nickname_permission_missing:return
        for row in self.ledger.db.execute('SELECT user_id,character FROM profiles').fetchall():
            try:
                member=await guild.fetch_member(row['user_id'])
                await self.update_alias(member,row['character'])
            except discord.NotFound:
                self.ledger.db.execute('DELETE FROM profiles WHERE user_id=?',(row['user_id'],))
            except discord.Forbidden:continue

    async def start(self,interaction:discord.Interaction):
        await interaction.response.send_message(embed=start_embed(self.config),view=links(self.config),ephemeral=True)

    async def preferences(self,interaction:discord.Interaction):
        if not interaction.guild or interaction.guild.id!=self.config.guild_id:return
        await interaction.response.send_message('Elige solo lo que te interesa.',view=Preferences(self.client,interaction.user),ephemeral=True)

    @app_commands.default_permissions(manage_guild=True)
    async def status(self,interaction:discord.Interaction):
        allowed={int(self.config.roles[k]) for k in ['founder','admin']}
        if not interaction.guild or not allowed.intersection(r.id for r in interaction.user.roles):
            return await interaction.response.send_message('Disponible para Fundador y Administrador.',ephemeral=True)
        await interaction.response.send_message(f'Bot conectado: {self.client.is_ready()}\nÚltima versión: {self.last_release or "pendiente"}\nÚltimo error: {self.last_error or "ninguno"}\nPermiso para avisos voluntarios pendiente: {self.notice_permission_missing}\nRevisión de versiones: cada 10 minutos. Actividad: máximo una pregunta semanal.',ephemeral=True)

    @app_commands.default_permissions(manage_guild=True)
    async def publish(self,interaction:discord.Interaction,referencia:str,titulo:str,resumen:str,
                      destino:Literal['Anuncios','Actualizaciones']='Anuncios'):
        allowed={int(self.config.roles[k]) for k in ['founder','admin']}
        if not interaction.guild or not allowed.intersection(r.id for r in interaction.user.roles):
            return await interaction.response.send_message('Disponible para Fundador y Administrador.',ephemeral=True)
        if not re.fullmatch(r'[a-zA-Z0-9][a-zA-Z0-9._-]{0,63}',referencia) or not 1<=len(titulo)<=180 or not 1<=len(resumen)<=1500:
            return await interaction.response.send_message('Usa una referencia corta y única, título de hasta 180 caracteres y resumen de hasta 1500.',ephemeral=True)
        await interaction.response.defer(ephemeral=True)
        key='oficial:'+referencia
        digest=hashlib.sha256(json.dumps([titulo,resumen,destino],ensure_ascii=False).encode()).hexdigest()
        channel=self.config.channels['news' if destino=='Anuncios' else 'updates']
        try:
            message=await self.deliver(key,digest,channel,discord.Embed(title=titulo,description=resumen,color=GOLD))
            await interaction.followup.send(f'Comunicado publicado o recuperado sin duplicar: https://discord.com/channels/{self.config.guild_id}/{channel}/{message}',ephemeral=True)
        except ValueError:
            await interaction.followup.send('Esa referencia ya identifica otro contenido. Revisa el aviso; no se ha sobrescrito.',ephemeral=True)
        except discord.HTTPException:
            await interaction.followup.send('Discord no confirmó el envío. Reintenta con la misma referencia para evitar duplicados.',ephemeral=True)

    async def deliver(self,key,digest,channel_id,embed,view=None,notify_role=None):
        row=self.ledger.reserve(key,digest)
        if row['status']=='SENT':return row['message_id']
        channel=await self.client.port.channel(int(channel_id))
        marker='PokeReport · '+key
        embed.set_footer(text=marker)
        # Recover sends accepted by Discord before a process interruption.
        async for message in channel.history(limit=100):
            if message.author.id==self.client.user.id and any(e.footer.text==marker for e in message.embeds):
                self.ledger.sent(key,message.id);return message.id
        payload={'embeds':[embed.to_dict()],'allowed_mentions':{'parse':[]},'flags':4096,
                 'nonce':hashlib.sha256(key.encode()).hexdigest()[:24],'enforce_nonce':True}
        if notify_role:
            guild=channel.guild;role=guild.get_role(int(notify_role))
            permitted=bool(role and (role.mentionable or channel.permissions_for(guild.me).mention_everyone))
            self.notice_permission_missing=not permitted
            if permitted and self.ledger.notice(key):
                payload['content']=f'<@&{role.id}>'
                payload['allowed_mentions']={'parse':[],'roles':[str(role.id)],'users':[],'replied_user':False}
                payload['flags']=0
        if view:payload['components']=view.to_components()
        data=await self.client.http.request(Route('POST','/channels/{channel_id}/messages',channel_id=channel.id),json=payload)
        self.ledger.sent(key,int(data['id']));return int(data['id'])

    async def panels(self):
        await self.deliver('entrada:v1','v1',self.config.channels['start'],start_embed(self.config),links(self.config))
        embed=discord.Embed(title='Elige qué quieres recibir',description='Pulsa el botón para activar o quitar avisos. Los intereses y canales opcionales están en **Canales y roles**.\n\nNo enviamos mensajes privados automáticos ni usamos `@everyone`.',color=GOLD)
        await self.deliver('preferencias:v1','v1',self.config.channels['roles'],embed,PreferencesButton(self.client))

    async def releases(self):
        release=await asyncio.to_thread(discover)
        embed=discord.Embed(title=f'Cliente {release.version} · {release.title}',color=GOLD,
            description=release.summary+'\n\n**Launcher oficial:** cierra Minecraft y pulsa Jugar.\n'
               '**Otro launcher, instalación nueva:** importa el MRPACK completo.\n'
               '**Actualización manual:** sigue el LEEME del ZIP si está disponible; retira el JAR anterior de `mods` antes de añadir el nuevo. Conserva los otros mods, configuración y mundos.')
        await self.deliver('cliente:'+release.version,release.digest,self.config.channels['updates'],embed,links(self.config,release),
                           notify_role=self.config.roles['updates_alert'])
        if self.config.channels.get('manual_downloads'):
            manual=discord.Embed(title=f'Descargas manuales · Base {release.base}',color=GOLD,
                description='**Opción recomendada:** importa el MRPACK completo en tu launcher favorito.\n'
                '**Instancia existente:** sigue el LEEME del ZIP. Puede requerir sustituir otros mods además de Luna Eternal; no mezcles versiones.\n'
                '**JAR principal:** el botón descarga Luna Eternal directamente. Por sí solo no actualiza Cobblemon ni TMCraft.\n'
                'Cierra Minecraft antes de cambiar archivos. Conserva tus mundos y configuración y guarda los JAR antiguos fuera de `mods`.')
            view=links(self.config,release)
            if release.jar:view.add_item(discord.ui.Button(label='JAR Luna Eternal',url=release.jar))
            await self.deliver('manual:'+release.version,release.digest,self.config.channels['manual_downloads'],manual,view)
        self.last_release=release.version;self.last_check=int(time.time())

    async def engagement(self,now=None):
        now=now or datetime.now(ZoneInfo('America/Bogota'))
        # Saturday 19:00 Bogotá; no catch-up after missed weeks or long outages.
        if now.weekday()!=5 or not 19<=now.hour<21:return
        week=now.strftime('%G-W%V')
        question=QUESTIONS[int(now.strftime('%V'))%len(QUESTIONS)]
        embed=discord.Embed(title='La pregunta de la semana',description=question,color=GOLD)
        embed.add_field(name='¿Buscas compañeros?',value=f'Comparte tu objetivo en <#{self.config.channels["team"]}>.',inline=False)
        await self.deliver('comunidad:'+week,hashlib.sha256(question.encode()).hexdigest(),self.config.channels['general'],embed)

    async def run(self):
        next_release=0
        next_alias=0
        while not self.client.is_closed():
            if self.client.is_ready():
                try:
                    await self.panels()
                    if time.monotonic()>=next_release:
                        next_release=time.monotonic()+600
                        await self.releases()
                    await self.engagement()
                    if time.monotonic()>=next_alias:
                        next_alias=time.monotonic()+300
                        await self.aliases()
                    self.last_error=None
                except Exception as exc:
                    self.last_error=type(exc).__name__
                    self.client.port.log('community_retry_pending',error=self.last_error)
            await asyncio.sleep(60)

    async def close(self):
        if self.task:
            self.task.cancel()
            try:await self.task
            except asyncio.CancelledError:pass
        self.ledger.db.close()
