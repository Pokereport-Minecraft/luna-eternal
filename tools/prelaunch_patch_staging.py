"""Apply an entry-verified delta to the isolated staging JAR, never the live JAR."""
import hashlib
import json
from pathlib import Path
import zipfile

LIVE = Path('/var/lib/pterodactyl/volumes/1f23bff0-bc4c-4c00-bbcf-2fe81ff666dd/mods/lunaeternal-0.1.0.jar')
STAGE = Path('/srv/pokereport-prelaunch-20261002/mods/lunaeternal-0.1.0.jar')
DELTA = Path('/tmp/pokereport-prelaunch-delta.zip')
with zipfile.ZipFile(DELTA) as patch, zipfile.ZipFile(LIVE) as base:
    manifest = json.loads(patch.read('manifest.json'))
    if hashlib.sha256(LIVE.read_bytes()).hexdigest() != manifest['base_sha256']:
        raise SystemExit('Live baseline changed; inspect before patching staging.')
    temporary = STAGE.with_suffix('.prelaunch-candidate')
    with zipfile.ZipFile(temporary, 'w', compression=zipfile.ZIP_DEFLATED) as result:
        for name, digest in manifest['entries'].items():
            contents = patch.read('entries/' + name) if 'entries/' + name in patch.namelist() else base.read(name)
            if hashlib.sha256(contents).hexdigest() != digest:
                raise SystemExit('Entry mismatch: ' + name)
            result.writestr(name, contents)
    with zipfile.ZipFile(temporary) as result:
        assert result.testzip() is None
    temporary.replace(STAGE)
print('Staging JAR matches every entry in the clean build. Live JAR untouched.')
