"""Read-only preflight: reject duplicate Fabric mod IDs before a server starts.

Backups ending in .jar belong outside mods: Fabric can select an older candidate
with the same id/version without reporting two active mods in its startup list.
"""
import argparse
import hashlib
import json
from pathlib import Path
import sys
import zipfile


def audit(directory: Path) -> dict:
    candidates = {}
    invalid = []
    for path in sorted(directory.glob('*.jar')):
        try:
            with zipfile.ZipFile(path) as archive:
                if 'fabric.mod.json' not in archive.namelist():
                    continue
                metadata = json.loads(archive.read('fabric.mod.json'))
                candidates.setdefault(metadata['id'], []).append({
                    'file': path.name, 'version': metadata['version'],
                    'sha256': hashlib.sha256(path.read_bytes()).hexdigest(),
                })
        except (zipfile.BadZipFile, json.JSONDecodeError, KeyError) as failure:
            invalid.append({'file': path.name, 'error': str(failure)})
    return {'directory': str(directory.resolve()),
            'duplicates': {name: jars for name, jars in candidates.items() if len(jars) > 1},
            'invalid': invalid, 'fabricCandidates': sum(map(len, candidates.values()))}


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('mods', type=Path)
    args = parser.parse_args()
    if not args.mods.is_dir():
        parser.error('mods must be an existing directory')
    report = audit(args.mods)
    print(json.dumps(report, indent=2, ensure_ascii=False))
    sys.exit(1 if report['duplicates'] or report['invalid'] else 0)
