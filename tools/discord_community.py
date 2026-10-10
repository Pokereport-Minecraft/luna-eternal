"""Audit/apply the existing guild by stable IDs. No deletes or bulk command sync."""
import argparse, copy, json, os
from pathlib import Path
from discord_setup import Discord

GUILD='1537115743898832906'
BOT='1537115306424672297'
IDS={'info':'1537117534694674502','community':'1537117629381091360',
 'optional':'1555775280809902200','support':'1537117633520996503',
 'welcome':'1537117535575474359','guides':'1555775303329255546','rules':'1537118365812920471',
 'updates':'1537117628274057288','news':'1537117626927554682','roles':'1537136191458115786',
 'general':'1537115745585074199','team':'1555775321574346874','media':'1537117632556437566',
 'suggestions':'1555783330723536949','old_suggestions':'1537117631289757786',
 'help':'1537117634993332345','tickets':'1555783320166735985','bugs':'1555775313898639400',
 'roleplay':'1555783334473367592','trade':'1555775317291962479'}

def credentials(path):
    values=dict(os.environ)
    if path.exists():
        for line in path.read_text(encoding='utf-8').splitlines():
            if '=' in line and not line.lstrip().startswith('#'):
                key,value=line.split('=',1); values[key.strip()]=value.strip().strip('"').strip("'")
    return values

def onboarding(current):
    prompts=[]
    for prompt in current['prompts']:
        p={k:copy.deepcopy(prompt[k]) for k in ['id','type','title','single_select','required','in_onboarding','options']}
        p['required']=False
        if p['id']=='1555783361803456649':
            p['in_onboarding']=False
            p['title']='¿Qué avisos quieres recibir? Puedes cambiarlos cuando quieras'
        for option in p['options']:
            option.pop('emoji',None)
            option['description']='Opcional. Puedes cambiar tu elección en Canales y roles.'
        prompts.append(p)
    return {'enabled':True,'mode':1,'prompts':prompts,'default_channel_ids':
        [IDS[k] for k in ['welcome','updates','general','help','tickets']]}

