"""Run on the node: restore an isolated, loopback-only prelaunch QA server.

Never modifies the main volume or its database. Credentials are generated on
the node and never printed. Existing staging directories are not overwritten.
"""
from pathlib import Path
import secrets
import shutil
import subprocess

LIVE = Path('/var/lib/pterodactyl/volumes/1f23bff0-bc4c-4c00-bbcf-2fe81ff666dd')
STAGE = Path('/srv/pokereport-prelaunch-recovery-20261002')
SCHEMA = 'prelaunch_recovery_20261002'
FILES = Path('/var/backups/pokereport/files/cobblemon_2026-10-02_152009.tar.zst')
DATABASE = Path('/var/backups/pokereport/database/s11945_luna_2026-10-02_040000.sql.gz')

def run(*args, **kwargs):
    return subprocess.run(args, check=True, **kwargs)

if STAGE.exists():
    raise SystemExit('Staging already exists; inspect it instead of overwriting it.')
if not FILES.is_file() or not DATABASE.is_file():
    raise SystemExit('The expected backups are missing.')
run('sha256sum', '-c', str(DATABASE) + '.sha256')
run('sha256sum', '-c', str(FILES) + '.sha256')
STAGE.mkdir(mode=0o700, parents=True)
print('Restoring files backup into isolated directory...', flush=True)
run('tar', '--zstd', '-xf', str(FILES), '-C', str(STAGE))
# Corrected archive must restore without live supplements.
required = ['fabric-server-launch.jar', 'libraries', 'world/datapacks', 'ops.json']
for name in required:
    if not (STAGE / name).exists(): raise SystemExit('Corrected archive incomplete: ' + name)

# The restored Tebex connector carries the live store credentials and starts
# polling on boot. Quarantine only that external connector in this QA copy.
for connector in (STAGE / 'mods').glob('tebex-*.jar'):
    connector.rename(connector.with_suffix(connector.suffix + '.disabled'))

# A distinct account has privileges only on the staging schema.
password = secrets.token_hex(24)
sql = (f"CREATE DATABASE `{SCHEMA}`;"
       f"CREATE USER 'prelaunch_recovery_qa'@'localhost' IDENTIFIED BY '{password}';"
       f"GRANT ALL PRIVILEGES ON `{SCHEMA}`.* TO 'prelaunch_recovery_qa'@'localhost';")
run('mariadb', input=sql.encode())
with subprocess.Popen(['gzip', '-dc', str(DATABASE)], stdout=subprocess.PIPE) as dump:
    run('mariadb', SCHEMA, stdin=dump.stdout)
    dump.stdout.close()
    if dump.wait() != 0:
        raise SystemExit('Database restore failed.')

config = STAGE / 'config/lunaeternal.properties'
properties = {}
for line in config.read_text().splitlines():
    if '=' in line and not line.strip().startswith('#'):
        key, value = line.split('=', 1)
        properties[key.strip()] = value.strip()
properties.update({'db.host': '127.0.0.1', 'db.port': '3306', 'db.name': SCHEMA,
                   'db.user': 'prelaunch_recovery_qa', 'db.password': password,
                   'builder.key': '', 'db.failFast': 'true'})
config.write_text('\n'.join(f'{key}={value}' for key, value in properties.items()) + '\n')
config.chmod(0o600)
properties = {}
for line in (STAGE / 'server.properties').read_text().splitlines():
    if '=' in line and not line.strip().startswith('#'):
        key, value = line.split('=', 1)
        properties[key] = value
properties.update({'server-ip': '127.0.0.1', 'server-port': '25590',
                   'enable-rcon': 'false', 'enable-query': 'false',
                   'white-list': 'true', 'max-players': '5', 'view-distance': '4',
                   'simulation-distance': '4', 'motd': 'ISOLATED PRELAUNCH QA'})
(STAGE / 'server.properties').write_text('\n'.join(f'{key}={value}' for key, value in properties.items()) + '\n')
print('Staging prepared. Database isolated; server binds only to localhost.', flush=True)
