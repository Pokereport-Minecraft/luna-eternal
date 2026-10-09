# SERVER-SIDE MODSET ROADMAP & DEPLOYMENT ARCHITECTURE

## 1. Target Server Runtime Environment
- **Platform**: Ubuntu 24.04 LTS / Pterodactyl Container
- **Java**: Eclipse Temurin JDK 21.0.4+7 (ZGC Generational enabled)
- **Fabric Loader**: Runtime `0.18.4` (Bootstrap `0.16.9` compatible)
- **Fabric API**: `0.116.15+1.21.1`
- **Cobblemon**: `1.8.0+1.21.1`
- **RCT Engine**: `rct-0.19.0-beta.jar` + `rctapi-0.16.0-beta.jar`
- **Core Orchestrator**: `lunaeternal-0.1.0.jar` (Staged Artifact SHA-256: `7DAF6E8988BD3FE5287A90905E4759E8A6E1D598EDED2834F8D51A67ADEFBDE4`)

---

## 2. Server Optimization & Hardening Stack

```text
Server Environment Architecture
├── Core Simulation
│   ├── Minecraft 1.21.1 (Dedicated Server)
│   ├── Lithium 0.15.3 (Physics, chunk ticking, AI pathfinding optimizations)
│   ├── FerriteCore 7.0.3 (Blockstate & collision box memory deduplication)
│   └── ModernFix 5.25.1 (Classloading optimization, dynamic memory leak fixes)
├── Pokémon & Combat Layer
│   ├── Cobblemon 1.8.0 (Showdown subprocess, spawning logic)
│   ├── RCT 0.19.0-beta (Trainer entities, dialogue trees)
│   └── LunaEternal 0.1.0 (RCT mixin hardening, GymTeamValidator, BattleDiagnostics)
└── Network & Telemetry Layer
    ├── Krypton 0.2.8 (Netty pipeline optimization)
    ├── SanctuaryImagePipeline (SSRF protection & redirect isolation)
    └── MariaDB Pool (HikariCP, Schema 59)
```

---

## 3. Database & Persistence Layer Status
- **Database Name**: `s11945_luna`
- **Current Schema Version**: **59**
- **Pending Migrations**: **0** (The P0 trainer battle fixes, mixins, SSRF guards, and adaptive gym bounds operate entirely in memory/bytecode and do not alter MariaDB relational tables).
- **Production Safety**: Zero DDL or DML statements required for this hotfix.

---

## 4. Server Deployment Procedure (When Authorized)
When owner authorization is granted:
1. Execute MariaDB snapshot: `mysqldump s11945_luna > backup_pre_hotfix.sql`.
2. Push `lunaeternal-0.1.0.jar` (`7DAF6E...`) to remote `/mods/` directory.
3. Remove old unpatched `lunaeternal-0.1.0.jar`.
4. Gracefully restart server via Pterodactyl panel.
5. Tail console output: verify mixin application, verify schema 59 confirmation, verify `BattleDiagnostics` startup.
6. Run live battle check on Brock (`/warp gimnasio_roca`).
