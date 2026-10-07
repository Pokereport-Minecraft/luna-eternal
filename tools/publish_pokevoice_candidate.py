"""Publish only the own client JAR, preserving the current live pack and its other entries."""
import argparse,hashlib,json,subprocess,time,urllib.request
from pathlib import Path
from datetime import datetime,timezone
ROOT=Path(__file__).resolve().parent.parent
REPO='corderovibes-collab/luna-eternal-pack'
BASE=f'https://github.com/{REPO}/releases/download'
def run(*args):subprocess.run(args,check=True)
def fetch(url):return urllib.request.urlopen(url,timeout=60).read()
def main():
    parser=argparse.ArgumentParser();parser.add_argument('--publicar',action='store_true');args=parser.parse_args()
    out=ROOT/'build'/'pokevoice-publication';out.mkdir(parents=True,exist_ok=True)
    pointer=json.loads(fetch(BASE+'/pack-manifest/latest.json?check='+str(time.time_ns())))
    raw=fetch(pointer['manifest']);assert hashlib.sha1(raw).hexdigest()==pointer['sha1']
    manifest=json.loads(raw);before=json.loads(raw)
    jar=ROOT/'mod'/'build'/'libs'/'lunaeternal-0.1.0.jar';data=jar.read_bytes();sha=hashlib.sha1(data).hexdigest()
    asset='lunaeternal-pokevoice-'+sha[:10]+'.jar';(out/asset).write_bytes(data)
    entry=next(x for x in manifest['files'] if x['path']=='mods/lunaeternal-0.1.0.jar')
    url=BASE+'/pack-assets/'+asset;entry.update(sha1=sha,size=len(data),url=url,urls=[url])
    assert len(manifest['files'])==len(before['files'])
    assert [x for x in manifest['files'] if x['path']!='mods/lunaeternal-0.1.0.jar']==[x for x in before['files'] if x['path']!='mods/lunaeternal-0.1.0.jar']
    encoded=json.dumps(manifest,indent=2,ensure_ascii=False).encode();h=hashlib.sha1(encoded).hexdigest();name='manifest-'+h[:10]+'.json';(out/name).write_bytes(encoded)
    new={**pointer,'manifest':BASE+'/pack-assets/'+name,'sha1':h,'size':len(encoded),'publicado':datetime.now(timezone.utc).strftime('%Y-%m-%dT%H:%M:%SZ')}
    receipt={'previous_pointer':pointer,'new_pointer':new,'asset':asset,'only_entry_changed':'mods/lunaeternal-0.1.0.jar','sha256':hashlib.sha256(data).hexdigest(),'published':False}
    if args.publicar:
        run('gh','release','upload','pack-assets',str(out/asset),str(out/name),'--repo',REPO)
        assert hashlib.sha1(fetch(url)).hexdigest()==sha
        assert hashlib.sha1(fetch(new['manifest'])).hexdigest()==h
        # Ensure another publisher did not change the pointer while assets uploaded.
        current=json.loads(fetch(BASE+'/pack-manifest/latest.json?check='+str(time.time_ns())))
        assert current['sha1']==pointer['sha1'],'Live pointer changed; keep new assets but do not overwrite publication'
        (out/'latest.json').write_text(json.dumps(new,indent=2),encoding='utf-8')
        run('gh','release','upload','pack-manifest',str(out/'latest.json'),'--repo',REPO,'--clobber')
        current=json.loads(fetch(BASE+'/pack-manifest/latest.json?check='+str(time.time_ns())));assert current['sha1']==h
        receipt.update(published=True,download_hash_verified=True,pointer_verified=True)
    (ROOT/'docs'/'pokevoice'/'ui-auth-publication.json').write_text(json.dumps(receipt,indent=2),encoding='utf-8')
    print(json.dumps(receipt,indent=2))
if __name__=='__main__':main()
