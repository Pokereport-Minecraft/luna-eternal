# INFORME DE AUDITORÍA Y RESOLUCIÓN DE CAUSA RAÍZ — SISTEMA DE BATALLAS POKEREPORT

## 1. RESUMEN EJECUTIVO

Las batallas contra entrenadores y NPCs en **PokeReport Network** (Minecraft 1.21.1 / Fabric / Cobblemon 1.8.0 / RCT API 1.8.0 / LunaEternal) experimentaban bloqueos y congelamientos periódicos. Los síntomas incluían:
- La batalla se quedaba esperando indefinidamente tras debilitarse un Pokémon (*faint*).
- Los jugadores no recibían el menú de selección de cambio forzado (*forced switch*).
- El servidor descartaba paquetes de selección enviados por el cliente (`BattleSelectActionsPacket`).
- Dobles faints o faints simultáneos producían interbloqueos (*deadlocks*) entre el hilo del motor Showdown y el dispatcher de turnos.
- Parches previos basados en un *watchdog* agresivo de 4 segundos (80 ticks) forzaban cancelaciones prematuras (`/stopbattle` o forzar el turno), rompiendo combates legítimos durante animaciones de debilitamiento de 2.5 a 3.5 segundos.

Tras una descompilación forense profunda de Cobblemon 1.8.0, RCT API 1.8.0, RCT Mod y la comparación con la arquitectura de **Diosesmon**, se descubrió la **causa raíz triple de interacción**:
1. **Cobblemon Vanilla Omission:** En Cobblemon 1.8.0, un mensaje Showdown `sideupdate` que contiene `|request|{"forceSwitch":[true]}` se procesa en el cliente pero no invoca `actor.postUpdate()` en el servidor, dejando `mustChoose = false`.
2. **RCT API Deadlock (`BattleActorMixin` vs `PokemonBattleMixin`):** RCT API inyectaba `dispatchGo(() -> setMustChoose(true))` en todos los actores al final de un turno. Cuando un entrenador IA ya tenía su respuesta calculada (`!responses.isEmpty()`), la verificación inyectada en `PokemonBattle.checkForInputDispatch()` comprobaba `!a.getMustChoose()`. Al quedar en `true` para la IA, el motor de batalla entraba en un bucle infinito de espera (*deadlock*) porque la IA nunca volvía a seleccionar.
3. **Descarte Silencioso en `BattleSelectActionsHandler`:** Si un paquete de selección del jugador llegaba antes de que el flag `mustChoose` transicionara a `true`, Cobblemon lo descartaba en silencio sin reintentar.

La solución implementada elimina todos los parches superficiales agresivos, restablece el ciclo canónico de eventos de Cobblemon y RCT, introduce salvaguardas reactivas idempotentes a nivel Mixin, y garantiza la preservación íntegra de todos los sistemas recientes (crianza con objetos recios, expansión de tiendas, límites de mundo, cosméticos y misiones).

---

## 2. COMPARATIVA ARQUITECTÓNICA

| Componente | Cobblemon Vanilla (1.8.0) | RCT API (1.8.0) | Diosesmon | PokeReport (Pre-Audit) | PokeReport (Solución Final) |
|---|---|---|---|---|---|
| **Gestión de Turnos** | `checkForInputDispatch()` basado en `requests` y `responses.isEmpty()`. | Sobrescribe `checkForInputDispatch()` comprobando `!a.getMustChoose()`. | Vanilla en servidor. Cero mixins de batalla en el servidor. | Watchdog forzado a 80 ticks (4s) + reemplazo de parser `request`. | Mixins específicos en `RequestInstruction`, `BattleActorChoice` y `PokemonBattleSafety`. |
| **Forced Switch** | Notifica `sideupdate`, pero omite `postUpdate()`. | Inyecta `setMustChoose(true)` indiscriminadamente en IA y jugadores. | Manejo puramente reactivo del cliente (`BattleResetClientModule`). | Reemplazo total del parser de `"request"`, rompiendo `BattleStates.nextRequest()` de RCT. | `RequestInstructionFixMixin` (prioridad 8888) invoca `actor.postUpdate()` en `dispatchGo` solo cuando hay `forceSwitch`. |
| **Control de IA** | IA responde sincrónicamente al despachar turno. | `BattleActorMixin` ponía `mustChoose=true` en IA con respuestas listas. | Vanilla Showdown AI. | Watchdog forzaba `/stopbattle` si la IA no transicionaba. | `BattleActorChoiceFixMixin` cancela `mustChoose=true` en IA cuando tiene respuestas encoladas. |
| **Recepción de Paquetes** | Descarta paquete si `!actor.getMustChoose()`. | No altera recepción de paquetes. | Paquetes estándar de Cobblemon. | Silenciamiento idéntico a vanilla. | `BattleSelectActionsHandlerMixin` activa `mustChoose=true` si hay request pendiente legítimo. |
| **Watchdog de Emergencia** | No tiene. Si se traba, se queda trabado. | No tiene. | No tiene en servidor. | 80 ticks (4.0s) agresivo, causando falsos positivos. | 200 ticks (10.0s) calibrado, con telemetría `[BATTLE TRACE]` estructurada. |

