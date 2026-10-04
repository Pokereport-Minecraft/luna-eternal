"""Apply this hotfix to the verified live JAR, preserving unrelated entries."""
import copy
import hashlib
import json
import sys
import zipfile
from pathlib import Path

base, compiled, output = map(Path, sys.argv[1:4])
roots = {
    'net/pokereport/luna/client/DialogoFoto',
    'net/pokereport/luna/client/TexturasFoto',
    'net/pokereport/luna/quest/QuestService',
    'net/pokereport/luna/command/LunaCommand',
    'net/pokereport/luna/net/Red',
    'net/pokereport/luna/test/AutoTest',
}
if '--client-only' in sys.argv[4:]:
    roots = {'net/pokereport/luna/client/DialogoFoto', 'net/pokereport/luna/client/TexturasFoto'}
def selected(name):
    return any(name == root + '.class' or
               (name.startswith(root + '$') and name.endswith('.class')) for root in roots)

with zipfile.ZipFile(base) as old, zipfile.ZipFile(compiled) as new:
    replacements = {n: new.read(n) for n in new.namelist() if selected(n)}
    assert all(root + '.class' in replacements for root in roots)
    for language in ('es_es', 'es_mx', 'en_us'):
        name = 'assets/lunaeternal/lang/' + language + '.json'
        if name not in old.namelist():
            continue
        before = json.loads(old.read(name))
        key = 'pokepad.lunaeternal.mision.error_cobro'
        source = 'es_es' if language == 'es_mx' else language
        before[key] = json.loads(new.read('assets/lunaeternal/lang/' + source + '.json'))[key]
        replacements[name] = (json.dumps(before, ensure_ascii=False, indent=2) + '\n').encode()
    output.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(output, 'w', zipfile.ZIP_DEFLATED) as result:
        for info in old.infolist():
            if not selected(info.filename):
                result.writestr(copy.copy(info), replacements.pop(info.filename, old.read(info.filename)))
        for name, data in replacements.items():
            result.writestr(name, data)
    with zipfile.ZipFile(output) as result:
        assert result.testzip() is None
        changed = [n for n in old.namelist() if n not in result.namelist() or old.read(n) != result.read(n)]
        assert all(selected(n) or n.startswith('assets/lunaeternal/lang/') for n in changed)
        for name in old.namelist():
            if not selected(name) and not name.startswith('assets/lunaeternal/lang/'):
                assert old.read(name) == result.read(name), name
            elif name.startswith('assets/lunaeternal/lang/') and name.endswith('.json'):
                before, after = json.loads(old.read(name)), json.loads(result.read(name))
                key = 'pokepad.lunaeternal.mision.error_cobro'
                assert {k:v for k,v in before.items() if k != key} == {k:v for k,v in after.items() if k != key}
receipt = {'base_sha256': hashlib.sha256(base.read_bytes()).hexdigest(),
           'sha256': hashlib.sha256(output.read_bytes()).hexdigest(), 'changed': changed,
           'unrelated_entries_preserved': True}
output.with_suffix('.audit.json').write_text(json.dumps(receipt, indent=2))
print(json.dumps(receipt, indent=2))
