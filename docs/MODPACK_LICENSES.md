# Inventario de Licencias y Procedencia de Mods

Este documento audita y cataloga los componentes externos e internos incluidos en la distribución de **PokeReport Network**, asegurando cumplimiento legal y compatibilidad de licencias.

---

## 1. Componentes Propietarios (PokeReport Network)

| Nombre / Archivo | Identificador | Licencia | Procedencia | Distribución |
|---|---|---|---|---|
| `lunaeternal-0.1.0.jar` | `lunaeternal` | All Rights Reserved (ARR) | Interno (`mod/`) | Client Bundle / Overrides |
| `lunaneon-0.1.0.jar` | `lunaneon` | All Rights Reserved (ARR) | Interno (`neon/`) | Client Bundle / Overrides |
| `armaduraspokereport-kits-completo-1.0.0.jar` | `armaduraspokereport` | All Rights Reserved (ARR) | Interno (`tools/armaduras_export/`) | Client Bundle / Overrides |
| `cobblemon-cards-1.0.5-luna1.jar` | `cobblemon_cards` | MIT / Propietario fork | Fork interno | Client Bundle / Overrides |
| `cobblemon-battle-positions-1.1.3.jar` | `cobblemon_battle_positions` | MIT | Compilación interna | Client Bundle / Overrides |
| `cobblemon-additions-4.3.0-luna1.jar` | `cobblemon_additions` | MIT | Compilación interna | Client Bundle / Overrides |
| `magikarp-animated-armor-1.0.0.jar` | `magikarp_armor` | ARR | Modelado propio | Client Bundle / Overrides |
| `pikachu-animated-armor-1.0.0.jar` | `pikachu_armor` | ARR | Modelado propio | Client Bundle / Overrides |
| `eeveelution-armor-1.0.0.jar` | `eeveelution_armor` | ARR | Modelado propio | Client Bundle / Overrides |

---

## 2. Componentes Principales de Terceros (Modrinth CDN)

Todos los componentes de terceros listados a continuación se descargan **directamente** desde la CDN oficial de Modrinth durante la instalación del modpack, respetando los términos de distribución y atribución de los autores originales.

| Nombre del Mod | ID de Proyecto Modrinth | Licencia | Entorno |
|---|---|---|---|
| **Cobblemon** | `Cobblemon` | GNU AGPLv3 / ARR Assets | Ambos (Cliente/Servidor) |
| **Fabric API** | `fabric-api` | Apache 2.0 | Ambos |
| **Radical Cobblemon Trainers (RCT)** | `rctmod` | MIT | Ambos |
| **RCT API** | `rctapi` | MIT | Ambos |
| **GeckoLib** | `geckolib` | MIT | Ambos |
| **Simple Voice Chat** | `voicechat` | GNU LGPLv3 | Ambos |
| **Sodium** | `sodium` | GNU LGPLv3 | Cliente |
| **Iris Shaders** | `iris` | GNU LGPLv3 | Cliente |
| **FerriteCore** | `ferritecore` | MIT | Ambos |
| **Lithium** | `lithium` | GNU LGPLv3 | Ambos |
| **ImmediatelyFast** | `immediatelyfast` | GNU LGPLv3 | Ambos |
| **ModernFix** | `modernfix` | GNU LGPLv3 | Ambos |
| **Entity Culling** | `entityculling` | MIT | Cliente |
| **Xaero's Minimap** | `xaerominimap` | All Rights Reserved (Modrinth Allowed) | Cliente |
| **Xaero's World Map** | `xaeroworldmap` | All Rights Reserved (Modrinth Allowed) | Cliente |
| **Roughly Enough Items (REI)**| `roughlyenoughitems` | MIT | Ambos |
| **Sound Physics Remastered** | `sound-physics-remastered` | GNU GPLv3 | Ambos |
| **Cloth Config** | `cloth-config` | GNU LGPLv3 | Ambos |
| **Mod Menu** | `modmenu` | MIT | Cliente |
| **Architectury API** | `architectury-api` | GNU LGPLv3 | Ambos |

*(El manifiesto de distribución `PokeReport-Base-0.2.0.mrpack` contiene 148 entradas verificadas referenciadas a través de Modrinth).*