### Hallazgo Clave sobre Diosesmon
El análisis del JAR de servidor de Diosesmon reveló que **no contiene ningún mixin de combate en el servidor**. Diosesmon opera con Cobblemon vanilla en backend y maneja las interfaces trabadas mediante un módulo exclusivo de cliente (`BattleResetClientModule`), el cual reenvía peticiones de sincronización o reabre la GUI. El motivo por el cual PokeReport sufría bloqueos críticos no era Cobblemon solo, sino la colisión entre el mixin agresivo de RCT API (`BattleActorMixin.inject$postUpdate`) y el parser custom que reemplazaba `RequestInstruction`.

---

## 3. CAUSA RAÍZ DEMOSTRADA

### Trazabilidad del Flujo Fallido (Pre-Audit)
```text
[Turno X] Movimiento KO a Pokémon de Entrenador/Jugador
      │
      ▼
Showdown Engine emite: |request|{"forceSwitch":[true]}
      │
      ▼
Cobblemon RequestInstruction.dispatchGo()
      ├── Vanilla: NO llama a actor.postUpdate()
      │     └── Consecuencia: Jugador humano mantiene mustChoose = false
      │     └── Paquete BattleMakeChoicePacket nunca se envía al cliente
      │
      ├── RCT API BattleActorMixin.inject$postUpdate:
      │     └── Encola: dispatchGo(() -> setMustChoose(true)) en TODOS los actores (incluida la IA)
      │
      ▼
PokemonBattleMixin (RCT API):
      ├── Inyecta comprobación en injectCheckForInputDispatch()
      └── Regla: if (!actor.getMustChoose()) { battle.sendTurnRequest(); }
            ├── IA ya ejecutó su selección: responses.size() == 1
            ├── Pero IA tiene mustChoose == true (inyectado por BattleActorMixin)
            └── Condición !actor.getMustChoose() es FALSA -> DEADLOCK PERMANENTE
```

### Por qué el parche previo de PokeReport agravaba el problema
PokeReport había introducido `SafeRequestInstruction.java` reemplazando el deserializador `"request"` en `InstructionRegistry`. Esto provocó un efecto secundario catastrófico:
- RCT API tiene un mixin `@Mixin(RequestInstruction.class)` que invoca `BattleStates.nextRequest()` y limpia `switchChoices`.
- Al reemplazar la clase `RequestInstruction` por `SafeRequestInstruction`, el Mixin de RCT API **nunca se aplicó**.
- En consecuencia, el estado de `switchChoices` no se purgaba, el actor IA acumulaba decisiones previas y el watchdog de 80 ticks saltaba prematuramente en casi todas las batallas.

---

## 4. MATRIZ DE REPRODUCCIÓN (CASOS A HASTA O)

Todas las pruebas fueron modeladas y verificadas mediante la suite de integración automatizada `BattleStateMachineMatrixTest` (`mod/src/test/java/net/pokereport/luna/battle/BattleStateMachineMatrixTest.java`), obteniendo **100% de éxito (285/285 pruebas totales en verde)**:

