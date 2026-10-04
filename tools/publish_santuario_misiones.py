"""Publish the tested hotfix only; preserve all other live pack entries."""
import argparse, hashlib, json, subprocess, time, urllib.request
from pathlib import Path
from datetime import datetime, timezone
ROOT=Path(__file__).resolve().parent.parent
REPO='corderovibes-collab/luna-eternal-pack'
BASE=f'https://github.com/{REPO}/releases/download'
def fetch(url): return urllib.request.urlopen(url,timeout=60).read()
def main():
    parser=argparse.ArgumentParser();parser.add_argument('jar',type=Path);parser.add_argument('--publicar',action='store_true');parser.add_argument('--expected-base-sha1',required=True);args=parser.parse_args()
    out=ROOT/'build'/'santuario-misiones-publication';out.mkdir(parents=True,exist_ok=True)
    pointer=json.loads(fetch(BASE+'/pack-manifest/latest.json?check='+str(time.time_ns())))
    raw=fetch(pointer['manifest']);assert hashlib.sha1(raw).hexdigest()==pointer['sha1']
    manifest=json.loads(raw);before=json.loads(raw)
    data=args.jar.read_bytes();sha=hashlib.sha1(data).hexdigest()
    asset='lunaeternal-santuario-misiones-'+sha[:10]+'.jar';(out/asset).write_bytes(data)
    entry=next(x for x in manifest['files'] if x['path']=='mods/lunaeternal-0.1.0.jar')
    assert entry['sha1']==args.expected_base_sha1,'The live client JAR changed; do not overwrite another hotfix'
    url=BASE+'/pack-assets/'+asset;entry.update(sha1=sha,size=len(data),url=url,urls=[url])
    assert len(manifest['files'])==len(before['files'])
    assert [x for x in manifest['files'] if x['path']!='mods/lunaeternal-0.1.0.jar']==[x for x in before['files'] if x['path']!='mods/lunaeternal-0.1.0.jar']
    encoded=json.dumps(manifest,indent=2,ensure_ascii=False).encode();h=hashlib.sha1(encoded).hexdigest();name='manifest-'+h[:10]+'.json';(out/name).write_bytes(encoded)
    new={**pointer,'manifest':BASE+'/pack-assets/'+name,'sha1':h,'size':len(encoded),'publicado':datetime.now(timezone.utc).strftime('%Y-%m-%dT%H:%M:%SZ')}
    receipt={'previous_pointer':pointer,'new_pointer':new,'asset':asset,'only_entry_changed':'mods/lunaeternal-0.1.0.jar','sha256':hashlib.sha256(data).hexdigest(),'published':False}
    if args.publicar:
        subprocess.run(['gh','release','upload','pack-assets',str(out/asset),str(out/name),'--repo',REPO],check=True)
        assert hashlib.sha1(fetch(url)).hexdigest()==sha
        assert hashlib.sha1(fetch(new['manifest'])).hexdigest()==h
        current=json.loads(fetch(BASE+'/pack-manifest/latest.json?check='+str(time.time_ns())))
        assert current['sha1']==pointer['sha1'],'Another publication changed the pointer; do not overwrite it'
        (out/'latest.json').write_text(json.dumps(new,indent=2),encoding='utf-8')
        subprocess.run(['gh','release','upload','pack-manifest',str(out/'latest.json'),'--repo',REPO,'--clobber'],check=True)
        current=json.loads(fetch(BASE+'/pack-manifest/latest.json?check='+str(time.time_ns())))
        assert current['sha1']==h
        receipt.update(published=True,download_hash_verified=True,pointer_verified=True)
    (out/'receipt.json').write_text(json.dumps(receipt,indent=2),encoding='utf-8')
    print(json.dumps(receipt,indent=2))
if __name__=='__main__':main()
