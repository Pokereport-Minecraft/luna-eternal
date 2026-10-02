"""Node-only, minimal repair of the existing files backup script.

Preserves the old script and makes no changes to worlds or database data.
The regular timer will use the repaired script for its next backup.
"""
from pathlib import Path
import hashlib
import os
import subprocess

SCRIPT = Path('/usr/local/sbin/pokereport-files-backup')
BACKUP = Path('/var/backups/pokereport/maintenance/pokereport-files-backup.before-prelaunch-20261002')
original = SCRIPT.read_bytes()
needle = b'--exclude="*.zip" '
if original.count(needle) != 1:
    raise SystemExit('Expected ZIP exclusion not found exactly once; inspect before changing.')
old_files = b'world config mods server.properties '
if original.count(old_files) != 1:
    raise SystemExit('Expected archive file list not found exactly once.')
extra = (b'world config mods defaultconfigs server.properties eula.txt ops.json whitelist.json '
         b'banned-ips.json banned-players.json usercache.json fabric-server-launch.jar '
         b'fabric-server-launcher.properties minecraft-server.jar server.jar libraries versions ')
updated = original.replace(needle, b'').replace(old_files, extra)
old_auth = b'-C "$TMP_DIR"     EasyAuth\n'
assert updated.count(old_auth) == 1
updated = updated.replace(old_auth,
        b'-C "$TMP_DIR" EasyAuth -C / usr/local/sbin/pokereport-db-backup usr/local/sbin/pokereport-files-backup '
        b'usr/local/sbin/pokereport-offsite-backup usr/local/sbin/pokereport-server-cmd\n')
BACKUP.parent.mkdir(mode=0o700, parents=True, exist_ok=True)
with BACKUP.open('xb') as output:
    output.write(original)
BACKUP.chmod(0o600)
temporary = SCRIPT.with_name(SCRIPT.name + '.prelaunch-new')
with temporary.open('xb') as output:
    output.write(updated)
temporary.chmod(SCRIPT.stat().st_mode & 0o777)
subprocess.run(['bash', '-n', str(temporary)], check=True)
os.replace(temporary, SCRIPT)
print('Backup script repaired; original preserved. ZIP datapacks, launch files and maintenance scripts included.')
print('SHA256 before=' + hashlib.sha256(original).hexdigest())
print('SHA256 after=' + hashlib.sha256(updated).hexdigest())
