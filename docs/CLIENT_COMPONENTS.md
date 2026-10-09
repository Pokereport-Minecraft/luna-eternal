# Inventario de Componentes del Cliente (SBOM)

Este documento detalla el inventario completo de componentes integrados en el cliente de **PokeReport Network** (Versión Base `0.2.0`, Client Bundle `0.1.0`).

---

## 1. Núcleo del Sistema

- **Motor**: Minecraft 1.21.1
- **Cargador**: Fabric Loader 0.19.5
- **Mapeos**: Yarn 1.21.1+build.3
- **JVM Objetivo**: Java 21 LTS
- **Protocolo de Red Propietario**: Protocolo 3 (`net.pokereport.luna.puerta.Puerta`)

---

## 2. Componentes Administrados (Client Bundle)

| ID | Ruta | Tamaño | SHA-256 | Origen |
|---|---|---|---|---|
| `lunaeternal` | `mods/lunaeternal-0.1.0.jar` | 108,526,702 B | `b03fbf048c35634cb3781f0c277c7cb83392c82a6d9da19faa6337e9a6cced05` | Compilación propia (`mod/`) |
| `lunaneon` | `mods/lunaneon-0.1.0.jar` | 5,843,451 B | `314325ef2a2eb0713d2a5a9cdb57f28479a781c65d158e0c52bb8d299bbca4f2` | Compilación propia (`neon/`) |
| `armaduras_kits` | `mods/armaduraspokereport-kits-completo-1.0.0.jar` | 674,002 B | `3a645733f5c030857ff91162db864b0a3c008e6d1e35f787bd71f38c4b2cd24d` | Compilación propia (`tools/armaduras_export/`) |
| `cobblemon_cards` | `mods/cobblemon-cards-1.0.5-luna1.jar` | 23,519,511 B | `4102963756cf9e24e077a64e6c860a300001cdcacec71ef1830282e481e4ccfe` | Fork interno |
| `cobblemon_battle_positions` | `mods/cobblemon-battle-positions-1.1.3.jar` | 58,572 B | `d76bbd753763dde4a552d1a86b8bc6ceee9abcb00fa1bd29b6c2e389079437ed` | Compilación propia |
| `cobblemon_additions` | `mods/cobblemon-additions-4.3.0-luna1.jar` | 605,112 B | `f19d78b8449beff176e7b6a6e326f1244c568c6bcb32c69625df02523d62d9f6` | Compilación propia |
| `magikarp_armor` | `mods/magikarp-animated-armor-1.0.0.jar` | 44,336 B | `19f42fd107a8f1a1b41e67f14844ca4ec8874a0fbf2f66e314cbd0f0ccd438a3` | Compilación propia |
| `pikachu_armor` | `mods/pikachu-animated-armor-1.0.0.jar` | 150,367 B | `c25730d669b8233aad8870d0f9603f542b44ef17fca174e4c3b0ea18232d98e3` | Compilación propia |
| `eeveelution_armor` | `mods/eeveelution-armor-1.0.0.jar` | 319,784 B | `a6c04c0ecf27e149e94c19d386aeb3374c435a39406a1d203cafce3673537638` | Compilación propia |
| `config_cobblemon_cards` | `config/cobblemon-cards.json` | Variable | Variable | Configuración administrada |

---

## 3. Resumen de Exclusiones Estrictas (Server-Only)

Los siguientes componentes del servidor fueron auditados y confirmados como **100% ausentes** en la distribución del cliente:
- `spark`
- `easyauth`
- `claimblocks`
- `geyser`
- `floodgate`
- `luckperms`
- `sqlite` / `mariadb` / `hikaricp`
- `bce`
- `rcon`
- Archivos `.db`, `.sqlite`, `.env`, tokens y claves privadas.
