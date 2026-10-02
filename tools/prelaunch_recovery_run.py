"""Node-only launcher and console for the isolated staging directory."""
from pathlib import Path
import secrets
import socket
import struct
import subprocess
import sys

STAGE = Path('/srv/pokereport-prelaunch-recovery-20261002')
NAME = 'pokereport-prelaunch-recovery-20261002'

def properties():
    return dict(line.split('=', 1) for line in (STAGE / 'server.properties').read_text().splitlines()
                if '=' in line and not line.startswith('#'))

if sys.argv[1] == 'start':
    props = properties()
    assert props['server-ip'] == '127.0.0.1' and props['server-port'] == '25590'
    props.update({'enable-rcon': 'true', 'rcon.port': '25591', 'rcon.password': secrets.token_hex(24)})
    (STAGE / 'server.properties').write_text('\n'.join(f'{k}={v}' for k, v in props.items()) + '\n')
    subprocess.run(['docker', 'run', '-di', '--name', NAME, '--network', 'host',
                    '--memory=5g', '--cpus=2', '--user=0', '-v', f'{STAGE}:/home/container',
                    '-w', '/home/container', '--entrypoint', 'java',
                    'ghcr.io/pterodactyl/yolks:java_21', '-Xms1G', '-Xmx4G',
                    '-jar', 'fabric-server-launch.jar', 'nogui'], check=True)
else:
    props = properties()
    def read_exact(conn, count):
        result = b''
        while len(result) < count:
            chunk = conn.recv(count - len(result))
            if not chunk:
                raise EOFError('RCON closed')
            result += chunk
        return result
    def request(conn, kind, text, request_id):
        data = struct.pack('<ii', request_id, kind) + text.encode() + b'\x00\x00'
        conn.sendall(struct.pack('<i', len(data)) + data)
        length = struct.unpack('<i', read_exact(conn, 4))[0]
        if length < 10 or length > 16 * 1024 * 1024:
            raise ValueError('Invalid RCON packet')
        return read_exact(conn, length)
    with socket.create_connection(('127.0.0.1', int(props['rcon.port'])), timeout=60) as conn:
        reply = request(conn, 3, props['rcon.password'], 1)
        if struct.unpack('<i', reply[:4])[0] == -1:
            raise SystemExit('RCON authentication failed')
        print(request(conn, 2, ' '.join(sys.argv[1:]), 2)[8:-2].decode(errors='replace'))
