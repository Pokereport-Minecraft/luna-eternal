"""Node-only recovery of the five original Oak decorations from fauna NBT backups.

Preserves UUID, position and Pokemon data. Only summons absent original UUIDs.
Run after confirming the laboratory chunks are loaded and taking a world backup.
"""
import gzip, io, json, struct, subprocess, sys
from pathlib import Path

FILES = {
    '268bbe3f-a87c-4ebe-9093-33a286dd88e9': 'charmander',
    'c213554e-d17f-483b-bb04-71416935949a': 'squirtle',
    '7abf428c-c3aa-4e27-8787-04fb05a455bd': 'bulbasaur',
    '11933479-dd23-41a0-8cd1-c74786b1bad4': 'omastar',
    '18f146ac-0a3a-4a2b-a7e3-eae5cdbc7afc': 'kabutops',
}

def decode(data):
    stream = io.BytesIO(data)
    def number(fmt):
        return struct.unpack('>' + fmt, stream.read(struct.calcsize('>' + fmt)))[0]
    def string():
        return stream.read(number('H')).decode('utf-8')
    def read(kind):
        if kind in range(1, 7):
            value = number({1:'b', 2:'h', 3:'i', 4:'q', 5:'f', 6:'d'}[kind])
            return value, str(value) + {1:'b', 2:'s', 3:'', 4:'L', 5:'f', 6:'d'}[kind]
        if kind == 8:
            value = string()
            return value, json.dumps(value, ensure_ascii=False)
        if kind in (7, 11, 12):
            values = [number({7:'b', 11:'i', 12:'q'}[kind]) for _ in range(number('i'))]
            prefix, suffix = {7:('B','b'), 11:('I',''), 12:('L','L')}[kind]
            return values, '[' + prefix + ';' + ','.join(str(v) + suffix for v in values) + ']'
        if kind == 9:
            entry_kind, count = number('b'), number('i')
            values = [read(entry_kind) for _ in range(count)]
            return [v[0] for v in values], '[' + ','.join(v[1] for v in values) + ']'
        if kind == 10:
            values, fields = {}, []
            while True:
                entry_kind = number('b')
                if entry_kind == 0:
                    return values, '{' + ','.join(fields) + '}'
                key = string()
                value, snbt = read(entry_kind)
                values[key] = value
                fields.append(json.dumps(key) + ':' + snbt)
        raise ValueError(kind)
    kind = number('b')
    string()
    return read(kind)

def main():
    root, evidence = map(Path, sys.argv[1:3])
    evidence.mkdir(parents=True, exist_ok=False)
    records = []
    for uid, species in FILES.items():
        source = root / (uid + '.nbt')
        raw = source.read_bytes()
        (evidence / source.name).write_bytes(raw)
        data, _ = decode(gzip.decompress(raw))
        entity = data['entity']
        assert data['dimension'] == 'lunaeternal:ciudadela'
        assert entity['Pokemon']['Species'] == 'cobblemon:' + species
        assert 'luna_decorativo' in entity['Tags'] and entity['PersistenceRequired'] == 1
        # Parse the entity separately without changing any NBT type.
        original = gzip.decompress(raw)
        # The compound decoder also provides its typed SNBT; use the matching entity field.
        _, document = decode(original)
        marker = '"entity":'
        start = document.index(marker) + len(marker)
        decoder = json.JSONDecoder()  # Only locate the compound via balanced quoted text.
        depth, quoted, escaped, end = 0, False, False, start
        for end in range(start, len(document)):
            char = document[end]
            if quoted:
                if escaped: escaped = False
                elif char == '\\': escaped = True
                elif char == '"': quoted = False
            elif char == '"': quoted = True
            elif char == '{': depth += 1
            elif char == '}':
                depth -= 1
                if depth == 0: break
        snbt = document[start:end+1]
        pos = entity['Pos']
        command = f'execute in lunaeternal:ciudadela unless entity {uid} run summon cobblemon:pokemon {pos[0]} {pos[1]} {pos[2]} {snbt}'
        subprocess.run(['/usr/local/sbin/pokereport-server-cmd', command], check=True)
        records.append(dict(uuid=uid, species=species, pos=pos, rotation=entity['Rotation']))
    (evidence / 'requested-restoration.json').write_text(json.dumps(records, indent=2))
    print(json.dumps(records, indent=2))

if __name__ == '__main__':
    main()
