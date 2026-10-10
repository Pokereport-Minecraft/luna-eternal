"""Verify seasonal coverage, native dimensions and preservation; never changes artwork."""
import hashlib
import json
import struct
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
GUI = ROOT / 'mod/src/client/resources/assets/lunaeternal/textures/gui'
THEME = GUI / 'pokepad/themes/halloween'

def dimensions(path):
    data = path.read_bytes()
    assert data[:8] == b'\x89PNG\r\n\x1a\n', path
    return struct.unpack('>II', data[16:24])

def main():
    preserved = json.loads((ROOT / 'arte/pokepad/normal-20261009/manifest.json').read_text())
    expected = []
    for relative, digest in preserved.items():
        original = GUI / relative
        assert hashlib.sha256(original.read_bytes()).hexdigest() == digest, relative
        if not relative.endswith('.png') or relative == 'tablist_logo.png':
            continue
        target = THEME / relative.removeprefix('pokepad/')
        assert target.is_file(), f'Missing Halloween asset: {relative}'
        assert dimensions(original) == dimensions(target), relative
        assert original.read_bytes() != target.read_bytes(), f'Unchanged seasonal copy: {relative}'
        metadata = original.with_name(original.name + '.mcmeta')
        if metadata.exists():
            assert metadata.read_bytes() == target.with_name(target.name + '.mcmeta').read_bytes(), relative
        expected.append({'original': relative, 'seasonal': target.relative_to(GUI).as_posix(),
                         'generation': 'new functional alternative' if relative in ('pokepad/cartas.png', 'pokepad/cosmeticos.png') else 'edit of original',
                         'size': dimensions(original),
                         'sha256': hashlib.sha256(target.read_bytes()).hexdigest()})
    assert len(expected) == 72, len(expected)
    (ROOT / 'arte/pokepad/halloween-manifest.json').write_text(
        json.dumps({'tool': 'built-in image_gen', 'variants': expected,
                    'preservedOriginalFiles': len(preserved)}, indent=2), encoding='utf-8')
    print(f'PASS: {len(expected)} redesigned PNGs, exact native dimensions, {len(preserved)} originals intact')

if __name__ == '__main__':
    main()
