# Gimnasios, protección y combate — reparación del 9 de octubre de 2026

## CURRENT STATE

Worktree recibido: detached `13bca46a`, anterior a producción. Se creó `codex/production-gym-battle-hardening` desde `719ada14`, sin reset ni sobrescribir el checkout principal. Su edición sin commit de Sabrina se conservó. El VPS instalado se identificó por SHA256 `f76f49bf314de761b749361ab748e0947c0d2472944e6f97e4ab35043118ac54`, coincidente con el despliegue de `719ada14`. La base conserva V060 y los últimos arreglos de borde y paso Johto 512.

## PROBLEM · ROOT CAUSE

1. `canModifyWorld` exigía Creative incluso después de habilitar editmode. Los callbacks, BlockItem y Axiom consultan esa política común.
2. Sabrina descartaba toda visita sin reserva y usaba umbral 78.0; su comentario describía un foso de otra ranura. Había una corrección parcial sin commit en el principal (80.0), insuficiente frente a las coordenadas aportadas.
3. `empezar` rechazaba visitas administrativas sin reserva. La reparación anterior del NPC consultaba el combate del visitante y podía cancelar el combate de otro jugador. NPCs con etiquetas incompletas consumían el clic sin explicación.
4. Los crashes reales del VPS confirman el fallo diferido en RequestInstruction/PassActionResponse. El primer fallo de inicialización no está preservado con su causa completa: varias capturas antiguas registraban únicamente `getMessage()`. No se afirma que falte el JAR: la clase existe en Cobblemon 1.8.0.

La inspección de RCT API confirma un ciclo concreto: `proxy$onChoiceRequested` detecta un request repetido y llama `setMustChoose(true)` si no hay respuestas. Nuestro antiguo mixin del setter volvía a llamar `onChoiceRequested`, pudiendo reentrar. Es un defecto comprobado; no se atribuye sin una primera traza íntegra toda inicialización fallida exclusivamente a él.

## OPTIONS · RECOMMENDED SOLUTION

- Mantener OP4 y los dos grants explícitos; quitar la dependencia de Creative, seguir excluyendo Spectator. Revocar grants sigue cerrando acceso aunque EDITING conserve temporalmente el UUID. Desconexión y parada limpian el modo.
- Foso X [5180.479,5200.551], Z [20.597,93.691] + 128r, Y <= 80.5, r=0..7. Sin asignación se infiere solo si Z pertenece a un foso real; no se redondea un hueco ni se concede una reserva. Se respeta una asignación a otro gimnasio. Inicio exacto (5189.95,95,90.78+128r), yaw180, velocidad y caída anuladas, debounce1500ms. Creative/Spectator y combates activos conservan exclusión.
- Clic de staff sin reserva abre la herramienta de prueba existente en una copia libre, sin usar el maestro ni la sala de otro retador. Debe hablar con el líder de esa copia. PRUEBAS evita medallas y progreso. Jugadores normales conservan cola, ranura propia y ruta Sabrina. La llegada a la cámara ya se registra en GymChallengeService; no se elimina esa puerta.
- TrainerStateRecovery verifica el oponente y las asociaciones reales de RCT antes de cancelar estado huérfano. Se ejecuta al interactuar y periódicamente tras dos observaciones; usa eventos de carga de entidades, no búsquedas de chunks ni SQL. Un combate activo conserva al NPC ocupado.
- El setter de elección deja de generar acciones. La recuperación usa el request completo y valida su tamaño; se eliminan pases indiscriminados de RequestInstruction, setter y watchdog. Los passes nativos siguen siendo legítimos para slots sin acción; un pase no sustituye una elección legal.
- Se captura RuntimeException, LinkageError (incluidos ambos errores de inicialización) y StackOverflowError dentro del closure diferido. Un WrapMethod de MixinExtras, ya incluido en Fabric, cubre además TODO el tick de esa batalla: también las llamadas nativas/RCT anteriores a nuestra inyección. Se cierra solo la batalla fallida, sin evento de victoria. No se silencian OutOfMemoryError ni errores fatales de JVM.

## PASE DE BATALLA

