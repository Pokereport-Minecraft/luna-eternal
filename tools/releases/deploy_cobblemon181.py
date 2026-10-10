"""One-time guarded production migration. Run on the game host after publication preparation.

Only Cobblemon/TMCraft are replaced. Existing Luna and all worlds/DB are retained.
Client QA and staging evidence must already be present on this host.
"""
import hashlib,json,os,shutil,subprocess,time
from pathlib import Path
import requests,yaml,urllib3
urllib3.disable_warnings()
ID='1f23bff0-bc4c-4c00-bbcf-2fe81ff666dd'
VOL=Path('/var/lib/pterodactyl/volumes')/ID
ROOT=Path('/root/cobblemon181')
LUNA='ed83825868a26ed50d333cb9fc9c33e9ccfb0098959b1a34a3df40d38174a18e'
REPLACEMENTS=[
 ('Cobblemon-fabric-1.8.0+1.21.1.jar','a6228f3291c70ed6348b9a47beadabc79cb241b6522312e7e2b56d428dc9ec31','Cobblemon-fabric-1.8.1+1.21.1.jar','4d90ba6775e3a332aad9d5e49a9f6366f973b3e1a12e0004d22ce21c600eebc5'),
 ('tmcraft-1.4.19+1.8.0.jar','8743b7764e07f8e5b3c9ce6db716f03bcd5249c1859b407cb29e446d2101fd19','tmcraft-1.4.19+1.8.1.jar','78830af88411be5f6fe09b4a28e283ae5d67502d008ce7a36d16915b97f364b6')]
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def running():return subprocess.check_output(['docker','inspect',ID,'--format','{{.State.Running}}'],text=True).strip()=='true'
def cmd(value):subprocess.run(['/usr/local/sbin/pokereport-server-cmd',value],check=True)
def announce(value):cmd('tellraw @a '+json.dumps({'text':'[PokeReport] '+value,'color':'yellow'},ensure_ascii=False))
assert running()
assert sha(VOL/'mods/lunaeternal-0.1.0.jar')==LUNA
stage=Path('/srv/pokereport-prelaunch-20261002')
assert sha(stage/'mods/lunaeternal-0.1.0.jar')==LUNA
assert 'Autotest final: passed=836 failed=0' in (stage/'logs/latest.log').read_text(errors='replace')
qa=json.loads(Path('/srv/pokereport-client181-qa/qa-result.json').read_text())
assert qa['startup']=='PASS' and qa['mixins']=='PASS' and qa['resourceReload']=='PASS'
assert next(m for m in qa['mods'] if m['id']=='cobblemon')['version']=='1.8.1+1.21.1'
before={p.name:sha(p) for p in (VOL/'mods').glob('*.jar')}
backup=Path('/root/backups_predeploy/cobblemon181-20261010');backup.mkdir(parents=True,exist_ok=False)
for old,oldsha,new,newsha in REPLACEMENTS:
 assert sha(VOL/'mods'/old)==oldsha and sha(ROOT/new)==newsha
 assert not (VOL/'mods'/new).exists()
 shutil.copy2(VOL/'mods'/old,backup/old)
 assert sha(backup/old)==oldsha
conf=yaml.safe_load(Path('/etc/pterodactyl/config.yml').read_text())
headers={'Authorization':'Bearer '+conf['token'],'Content-Type':'application/json'}
def power(action):
 result=requests.post(f'https://127.0.0.1:8080/api/servers/{ID}/power',headers=headers,json={'action':action},verify=False,timeout=15)
 result.raise_for_status();print('Wings',action,result.status_code,flush=True)
print('PRECHECK PASS: exact live hashes, native client and server QA; only 2 server JAR changes.',flush=True)
announce('Actualizacion a Cobblemon 1.8.1 en 60 segundos. Se guardaran los datos. Actualiza el pack desde el launcher oficial o el nuevo MRPACK Base 0.3.0 antes de reconectar.')
time.sleep(30);announce('Mantenimiento en 30 segundos: Cobblemon 1.8.1. Mundos, Pokemon y progreso se conservaran.')
time.sleep(20);announce('Mantenimiento en 10 segundos. Guardado seguro en curso.');time.sleep(10)
log=VOL/'logs/latest.log';position=len(log.read_bytes())
cmd('save-all flush');time.sleep(2);power('stop')
deadline=time.monotonic()+180;signalled=False
while running() and time.monotonic()<deadline:
 text=log.read_bytes()[position:].decode(errors='replace')
 if 'All dimensions are saved' in text and 'luna-db - Shutdown completed.' in text and not signalled:
  subprocess.run(['docker','kill','--signal','SIGTERM',ID],check=True);signalled=True
 time.sleep(2)
assert not running(),'Stop incomplete: binaries remain unchanged; inspect before continuing'
for old,oldsha,new,newsha in REPLACEMENTS:
 target=VOL/'mods'/old;assert sha(target)==oldsha
 stat=target.stat();pending=VOL/'mods'/(new+'.next')
 shutil.copyfile(ROOT/new,pending);os.chown(pending,stat.st_uid,stat.st_gid);os.chmod(pending,stat.st_mode & 0o777)
 assert sha(pending)==newsha
 # Old binaries leave mods; never two versions in loader input.
 os.replace(target,backup/(old+'.live-original'))
 os.replace(pending,VOL/'mods'/new)
assert sha(VOL/'mods/lunaeternal-0.1.0.jar')==LUNA
for name,value in before.items():
 if name not in {x[0] for x in REPLACEMENTS}:assert sha(VOL/'mods'/name)==value
(backup/'deployment.json').write_text(json.dumps({'changed':REPLACEMENTS,'unchangedMods':len(before)-2,'lunaSha256':LUNA,'worldOrDatabaseRollback':False},indent=2))
power('start');print('1.8.1 start requested. Worlds/DB/config untouched; Luna unchanged.',flush=True)
