# Reinicio de jugadores antes de apertura — 3 de octubre de 2026 (Bogotá)

Autorización explícita: borrar todos los registros y pertenencias de jugadores, incluyendo datos de Tebex. Es un reinicio del estado de jugadores; se conservan mods, mundo, NPCs, reglas y contenido del servidor.

## Alcance

- MariaDB: identidad, saldos y ledger, inventarios de mochila, Pokémon en mercado/GTS/crianza pendientes, rangos comerciales, compras/kits/cajas/pase, clanes, misiones, trabajos, medallas, Pokédex, hogares, voces y demás progreso.
- Tebex local: pagos, fulfillments y efectos quedan a cero. Se verificó la API oficial: 0 jugadores y 0 comandos pendientes. Se conserva la configuración de integración y catálogo; no se elimina el historial financiero del panel externo de Tebex.
- EasyAuth: esquema SQLite recreado vacío, sin contraseñas, IPs o sesiones antiguas.
- Minecraft/Cobblemon: playerdata, estadísticas, avances, party/PC, Pokédex, spawn history de CobbleNav, TM moves, skins y recibos de mercado retirados del volumen activo. Se incluyen copias .old y los registros RCT de jugador y memoria de entrenadores.
- OP, whitelist, bans, usercache, grants y etiquetas administrativas vacíos. Debe reasignarse el fundador después de crear la nueva cuenta y completar su entrada.
- Fotos y reservas de jugadores del Santuario borradas; se mantiene el catálogo de nichos construido, con reservas, fotos y honores a cero.
- Logs históricos y copias locales de permisos archivados fuera del volumen de juego. Los respaldos conservan los datos anteriores para recuperación; no son datos activos.

## Preservación

Se preservan el esquema y sus 57 migraciones, catálogos/calendarios globales de caza/kits/pase, política económica y posiciones de mineral colocado. Se verificaron SHA256 de 3.228 archivos de terreno, POI y datapacks antes de aplicar una corrección al adaptador Angry Alphas: el namespace real de las dimensiones es `lunaeternal`, no `pokereport`. No se quitaron entidades propias de NPCs; no se encontraron entidades persistidas con dueño entre los antiguos jugadores.

## Backup y ejecución

Backup coordinado: `/var/backups/pokereport/player-reset/20261003T235703Z`.
Incluye SQL, SQLite y `affected-files.tar` de 244.480.000 bytes / 6.240 entradas. Se referencia además el backup completo de mundo `/var/backups/pokereport/preopen-fixes/20261003T233132Z/server.tar`. Prueba de restauración aislada PASS: 26 jugadores, 23 pagos Tebex, schema 57 y 26 cuentas EasyAuth. La base temporal de validación fue retirada.

Dos comprobaciones de formato detuvieron la fase de archivos antes del borrado SQL. Se restauraron los archivos desde el backup y se confirmó el estado original antes de repetir. El lector admite ahora regiones vacías y sectores finales válidos sin relleno. El borrado SQL posterior se ejecutó en una transacción con clasificación explícita de todas las tablas. Staging permanece detenido y conserva su copia histórica de QA; los backups y esa copia no se presentan como borrados.

## Recuperar el fundador

Entrar con TheJuanCE, crear la cuenta mediante `/register` y completar la entrada al mundo. En la consola de Pterodactyl, sin barra inicial:

```text
pradmin rol asignar TheJuanCE owner
```

La consola es necesaria porque también se retiraron los accesos administrativos antiguos. Para construir, una vez asignado el rol, dentro del juego:

```text
/pradmin editmode on
```

No se debe restaurar una copia vieja de userdata/SQL después de este reset salvo recuperación deliberada: reintroduciría el progreso borrado. Una nueva conexión crea registros nuevos de forma normal.

Resultado posterior al arranque: cuentas de Minecraft/MariaDB y EasyAuth 0; pagos, fulfillments y demás tablas de jugador vacíos; archivos personales antiguos ausentes. Servidor `Done`, whitelist original restaurada y función Angry Alphas ejecutada en el namespace real. Evidencia: [recibo](preopening-player-reset-receipt.json). No se ejecutó el autotest con fixtures sobre producción después de vaciarla, para no crear registros de prueba.
