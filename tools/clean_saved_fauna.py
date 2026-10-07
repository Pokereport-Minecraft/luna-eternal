"""Offline, backed-up cleanup of pregenerated wild-world fauna; never terrain."""
import argparse,collections,gzip,io,json,os,pathlib,shutil,subprocess,time,zlib
import nbtlib

MOBS=set('chicken sheep pig cow armadillo llama wolf turtle donkey horse villager cat iron_golem parrot ocelot panda drowned frog rabbit fox allay elder_guardian witch camel goat zombie_villager'.split())
SPECIAL=set('articuno zapdos moltres mewtwo mew raikou entei suicune lugia hooh ho-oh celebi'.split())

def present(value):
    if isinstance(value,(nbtlib.IntArray,nbtlib.LongArray,nbtlib.ByteArray)):return len(value)>0
    return bool(value)

def protected(e):
    p=e.get('Pokemon',{})
    if any(present(e.get(k)) for k in ('CustomName','Tags','PersistenceRequired','Invulnerable','Passengers','Tame','Owner','OwnerUUID','BattleId','StoreCoordinates','StoreUUID','PastureId')): return True
    if any(present(p.get(k)) for k in ('Shiny','Alpha','Nickname','Owner','OwnerUUID','OwnerNPC','BattleId','BattleClone','PokemonOriginalTrainer','OriginalTrainer')): return True
    # CobbleNav adds this spawn-detail identifier to every naturally spawned
    # Pokemon. Any other persistent metadata remains protected.
    if set(p.get('PersistentData',{})) - {'cobblenavdetailid'}:return True
    if any(any(x in k.lower() for x in ('owner','battle','pasture','storecoordinate')) and present(v) for d in (e,p) for k,v in d.items()):return True
    if str(p.get('Species','')).split(':')[-1] in SPECIAL:return True
    if 'alpha' in str(p.get('Features',[])).lower():return True
    if e.get('BehavioursAreCustom'):return True
    return False

def retain(entities):
    kept=[];removed=[];normal=0
    for e in entities:
        typ=str(e.get('id',''))
        if protected(e):kept.append(e);continue
        if typ.startswith('minecraft:') and typ.split(':')[-1] in MOBS:removed.append(e);continue
        if typ=='cobblemon:pokemon':
            normal+=1
            if normal>1:removed.append(e);continue
        kept.append(e)
    return kept,removed

def chunks(raw):
    assert len(raw)>=8192
    for i in range(1024):
        loc=int.from_bytes(raw[4*i:4*i+4],'big');sector=loc>>8;capacity=loc&255
        if not sector:continue
        off=sector*4096;length=int.from_bytes(raw[off:off+4],'big');kind=raw[off+4]
        assert kind in (1,2,3),'Unsupported external/compressed entity chunk'
        payload=bytes(raw[off+5:off+4+length])
        payload=gzip.decompress(payload) if kind==1 else zlib.decompress(payload) if kind==2 else payload
        yield i,off,capacity,kind,nbtlib.File.parse(io.BytesIO(payload))

def clean(root,backup,apply=False,container=None):
    root=root.resolve();backup=backup.resolve()
    assert root!=backup and root not in backup.parents
    if apply:
        assert container,'An offline container check is mandatory'
        r=subprocess.run(['docker','inspect','-f','{{.State.Running}}',container],capture_output=True,text=True)
        assert r.returncode!=0 or r.stdout.strip()=='false','Server must be stopped'
        backup.mkdir(parents=True,exist_ok=False)
    removed_counts=collections.Counter();kept_counts=collections.Counter();records=[];modified=[]
    try:
        for region in sorted(root.glob('world/dimensions/lunaeternal/salvaje*/entities/*.mca')):
            raw=bytearray(region.read_bytes())
            if not raw:continue
            expected={};changed=False
            for i,off,capacity,kind,n in list(chunks(raw)):
                entities=n.get('Entities',[]);kept,removed=retain(entities)
                kept_counts.update(str(e.get('id','')) for e in kept)
                if not removed:continue
                changed=True;removed_counts.update(str(e.get('id','')) for e in removed)
                for e in removed:
                    assert not protected(e)
                    records.append({'region':str(region.relative_to(root)),'chunk_index':i,'uuid':str(e.get('UUID','')),'type':str(e.get('id',''))})
                expected[i]=kept
                n['Entities']=nbtlib.List[nbtlib.Compound](kept);buf=io.BytesIO();n.write(buf);payload=buf.getvalue()
                payload=gzip.compress(payload) if kind==1 else zlib.compress(payload) if kind==2 else payload
                encoded=(len(payload)+1).to_bytes(4,'big')+bytes([kind])+payload
                assert len(encoded)<=capacity*4096,'Cleanup unexpectedly grew an entity chunk'
                raw[off:off+capacity*4096]=encoded+b'\0'*(capacity*4096-len(encoded))
                raw[4096+4*i:4100+4*i]=int(time.time()).to_bytes(4,'big')
            if not changed:continue
            verified={i:n for i,_,_,_,n in chunks(raw)}
            for i,kept in expected.items():
                assert verified[i]['Entities']==kept,'Retained entity data changed'
            if apply:
                dest=backup/region.relative_to(root);dest.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(region,dest)
                modified.append(region)
                tmp=region.with_suffix('.fauna-ready');tmp.write_bytes(raw);s=region.stat();os.chown(tmp,s.st_uid,s.st_gid);os.chmod(tmp,s.st_mode);tmp.replace(region)
        receipt={'apply':apply,'removed':dict(removed_counts),'kept':dict(kept_counts),'removed_total':sum(removed_counts.values()),'modified_regions':len(modified),'terrain_modified':False,'records':records}
        if apply:(backup/'receipt.json').write_text(json.dumps(receipt,indent=2))
        return receipt
    except Exception:
        for region in modified:shutil.copy2(backup/region.relative_to(root),region)
        raise

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('root',type=pathlib.Path);p.add_argument('backup',type=pathlib.Path);p.add_argument('--apply',action='store_true');p.add_argument('--container');a=p.parse_args()
    r=clean(a.root,a.backup,a.apply,a.container);r.pop('records');print(json.dumps(r,indent=2))