def apply(d, output):
    guild=d.get('/guilds/'+GUILD)
    assert guild['name']=='PokeReport Network', 'Wrong guild; refusing changes'
    channels=d.get('/guilds/'+GUILD+'/channels')
    roles=d.get('/guilds/'+GUILD+'/roles')
    before=d.get('/guilds/'+GUILD+'/onboarding')
    output.mkdir(parents=True,exist_ok=True)
    snapshot=output/'before.json'
    if not snapshot.exists(): snapshot.write_text(json.dumps({'guild':guild,'channels':channels,'roles':roles,'onboarding':before},ensure_ascii=False),encoding='utf-8')
    byid={c['id']:c for c in channels}
    changes={
      'info':{'name':'🚀 EMPIEZA AQUÍ'}, 'optional':{'name':'🎮 EXPLORA A TU RITMO'},
      'welcome':{'name':'empieza-aqui','topic':'Instala, entra y encuentra ayuda en tres pasos. Launcher oficial o MRPACK en tu launcher favorito. Guía de entrada; sin notificaciones masivas.'},
      'roles':{'name':'avisos-y-roles','topic':'Elige intereses y avisos en Canales y roles o con /avisos. Son voluntarios y puedes quitarlos cuando quieras.'},
      'updates':{'topic':'Actualizaciones verificadas del cliente. Launcher, MRPACK y paquete manual; una publicación por versión. Sin @everyone.'},
      'news':{'topic':'Eventos y comunicados importantes. Las notas técnicas están en actualizaciones; tus avisos son voluntarios.'},
      'old_suggestions':{'name':'sugerencias-archivo','parent_id':IDS['optional'],'topic':'Historial conservado. Nuevas propuestas en el foro sugerencias.'},
      'suggestions':{'name':'sugerencias'},
      'team':{'parent_id':IDS['community'],'topic':'Encuentra compañeros: objetivo, horario y cómo unirse. Puedes responder con una frase; sin datos personales.'},
      'roleplay':{'parent_id':IDS['optional']}}
    names={'info':'🚀・EMPIEZA AQUÍ','community':'💬・COMUNIDAD','support':'🆘・AYUDA',
      'optional':'🎮・A TU RITMO','welcome':'👋・empieza-aquí','guides':'📥・instalar-y-jugar',
      'rules':'📜・normas','news':'📢・anuncios','updates':'🆕・actualizaciones',
      'roles':'🔔・avisos-y-roles','general':'💬・general','team':'🤝・busco-equipo',
      'media':'📸・capturas','suggestions':'💡・sugerencias','old_suggestions':'📚・sugerencias-archivo',
      'help':'❓・ayuda','tickets':'🎫・abrir-ticket','bugs':'🐞・reportar-bug',
      'trade':'🛒・comercio','roleplay':'🎭・roleplay'}
    for key,name in names.items():changes.setdefault(key,{})['name']=name
    for key,payload in changes.items():
        if any(byid[IDS[key]].get(k)!=v for k,v in payload.items()):
            d.patch('/channels/'+IDS[key],payload);print('Updated',key)
    # Scoped access for this bot only. Public channels remain read-only for members.
    for key in ['welcome','guides','roles','updates','news']:
        d._req('PUT','/channels/'+IDS[key]+'/permissions/'+BOT,
               {'type':1,'allow':str((1<<10)|(1<<11)|(1<<14)|(1<<15)|(1<<16)),'deny':'0'})
    marker='pokereport-community-arrivals'
    arrivals=next((c for c in channels if c.get('topic')==marker),None)
    if arrivals is None:
        arrivals=d.post('/guilds/'+GUILD+'/channels',{'name':'bienvenidas','type':0,'parent_id':IDS['optional'],
          'topic':marker,'permission_overwrites':[
          {'id':GUILD,'type':0,'allow':str((1<<10)|(1<<16)),'deny':str(1<<11)},
          {'id':BOT,'type':1,'allow':str((1<<10)|(1<<11)|(1<<14)|(1<<15)|(1<<16)),'deny':'0'}]})
    if arrivals['name']!='👋・bienvenidas':d.patch('/channels/'+arrivals['id'],{'name':'👋・bienvenidas'})
    order=['info','community','support','optional','welcome','guides','rules','updates','news','roles',
           'general','team','media','suggestions','help','tickets','bugs']
    d._req('PATCH','/guilds/'+GUILD+'/channels',[{'id':IDS[k],'position':n} for n,k in enumerate(order)])
    new=d._req('PUT','/guilds/'+GUILD+'/onboarding',onboarding(before))
    # Keep all existing AutoMod rules and alerts; improve their existing mention threshold.
    for rule in d.get('/guilds/'+GUILD+'/auto-moderation/rules'):
        if rule['trigger_type']==5:
            metadata=dict(rule['trigger_metadata']);metadata['mention_raid_protection_enabled']=True
            d.patch('/guilds/'+GUILD+'/auto-moderation/rules/'+rule['id'],{'trigger_metadata':metadata})
    result={'guild_id':GUILD,'arrivals_id':arrivals['id'],'channels':IDS,
            'default_channel_ids':new['default_channel_ids'],'mode':new['mode']}
    (output/'applied.json').write_text(json.dumps(result,indent=2),encoding='utf-8')
    print('Applied without deleting any channel, role, conversation or ticket')

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--env-file',type=Path,default=Path('.env'))
    parser.add_argument('--apply',action='store_true');parser.add_argument('--output',type=Path,default=Path('build/discord-community'))
    args=parser.parse_args();env=credentials(args.env_file)
    d=Discord(env['DISCORD_TOKEN'],GUILD,args.apply)
    assert d.get('/users/@me')['id']==BOT
    if args.apply:apply(d,args.output)
    else:print(json.dumps({'guild':GUILD,'channels':IDS,'operations':'Rename/order existing channels, seven defaults with optional interests, separate welcomes. No deletes.'},indent=2))

if __name__=='__main__':main()