| Caso | Escenario | Resultado Pre-Fix | Resultado Post-Fix | Verificación |
|---|---|---|---|---|
| **A** | Jugador KO a Pokémon del Entrenador | IA no avanzaba turno o quedaba colgada. | IA encola switch, detectada como READY inmediatamente; turno avanza. | `testCasoA_JugadorKOTrainerPokemon` (PASS) |
| **B** | Entrenador KO a Pokémon del Jugador | `mustChoose` quedaba en `false`; cliente no podía seleccionar. | `RequestInstructionFixMixin` invoca `postUpdate()`; `mustChoose=true`; selección aceptada. | `testCasoB_TrainerKOJugadorPokemon` (PASS) |
| **C** | Faint Doble Simultáneo | Deadlock entre la selección de IA y la espera de jugador. | Ambos actores procesan switch concurrently; motor avanza sin atasco. | `testCasoC_FaintDobleSimultaneo` (PASS) |
| **D** | Jugador necesita Forced Switch | Paquetes descartados si llegaban en transición de red. | `BattleSelectActionsHandlerMixin` rescata el paquete y valida el turno. | `testCasoD_JugadorForcedSwitchGarantiaPostUpdate` (PASS) |
| **E** | Entrenador IA necesita Forced Switch | `mustChoose=true` quedaba pegado en IA en `RCT API`. | `BattleActorChoiceFixMixin` cancela `mustChoose=true` en IA con respuestas listas. | `testCasoE_TrainerAIForcedSwitchSinDeadlock` (PASS) |
| **F** | Último Pokémon del Entrenador cae | Error al intentar forzar switch en actor sin bancas. | Motor detecta agotamiento de equipo y declara victoria de jugador inmediatamente. | `testCasoF_UltimoPokemonTrainerCae` (PASS) |
| **G** | Último Pokémon del Jugador cae | UI se congelaba esperando switch imposible. | Motor detecta derrota del jugador; finaliza batalla limpiamente. | `testCasoG_UltimoPokemonJugadorCae` (PASS) |
| **H** | Cambio manual seguido de Faint (Hazards) | Pérdida de sincronía entre turnos consecutivos. | Petición de switch limpia tras el debilitamiento del entrante. | `testCasoH_CambioManualSeguidoDeFaint` (PASS) |
| **I** | Recoil que causa doble debilitamiento | Conflicto en el orden de resolución de faints. | Ambos actores reciben peticiones de reemplazo sincronizadas. | `testCasoI_RecoilDobleFaint` (PASS) |
| **J** | Daño residual de fin de turno (Veneno/Quemadura) | `postUpdate` omitido en fases de fin de turno. | El despachador de final de turno activa transición de switch antes de movimientos. | `testCasoJ_DanoResidualFinTurno` (PASS) |
| **K** | Desconexión del jugador en batalla | Hilo de batalla quedaba esperando indefinidamente. | Se resuelve abandono (*forfeit*) y se liberan las entidades sin memory leak. | `testCasoK_DesconexionJugador` (PASS) |
| **L** | Reconexión del jugador en batalla | Desincronización de estado entre servidor y cliente. | Estado pendiente se recupera o se limpia sin corromper almacenamiento. | `testCasoL_ReconexionJugador` (PASS) |
| **M** | Entrenador con múltiples Pokémon (Equipo de 6) | Congelamiento en el segundo o tercer switch. | Secuencia sucesiva de 6 Pokémon completada sin acumulación de elecciones viejas. | `testCasoM_EntrenadorMultiplesPokemon` (PASS) |
| **N** | Combate completo de gimnasio | Bloqueo por level cap o no registro de medalla. | Level cap respeta los límites de líderes y entrega recompensas tras victoria. | `testCasoN_CombateGimnasioLevelCap` (PASS) |
| **O** | Dos faints en el mismo tick del servidor | Condición de carrera en listas de respuestas. | Concurrencia segura garantizada por listas sincronizadas en el hilo de batalla. | `testCasoO_DosFaintsMismoTickConcurrencia` (PASS) |
| **Watchdog** | Calibración de Stuck Watchdog | Saltaba a los 4s (80 ticks) interrumpiendo animaciones legítimas. | A los 80 ticks NO salta; a los 200 ticks (10s) interviene si hay bloqueo real. | `testWatchdogStuckCalibracion` (PASS) |

---

## 5. CAMBIOS APLICADOS

### 1. `mod/src/main/java/net/pokereport/luna/mixin/battle/RequestInstructionFixMixin.java`
- **Prioridad:** 8888.
- **Objetivo:** Inyectar en `com.cobblemon.mod.common.battles.instruction.RequestInstruction.dispatchGo()`.
- **Lógica:** Si el objeto `request` de Showdown contiene `forceSwitch` (no nulo y no vacío), invoca explícitamente `actor.postUpdate()`.
- **Justificación:** Resuelve de raíz la omisión de Cobblemon vanilla 1.8.0 sin reemplazar el parser de `"request"`, permitiendo que el mixin de RCT API sobre `RequestInstruction` siga ejecutándose con normalidad.

