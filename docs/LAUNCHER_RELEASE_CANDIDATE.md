# POKEREPORT NETWORK — LAUNCHER RELEASE CANDIDATE MANIFEST

**Fecha de Generación:** 08/10/2026  
**Versión del Bundle:** `0.1.0`  
**Protocolo:** `3`  
**Schema Version:** `2`  
**Ubicación Staging:** `build/release-candidate/client/client-latest.json`  
**Firma Ed25519:** `build/release-candidate/client/client-latest.json.sig`  
**Estado:** `STAGED — NOT PUBLISHED TO PRODUCTION ENDPOINTS`

---

## 1. RESUMEN CRIPTOGRÁFICO Y MANIFIESTO

El archivo de manifiesto `client-latest.json` define los módulos atómicos distribuidos dinámicamente por el Launcher oficial de PokeReport.

### Hashes y Metadatos:
- **Tamaño del Manifiesto:** `4,922 bytes`
- **SHA-256 del Manifiesto:** `d878d1c257fdcb424bd6ea5312969b3e9be5ddfe57b04ce91994c95f8b514579`
- **Firma Ed25519:** Archivo binario de 64 bytes (`client-latest.json.sig`).
- **Verificación Criptográfica:**  
  Ejecutada exitosamente contra la clave pública oficial:  
  `tools/keys/pokereport_release_ed25519_public.der` -> `VERIFIED: Signature is VALID`.

---

## 2. LISTA DE MÓDULOS GESTIONADOS (MANAGED JARS)

| Mod ID | Ruta de Destino | Tamaño (bytes) | Hash SHA-256 |
| :--- | :--- | :--- | :--- |
| **lunaeternal** | `mods/lunaeternal-0.1.0.jar` | 108,689,445 | `b16a8bef26b4cb3844ca0f8afb54015a27979c67c030691908a872c5e7efd250` |
| **lunaneon** | `mods/lunaneon-0.1.0.jar` | 5,843,451 | `314325ef2a2eb0713d2a5a9cdb57f28479a781c65d158e0c52bb8d299bbca4f2` |
| **cobblemon_cards** | `mods/cobblemon-cards-1.0.5-luna1.jar` | 23,519,511 | `4102963756cf9e24e077a64e6c860a300001cdcacec71ef1830282e481e4ccfe` |
| **cobblemon_battle_positions** | `mods/cobblemon-battle-positions-1.1.3.jar` | 58,572 | `d76bbd753763dde4a552d1a86b8bc6ceee9abcb00fa1bd29b6c2e389079437ed` |
| **cobblemon_additions** | `mods/cobblemon-additions-4.3.0-luna1.jar` | 605,112 | `f19d78b8449beff176e7b6a6e326f1244c568c6bcb32c69625df02523d62d9f6` |
| **armaduras_kits** | `mods/armaduraspokereport-kits-completo-1.0.0.jar` | 674,002 | `3a645733f5c030857ff91162db864b0a3c008e6d1e35f787bd71f38c4b2cd24d` |
| **magikarp_armor** | `mods/magikarp-animated-armor-1.0.0.jar` | 44,336 | `19f42fd107a8f1a1b41e67f14844ca4ec8874a0fbf2f66e314cbd0f0ccd438a3` |
| **pikachu_armor** | `mods/pikachu-animated-armor-1.0.0.jar` | 150,367 | `c25730d669b8233aad8870d0f9603f542b44ef17fca174e4c3b0ea18232d98e3` |
| **eeveelution_armor** | `mods/eeveelution-armor-1.0.0.jar` | 838,349 | `95dd5d5bf0e854999f848c0827ea8061e389e1a90f13e7ae9b990e66db9f9aa4` |
| **config_cobblemon_cards** | `config/cobblemon-cards.json` | 2,266 | `2266d59eb4f4cbcefe6b1b2685718ebae7f4bcff6fa34d588daaf0f9c21ca947` |

---

## 3. AISLAMIENTO DE PRODUCCIÓN (SEGURIDAD)

De acuerdo con las directivas del propietario:
- ❌ **NO se ha subido a GitHub Releases.**
- ❌ **NO se ha subido a ningún bucket S3 ni CDN.**
- ❌ **NO se ha modificado el endpoint del servidor web de producción.**
- Los archivos descansan exclusivamente en el directorio local de staging:
  `build/release-candidate/client/`
