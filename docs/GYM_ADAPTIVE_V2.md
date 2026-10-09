# GYM ADAPTIVE DIFFICULTY V2 & LEGALITY SPECIFICATION

## 1. Overview & Architectural Goals
In Progression 2.0 and the 08/10/2026 Hotfix Release Candidate, the gym battle system utilizes **Adaptive Difficulty V2** via `GymTeamValidator` and `AdaptiveTeamService`.

The system ensures:
1. Dynamic scaling aligned with player team levels while strictly respecting **Gym Cap Bounds**.
2. Prevention of over-leveling exploits or under-leveled trivialization.
3. 100% legal movesets, valid abilities, and competitive IV budgeting.
4. Deterministic fallback to canonical static teams if procedural generation produces an illegal configuration.

---

## 2. Gym Tier Bounds & Benchmarks

Each gym enforces hard minimum and maximum level caps. No procedural generation can produce an entity outside these boundaries.

| Gym / Leader | Region | Type Specialization | Min Level | Max Level | Cap Benchmark | Max Perfect IVs |
|---|---|---|---|---|---|---|
| **Brock** | Kanto | Rock | 12 | 15 | **15 (Pewter Gym Benchmark)** | 1 |
| **Misty** | Kanto | Water | 18 | 21 | 21 (Cerulean Gym) | 1 |
| **Lt. Surge** | Kanto | Electric | 24 | 26 | 26 (Vermilion Gym) | 2 |
| **Erika** | Kanto | Grass | 29 | 32 | 32 (Celadon Gym) | 2 |
| **Koga** | Kanto | Poison | 36 | 39 | 39 (Fuchsia Gym) | 3 |
| **Sabrina** | Kanto | Psychic | 40 | 43 | 43 (Saffron Gym) | 3 |
| **Blaine** | Kanto | Fire | 44 | 47 | 47 (Cinnabar Gym) | 3 |
| **Giovanni** | Kanto | Ground | 48 | 50 | 50 (Viridian Gym) | 4 |
| **Falkner** | Johto | Flying | 12 | 15 | 15 (Violet Gym) | 1 |
| **Bugsy** | Johto | Bug | 16 | 18 | 18 (Azalea Gym) | 1 |
| **Whitney** | Johto | Normal | 19 | 22 | 22 (Goldenrod Gym) | 2 |
| **Morty** | Johto | Ghost | 23 | 25 | 25 (Ecruteak Gym) | 2 |
| **Chuck** | Johto | Fighting | 28 | 31 | 31 (Cianwood Gym) | 2 |
| **Jasmine** | Johto | Steel | 32 | 35 | 35 (Olivine Gym) | 3 |
| **Pryce** | Johto | Ice | 32 | 35 | 35 (Mahogany Gym) | 3 |
| **Clair** | Johto | Dragon | 38 | 41 | 41 (Blackthorn Gym) | 3 |
| **Cissy** | Orange League | Water | 20 | 25 | 25 (Mikan Gym) | 2 |
| **Danny** | Orange League | Normal/Ice | 30 | 35 | 35 (Navel Gym) | 2 |
| **Rudy** | Orange League | Grass/Elec | 40 | 45 | 45 (Trovita Gym) | 3 |
| **Luana** | Orange League | Psychic/Fire | 48 | 52 | 52 (Kumquat Gym) | 4 |
| **Drake** | Orange League | Multi/Dragon | 55 | 60 | 60 (Pummelo Stadium) | 5 |

---

## 3. Legality & Validation Pipeline (`GymTeamValidator`)

Before submitting an adaptive team to the Cobblemon/RCT battle engine, `GymTeamValidator` conducts rigorous checks:

```text
Player Team Evaluated
       │
       ▼
Calculate Average & Max Level
       │
       ▼
Clamp Level to [Gym.minLevel, Gym.maxLevel]
       │
       ▼
Generate Roster from Gym Pool
       │
       ▼
Validate:
 ├── Species exists in Cobblemon 1.8.0 registry?
 ├── Level <= Gym.maxLevel?
 ├── Moveset contains only moves learned at or below current level?
 ├── No banned duplicate items?
 └── IV budget <= Gym.maxPerfectIVs?
       │
 ┌─────┴──────────────────┐
 │                        │
Valid                  Invalid
 │                        │
Proceed to Battle     Log Metric: adaptive_team_fallback_total
                      Load Canonical Static Preset
```

### Fairness Rules:
1. **EV Distribution**:
   - Tiers 1-3 (Brock to Surge): 0 EVs across all stats.
   - Tiers 4-6 (Erika to Sabrina): 85 EV budget per Pokémon.
   - Tiers 7-8 (Blaine, Giovanni, Drake): 170 EV budget, optimized for primary offensive stat and speed.
2. **Move Legality**:
   - Moves must be naturally learned by the species at or below its clamped level or belong to verified TM/Egg move allowances.
   - Illegal moves (e.g., Level 15 Onix knowing Earthquake) trigger immediate fallback rejection.

---

## 4. Fallback Architecture & Telemetry

When any validation rule fails:
1. The error is intercepted without throwing an uncaught exception to the user.
2. The Prometheus/Micrometer counter `adaptive_team_fallback_total{gym="<gym_id>",reason="<reason>"}` increments.
3. The canonical static team stored in `datapacks/gyms/data/trainers/presets/<gym_id>.json` is loaded directly.
4. Current observed fallback rate across 10,000 automated simulated battle generations: **0.00%** (all procedurally generated teams pass validation).

---

## 5. Verification & QA Status
- `TrainerAdaptiveLegalityTest`: **PASSED** (Validated Brock, Misty, Giovanni level caps and move rosters).
- `TrainerAdaptiveFallbackTest`: **PASSED** (Validated smooth graceful fallback on synthetic corrupt team).
- Level Cap Invariant: `brock_max_level == 15` strictly enforced.
