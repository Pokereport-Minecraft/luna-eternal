# Poképad Halloween — colección PNG completa

## Alcance y conservación

El tema renueva todo el Poképad: 52 PNG de `textures/gui/pokepad`, siete destinos de viajes, cuatro imágenes de Santuario, cuatro de tesoros y cinco medallas propias. Son 70 ediciones de originales y dos iconos nuevos para Cartas/Cosméticos, cuyas ediciones fueron rechazadas por el generador sin motivo concreto. El logo del tablist no pertenece al Poképad y se conserva. Las texturas de Pokémon, skins de jugadores y objetos de mods externos conservan su identidad.

Los 125 archivos originales de GUI están copiados en `arte/pokepad/normal-20261009`, con manifiesto SHA256. Los archivos originales de recursos también se conservan sin modificaciones. Las 72 variantes nuevas se empaquetan en `textures/gui/pokepad/themes/halloween`, incluyendo los subdirectorios de viajes, Santuario, tesoros y medallas. `arte/pokepad/halloween-manifest.json` documenta tamaño, correspondencia y hash de cada variante.

`PadTheme.resolve` selecciona los PNG nuevos en octubre y los originales en noviembre según `America/Bogota`. La misma selección se usa para el filtrado de texturas. No se pinta una decoración encima de los fondos originales ni se tapa el dibujo generado con un rectángulo. Coordenadas interactivas, protocolo y IDs de aplicaciones permanecen estables.

## Diseño y prompts

Herramienta: **image_gen integrada**, una petición por recurso. Se inspeccionaron los originales antes de usarlos como objetivos de edición. Los dos chasis principales se generaron primero y se usaron como referencia de paleta para imágenes grandes. Los símbolos pequeños se solicitaron individualmente, sin referencia del chasis, cuando la primera variante producía una lámina de iconos. Las láminas se descartaron.

Prompt de los fondos principales:

> Edit the entire hardware chassis into a Halloween midnight plum/charcoal metal, amber trim and violet insets theme. Preserve the layout, aspect ratio, transparency, silhouette, panels, cutouts and exact POKEREPORT NETWORK logo. Add pumpkins, bats, cobwebs and a witch hat to Rotom only in the decorative areas. Keep the main content panel empty and light warm ivory, with an empty dark plum sidebar. No invented buttons, text or content. Main content rectangle x460 y204 w801 h494 in the original 1380x828 design. Preserve the avatar pocket in pokepad.png; no avatar pocket in pokepad_cosmeticos.png.

Prompt de aplicaciones y botones:

> First image is the original edit target; second supplies palette/style only. Produce ONE standalone Halloween replacement asset. Retain the recognizable original Pokemon/object and its function, framing, aspect ratio, transparency and facing direction. Midnight plum, warm amber and violet accents with restrained pumpkins, bats or cobwebs. Clean polished game illustration with moderately bold readable contours. Buttons must retain a large readable arrow, gear, X or plus. No labels, text, grid, asset sheet or chassis.

Variaciones por función: monedas Luna conservan C; Plata sigue siendo plateada; macho/hembra conservan sus símbolos; sobres diario/luna/plata se distinguen por violeta/oro/plata; cofres legendario/shiny se distinguen por violeta/oro. Viajes y escenas de Torre conservan composición y personajes, cambiando iluminación y decoración estacional. Santuario mantiene un ambiente respetuoso, tranquilo y sin horror gráfico.

Prompt corregido de estrellas:

> Create ONLY ONE isolated five-point HOLLOW/EMPTY outlined star with completely transparent center [or SOLID/FILLED star with gold center]. A single centered large star occupying 90% of a square transparent canvas. Halloween Pokepad palette: luminous amber/gold with a restrained plum edge. Extremely simple bold legible game UI symbol designed to downsample to14x14px. No box/tile/frame, no pumpkins, no bats, no other icons, no sheets, no rows or grid. ONE STAR ONLY.

Prompt corregido de indicadores pequeños:

> Edit the single tiny reference icon into a Halloween game UI variant: ONE BLUE SHIELD with centered Pokeball and amber edge [division: ONE GOLD TROPHY CUP; work: ONE ORANGE CONSTRUCTION HARD HAT]. ONLY THIS ONE OBJECT, isolated and centered occupying90% of a square transparent canvas. Retain original meaning and silhouette. Dark plum shadow and violet accents with warm amber edging. Strong simple moderately bold readable illustration for36x36 pixels. No background tile, frame, logo, chassis, text, other icons, asset sheet or grid.

`tools/install_pokepad_halloween.ps1` realiza únicamente la exportación al tamaño nativo con alfa; el contenido gráfico lo crea image_gen. Copia los `.mcmeta` originales. No recolorea ni dibuja las imágenes mediante Python.

Prompts de las dos alternativas originales:

> Create a new original standalone Halloween game menu icon. Cards: ONE icon of THREE slightly fanned collectible game cards with amber borders and midnight plum backs; front card shows a cheerful jack-o-lantern, other cards a crescent moon and bat. Cosmetics: ONE costume-accessories icon of a midnight plum witch hat with amber band, pumpkin buckle and violet sunglasses. No person or recognizable fictional character. Professional polished illustration, bold clean readable shapes, amber/violet highlights. Centered on a square transparent background, object fills90% of canvas. No words, labels, watermark, grid, sheet or extra icons. Clear at100x100 pixels. One composed icon only.

## Tipografía y revisión

Atkinson Hyperlegible Regular, licencia OFL incluida, proveedor `lunaeternal:pokepad`, oversampling4. Dibujo, ajuste de ancho, centrado y tooltips usan el mismo proveedor; se retira el contorno de cuatro copias y la sombra de etiquetas propias. Los campos editables nativos conservan su lógica de cursor y selección de Minecraft. La mejora tipográfica permanece al terminar octubre.

`docs/ui/pokepad-halloween-preview.html` permite comparar los PNG originales y Halloween y recorrer la colección completa. La maqueta web sirve para revisar los recursos; no se presenta como una captura del cliente Minecraft.

Verificación reproducible: `tools/verify_pokepad_halloween.py` exige 72 variantes distintas, dimensiones nativas iguales, metadatos conservados y hashes originales intactos. La verificación visual descarta láminas accidentales y símbolos irreconocibles antes de publicar.
