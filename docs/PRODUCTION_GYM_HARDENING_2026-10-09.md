# Gimnasios, protecciÃ³n y combate â€” reparaciÃ³n del 9 de octubre de 2026

## CURRENT STATE

Worktree recibido: detached `13bca46a`, anterior a producciÃ³n. Se creÃ³ `codex/production-gym-battle-hardening` desde `719ada14`, sin reset ni sobrescribir el checkout principal. Su ediciÃ³n sin commit de Sabrina se conservÃ³. El VPS instalado se identificÃ³ por SHA256 `f76f49bf314de761b749361ab748e0947c0d2472944e6f97e4ab35043118ac54`, coincidente con el despliegue de `719ada14`. La base conserva V060 y los Ãºltimos arreglos de borde y paso Johto 512.

## PROBLEM Â· ROOT CAUSE

1. `canModifyWorld` exigÃ­a Creative incluso despuÃ©s de habilitar editmode. Los callbacks, BlockItem y Axiom consultan esa polÃ­tica comÃºn.
2. Sabrina descartaba toda visita sin reserva y usaba umbral 78.0; su comentario describÃ­a un foso de otra ranura. HabÃ­a una correcciÃ³n parcial sin commit en el principal (80.0), insuficiente frente a las coordenadas aportadas.
3. `empezar` rechazaba visitas administrativas sin reserva. La reparaciÃ³n anterior del NPC consultaba el combate del visitante y podÃ­a cancelar el combate de otro jugador. NPCs con etiquetas incompletas consumÃ­an el clic sin explicaciÃ³n.
4. Los crashes reales del VPS confirman el fallo diferido en RequestInstruction/PassActionResponse. El primer fallo de inicializaciÃ³n no estÃ¡ preservado con su causa completa: varias capturas antiguas registraban Ãºnicamente `getMessage()`. No se afirma que falte el JAR: la clase existe en Cobblemon 1.8.0.

La inspecciÃ³n de RCT API confirma un ciclo concreto: `proxy$onChoiceRequested` detecta un request repetido y llama `setMustChoose(true)` si no hay respuestas. Nuestro antiguo mixin del setter volvÃ­a a llamar `onChoiceRequested`, pudiendo reentrar. Es un defecto comprobado; no se atribuye sin una primera traza Ã­ntegra toda inicializaciÃ³n fallida exclusivamente a Ã©l.

## OPTIONS Â· RECOMMENDED SOLUTION

- Mantener OP4 y los dos grants explÃ­citos; quitar la dependencia de Creative, seguir excluyendo Spectator. Revocar grants sigue cerrando acceso aunque EDITING conserve temporalmente el UUID. DesconexiÃ³n y parada limpian el modo.
- Foso X [5180.479,5200.551], Z [20.597,93.691] + 128r, Y <= 80.5, r=0..7. Sin asignaciÃ³n se infiere solo si Z pertenece a un foso real; no se redondea un hueco ni se concede una reserva. Se respeta una asignaciÃ³n a otro gimnasio. Inicio exacto (5189.95,95,90.78+128r), yaw180, velocidad y caÃ­da anuladas, debounce1500ms. Creative/Spectator y combates activos conservan exclusiÃ³n.
- Clic de staff sin reserva abre la herramienta de prueba existente en una copia libre, sin usar el maestro ni la sala de otro retador. Debe hablar con el lÃ­der de esa copia. PRUEBAS evita medallas y progreso. Jugadores normales conservan cola, ranura propia y ruta Sabrina. La llegada a la cÃ¡mara ya se registra en GymChallengeService; no se elimina esa puerta.
- TrainerStateRecovery verifica el oponente y las asociaciones reales de RCT antes de cancelar estado huÃ©rfano. Se ejecuta al interactuar y periÃ³dicamente tras dos observaciones; usa eventos de carga de entidades, no bÃºsquedas de chunks ni SQL. Un combate activo conserva al NPC ocupado.
- El setter de elecciÃ³n deja de generar acciones. La recuperaciÃ³n usa el request completo y valida su tamaÃ±o; se eliminan pases indiscriminados de RequestInstruction, setter y watchdog. Los passes nativos siguen siendo legÃ­timos para slots sin acciÃ³n; un pase no sustituye una elecciÃ³n legal.
- Se captura RuntimeException, LinkageError (incluidos ambos errores de inicializaciÃ³n) y StackOverflowError dentro del closure diferido. Un WrapMethod de MixinExtras, ya incluido en Fabric, cubre ademÃ¡s TODO el tick de esa batalla: tambiÃ©n las llamadas nativas/RCT anteriores a nuestra inyecciÃ³n. Se cierra solo la batalla fallida, sin evento de victoria. No se silencian OutOfMemoryError ni errores fatales de JVM.

