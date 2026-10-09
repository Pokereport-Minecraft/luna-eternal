# POKEREPORT NETWORK — AUDITORÍA EXHAUSTIVA DE SPAWNS Y PROGRESIÓN DE FAUNA

**Fecha:** 8 de Octubre de 2026  
**Ambiente:** Servidor de Producción (`15.235.16.131`, puerto `25567`, Container `1f23bff0-bc4c-4c00-bbcf-2fe81ff666dd`)  
**Base de Código:** `mod/src/main/java/net/pokereport/luna/world/` (`PoliticaFauna.java`, `FaunaControl.java`)  
**Motor:** Fabric 1.21.1 / Cobblemon 1.7.3

---

## 1. ESTADO ACTUAL DETECTADO (LÍNEA BASE EN PRODUCCIÓN)

### 1.1 Configuración de Cobblemon (`config/cobblemon/main.json`)
* `ticksBetweenSpawnAttempts`: **20.0** (El motor intenta generar Pokémon cada 20 ticks = 1 segundo por jugador activo).
* `allowExperienceFromPvP`: **true** (VULNERABILIDAD CRÍTICA P0: Duelos amistosos entre jugadores otorgan experiencia completa).
* `experienceMultiplier`: **2.0** (Multiplicador global del 200% para toda la experiencia en el juego).
* `shinyRate`: **16384.0** (Tasa base 1 entre 16,384).
* `fossilMachineShinyChance`: **100** (ALERTA: 1 en 100 de probabilidad de Shiny en máquinas de fósiles = 1% por fósil).
* `honeySlatherShinyChance`: **4000** (1 en 4,000 en árboles untados de miel).
* `allowFreeCam`: **false**.

### 1.2 Filtros Generacionales y Dimensiones (`PoliticaFauna.java` & Datapack)
* **Generaciones permitidas:** Estrictamente Generación 1 y Generación 2 (Dex 1 a 251). Formas regionales (Alola, Galar, Hisui, Paldea) y Gen 3+ son bloqueadas automáticamente.
* **Overworld (Mundo Hogar):** Densidad `0.04` Pokémon por chunk. Shinies, legendarios y criaturas de alta rareza están totalmente prohibidos en el Hogar.
* **Dimensiones Salvajes (`lunaeternal:salvaje[1-6]`):** Densidad `0.75` Pokémon por chunk. Multiplicador de ultrarraro actual: `+10%` (`1.10f`).
* **Ciudadela e Islas Naranja:** Sin spawns de fauna salvaje (solo combatientes y entrenadores registrados).

---

## 2. AUDITORÍA DETALLADA DE POOLS Y PESOS POR ESPECIE

Tras auditar todos los archivos de pool en `data/cobblemon/spawn_pool_world/`:

### 2.1 Especies Problemáticas y Sobreasignadas (P0)

1. **Dratini (Pseudolegendario Gen 1):**
   * *Bucket actual:* `uncommon` y `rare`.
   * *Peso en océano:* **84.0** en bucket `uncommon` (`condition: #cobblemon:is_ocean`).
   * *Impacto:* Un jugador nadando en océano se encuentra Dratini de forma casi garantizada cada minuto. Desvaloriza completamente al pseudolegendario principal de Gen 1.
   * *Recomendación:* Clasificar a Dratini en Tier Pseudolegendario (`weight` efectivo reducido drásticamente mediante multiplicador `0.20`, y bucket `rare`/`ultra-rare`).

2. **Dragonair / Dragonite:**
   * *Dragonair:* `uncommon` con peso **14.4** en océanos.
   * *Dragonite:* `uncommon` peso **1.6** y `ultra-rare` peso **0.4** en cielo.
   * *Impacto:* Dragonite salvaje accesible prematuramente en zonas de altura.

3. **Iniciales Gen 1 y Gen 2 (Bulbasaur, Charmander, Squirtle, Chikorita, Cyndaquil, Totodile):**
   * *Bucket actual:* `ultra-rare`.
   * *Peso actual:* **6.0** en biomas templados, taigas, colinas y pantanos.
   * *Impacto:* Debido al intervalo agresivo de 20 ticks (1s), el bucket `ultra-rare` se evalúa cientos de veces por hora, haciendo que los iniciales salvajes se consigan en las primeras 2 horas de juego libre.
   * *Recomendación:* Reducción mediante multiplicador de iniciales (`starter_multiplier = 0.35`).