Intercambio del Pokémon existente entre filas 1 y100: el Charizard shiny mecha de nivel50 pasa al1 con `forcedAspects=luna_mecha`; el shiny15 anterior pasa al100. Se conserva la Master Ball adicional del100, los dos Pokémon del catálogo y el único aspecto mecha. No cambian modelos, resolver ni megas. La tarjeta usa los aspectos serializados por Red. No se borran claims ni se duplican entregas ya cobradas; el cambio rige para reclamaciones nuevas.

## DEPENDENCIES · RISKS

Java21, Fabric1.21.1, Cobblemon Fabric1.8.0, RCT0.19.0-beta/RCTAPI0.16.0-beta. MixinExtras0.4.1 está en el classpath existente. Ningún registro, canal de red, ID de dimensión o migración se añade. Un fallo de IA no recuperable cierra ese combate; no se promete que continúe un combate con una clase ya inutilizable en su JVM.

## IMPLEMENTATION PLAN · TEST PLAN

1. Build por mod/build.sh en JDK21. Suite:512 casos,510PASS,2SKIPPED,0fallos. Los casos omitidos se mantienen como limitación.
2. Prueba real de singleton Java que falla: ExceptionInInitializerError, nuevo acceso NoClassDefFoundError y siguiente operación independiente ejecutada.
3. Tests con funciones de producción para fronteras exactas, altura80.5 y ocho ranuras. Tests del catálogo y claims actualizados.
4. Invariantes añadidos a /luna autotest; /pradmin selftest añade edición Survival autorizada. Estos comandos escriben datos de ensayo y se ejecutan exclusivamente en staging con `db.name=prelaunch_20261002`, nunca en producción.
5. Arranque real de staging con los mismos Cobblemon/RCT y validación de mixins. Después publicación dirigida de Luna, preservando los demás mods, y parada limpia con guardado para cargar el jar de servidor. No se sustituye un JAR abierto ni se usa SIGKILL. Si quedan hilos no daemon tras completar el guardado y cerrar el pool SQL, se solicita terminación normal de JVM mediante SIGTERM.
6. Verificar SHA del JAR servido y manifiestos del launcher y del actualizador firmado del mrpack. Conservar pack base, mundo y BD; no regenerar dependencias ajenas para una corrección de servidor.

## DOCUMENTATION CHANGES · EVIDENCIA

Este informe sustituye, para estos fallos, las afirmaciones antiguas de certificación definitiva. Las pruebas simuladas anteriores no demuestran integración real de mixins. Estado de staging, producción y publicación se registra tras su ejecución; QA visual con cliente humano y combate completo Blue/Sabrina no se afirma sin observarlo.


### Hallazgo del primer staging

El arranque aplicó los mixins y /pradmin selftest pasó54/54, incluyendo Survival. El primer autotest tuvo834PASS/1FAIL por una expectativa antigua del GTS: exigía1%, pero ProgressionBalanceConfig vigente establece2% y el controlador puede aplicar multiplicador. Se corrigió exclusivamente la expectativa del autotest para leer ese balance, conservando GtsService y toda configuración/BD. Los errores de unicidad de pruebas de idempotencia y las excepciones programadas a propósito se distinguen del contador final de fallos.

El cliente publicado estaba en SHA256 b03fbf048c35634cb3781f0c277c7cb83392c82a6d9da19faa6337e9a6cced05, anterior al servidor; tanto launcher como bundle firmado lo referenciaban. El mrpack público embebe ese JAR y no contiene actualizador: también requiere sustituir exclusivamente overrides/mods/lunaeternal-0.1.0.jar. Se preservan sus147 dependencias, loader0.19.5 y todos los demás overrides. No se regenera el pack desde versiones remotas.

Auditoría del JAR candidato frente al instalado: cero entradas eliminadas; único cambio semántico no-clase: lunaeternal.mixins.json. SQL, dimensiones, modelos y texturas conservados. Algunas entradas de texto varían únicamente LF/CRLF por checkout Windows. SHA256 candidato final:88778cfda70eed9fde86299de28a332f4bd2befaef63946cc4f988739be5acbc.

## RESULTADO FINAL · DESPLIEGUE Y PUBLICACIÓN