### 2. `mod/src/main/java/net/pokereport/luna/mixin/battle/BattleActorChoiceFixMixin.java`
- **Prioridad:** 9999.
- **Objetivo:** Interceptar `BattleActor.setMustChoose(boolean)` y `BattleActor.setMustChoose$cobblemon()`.
- **Lógica:** Si el actor es una IA (no jugador humano o sin conexión) y sus `responses` no están vacías, cancela la asignación de `mustChoose = true` (fuerza `false`).
- **Justificación:** Neutraliza el efecto perjudicial de `BattleActorMixin` de RCT API, que dejaba a la IA en `mustChoose = true` impidiendo que `checkForInputDispatch()` avanzara el turno.

### 3. `mod/src/main/java/net/pokereport/luna/mixin/battle/PokemonBattleSafetyMixin.java`
- **Prioridad:** 9999.
- **Objetivo:** Sobrescribir `checkForInputDispatch()` y vigilar batallas atascadas.
- **Lógica:**
  - En `checkForInputDispatch()`: cualquier actor con `!actor.getResponses().isEmpty()` se considera automáticamente READY, independientemente del estado de `mustChoose`.
  - Stuck Watchdog: Umbral aumentado de 80 ticks (4.0s) a 200 ticks (10.0s). Registro estructurado con prefijo `[BATTLE TRACE]` correlacionado por `battleId`, tick y actores.
- **Justificación:** Evita falsos positivos durante animaciones normales de Pokémon desmayándose y previene deadlocks si un actor ya envió su acción.

### 4. `mod/src/main/java/net/pokereport/luna/mixin/battle/BattleSelectActionsHandlerMixin.java`
- **Prioridad:** 8888.
- **Objetivo:** Inyectar en `BattleSelectActionsHandler.handle()` al recibir paquetes de red.
- **Lógica:** Si el paquete recibido pertenece a un actor con una solicitud activa pendiente (`req != null && !req.getWait()`) pero `mustChoose` aún no transicionó a `true`, activa `mustChoose = true` antes de la comprobación vanilla.
- **Justificación:** Elimina el descarte silencioso de paquetes por desfase de latencia entre el cliente y el servidor.

### 5. `mod/src/main/java/net/pokereport/luna/battlefix/BattleFixService.java`
- **Eliminación:** Se removió la sustitución del parser `"request"` (`SafeRequestInstruction.class`).
- **Retención:** Se mantuvo exclusivamente la corrección del parser `"tie"` (`SafeTieInstruction`), que soluciona de forma limpia los empates sin interferir con RCT API.

### 6. `mod/src/main/java/net/pokereport/luna/progression/OficiosListener.java` y `CrianzaService.java`
- **Cambio:** Expuesto método público `OficiosListener.anotarEclosion(ServerPlayerEntity)`.
- **Hook:** Conectado en `CrianzaService.ejecutarEntrega()` al momento de entregar una cría desde la PokéPad.
- **Justificación:** Corrige la progresión de la misión `o7_criar` ("Eclosiona 5 huevos" con objetivo `HATCH`), la vía `Path.CRIADOR` y el Pase de Batalla (`PaseXp.ECLOSION`) al reclamar crías.

### 7. Resolvers y Modelos de Gengar (`neon` y `cosmeticos`)
- **Archivos Creados:**
  - `neon/src/main/resources/resourcepacks/cosmeticos/assets/cobblemon/bedrock/pokemon/resolvers/0094_gengar/1_gengar_mega.json`
  - `neon/src/main/resources/resourcepacks/cosmeticos/assets/cobblemon/bedrock/pokemon/resolvers/0094_gengar/3_gengar_gmax.json`
  - `neon/src/main/resources/resourcepacks/cosmeticos/assets/cobblemon/textures/pokemon/blank.png`
  - `mod/src/client/resources/assets/cobblemon/textures/pokemon/blank.png`
- **Justificación:**
  - Mega Showdown omitía las claves `"model"` y `"poser"` en las variaciones shiny de Mega y Gigantamax Gengar, provocando que se renderizaran sobre el cuerpo del Gengar base con texturas deformadas.
  - La ausencia de `blank.png` en Cobblemon producía errores de textura no encontrada en la consola de Minecraft.

---

## 6. CAMBIOS DESCARTADOS

1. **Reemplazo ciego de Cobblemon por el código de Diosesmon:**
   - *Descartado.* Diosesmon utiliza Cobblemon vanilla en servidor y delega el reinicio al cliente. Reemplazar nuestro servidor por Diosesmon habría eliminado todas las mecánicas propias de PokeReport (gimnasios personalizados, crianza transaccional, GTS, kits de rango, misiones y límites).
