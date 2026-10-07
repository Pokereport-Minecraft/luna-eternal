"""Node-only recovery drill; modifies only a named isolated schema."""
import subprocess
import hashlib
import json

SCHEMA = 'prelaunch_recovery_20261002'
SECOND = 'prelaunch_recovery_verify_20261002'
DUMP = '/var/backups/pokereport/database/s11945_luna_2026-10-02_040000.sql.gz'


def sql(database, query):
    return subprocess.check_output(['mariadb', '-N', '-B', database, '-e', query], text=True)


if sql('information_schema', f"SELECT COUNT(*) FROM SCHEMATA WHERE SCHEMA_NAME='{SECOND}'").strip() != '0':
    raise SystemExit('Verification schema already exists; inspect instead of overwriting it.')
query = 'SELECT player_id,currency,balance FROM player_economy ORDER BY player_id,currency'
original = sql(SCHEMA, query)
player, currency, balance = original.splitlines()[0].split('\t')
assert currency in ('POKEDOLLAR', 'REPORTCOIN')
try:
    sql(SCHEMA, f"UPDATE player_economy SET balance=balance+1 WHERE player_id={int(player)} AND currency='{currency}'")
    assert sql(SCHEMA, query) != original
    subprocess.run(['mariadb', '-e', f'CREATE DATABASE `{SECOND}`'], check=True)
    with subprocess.Popen(['gzip', '-dc', DUMP], stdout=subprocess.PIPE) as dump:
        subprocess.run(['mariadb', SECOND], stdin=dump.stdout, check=True)
        dump.stdout.close()
        assert dump.wait() == 0
    assert sql(SECOND, query) == original
finally:
    sql(SCHEMA, f"UPDATE player_economy SET balance={int(balance)} WHERE player_id={int(player)} AND currency='{currency}'")
assert sql(SCHEMA, query) == original
print(json.dumps({'database_alter_and_restore': True,
                  'wallet_snapshot_sha256': hashlib.sha256(original.encode()).hexdigest(),
                  'schema_version': sql(SCHEMA, 'SELECT MAX(version) FROM schema_version').strip(),
                  'main_database_modified': False}))
