"""Targeted pack publication: preserve all unrelated live entries and rollback pointer swaps."""
import argparse, copy, hashlib, json, subprocess, time, urllib.request
from pathlib import Path
from datetime import datetime, timezone
ROOT = Path(__file__).resolve().parent.parent
REPO = "corderovibes-collab/luna-eternal-pack"
BASE = f"https://github.com/{REPO}/releases/download"
OUT = ROOT / "build/player-defaults-publication"
def fetch(url):
    return urllib.request.urlopen(url, timeout=60).read()
def api(endpoint, *args):
    return json.loads(subprocess.check_output(["gh", "api", endpoint, *args]))
def upload(release, name, source):
    return api(f"https://uploads.github.com/repos/{REPO}/releases/{release}/assets?name={name}",
               "-X", "POST", "-H", "Content-Type: application/octet-stream", "--input", str(source))
def main():
    parser=argparse.ArgumentParser();parser.add_argument("--publicar",action="store_true");args=parser.parse_args()
    OUT.mkdir(parents=True,exist_ok=True)
    pointer=json.loads(fetch(BASE+"/pack-manifest/latest.json?check="+str(time.time_ns())))
    raw=fetch(pointer["manifest"]);assert hashlib.sha1(raw).hexdigest()==pointer["sha1"]
    before=json.loads(raw);manifest=copy.deepcopy(before)
    retired=[x for x in manifest["files"] if x["path"].startswith("mods/musicnotification-")]
    assert len(retired)==1, "Expected exactly one MusicNotification client mod"
    manifest["files"]=[x for x in manifest["files"] if x not in retired]
    assets=[]
    def asset(data,name):
        path=OUT/name;path.write_bytes(data);assets.append(path)
        return BASE+"/pack-assets/"+name,hashlib.sha1(data).hexdigest()
    jar=(ROOT/"mod/build/libs/lunaeternal-0.1.0.jar").read_bytes()
    url,sha=asset(jar,"lunaeternal-player-defaults-"+hashlib.sha1(jar).hexdigest()[:10]+".jar")
    own=next(x for x in manifest["files"] if x["path"]=="mods/lunaeternal-0.1.0.jar")
    own.update(url=url,urls=[url],sha1=sha,size=len(jar))
    for name in ("options.txt","defaultoptions.journal.json"):
        assert not any(x["path"]==name for x in manifest["files"])
        data=(ROOT/"launcher-defaults"/name).read_bytes()
        url,sha=asset(data,"player-defaults-"+hashlib.sha1(data).hexdigest()[:10]+"-"+name)
        manifest["files"].append(dict(path=name,url=url,urls=[url],sha1=sha,size=len(data),once=True))
    changed={"mods/lunaeternal-0.1.0.jar",retired[0]["path"],"options.txt","defaultoptions.journal.json"}
    assert [x for x in before["files"] if x["path"] not in changed]==[x for x in manifest["files"] if x["path"] not in changed]
    encoded=json.dumps(manifest,indent=2,ensure_ascii=False).encode();h=hashlib.sha1(encoded).hexdigest()
    manifest_url,_=asset(encoded,"manifest-"+h[:10]+".json")
    new={**pointer,"manifest":manifest_url,"sha1":h,"size":len(encoded),"publicado":datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")}
    receipt=dict(previous_pointer=pointer,new_pointer=new,removed=retired[0]["path"],initial_settings_only=True,unrelated_entries_preserved=True,jar_sha256=hashlib.sha256(jar).hexdigest(),published=False)
    if args.publicar:
        # Known release IDs avoid cached failures on GitHub's tag lookup endpoints.
        for path in assets:
            upload(371810543,path.name,path)
            assert hashlib.sha1(fetch(BASE+"/pack-assets/"+path.name)).hexdigest()==hashlib.sha1(path.read_bytes()).hexdigest()
        current=json.loads(fetch(BASE+"/pack-manifest/latest.json?check="+str(time.time_ns())))
        assert current["sha1"]==pointer["sha1"], "Concurrent publication; pointer left untouched"
        old=next(x for x in api(f"repos/{REPO}/releases/372046779/assets?per_page=100&check={time.time_ns()}") if x["name"]=="latest.json")
        path=OUT/"latest.json";path.write_text(json.dumps(new,indent=2),encoding="utf-8")
        candidate=upload(372046779,"latest-candidate-"+h[:10]+".json",path)
        assert json.loads(fetch(candidate["browser_download_url"]))["sha1"]==h
        endpoint=f"repos/{REPO}/releases/assets/"
        api(endpoint+str(old["id"]),"-X","PATCH","-f","name=latest-before-player-defaults-"+pointer["sha1"][:10]+".json")
        try:
            api(endpoint+str(candidate["id"]),"-X","PATCH","-f","name=latest.json")
        except Exception:
            api(endpoint+str(old["id"]),"-X","PATCH","-f","name=latest.json");raise
        assert json.loads(fetch(BASE+"/pack-manifest/latest.json?check="+str(time.time_ns())))["sha1"]==h
        receipt.update(published=True,verified_downloads=True,pointer_asset_id=candidate["id"])
    (ROOT/"docs/player-defaults-publication.json").write_text(json.dumps(receipt,indent=2),encoding="utf-8")
    print(json.dumps(receipt,indent=2))
if __name__=="__main__": main()