## PASE DE BATALLA

Intercambio del PokÃ©mon existente entre filas 1 y100: el Charizard shiny mecha de nivel50 pasa al1 con `forcedAspects=luna_mecha`; el shiny15 anterior pasa al100. Se conserva la Master Ball adicional del100, los dos PokÃ©mon del catÃ¡logo y el Ãºnico aspecto mecha. No cambian modelos, resolver ni megas. La tarjeta usa los aspectos serializados por Red. No se borran claims ni se duplican entregas ya cobradas; el cambio rige para reclamaciones nuevas.

## DEPENDENCIES Â· RISKS

Java21, Fabric1.21.1, Cobblemon Fabric1.8.0, RCT0.19.0-beta/RCTAPI0.16.0-beta. MixinExtras0.4.1 estÃ¡ en el classpath existente. NingÃºn registro, canal de red, ID de dimensiÃ³n o migraciÃ³n se aÃ±ade. Un fallo de IA no recuperable cierra ese combate; no se promete que continÃºe un combate con una clase ya inutilizable en su JVM.

## IMPLEMENTATION PLAN Â· TEST PLAN

1. Build por mod/build.sh en JDK21. Suite:512 casos,510PASS,2SKIPPED,0fallos. Los casos omitidos se mantienen como limitaciÃ³n.
2. Prueba real de singleton Java que falla: ExceptionInInitializerError, nuevo acceso NoClassDefFoundError y siguiente operaciÃ³n independiente ejecutada.
3. Tests con funciones de producciÃ³n para fronteras exactas, altura80.5 y ocho ranuras. Tests del catÃ¡logo y claims actualizados.
4. Invariantes aÃ±adidos a /luna autotest; /pradmin selftest aÃ±ade ediciÃ³n Survival autorizada. Estos comandos escriben datos de ensayo y se ejecutan exclusivamente en staging con `db.name=prelaunch_20261002`, nunca en producciÃ³n.
5. Arranque real de staging con los mismos Cobblemon/RCT y validaciÃ³n de mixins. DespuÃ©s publicaciÃ³n dirigida de Luna, preservando los demÃ¡s mods, y parada limpia con guardado para cargar el jar de servidor. No se sustituye un JAR abierto ni se usa kill.
6. Verificar SHA del JAR servido y manifiestos del launcher y del actualizador firmado del mrpack. Conservar pack base, mundo y BD; no regenerar dependencias ajenas para una correcciÃ³n de servidor.

## DOCUMENTATION CHANGES Â· EVIDENCIA

Este informe sustituye, para estos fallos, las afirmaciones antiguas de certificaciÃ³n definitiva. Las pruebas simuladas anteriores no demuestran integraciÃ³n real de mixins. Estado de staging, producciÃ³n y publicaciÃ³n se registra tras su ejecuciÃ³n; QA visual con cliente humano y combate completo Blue/Sabrina no se afirma sin observarlo.


### Hallazgo del primer staging

El arranque aplicó los mixins y /pradmin selftest pasó54/54, incluyendo Survival. El primer autotest tuvo834PASS/1FAIL por una expectativa antigua del GTS: exigía1%, pero ProgressionBalanceConfig vigente establece2% y el controlador puede aplicar multiplicador. Se corrigió exclusivamente la expectativa del autotest para leer ese balance, conservando GtsService y toda configuración/BD. Los errores de unicidad de pruebas de idempotencia y las excepciones programadas a propósito se distinguen del contador final de fallos.

El cliente publicado estaba en SHA256 b03fbf048c35634cb3781f0c277c7cb83392c82a6d9da19faa6337e9a6cced05, anterior al servidor; tanto launcher como bundle firmado lo referenciaban. El mrpack público embebe ese JAR y no contiene actualizador: también requiere sustituir exclusivamente overrides/mods/lunaeternal-0.1.0.jar. Se preservan sus147 dependencias, loader0.19.5 y todos los demás overrides. No se regenera el pack desde versiones remotas.

Auditoría del JAR candidato frente al instalado: cero entradas eliminadas; único cambio semántico no-clase: lunaeternal.mixins.json. SQL, dimensiones, modelos y texturas conservados. Algunas entradas de texto varían únicamente LF/CRLF por checkout Windows. SHA256 candidato final:88778cfda70eed9fde86299de28a332f4bd2befaef63946cc4f988739be5acbc.