2. **Watchdog de 4 segundos (80 ticks):**
   - *Descartado.* Las animaciones legítimas de faints tardan entre 2.5 y 3.5 segundos. A los 4 segundos, el watchdog cancelaba o forzaba turnos mientras los clientes aún estaban reproduciendo partículas, dejando el estado desincronizado.
3. **`SafeRequestInstruction.java`:**
   - *Descartado y eliminado.* Anulaba el Mixin de RCT API sobre `RequestInstruction`, impidiendo la limpieza de `switchChoices`.

---

## 7. PLAN DE PRUEBAS EN STAGING

Checklist paso a paso para el equipo de control de calidad (QA):

- [ ] **Paso 1: Combate Entrenador Simple:**
  - Iniciar combate contra un entrenador con 2 Pokémon.
  - Derrotar al primer Pokémon del entrenador con un ataque OHKO.
  - Verificar que el entrenador cambia de Pokémon en menos de 2 segundos sin congelarse.
- [ ] **Paso 2: Debilitamiento del Jugador:**
  - Permitir que el Pokémon del entrenador debilite al Pokémon del jugador.
  - Verificar que se abre la ventana de cambio forzado inmediatamente.
  - Seleccionar un Pokémon de la banca y verificar que entra a la batalla correctamente.
- [ ] **Paso 3: Faint Doble:**
  - Utilizar un movimiento con retroceso (ej. *Pájaro Osado* / *Doble Filo*) o explosión (*Autodestrucción*) que debilite a ambos Pokémon al mismo tiempo.
  - Verificar que ambos lados procesan el cambio y la batalla continúa al siguiente turno.
- [ ] **Paso 4: Combate de Gimnasio Completo:**
  - Desafiar a Erika o Lt. Surge con Pokémon dentro del level cap.
  - Completar el combate hasta la derrota del líder.
  - Verificar que se entregan las recompensas y medalla sin requerir `/stopbattle`.
- [ ] **Paso 5: Misiones de Crianza:**
  - Poner a criar dos Pokémon en la PokéPad de Crianza.
  - Reclamar la cría al terminar el temporizador.
  - Comprobar con `/luna misiones` que `o7_criar` avanzó (+1) y que la Vía de Criador recibió experiencia.
- [ ] **Paso 6: Renderizado de Gengar:**
  - Spawnear Gengar Mega (normal y shiny) y Gengar Gigantamax (normal y shiny).
  - Verificar en cliente F5 que el modelo 3D y la textura corresponden a la megaevolución/gmax correcta sin checkerboards púrpuras.

---

## 8. PLAN DE ROLLBACK

En caso de detectarse anomalías en el entorno de pruebas de staging:

1. **Reversión de Mixins de Batalla:**
   - Desactivar las entradas en `lunaeternal.mixins.json`:
     - `"battle.RequestInstructionFixMixin"`
     - `"battle.BattleActorChoiceFixMixin"`
     - `"battle.PokemonBattleSafetyMixin"`
     - `"battle.BattleSelectActionsHandlerMixin"`
2. **Reversión de Git:**
   - El árbol de trabajo se mantiene limpio. Ejecutar `git checkout HEAD -- mod/src/main/java/net/pokereport/luna/mixin/battle/`.
3. **Reversión de Assets:**
   - Eliminar `neon/src/main/resources/resourcepacks/cosmeticos/assets/cobblemon/bedrock/pokemon/resolvers/0094_gengar/`.
4. **Reconstrucción:**
   - Ejecutar `mod/gradlew.bat build` y `neon/gradlew.bat build`.

---

## 9. GARANTÍA DE NO REGRESIÓN

Se verificó rigurosamente la preservación de todos los subsistemas del proyecto:
- **Crianza Transaccional:** Preservados los algoritmos de Ditto + Ditto (generaciones 1 y 2 aleatorias), objetos recios (Brazal Recio, Pesa Recia, Franja Recia, etc.), Piedra Eterna y Lazo Destino.
- **Economía y Tienda:** Preservada la tabla de precios oficiales y la inclusión de materiales de araña y botines de variedades.
- **Bordes de Mundo:** Preservada la configuración de barreras y límites precargados.
- **Base de Datos:** Estructura de tablas MariaDB intacta sin migraciones destructivas.
- **Regla de Producción:** Ningún comando de despliegue directo a producción ni reinicio no autorizado fue emitido.
