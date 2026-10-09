# ASSET & RESOURCEPACK COMPARISON: DIOSÉSMON VS POKEREPORT

## 1. Asset Inventory Overview
This forensic comparison examines visual models, textures, animations, and sound assets between:
- **Diosésemon Reference**: `minecraft\resourcepacks\` and internal JAR assets (`Cobblemon 1.7.3`, `diosesmon-1.7.12.jar`).
- **PokeReport Network**: `neon/src/main/resources/resourcepacks/cosmeticos` and `mod/src/main/resources/` (`Cobblemon 1.8.0`).

---

## 2. Model & Texture Format Matrix

| Category | Diosésemon Instance | PokeReport Network | Compatibility / Notes |
|---|---|---|---|
| **Cobblemon Model Schema** | Bedrock Entity JSON 1.12.0 (1.7.3 format) | Bedrock Entity JSON 1.12.0 (1.8.0 format) | 1.8.0 features modernized bone hierarchies, native tail physics, and dynamic eyelid bones. |
| **Animation Engine** | Molang v1.7.3 runtime | Molang v1.8.0 native runtime | 1.8.0 includes smooth transitions for battle entrance, sleep loops, and fainting. |
| **Custom Rank Armor** | External cosmetic datapack | Native 3D Geckolib / Custom Model Data (`cosmeticos`) | PokeReport renders distinct visual 3D sets for VIP, Elite, Champion, and Master. |
| **Poképaradas (Pokéstops)** | Standard custom block texture | Dedicated animated 3D lunar model with spin animations | PokeReport native model verified and functional. |
| **GUI & HUD Elements** | ImmersiveUI + Diosesmon custom theme | Vanilla Cobblemon + Modern Material UI | Diosésemon uses heavy dark-slate textures; PokeReport preserves clarity. |
| **Shaders Supported** | Complementary Reimagined r5.1.1 | Iris Shader Pipeline compatible | Full parity on Iris / Sodium shader pipeline. |

---

## 3. Visual Divergences & Fixes
1. **Gen 2 Sprite/Model Glitches in 1.7.3**:
   - Diosésemon (1.7.3) exhibits unanchored wing textures on Skarmory and disjointed hip bones on Tyranitar during walk cycles.
   - PokeReport (1.8.0) natively incorporates upstream Bedrock geometry fixes for these species.
2. **Shiny Textures**:
   - Both instances support 100% of Gen 1 and Gen 2 shiny variants.
   - PokeReport includes upstream fixes for fossil Pokémon shiny palettes (Omanyte, Kabuto, Aerodactyl).
3. **Sound Assets**:
   - Diosésemon integrates `Presence Footsteps` for terrain-aware audio immersion.
   - Recommended for client pack integration without modifying server-side protocol.

---

## 4. Clean-Room Boundary
Under no circumstances are custom textures, GUI skins, or branding elements from `diosesmon-1.7.12.jar` or Diosesmon-specific resource packs to be imported into PokeReport. All PokeReport assets are developed in-house or sourced from open-source MIT/LGPL upstream repositories.
