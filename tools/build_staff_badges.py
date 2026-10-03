"""Create original staff UI sprites following the existing 28px metal badge system."""
from pathlib import Path
import json
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
RESOURCES = ROOT / 'mod/src/client/resources'
FONT = ImageFont.truetype('C:/Windows/Fonts/ariblk.ttf', 17)
BADGES = [('owner', 'OWNER', 150, (145, 7, 35), '\ue018'),
          ('head_admin', 'HEAD ADMIN', 196, (145, 7, 35), '\ue019'),
          ('builder', 'BUILDER', 174, (163, 98, 10), '\ue01a')]

def badge(name, title, width, color):
    im = Image.new('RGBA', (width, 28)); d = ImageDraw.Draw(im)
    # Exact grid guide: 28px high, 2px steel bevel, clipped corners, dark inset.
    outline = [(0,7),(7,0),(width-8,0),(width-1,7),(width-1,20),(width-8,27),(7,27),(0,20)]
    d.polygon(outline, fill=(31,37,43));d.line(outline+[outline[0]], fill=(13,18,22), width=1)
    d.line([(2,7),(8,2),(width-9,2),(width-3,7)],fill=(130,144,148),width=2)
    d.line([(2,8),(2,19),(8,25),(width-9,25),(width-3,19),(width-3,8)],fill=(67,83,91),width=2)
    d.polygon([(5,8),(10,5),(width-11,5),(width-6,8),(width-6,19),(width-11,23),(10,23),(5,19)],fill=(*color,255))
    for y in range(6,23):
        factor=1.18-(y-6)*0.024
        shade=tuple(max(0,min(255,int(v*factor))) for v in color)
        d.line([(10,y),(width-11,y)],fill=shade)
    d.line([(11,5),(width-12,5)],fill=tuple(min(255,int(v*1.6)+15) for v in color))
    d.line([(10,22),(width-11,22)],fill=tuple(int(v*.55) for v in color))
    # Steel rivets, restrained pattern matching the existing right-hand ornament.
    for x,y in [(6,6),(width-7,6),(6,21),(width-7,21)]:
        d.rectangle((x-1,y-1,x+1,y+1),fill=(125,142,148));d.point((x-1,y-1),fill=(217,226,222));d.point((x+1,y+1),fill=(26,35,41))
    for x,y in [(width-21,11),(width-18,14),(width-21,17)]:d.rectangle((x,y,x+2,y+2),fill=tuple(min(255,v+20) for v in color))
    if name=='owner':
        d.polygon([(12,10),(16,13),(21,7),(26,13),(30,10),(28,20),(14,20)],fill=(15,18,22))
        d.polygon([(14,11),(17,14),(21,9),(25,14),(28,11),(26,18),(16,18)],fill=(248,193,59))
        d.line([(16,15),(21,11),(25,15)],fill=(255,237,160));d.rectangle((16,19,26,20),fill=(166,103,20))
        d.rectangle((20,15,22,17),fill=(196,33,52))
    elif name=='head_admin':
        d.polygon([(12,8),(22,5),(32,8),(30,18),(22,23),(14,18)],fill=(12,19,22))
        d.polygon([(15,9),(22,7),(29,9),(27,17),(22,20),(17,17)],fill=(187,201,208))
        d.polygon([(22,9),(24,12),(27,12),(25,15),(26,18),(22,16),(18,18),(19,15),(17,12),(20,12)],fill=(250,193,51))
        d.line([(15,9),(22,7),(29,9)],fill=(248,251,246))
    else:
        d.line([(16,21),(27,10)],fill=(14,18,22),width=7)
        d.line([(17,20),(26,11)],fill=(196,128,50),width=3)
        d.polygon([(16,7),(21,5),(31,14),(27,18),(18,10)],fill=(16,22,24))
        d.polygon([(18,7),(21,7),(29,14),(27,16),(19,9)],fill=(179,195,198))
        d.line([(19,7),(27,14)],fill=(240,249,242),width=1)
    box=d.textbbox((0,0),title,font=FONT);text_width=box[2]-box[0]
    x=45+(width-56-text_width)//2;y=(28-(box[3]-box[1]))//2-box[1]-1
    d.text((x+1,y+2),title,font=FONT,fill=(10,15,19),stroke_width=2,stroke_fill=(10,15,19))
    d.text((x,y),title,font=FONT,fill=(249,251,248),stroke_width=1,stroke_fill=(20,26,29))
    return im

for name,title,width,color,glyph in BADGES:
    im=badge(name,title,width,color)
    for prefix in [RESOURCES, RESOURCES/'resourcepacks/recursos']:
        path=prefix/f'assets/lunaeternal/textures/font/ranks/badge_{name}.png';im.save(path)
for prefix in [RESOURCES, RESOURCES/'resourcepacks/recursos']:
    for rel in ['assets/minecraft/font/default.json','assets/lunaeternal/font/default.json','assets/lunaeternal/font/iconos.json']:
        path=prefix/rel;data=json.loads(path.read_text(encoding='utf-8'));providers=data['providers']
        for name,title,width,color,glyph in BADGES:
            entry=dict(type='bitmap',file=f'lunaeternal:font/ranks/badge_{name}.png',height=10,ascent=8,chars=[glyph])
            providers[:]=[p for p in providers if p.get('file')!=entry['file']];providers.append(entry)
        path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
preview=Image.new('RGBA',(824,440),(25,25,30,255));d=ImageDraw.Draw(preview)
for i,(name,title,width,color,glyph) in enumerate(BADGES):
    im=badge(name,title,width,color).resize((width*4,112),Image.Resampling.NEAREST);preview.paste(im,(16,16+i*140),im)
(ROOT/'build').mkdir(exist_ok=True);preview.save(ROOT/'build/staff-badges-preview.png')
print('Created 3 staff badges and registered E018–E01A in both resource layers.')
