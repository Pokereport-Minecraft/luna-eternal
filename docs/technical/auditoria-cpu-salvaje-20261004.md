# Auditoría urgente de CPU y fauna — 2026-10-04

## Causa medida

Producción tenía 7,17–7,85 TPS y ticks medianos de 120,8–131,3 ms con cinco jugadores. Dos muestras independientes del hilo principal estaban en serialización NBT de `PokemonEntity`, invocada por selectores de comandos. Perfil Spark previo: https://spark.lucko.me/SLvuNMLl1T.

`ws-angry-alphas-1.1.0.zip` ejecutaba dos selectores `@e[type=cobblemon:pokemon,nbt=...]` cada tick. El dispatch adicional repetía la función desde nueve dimensiones. Sin restricción de distancia, `@e` recorre todas las dimensiones: `execute in` por sí solo no lo limita. Cada Pokémon podía serializarse veinte veces por tick.

Tras deshabilitar ambos packs, manteniendo jugadores conectados, producción volvió a 20 TPS; mediana de 7–10,8 ms, p95 de 10,2–16,4 ms. CPU Docker observado después de estabilizarse: 36,87–58,12 %. El pico de 322,63 % registrado durante las recargas no se usa como medida estable.

## Corrección

- El pack original queda deshabilitado. Se habilita al final el dispatch corregido, que puede operar independientemente y anula `cmds:tick` si alguien habilita otra vez el original con menor prioridad.
- Una búsqueda de Alphas por dimensión cada 20 ticks, con `distance=0..` para limitarla a ese mundo. Mantiene las condiciones originales de distancia al jugador y combate.
- Salvaje pasa de 3 a 0,75 Pokémon/chunk; intentos cada 60 ticks en lugar de 15. Hogar conserva 0,04 y 200 ticks.
- `maximumSpawnsPerPass` baja de 8 a 2 en la configuración del servidor.
- El parche de JAR cambia únicamente `FaunaControl.class` y `PoliticaFauna.class`, preservando las correcciones previas de fotos, misiones, economía y acceso remoto a PC. No requiere actualizar clientes.

## Precarga y limpieza

Chunky no tenía tareas activas, `continueOnRestart=false` y `forceLoadExistingChunks=false`. Ninguno de los seis Salvajes tenía chunks forzados. El censo de regiones guardadas no encontró UUID duplicados. Encontró 1.664 Pokémon guardados en Salvaje/Salvaje2 y hasta 12 por chunk, además de decenas de miles de mobs vanilla procedentes de generación.

`tools/clean_saved_fauna.py` exige servidor detenido para escribir. Retira mobs ambientales vanilla no protegidos y conserva como máximo un Pokémon común por chunk guardado. Conserva dueños, combate, persistencia explícita, nombres, tags, pasajeros, shinies, Alphas, legendarios de Gen 1–2 y metadatos personalizados. El identificador natural de spawn `cobblenavdetailid` no se interpreta como propiedad de un jugador.

Solo modifica archivos `entities/*.mca` de los seis Salvajes. No modifica terreno, POI, inventarios, cofres, economía ni Ciudadela. Guarda copia completa de cada región cambiada y un recibo con UUID/tipo/chunk de cada entidad retirada. Relee el resultado y compara exactamente los NBT conservados; restaura las regiones cambiadas si falla.

Limpieza de producción: **63.902 entidades retiradas con respaldo**: 627 Pokémon comunes y 63.275 mobs ambientales. El detalle y los NBT originales están en `/var/backups/pokereport/cpu-audit-20261004/fauna-regions/`; las entidades protegidas pueden seguir presentes deliberadamente.

## Shiny

Probabilidad base de producción: `shinyRate=8192`. CobbleCuisine configura un multiplicador de 2 para su efecto de comida. Nuestro mod no modifica la probabilidad shiny natural en Salvaje. El volumen excesivo de apariciones aumenta el número total de oportunidades; no se alteró la rareza ni se retiraron shinies.

## Validación y despliegue

Build: 244 pruebas, 242 correctas, 2 omitidas, cero fallos. Staging aislado arrancó con el JAR parcheado, cargó y ejecutó las funciones corregidas sin errores de sintaxis, manteniendo 20 TPS. Fixtures adicionales verificaron la conservación de dueño, NBT UUID, combate, decoración, shiny, Alpha, legendario y metadatos ajenos a CobbleNav. La limpieza real también se ensayó en staging, conservando exactamente los NBT restantes; regiones y configuración de staging se restauraron al terminar.

Se avisó en chat y dentro del juego antes de la parada. Todos los mundos y la base de datos cerraron correctamente. El proceso Java quedó vivo después del cierre; Wings finalizó el proceso solo tras confirmar el guardado. Se publicó el JAR y se limpió la fauna con el servidor detenido; el arranque se solicitó mediante Wings.

JAR anterior SHA256: `9da557e949f9fc263ce1a1ef8790cb580872843df8c96a868ed274a70b9b5493`.

JAR desplegado SHA256: `bc72a948aa9eaf08e7d6f35f5a8162ae516b90269bcdf1a1e69d94909481f26b`.

Pack corregido SHA256: `bcb8d837b9ba0d4598dd9f097625588b66d89b5349aa91d83f6e3cd8a609b935`.

Producción arrancó a las 23:26:50 UTC, confirmó la nueva densidad a las 23:26:57 y habilitó el pack corregido con prioridad final a las 23:28:06. El original no figura habilitado. El contador de 20 ticks funciona, y la función `scan` responde. La comparación de configuraciones confirma que el único cambio en `main.json` es `maximumSpawnsPerPass: 8 -> 2`.

Perfil posterior de 60 segundos: https://spark.lucko.me/0peElyrw6s. A las 23:29:35, con tres jugadores reconectados: TPS 19,92/19,96/19,99 (5 s/10 s/1 min), mediana 3,8–4,2 ms, p95 6–12,1 ms. Una muestra de hilos de 5 s midió el hilo principal al 13,19 % de un núcleo y unos 24 % agregados. Se observó también un pico puntual de CPU al reconectar jugadores; no se afirma que todos los picos de arranque/reconexión hayan desaparecido. La validación posterior se limita a esta carga, no certifica capacidad máxima.

A las 23:30:51 había cinco jugadores conectados: 20 TPS en 5 s/10 s/1 min, mediana 4,3–4,5 ms, p95 7,7–8,2 ms y Docker CPU 24,20 %. En ese instante no había Pokémon cargados en Salvaje: los jugadores estaban reconectando y pasando por Ciudadela. La caída del coste del datapack sí se verificó previamente con los cinco jugadores que permanecían jugando durante su desactivación. Queda por observar el balance visual del nuevo spawn cuando regresen a Salvaje.

Backups operativos y recibos: `/var/backups/pokereport/cpu-audit-20261004/`. El disco del nodo estaba al 95 %, con unos 12 GB disponibles; no se borraron respaldos para ocultar ese estado.
