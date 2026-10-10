"""Deterministic cards from existing PokeReport art; no browser or avatar cache."""
from io import BytesIO
from pathlib import Path
import unicodedata
from PIL import Image, ImageDraw, ImageFont, ImageOps

ASSETS = Path(__file__).parent / 'assets'
GOLD = '#ffbd39'
SIZE = (1200, 640)
Image.MAX_IMAGE_PIXELS = 16_000_000


def clean(text, limit=240):
    value = unicodedata.normalize('NFC', str(text))
    # Keep accents and scripts; unsupported decorative emoji use a plain star.
    value = ''.join('✦' if unicodedata.category(c) == 'So' and ord(c) > 0xFFFF else c
                    for c in value if unicodedata.category(c) not in ('Cc', 'Cf') and c not in '\ufe0e\ufe0f')
    return ' '.join(value.split())[:limit] or 'Entrenador'


def font(size, bold=False):
    return ImageFont.truetype(str(ASSETS / ('DejaVuSans-Bold.ttf' if bold else 'DejaVuSans.ttf')), size)


def fit(draw, text, max_width, size=46, minimum=28):
    for n in range(size, minimum-1, -1):
        f = font(n, True)
        if draw.textlength(text, font=f) <= max_width:
            return text, f
    f = font(minimum, True)
    while text and draw.textlength(text+'…', font=f) > max_width:
        text = text[:-1]
    return text+'…', f


def wrap(draw, text, width, f, lines=3):
    words, result, line = clean(text, 1200).split(), [], ''
    for word in words:
        candidate = (line+' '+word).strip()
        if draw.textlength(candidate, font=f) > width and line:
            result.append(line)
            line = word
        else:
            line = candidate
    if line:
        result.append(line)
    if len(result) > lines:
        result = result[:lines]
        result[-1] = result[-1].rstrip()+'…'
    # A long unbroken word must not exceed the card even at a fixed font size.
    for i, line in enumerate(result):
        while draw.textlength(line, font=f) > width:
            line = line[:-2]+'…'
        result[i] = line
    return '\n'.join(result)


def avatar_image(data, size=218):
    try:
        if not data or len(data) > 4_000_000:
            raise ValueError('No bounded avatar')
        with Image.open(BytesIO(data)) as original:
            if original.width*original.height > 4_000_000:
                raise ValueError('Avatar exceeds bounds')
            avatar = ImageOps.fit(original.convert('RGB'), (size, size), method=Image.Resampling.LANCZOS)
    except (OSError, ValueError, Image.DecompressionBombError):
        avatar = Image.new('RGB', (size, size), '#1b2332')
        d = ImageDraw.Draw(avatar)
        d.ellipse((76, 42, 142, 108), fill=GOLD)
        d.rounded_rectangle((44, 120, 174, 198), radius=44, fill=GOLD)
    mask = Image.new('L', (size, size))
    ImageDraw.Draw(mask).ellipse((0, 0, size-1, size-1), fill=255)
    return avatar, mask


def card(kind, name, avatar=None, title=None, summary=None, category=None, date=None):
    with Image.open(ASSETS/'ciudadela.webp') as original:
        image = ImageOps.fit(original.convert('RGB'), SIZE, method=Image.Resampling.LANCZOS).convert('RGBA')
    # Preserve the city on the right, keep the entire text area readable.
    overlay = Image.new('RGBA', SIZE)
    d = ImageDraw.Draw(overlay)
    for x in range(SIZE[0]):
        alpha = int(224-80*x/SIZE[0])
        d.line((x,0,x,SIZE[1]), fill=(7,12,23,alpha))
    image = Image.alpha_composite(image, overlay)
    d = ImageDraw.Draw(image)
    d.rounded_rectangle((22,22,1178,618), radius=22, outline=GOLD, width=2)
    d.rectangle((50,58,56,132), fill=GOLD)
    d.text((74,58), 'POKEREPORT NETWORK', font=font(25, True), fill='white')
    d.text((74,100), 'TU COMUNIDAD · TU AVENTURA', font=font(17), fill='#c5cad6')
    with Image.open(ASSETS/'logo.webp') as original:
        logo = original.convert('RGBA')
        logo.thumbnail((310, 90), Image.Resampling.LANCZOS)
        image.alpha_composite(logo, (835,53))
    picture, mask = avatar_image(avatar)
    d.ellipse((67,191,301,425), fill=GOLD)
    image.paste(picture, (75,199), mask)
    heading = 'BIENVENIDO A LA AVENTURA' if kind == 'welcome' else 'MEJORA DESTACADA'
    d.text((340,184), heading, font=font(19, True), fill=GOLD)
    display, f = fit(d, clean(name, 100), 785, size=48)
    d.text((340,226), display, font=f, fill='white')
    if kind == 'welcome':
        d.text((340,302), 'Tu aventura comienza aquí.', font=font(29, True), fill='white')
        d.multiline_text((340,355), 'Explora, construye y encuentra tu equipo.\nNos alegra tenerte en PokeReport.',
                         font=font(21), fill='#d0d5df', spacing=12)
        footer = 'LEE LAS NORMAS  ·  ELIGE TUS INTERESES  ·  EMPIEZA A JUGAR'
    else:
        label, f = fit(d, clean(title, 120), 785, size=29, minimum=24)
        d.text((340,296), label, font=f, fill='white')
        d.multiline_text((340,344), wrap(d, summary, 770, font(21), 3), font=font(21),
                         fill='#d0d5df', spacing=9)
        d.text((340,459), clean(category, 60).upper()+'  ·  '+str(date or ''), font=font(17), fill=GOLD)
        footer = 'GRACIAS POR AYUDARNOS A CONSTRUIR UNA COMUNIDAD MEJOR'
    d.line((70,525,1130,525), fill=(255,189,57,110), width=1)
    d.text((70,560), footer, font=font(17, True), fill='#ece6d5')
    buffer = BytesIO()
    image.convert('RGB').save(buffer, 'PNG', optimize=True)
    return buffer.getvalue()
