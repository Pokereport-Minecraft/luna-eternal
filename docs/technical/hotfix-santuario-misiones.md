# Santuario y cobro de misiones · 2026-10-04

Se revisaron los seis worktrees y se integro el hotfix sobre `6058b04a`, que
incluye las actualizaciones recientes de economia, protecciones y PokeVoice.
Los parches conservan byte por byte todas las entradas ajenas al cambio.

## Hallazgos y cambios

- `TexturasFoto` cerraba la imagen nativa aun conservada por la textura.
  Ahora el gestor de texturas conserva su propiedad y la libera al desalojar
  la cache o desconectarse. La cache respeta el orden de insercion.
- `DialogoFoto` leia el archivo entero antes de comprobar su peso. Se limita
  la lectura a 2,5 MB + 1 byte, se impiden selectores simultaneos y se evita
  enviar trozos a otra conexion despues de elegir el archivo.
- Reiniciar el inicial borraba la mision cobrada pero mantenia su asiento.
  El log real confirmo `Duplicate entry 'quest:4:t1_inicial::pd'`. Se conserva
  el progreso cobrado y se recupera la marca de un reinicio antiguo al
  detectar el pago existente, sin volver a dar Plata, XP ni premio del pase.
- El periodo del cobro se calcula una sola vez por transaccion. Los errores
  se notifican al jugador y reiniciar el inicial actualiza tambien misiones.
- Una comprobacion antigua de la puerta contaba jugadores nuevos como
  anteriores a V036. Ahora compara `player.first_seen` con la fecha real de
  aplicacion de V036. Los seis jugadores reales fueron creados despues.

## Validacion

- Build completo y 244 pruebas JUnit: cero fallos.
- Staging en loopback con copia de la base real y usuario SQL exclusivo:
  **807 comprobaciones correctas, cero fallos**.
- Incluye cobro normal, doble cobro, reinicio con recompensa previa,
  conservacion de saldo y XP, avance de la cadena y pruebas de fotos validas,
  invalidas, demasiado pesadas, moderacion y colocacion.
- La recuperacion de Freddysillo se ensayo primero en la copia y despues se
  aplico en produccion con el servidor parado: una sola fila de
  `quest_progress`, con saldo, `player_path` y ledger exactamente iguales.
- La prueba visual en un cliente real y el crash log del jugador no estuvieron
  disponibles. Las comprobaciones de fotos del staging validan el servidor;
  no sustituyen probar el selector nativo y el renderizado en el juego.
- El autotest permanece prohibido en produccion. Se autoriza expresamente
  solo el staging existente y la base aislada de este hotfix con su usuario QA.

## Artefactos y recuperacion

Servidor SHA-256:
`9da557e949f9fc263ce1a1ef8790cb580872843df8c96a868ed274a70b9b5493`.

Cliente SHA-256:
`4851dd53662d1c0a1103ddb2fe349b113161930572cae65dba6d6ff6041a81b3`.

Son parches de sus respectivas bases reales: se conservan los ajustes propios
del cliente y del servidor. Las clases corregidas de fotos son identicas en
ambos artefactos. Solo se actualizo la entrada `mods/lunaeternal-0.1.0.jar`
del manifiesto; todos los demas archivos y perfiles permanecen iguales.
Manifiesto publicado y descargas verificadas: `15b39a6f34`.
Puntero anterior para rollback: `38a9240999`.

Backup del jar anterior, dumps SQL previos y logs de QA:
`/var/backups/pokereport/santuario-misiones-20261004` en el nodo.
Backup completo del 4-oct verificado con SHA-256 y `zstd -t`:
`cobblemon_2026-10-04_043002.tar.zst`.

Se aviso antes de la parada en esta conversacion y con cuenta regresiva en
el chat del juego. El primer intento de parada directa de Docker provoco
el reinicio automatico de Wings; se repuso el jar anterior y se continuo
con parada/arranque controlados por la API de Wings. La configuracion y el
jar originales del staging se restauraron y el contenedor QA quedo parado.
