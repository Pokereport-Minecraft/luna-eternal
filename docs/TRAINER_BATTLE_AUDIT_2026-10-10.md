# Auditoría de turnos de entrenadores — 2026-10-10

Estado: **desplegado en producción con autorización explícita del usuario**, 10 de octubre20:59 Bogotá (11 de octubre01:59 UTC). No se han sustituido mundos, datos ni configuraciones privadas. Rama `codex/production-gym-battle-hardening`, implementación `a2007876`.

Entrega compilada y guardada: SHA-256 `d3e8407e38ea105e0ca07f4c5b98d3f9b05545d304561316359cf89d9775fc81`, local `build/trainer-audit-181/luna-trainer-release.jar`, VPS `/root/luna-trainer-release-d3e8407e.jar` (fuera de mods de producción). Build completo aprobado. Respecto al candidato42a8, el último cambio de fuente es la expectativa de autotest para Mewtwo.

Producción: hash instalado verificado;71 JAR ajenos idénticos. Aviso60segundos, `save-all flush`, parada mediante Wings y cierre confirmado de dimensiones y base de datos. Arranque `Done (3.846s)` a01:59:54UTC. Copia del JAR previo y registro de hashes en `/root/backups_predeploy/trainer-release-20261011-015822`. No rollback, SIGKILL ni cambios de SQL. Distribución cliente todavía conserva el catálogo anterior; la entrega del kit depende del catálogo nuevo del servidor, pero la previsualización local requiere actualizar su JAR.

## Binarios examinados

Se extrajeron clases de los JAR exactos, se decompilaron con CFR y se revisó el motor Showdown incluido en Cobblemon; no se sustituyó por una versión upstream distinta.

| Componente | Versión | SHA-256 |
|---|---|---|
| Cobblemon | 1.8.1+1.21.1 | `4d90ba6775e3a332aad9d5e49a9f6366f973b3e1a12e0004d22ce21c600eebc5` |
| RCT API | 0.16.0-beta | `2fe8643f28a60f9159138c2515253cc24ac34e04f44d937d52c87e480c688b75` |
| RCT Mod | 0.19.0-beta | `0cad7a16f47b0e58c2aff835d0bc75ef010ade9b1696a383f0f7315438f1ac27` |
| Luna de producción | 0.1.0 | `ed83825868a26ed50d333cb9fc9c33e9ccfb0098959b1a34a3df40d38174a18e` |

También se inspeccionaron los mixins de combate presentes de MegaShowdown, Raid Dens y Cobblemon Battle Positions. Esta entrega no actualiza esas dependencias. Los extractos locales y el inventario están en `build/trainer-audit-181/`; no se versionan binarios de terceros.

## Causas verificadas y responsabilidades

1. El watchdog de Luna instalado en producción fuerza `GO` y retira un elemento de la cola tras 200 ticks. También deduce victorias desde HP locales. Los logs de producción muestran intervenciones «Desatascando» en Misty y Blue. Saltarse la cola rompe el orden entre mensajes, cambios y animaciones. El candidato conserva las barreras y, ante un bloqueo persistente, cierra solamente la batalla sin conceder victoria.
2. Showdown emite la solicitud de una fase antes del bloque `update` que contiene movimientos, debilitamientos y cambios anteriores. El antiguo `RequestInstructionFixMixin` llamaba a `postUpdate` y elegía por la IA desde la recepción de la solicitud. Se elimina ese mixin y el parche global de `mustChoose`.
3. `BattleInputCoordinator` concentra la entrega de elecciones de entrenadores: exige la misma solicitud procesada por `postUpdate`, cola vacía y barrera completada. El actor con `wait` no envía ataques. No fabrica pases para completar listas.
4. El setter nativo agrega respuestas antes de terminar de validar todos los puestos. En dobles, una segunda elección inválida podía dejar media selección. La operación ahora restaura la lista anterior al rechazarla. Se comprueba cantidad, nulos y cambios duplicados o fuera del equipo.
5. Los paquetes de selección se aceptan solo para el participante, solicitud y fase disponibles. Un error antiguo no reabre una solicitud posterior. La recuperación de IA está limitada; el error no se transforma en una nueva solicitud de cambio inventada.
6. RCT filtra Pokémon debilitados antes de curar el equipo. La adaptación de Luna contaba todos los miembros. Ahora usa los participantes admitidos por RCT y mantiene la política mínima propia del reto.
7. El resultado del gimnasio queda asociado al UUID de la batalla, se consume una sola vez y no puede proceder de otra batalla del mismo jugador. Los retornos diferidos verifican asignación y ausencia de otro combate activo.

Showdown conserva turnos, legalidad final y resultado; RCT conserva actores y ciclo de entrenadores; Cobblemon conserva protocolo y animaciones; Luna configura equipos, gestiona arena/progreso y coordina el envío al terminar las animaciones. El coordinador solo interviene en combates RCT o con `TrainerBattleAI`.

## Qué es y qué no es un ataque extra

El reemplazo obligatorio después de un KO no concede un ataque nuevo al rival. El cambio voluntario consume acción. Un cambio provocado por U-turn antes de que el rival actúe puede ir seguido por el ataque pendiente de ese mismo turno. Las pruebas separan estos casos para no eliminar mecánicas legítimas al corregir el fallo.

## Validación reproducible

