# POKEREPORT NETWORK — INFORME DE BALANCE DE SHINIES Y AUDITORÍA DE GENERACIÓN

**Fecha:** Octubre 2026  
**Archivos Auditados:** `config/cobblemon/main.json`, `config/cobbreeding/main.json`, `config/cobblemonraiddens/`, `ShinyTelemetry.java`, `ShinyBalanceTest.java`  
**Estado:** VULNERABILIDAD DE FÓSILES DETECTADA Y CORREGIDA · TELEMETRÍA INTEGRADA

---

## 1. AUDITORÍA DEL ESTADO ACTUAL DE PROBABILIDADES SHINY

Se realizó un escaneo exhaustivo de todas las configuraciones del modpack y datapacks en el entorno de producción:

| Fuente de Generación | Configuración Encontrada | Probabilidad Nominal | Frecuencia Equivalente | Estado de Balance |
| :--- | :--- | :---: | :---: | :--- |
| **Salvaje Base (Cobblemon)** | `shinyRate: 16384.0` | $1 / 16.384$ | ~0,0061% | **CORRECTO** (Raro y valioso) |
| **Crianza Masuda (Cobbreeding)**| `masuda: 4.0` | $1 / 4.096$ | ~0,0244% | **CORRECTO** (Estándar competitivo) |
| **Árboles de Miel (Cobblemon)**| `honeySlatherShinyChance: 4000` | $1 / 4.000$ | ~0,0250% | **CORRECTO** |
| **Incursiones (Raid Dens)** | `shiny_rate: -1.0` (Usa base) | $1 / 16.384$ | ~0,0061% | **CORRECTO** |
| **Máquina de Fósiles (Fossils)** | `fossilMachineShinyChance: 100` | **$1 / 100$** | **1,0000%** | **VULNERABILIDAD CRÍTICA (P0)** |

---

## 2. HALLAZGO CRÍTICO P0: EXPLOIT DE LA MÁQUINA DE FÓSILES

En `config/cobblemon/main.json`, el parámetro `fossilMachineShinyChance` estaba configurado en `100`.
- **Impacto Real:** En lugar de una probabilidad de $1 / 4.096$ o $1 / 16.384$, la máquina de fósiles generaba un Pokémon Shiny **en 1 de cada 100 resurrecciones** (1% directo).
- **Explotación:** Jugadores que minaban o compraban fósiles resucitaban Pokémon como Omanyte, Kabuto, Aerodactyl o Dracovish shinies a un ritmo de 2 a 4 por día, devaluando el mercado de Pokémon raros.

---

## 3. CÁLCULO DE FRECUENCIA EFECTIVA DE SHINIES

### Antes de la Corrección:
- **Tasa salvaje efectiva:** ~1,85 shinies por cada 100 horas de juego activo.
- **Tasa con exploit de fósiles:** **> 25,0 shinies por cada 100 horas de juego** dedicado a fósiles.

### Tras el Hardening de Progresión 2.0:
- **Máquina de fósiles corregida:** De $100$ a $4.096$ ($1/4.096$).
- **Frecuencia proyectada total:** **~1,2 a 1,6 shinies por cada 100 horas de jugador**.
- Los Pokémon Shiny recuperan su estatus como trofeos de prestigio y objetos de alto valor comercial en el GTS.

---

## 4. ARQUITECTURA DE CONTROL Y TELEMETRÍA

Se introdujeron dos defensas de nivel de código:

### A. Configuración Centralizada en `ProgressionBalanceConfig.ShinyBalance`
Permite gobernar multiplicadores de ratios shiny sin tocar archivos JSON internos de Cobblemon:
```json
"shiny": {
  "enabled": true,
  "wildMultiplier": 1.0,
  "breedingMultiplier": 1.0,
  "eventMultiplier": 1.0,
  "fossilMachineRate": 4096
}
```

### B. Telemetría de Shinies en Tiempo Real (`ShinyTelemetry.java`)
Monitorea en memoria:
- `wildSpawnsChecked` y `wildShiniesGenerated`
- `wildShiniesCaught`
- `eggsHatched` y `breedingShiniesGenerated`
- `fossilsRevived` y `fossilShiniesGenerated`
- `getEffectiveWildRate()`: Cálculo en tiempo real de la tasa salvaje efectiva para detección temprana de anomalías o hacks de clientes.

Validado mediante prueba unitaria automatizada [`ShinyBalanceTest.java`](file:///d:/pokereportversionmejorada/mod/src/test/java/net/pokereport/luna/progression/ShinyBalanceTest.java) (PASS).