- Código implementado en `0dec27e1`, rama `codex/production-gym-battle-hardening`, basado en `719ada14`. Se conserva el cambio sin commit del checkout principal; no se hizo reset, merge sobre ese checkout ni rollback.
- Suite de build: 512 casos, 510 aprobados, dos omitidos, cero fallos. El candidato final arrancó en staging con Cobblemon1.8.0/RCT y produjo `Autotest final: passed=835 failed=0`; protección54/54. Incluye la inicialización real de PassActionResponse y la contención de fallos de inicialización en JVM. Las pruebas con datos se limitaron a `prelaunch_20261002`.
- Producción: aviso de60 segundos, `save-all flush`, parada por Wings, confirmación de `All dimensions are saved` y `luna-db - Shutdown completed.`, después SIGTERM normal para hilos residuales. Reemplazo atómico únicamente con el proceso detenido. Arranque solicitado por Wings y confirmado a las23:00:07UTC del9 de octubre (`Done (3.955s)`). Docker iniciado22:59:43UTC, activo. `/pradmin status` respondió `World Protection: ENABLED`. Ningún fallo de aplicación de mixins, tick fatal ni migración reaplicada en este arranque.
- JAR instalado y distribuido: SHA256 `88778cfda70eed9fde86299de28a332f4bd2befaef63946cc4f988739be5acbc`, SHA1 `b83439ae75188614a7777f9780bbba2fc48cabb8`, 108754525bytes. Artefacto inmutable: https://pokereport.online/client/0.2.0-gymfix-20261009/lunaeternal-0.1.0-b83439ae75.jar.
- Launcher: puntero público y fallback `master/manifest.json` verificados; manifiesto `manifest-149930a632.json`, conservando exactamente los otros163 archivos. No se regeneraron dependencias desde un catálogo actual distinto del publicado.
- Cliente nativo: bundle firmado0.1.1, base0.2.0, protocolo3; firmas verificadas con la clave pública después de publicar. Los otros nueve archivos administrados se conservaron. `/client/latest.json` responde con el bundle nuevo.
- `.mrpack`: sustituido exclusivamente el JAR propio en `overrides/mods/lunaeternal-0.1.0.jar`, conservando índice,147 dependencias y todos los demás overrides. ZIP y hash verificados. SHA256 `76a675d4cdb372c940abd10c94ed8a692263925392e29aa79729b4ef856e3bd7`. Actualizados tanto el enlace canónico de `/jugar` como el artefacto inmutable https://pokereport.online/client/0.2.0-gymfix-20261009/PokeReport-Base-0.2.0.mrpack. Quien ya importó un pack sin actualizador necesita actualizar su JAR o reimportar el pack en su launcher elegido.
- Staging quedó guardado y detenido, como estaba inicialmente. Copias preventivas del JAR y de los archivos publicados están conservadas; ninguna fue restaurada. No hubo rollback ni borrado de mundos, esquemas o claims.

### Aviso de checksum SQL y límites observados

El arranque registra diferencias de checksum de migraciones por CRLF del checkout Windows. Se compararon las60 migraciones del JAR anterior y el instalado: todas son idénticas normalizando exclusivamente CRLF a LF. V060 anterior/registrado: `9ae1a242ead8ad9a90e96609e00f37bf0d84a7091cf88efbd3155de8704dd769`; empaquetado actual: `1920021e1655b9481c61d8146129437c5257f9e85ad99f166655d3aa1c18e1d0`. Database.migrate detecta versión ya aplicada, registra el aviso y ejecuta `continue`; no vuelve a ejecutar SQL ni cambia el checksum registrado. No se alteró la tabla para esconder el aviso. El contenido de V060 y el esquema aplicado se conservan.

No se afirma QA visual del modelo del Charizard en el Poképad ni haber completado con un cliente humano los combates de Blue/Sabrina. Build, geometría, permisos, catálogo, contención de errores y arranques reales sí están verificados. Un error irrecuperable de IA cierra ese combate; los problemas fatales de JVM no se silencian. El nivel1 ya cobrado no vuelve a entregarse automáticamente: hacerlo requeriría una política de compensación separada.