- `mod/gradlew.bat build --console=plain`: 505 casos, 0 fallos, 0 errores, 2 omitidos (503 ejecutados correctamente).
- `node tools/tests/trainer_showdown_regression.cjs build/trainer-audit-181/showdown`: 8 PASS sobre el motor incluido. KO de ambos lados, cambio voluntario, U-turn, KO simultáneo, actor en espera, dobles con un suplente y dobles con cambio duplicado.
- Consola **solo staging**: `luna battle_regression`. Fixtures con clases nativas y mixins transformados: barrera conservada tras 250 ticks, ejecución única al liberar, selección desde roster autoritativo, identidad de solicitud, retorno temprano de `postUpdate`, IA aplazada y validación atómica.
- Consola **solo staging**: `luna battle_regression engine`. Equipos adaptados reales de los 23 retos, RCT + Cobblemon + Showdown, incluidos Giovanni y Luana en dobles. Entidades temporales, sin jugadores humanos ni medallas.

El ensayo de motor permite exclusivamente el caso NPC contra NPC en el validador de RCT, de forma síncrona y restaurándolo inmediatamente. Requiere consola OP4 y base `prelaunch_20261002`. No demuestra por sí solo interfaz de jugador, clics de NPC, objetos de bolsa ni todos los movimientos/gimmicks existentes.

Primera ronda completa, candidato `8e815121e05759ba7c9833ab4cca58016a9adbd760c9a3041606bf9add13febc`: **23/23** resultados reales, sin cierre de aislamiento ni desatasco forzado. Turnos: Brock31, Misty22, Surge12, Erika24, Koga22, Sabrina25, Blaine27, Giovanni11, Cissy17, Danny39, Rudy15, Luana9, Drake24, campeón Kanto14, Pegaso20, Antón19, Blanca18, Morti18, Aníbal25, Yasmina18, Fredo14, Débora17, campeón Johto18. Esta evidencia no se atribuye a versiones posteriores.

Ronda posterior sobre `178e0a684c6babe0db3cbf6d914a25b9c8bc759df139626269e40f24ecab7906`: los seis primeros fixtures pasan; el séptimo detectó una solicitud de prueba sin `side`, corregida en el fixture. Autotest general: **836 PASS / 0 FAIL**. Ejecutarlo durante el ensayo de motor borró las tareas de seguimiento mediante `Programador.olvidarTodo()`: esa ronda queda **incompleta**, aunque sus tres batallas terminaron. Ejecutar autotest y ensayo de motor secuencialmente, nunca en paralelo.

Candidato con fixture corregido y peticiones adicionales: `42a8b9038539978f6e7806989a9f55bf0f35a7f4db72dfcd640241b80a113edd`. **7/7 fixtures nativos y176/176 comprobaciones de adaptación PASS**, incluyendo Mewtwo obligatorio. Evidencia remota `/root/pokehardening-luna-1791665818.txt`.

Autotest de este candidato:835 PASS /1 FAIL. Única discrepancia: la prueba histórica exigía que todo Giovanni fuese tipo Tierra, incompatible con la nueva petición explícita de Mewtwo. Se modifica únicamente esa expectativa para admitir a Mewtwo como firma de Giovanni y conservar la comprobación de tipo en los demás miembros. No se atribuye a este candidato un836/836 que no obtuvo.

Por petición posterior del usuario se cierra la ampliación de ensayos y se prepara la entrega; no se ejecutó otra ronda completa de23 sobre el último candidato. Staging detenido con guardado limpio. Producción conserva el JAR anterior y tenía3 jugadores al comprobarla.

## Cambios adicionales solicitados

- **Giovanni siempre incluye Mewtwo.** El datapack instalado ya lo llevaba al nivel46, pero el repertorio adaptativo de Luna no lo incluía. Ahora es su primera firma, que el selector conserva obligatoriamente. Repertorio legal con Pressure, naturaleza Timid, Leftovers y cobertura adaptada. El resto del equipo continúa adaptándose; el combate sigue siendo doble. Ensayos específicos para tres perfiles y cantidades4–6.
- **Kit Campeón:** reemplaza64 troncos y64 bloques de césped por1 `cobblemon:power_weight` (Pesa Recia) y1 `cobblemon:power_anklet` (Franja Recia). Se comprobó el nombre en `assets/cobblemon/lang/es_es.json` del JAR1.8.1: `power_band` es Banda Recia, un objeto distinto ya presente. Mantiene resto del kit, permisos y cooldown120h. No altera entregas pasadas ni reinicia reclamaciones.
- Comparación binaria de recursos: de1244 recursos del JAR del servidor y1246 del cliente publicado, el único recurso modificado es `data/lunaeternal/kits.json`. Texturas, modelos, menús, Halloween y SQL conservados.

## Promoción

No basta copiar el JAR para activar mixins: necesitan un nuevo proceso Java. No usar `/reload` ni instrumentación en caliente para simular una actualización. Preparar hash, evidencia y comparación de recursos antes de una parada limpia de mantenimiento. No restaurar bases ni mundos. Conservar la revisión cliente publicada y las texturas Halloween al distribuir cualquier nuevo artefacto.

Activación completada. Pendiente comprobar con jugador real el reemplazo tras KO, incluido combate salvaje reportado durante el despliegue, antes de afirmar que todos los síntomas desaparecieron. No se amplían más baterías de prueba por petición del usuario. El nuevo coordinador se limita a RCT/TrainerBattleAI; se eliminaron los parches antiguos globales de RequestInstruction y setter de mustChoose y el watchdog que saltaba fases.