4. **Chansey y Blissey:**
   * *Chansey:* `rare`, peso **1.125** en bosques.
   * *Blissey:* `rare`, peso **0.125**.
   * *Explotación combinada:* Aunque el spawn natural es moderado, los jugadores capturan una sola Chansey/Blissey o la crían, la suben a nivel alto, y la explotan en batallas PvP por turnos para generar millones de XP.

5. **Snorlax:**
   * *Bucket:* `rare`, peso **2.5** en bosques nevados y colinas.
   * *Impacto:* Fácil de localizar con minimapas o vuelo.

6. **Fósiles (Omanyte, Kabuto, Aerodactyl):**
   * Tasa de Shiny en máquina de fósiles es de **1 en 100** (excesivamente inflada en el config default de Cobblemon).

---

## 3. AUDITORÍA DE LEGENDARIOS Y MÍTICOS (GEN 1 & 2)

* **Especies:** Articuno, Zapdos, Moltres, Mewtwo, Mew, Raikou, Entei, Suicune, Lugia, Ho-oh, Celebi.
* **Resultado del Análisis:** **NINGÚN legendario tiene spawns naturales en los archivos base de Cobblemon.**
* **Canales de Obtención Reales:**
  1. No spawnean por el mundo como mobs ordinarios.
  2. Solo pueden introducirse mediante: Cajas/Crates, Pase de Batalla, Tienda/Tebex, Misiones/Quests endgame, o Eventos Administrativos.
* **Decisión de Diseño Endurecida:**
  * Mantener 0% spawn natural deambulatorio.
  * Formato Endgame obligatorio:
    $$\text{Requisitos (Licencia Oro/Maestro)} \longrightarrow \text{Misiones de Expedición} \longrightarrow \text{Fragmentos de Santuario} \longrightarrow \text{Jefe de Incursión} \longrightarrow \text{Combate Capturable}$$
  * Toda aparición legendaria se registra en telemetría de auditoría:
    `[LEGENDARY_EVENT] player=<uuid> species=<spec> source=<src> dim=<dim> pos=<x,y,z> time=<timestamp>`

---

## 4. SISTEMA DE TIERS Y MULTIPLICADORES PROPUESTO

Se implementa una configuración centralizada desacoplada (`progression_balance.json`):

| Tier | Categoría | Multiplicador Propuesto | Multiplicador Anterior | Impacto Esperado |
|---|---|---|---|---|
| **Tier A** | Comunes (Pidgey, Rattata, Caterpie, etc.) | **1.00** (100%) | 1.00 | Preserva la vida ambiental del mundo. |
| **Tier B** | Poco Comunes (Poliwag, Oddish, Geodude, etc.) | **0.90** (90%) | 1.00 | Ajuste sutil del ritmo. |
| **Tier C** | Raros (Lapras, Snorlax, Porygon, Scyther, Pinsir) | **0.65** (65%) | 1.00 | Requiere exploración metódica. |
| **Tier D** | Iniciales (Starters Gen 1 & 2) | **0.35** (35%) | 1.00 | Eleva el valor de la elección inicial. |
| **Tier E** | Pseudolegendarios (Dratini, Larvitar) | **0.20** (20%) | 1.00 (Océano 84.0!) | Elimina la inflación masiva de Dragonite/Tyranitar. |
| **Tier S** | Legendarios / Míticos | **0.05** (5%) | 0.00 natural | Solo mediante eventos e instancias especiales. |

---

## 5. ACCIONES TÉCNICAS INMEDIATAS (P0)

1. Crear `ProgressionBalanceConfig.java` para gobernar multiplicadores de fauna centralizadamente con soporte de recarga en caliente (`/luna reload`).
2. Interceptar `CobblemonEvents.POKEMON_ENTITY_SPAWN` en `FaunaControl` aplicando los multiplicadores de tier probabilísticos antes del nacimiento de la entidad.
3. Tratamiento especial de anulación para el pool anómalo de Dratini en océanos.
4. Reducir la probabilidad de shiny en fósiles de `100` a `4096` en la configuración del servidor.
5. Telemetría de nacimientos y capturas por tier en SQLite/MariaDB.
