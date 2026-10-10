"""Preserve every original GUI asset and install the licensed Pokepad font.

Halloween PNG variants are designed separately using the built-in image generator.
Original assets remain byte-identical and November switches them back by calendar.
"""
import hashlib
import json
import shutil
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'mod/src/client/resources/assets/lunaeternal'
BACKUP = ROOT / 'arte/pokepad/normal-20261009'

def main():
    manifest = {}
    for source in sorted((ASSETS / 'textures/gui').rglob('*')):
        if not source.is_file() or '/themes/' in source.as_posix():
            continue
        relative = source.relative_to(ASSETS / 'textures/gui')
        destination = BACKUP / relative
        destination.parent.mkdir(parents=True, exist_ok=True)
        if destination.exists():
            assert destination.read_bytes() == source.read_bytes(), relative
        else:
            shutil.copy2(source, destination)
        manifest[str(relative).replace('\\', '/')] = hashlib.sha256(source.read_bytes()).hexdigest()
    (BACKUP / 'manifest.json').write_text(json.dumps(manifest, indent=2), encoding='utf-8')
    font = ASSETS / 'font'
    font.mkdir(exist_ok=True)
    for name, target in [('AtkinsonHyperlegible-Regular.ttf', 'pokepad.ttf'), ('OFL.txt', 'pokepad-OFL.txt')]:
        path = font / target
        if not path.exists():
            with urllib.request.urlopen('https://raw.githubusercontent.com/google/fonts/main/ofl/atkinsonhyperlegible/' + name) as response:
                path.write_bytes(response.read())
    providers = [{'type': 'ttf', 'file': 'lunaeternal:pokepad.ttf', 'size': 9.0,
                  'oversample': 4.0, 'shift': [0.0, 0.0], 'skip': '\ue000\ue010\ue011\ue012\ue013\ue014\ue015\ue016\ue017\ue018\ue019\ue01a'},
                 {'type': 'reference', 'id': 'minecraft:default'}]
    (font / 'pokepad.json').write_text(json.dumps({'providers': providers}, ensure_ascii=False, indent=2), encoding='utf-8')
    print('Preserved', len(manifest), 'GUI files; installed Atkinson Hyperlegible with OFL license')

if __name__ == '__main__':
    main()
