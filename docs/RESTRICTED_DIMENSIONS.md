# Nether y End restringidos — 2026-10-04

Petición: bloquear Nether y End para jugadores. RestrictedDimensions deniega los destinos minecraft:the_nether y minecraft:the_end, independientemente del rango comercial y del modo de juego. Mantenimiento: únicamente OP con rol operativo staff (Tablist.rankOf/esStaff); OP sin rol no basta.

RestrictedDimensionsMixin intercepta ServerPlayerEntity.teleportTo(TeleportTarget) antes del traslado por portales y las dos sobrecargas teleport a ServerWorld usadas por comandos/mods. Devuelve null/false o cancela según contrato, conservando el mundo de origen. El mensaje es personal en la barra de acción. La vigilancia por segundo lleva al lobby a jugadores no autorizados que reaparezcan en una dimensión restringida desde datos guardados; no borra sus inventarios ni sus mundos. Hogar, Ciudadela, Salvajes, Lobby y gimnasios conservan su política existente.

Validación: build 242 PASS / 2 SKIPPED; staging 803 autotests PASS y 53 ensayos de protección PASS, con jugador sintético y conexión local de ensayo. Doce comprobaciones ejercitan las tres rutas reales de traslado hacia ambos mundos en Survival y Creative. No equivalen a QA visual de un cliente humano entrando en portales.

Desplegado en principal, sin actualización de cliente ni cambios SQL/configuración. JAR SHA256 36dcd4dbb265378dfabcb75340cfc19c3dde4268297127e4eac747ce5833681e. Backup del JAR y configuración previa: /var/backups/pokereport/dimension-guard/20261004T172208Z. JAR anterior: 7eebcd18964bc8b29b27aaaa2efc2144036413660bfe6eaf39e38accd6ef91b6. Staging detenido después del ensayo.

Rollback: guardar mundos y detener el principal; restaurar únicamente lunaeternal-0.1.0.jar desde la copia, conservando propietario del volumen; arrancar y comprobar latest.log. No restaurar datos de jugadores ni SQL para revertir esta política.
