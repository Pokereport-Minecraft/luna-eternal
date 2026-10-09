# CONFIGURATION AUDIT & COMPARISON: DIOSÉSMON VS POKEREPORT

## 1. Executive Configuration Scope
This document compares configuration structures across:
- **Client Render Engine**: Sodium, Iris, ImmediatelyFast, ModernFix, FerriteCore.
- **Cobblemon Engine**: `cobblemon.json` (spawns, capture rates, battle anims, battle camera).
- **HUD & Utilities**: Jade, Xaero's Minimap, Xaero's WorldMap, Chat Heads, ImmersiveUI.

---

## 2. Core Config Matrix

| Category | Parameter | Diosésemon Configuration | PokeReport Current | Recommendation for Next Client |
|---|---|---|---|---|
| **Render Engine (Sodium)** | Chunk Update Threads | 4 (aggressive) | Default (2) | Adopt 4 threads for faster chunk load during riding/flying |
| **Render Engine (Sodium)** | Quality Preset | Medium / Balanced | High | Maintain High for aesthetics, optimize particle limits |
| **ImmediatelyFast** | Fast HUD / Fast Text | Enabled (`true`) | Enabled (`true`) | Keep Enabled (30% FPS uplift on complex scoreboard/dialogues) |
| **FerriteCore** | Deduplicate Strings/Predicates | `true` | `true` | Retain (saves ~450MB heap memory) |
| **Cobblemon Client** | Battle Camera Movement | Dynamic Follow | Dynamic Follow | Retain |
| **Cobblemon Spawning** | Max Wild Pokémon / Player | 28 (Aggressive) | 20 (Hardened Progression) | **Retain PokeReport 20** to protect server tick budget and rarity balance |
| **Xaero's Minimap** | Entity Radar Display | Enabled for wild Pokémon | Disabled in PvP/Gyms | Enforce radar restrictions in gym instances to prevent scouting |
| **Jade HUD** | Pokémon Health / Level Preview | Visible on hover | Visible on hover | Retain (Crucial QoL for players) |
| **Chat Heads** | Avatar Size & Padding | 8px, margin 2px | Default Minecraft chat | Adopt Chat Heads config for modern social immersion |

---

## 3. Server-Client Synchronization Discrepancies
1. **Cobblemon Versioning**: Diosésemon configs contain deprecated fields specific to 1.7.3 (`experimental_spawning_rules`) which are invalid in Cobblemon 1.8.0.
2. **Network Packets**: PokeReport enforces strict anti-spoof packet rate limiting in `LunaEternal` network listeners. Diosésemon relies primarily on vanilla Fabric networking defaults.
3. **Verdict**: Do NOT copy raw config directories from Diosésemon. Curate and backport only visual client options (Chat Heads, Sodium threads, Jade layout).
