"""Patch only fauna policy classes onto the verified deployed server artifact."""
import pathlib,zipfile,copy,hashlib,json,sys
base,compiled,out=map(pathlib.Path,sys.argv[1:4])
roots=['net/pokereport/luna/world/PoliticaFauna','net/pokereport/luna/world/FaunaControl']
def chosen(n):return any(n==r+'.class' or n.startswith(r+'$') and n.endswith('.class') for r in roots)
with zipfile.ZipFile(base) as a,zipfile.ZipFile(compiled) as b:
 replacements={n:b.read(n) for n in b.namelist() if chosen(n)}
 assert all(r+'.class' in replacements for r in roots)
 with zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED) as z:
  for i in a.infolist():
   if not chosen(i.filename):z.writestr(copy.copy(i),a.read(i.filename))
  for n,data in replacements.items():z.writestr(n,data)
 with zipfile.ZipFile(out) as z:
  assert z.testzip() is None
  assert set(a.namelist())==set(z.namelist())
  changed=[n for n in a.namelist() if a.read(n)!=z.read(n)]
  assert all(chosen(n) for n in changed)
receipt={'base_sha256':hashlib.sha256(base.read_bytes()).hexdigest(),'sha256':hashlib.sha256(out.read_bytes()).hexdigest(),'changed':changed,'unrelated_entries_preserved':True}
out.with_suffix('.audit.json').write_text(json.dumps(receipt,indent=2));print(json.dumps(receipt,indent=2))
