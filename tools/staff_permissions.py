"""Manage existing OP/grant files offline. Does not stop/start or deploy servers."""
import argparse
import datetime
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import uuid

EDIT = 'pokereport.world.edit'
MODE = 'pokereport.admin.editmode'
COMMANDS = 'pokereport.admin.commands'
ROLES = {
    'moderator': (3, ()),
    'builder': (4, (EDIT, MODE)),
    'developer': (4, (COMMANDS,)),
    'developer-edit': (4, (EDIT, MODE, COMMANDS)),
    'admin': (4, (EDIT, MODE, COMMANDS)),
    'head-admin': (4, (EDIT, MODE, COMMANDS)),
    'owner': (4, (EDIT, MODE, COMMANDS)),
}

def read_state(root):
    ops_file = root / 'ops.json'
    grants_file = root / 'config/lunaeternal/world-edit.properties'
    ops = json.loads(ops_file.read_text(encoding='utf-8'))
    if not isinstance(ops, list):
        raise ValueError('ops.json must contain a list')
    seen = set()
    for entry in ops:
        identity = str(uuid.UUID(entry['uuid']))
        if identity in seen or type(entry.get('level')) is not int or entry['level'] not in range(1, 5):
            raise ValueError('Duplicate UUID or invalid OP level')
        seen.add(identity)
    grants = {}
    if grants_file.exists():
        for line in grants_file.read_text(encoding='utf-8').splitlines():
            line = line.strip()
            if not line or line.startswith(('#', '!')):
                continue
            # Intentionally reject exotic Properties syntax rather than change its meaning.
            identity, separator, value = line.partition('=')
            identity = str(uuid.UUID(identity.strip()))
            nodes = tuple(x.strip() for x in value.split(','))
            if not separator or identity in grants or len(set(nodes)) != len(nodes) or not set(nodes) <= {EDIT, MODE, COMMANDS}:
                raise ValueError('Invalid grant file; repair before editing')
            grants[identity] = nodes
    return ops, grants

def updated_state(ops, grants, identity, name=None, role=None):
    identity = str(uuid.UUID(identity))
    result_ops = [dict(x) for x in ops if str(uuid.UUID(x['uuid'])) != identity]
    result_grants = dict(grants)
    result_grants.pop(identity, None)
    if role:
        if not re.fullmatch(r'[A-Za-z0-9_]{1,16}', name or ''):
            raise ValueError('Use the confirmed Minecraft username (1–16 letters/digits/underscores)')
        if any(x.get('name', '').lower() == name.lower() for x in result_ops):
            raise ValueError('Username belongs to another OP UUID; resolve identity first')
        level, nodes = ROLES[role]
        result_ops.append({'uuid': identity, 'name': name, 'level': level, 'bypassesPlayerLimit': False})
        if nodes:
            result_grants[identity] = nodes
    return result_ops, result_grants

def verify_stopped_container(container, root):
    info = json.loads(subprocess.check_output(['docker', 'inspect', container], text=True))[0]
    if info['State']['Running'] or info['State'].get('Restarting'):
        raise ValueError('Stop the server cleanly before assigning/revoking. No hot reload exists.')
    if not any(Path(m['Source']).resolve() == root for m in info.get('Mounts', []) if m.get('Source')):
        raise ValueError('Server root must exactly match a mounted directory of this stopped container')

def replace_bytes(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_name(path.name + '.staff-tmp')
    try:
        with temporary.open('xb') as output:
            output.write(value)
            output.flush()
            os.fsync(output.fileno())
        if path.exists():
            shutil.copystat(path, temporary)
            if hasattr(os, 'chown'):
                stat = path.stat()
                os.chown(temporary, stat.st_uid, stat.st_gid)
        else:
            temporary.chmod(0o600)
        temporary.replace(path)
    finally:
        if temporary.exists():
            temporary.unlink()

def save_state(root, ops, grants):
    stamp = datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%dT%H%M%S.%fZ')
    backup = root / '.staff-permission-backups' / stamp
    backup.mkdir(parents=True, mode=0o700)
    files = [root / 'ops.json', root / 'config/lunaeternal/world-edit.properties']
    previous = [p.read_bytes() if p.exists() else None for p in files]
    for i, value in enumerate(previous):
        if value is not None:
            (backup / files[i].name).write_bytes(value)
            (backup / files[i].name).chmod(0o600)
    (backup / 'manifest.json').write_text(json.dumps({'originally_present': [v is not None for v in previous]}, indent=2))
    values = [json.dumps(ops, indent=2).encode('utf-8'),
              ('# Explicit UUID grants; loaded on server startup.\n' +
               ''.join(f'{identity}={",".join(nodes)}\n' for identity, nodes in sorted(grants.items()))).encode('utf-8')]
    try:
        for path, value in zip(files, values):
            replace_bytes(path, value)
    except Exception:
        for path, value in zip(files, previous):
            if value is not None:
                replace_bytes(path, value)
            elif path.exists():
                path.unlink()
        raise
    return backup

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--server-root', required=True, type=Path)
    sub = parser.add_subparsers(dest='action', required=True)
    sub.add_parser('roles')
    sub.add_parser('list')
    assign = sub.add_parser('assign')
    assign.add_argument('--uuid', required=True)
    assign.add_argument('--name', required=True)
    assign.add_argument('--role', required=True, choices=ROLES)
    revoke = sub.add_parser('revoke')
    revoke.add_argument('--uuid', required=True)
    for command in [assign, revoke]:
        command.add_argument('--apply', action='store_true', help='Otherwise preview only')
        command.add_argument('--container', help='Required for apply; must be stopped and mount server-root')
    args = parser.parse_args()
    root = args.server_root.resolve(strict=True)
    if args.action == 'roles':
        print(json.dumps(ROLES, indent=2)); return
    ops, grants = read_state(root)
    if args.action == 'list':
        print(json.dumps({'ops': ops, 'grants': grants}, indent=2)); return
    ops, grants = updated_state(ops, grants, args.uuid, getattr(args, 'name', None), getattr(args, 'role', None))
    print(json.dumps({'uuid': str(uuid.UUID(args.uuid)), 'role': getattr(args, 'role', 'revoked'),
                      'op_level': ROLES[args.role][0] if args.action == 'assign' else 0,
                      'nodes': grants.get(str(uuid.UUID(args.uuid)), ()), 'mode': 'APPLY' if args.apply else 'PREVIEW'}, indent=2))
    if args.apply:
        if not args.container:
            parser.error('--container is required for --apply')
        verify_stopped_container(args.container, root)
        print('Backup:', save_state(root, ops, grants))
        print('Saved. Start the server and verify permissions. No server was started or deployed by this tool.')

if __name__ == '__main__':
    try:
        main()
    except (ValueError, OSError, KeyError, subprocess.SubprocessError) as error:
        raise SystemExit(str(error))
