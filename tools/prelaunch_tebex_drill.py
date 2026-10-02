from pathlib import Path
import json,uuid,subprocess,time,sys
STAGE=Path('/srv/pokereport-prelaunch-20261002'); NAME='pokereport-prelaunch-20261002'; DB='prelaunch_20261002'
assert not any((STAGE/'mods').glob('tebex-*.jar'))
started=subprocess.check_output(['docker','inspect','-f','{{.State.StartedAt}}',NAME],text=True).strip()
bootlog=subprocess.check_output(['docker','logs','--since',started,NAME],stderr=subprocess.STDOUT,text=True)
assert 'Done (' in bootlog,'Wait for staging to finish startup before sending commands'
config=json.loads((STAGE/'config/luna_tebex_packages.json').read_text())['packages']
coins=next((k,v) for k,v in config.items() if v['type']=='LUNACOINS')
rank=next((k,v) for k,v in config.items() if v['type']=='RANK' and v['rank']=='ELITE')
upgrade=next((k,v) for k,v in config.items() if v['type']=='RANK_UPGRADE' and v['from']=='ELITE')
replay='--replay' in sys.argv
if replay:
 previous=json.loads(Path('/tmp/pokereport-final-tebex-receipt.json').read_text())
 run=previous['run_id'];uid=previous['test_uuid'];pending=previous['pending_test_username']
else:
 run='qa-final-'+uuid.uuid4().hex[:12]; uid=str(uuid.uuid4()); pending='QA'+uuid.uuid4().hex[:10]
commands=[]
for label,package in [('coins',coins[0]),('rank',rank[0]),('upgrade',upgrade[0])]:
 command=f'luna tebex fulfill {run}-{label} {uid} {package} 1 QA_Tebex'
 commands.extend([command,command])
commands.extend([f'luna tebex fulfill {run}-pending {pending} {coins[0]} 1 {pending}']*2)
attach=subprocess.Popen(['docker','attach','--sig-proxy=false',NAME],stdin=subprocess.PIPE,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
try:
 attach.stdin.write(('\n'.join(commands)+'\n').encode());attach.stdin.flush();time.sleep(3)
finally:
 attach.terminate()
 try: attach.wait(timeout=3)
 except subprocess.TimeoutExpired: attach.kill();attach.wait()
def query(q): return subprocess.check_output(['mariadb','-N','-B',DB,'-e',q],text=True).strip()
rows=query(f"SELECT transaction_id,status FROM tebex_fulfillment WHERE transaction_id LIKE '{run}%' ORDER BY transaction_id").splitlines()
assert len(rows)==4,rows
assert sum('DELIVERED' in r for r in rows)==3,rows
assert sum('PENDING_PLAYER_RESOLUTION' in r for r in rows)==1,rows
balance=int(query(f"SELECT balance FROM player_economy e JOIN player p USING(player_id) WHERE p.mc_uuid='{uid}' AND currency='REPORTCOIN'"))
assert balance==coins[1]['amount'],balance
assert int(query(f"SELECT COUNT(*) FROM ledger_entry WHERE idempotency_key='{('TEBEX:'+run+'-coins:'+coins[0]+':CREDIT')}'"))==1
assert query(f"SELECT rank_id FROM player WHERE mc_uuid='{uid}'")==upgrade[1]['to']
receipt={'run_id':run,'test_uuid':uid,'pending_test_username':pending,'coin_amount':coins[1]['amount'],'balance':balance,'fulfillments':4,'delivered':3,'pending':1,'duplicate_delivery':False,'final_rank':upgrade[1]['to'],'real_external_purchase':False,'external_connector_disabled':True,'database':DB}
receipt['replayed_after_restart']=replay
Path('/tmp/pokereport-final-tebex-receipt.json').write_text(json.dumps(receipt,indent=2))
print(json.dumps({k:v for k,v in receipt.items() if k not in ['test_uuid','pending_test_username']}))

