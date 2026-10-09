# POKEREPORT NETWORK — AUDITORÍA DEL REGISTRO DE GIMNASIOS Y MUNDOS

**Fecha de corte:** 08/10/2026  
**Versión:** LunaEternal 0.1.0  
**Minecraft:** 1.21.1 | **Fabric** | **Cobblemon:** 1.8.0+1.21.1 | **RCT Mod:** 0.19.0-beta  
**Estado de despliegue:** WORKTREE AISLADO / STAGING (NO DEPLOY)

---

## 1. RESUMEN ARQUITECTÓNICO

El sistema de gimnasios de PokeReport Network implementa un modelo de instancias por ranuras para soportar combates concurrentes de múltiples jugadores sin colisiones espaciales ni interferencia mutua.

### Parámetros de Instanciación
* **Ranura 0:** Maestro / Plantilla original. Nunca se juega en ella; reservada para mantenimiento y pegado de esquemas.
* **Ranuras 1–7:** Instancias clonadas dinámicamente para los retadores.
* **Separación entre Gimnasios en X:** `SEPARACION = 1024` bloques.
* **Paso entre Ranuras en Z:**
  * Gimnasios estándar (Kanto, Johto excepto Morti): `128` bloques.
  * Gimnasios de gran envergadura (Liga Naranja, Morti): `512` bloques.

---

## 2. MATRIZ MAESTRA DE LOS 23 GIMNASIOS Y RETOS

| # | ID | Líder | Región | Dimensión | RCT Trainer ID | Bit | Medalla / Trofeo | Nivel | Pericia IA | Paso Z |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | `brock` | Brock | KANTO | `lunaeternal:gimnasios` | `kanto_brock` | 0 | Roca (`kanto_boulder_badge`) | 15 | 3 | 128 |
| 2 | `misty` | Misty | KANTO | `lunaeternal:gimnasios` | `kanto_misty` | 1 | Cascada (`kanto_cascade_badge`) | 19 | 3 | 128 |
| 3 | `surge` | Teniente Surge | KANTO | `lunaeternal:gimnasios` | `kanto_ltsurge` | 2 | Trueno (`kanto_thunder_badge`) | 24 | 3 | 128 |
| 4 | `erika` | Erika | KANTO | `lunaeternal:gimnasios` | `kanto_erika` | 3 | Arcoíris (`kanto_rainbow_badge`) | 28 | 4 | 128 |
| 5 | `koga` | Koga | KANTO | `lunaeternal:gimnasios` | `kanto_koga` | 4 | Alma (`kanto_soul_badge`) | 33 | 4 | 128 |
| 6 | `sabrina` | Sabrina | KANTO | `lunaeternal:gimnasios` | `kanto_sabrina` | 5 | Pantano (`kanto_marsh_badge`) | 37 | 4 | 128 |
| 7 | `blaine` | Blaine | KANTO | `lunaeternal:gimnasios` | `kanto_blaine` | 6 | Volcán (`kanto_volcano_badge`) | 42 | 4 | 128 |
| 8 | `giovanni` | Giovanni | KANTO | `lunaeternal:gimnasios` | `kanto_giovanni` | 7 | Tierra (`kanto_earth_badge`) | 46 | 5 | 128 |
| 9 | `cissy` | Cissy | NARANJA | `lunaeternal:islas_naranja` | `naranja_cissy` | 8 | Ojo de Coral (`naranja_ojo_coral`) | 49 | 4 | 512 |
| 10 | `danny` | Danny | NARANJA | `lunaeternal:islas_naranja` | `naranja_danny` | 9 | Rubí Marino (`naranja_rubi_marino`) | 52 | 4 | 512 |
| 11 | `rudy` | Rudy | NARANJA | `lunaeternal:islas_naranja` | `naranja_rudy` | 10 | Caracol (`naranja_caracol`) | 55 | 4 | 512 |
| 12 | `luana` | Luana | NARANJA | `lunaeternal:islas_naranja` | `naranja_luana` | 11 | Estrella de Jade (`naranja_jade`) | 58 | 5 | 512 |
| 13 | `drake` | Drake | NARANJA | `lunaeternal:islas_naranja` | `naranja_drake` | 12 | Liga Naranja (`naranja_trofeo`) | 62 | 5 | 512 |
| 14 | `campeon_kanto` | Blue | KANTO | `lunaeternal:gimnasios` | `kanto_champion_blue` | 13 | Campeón de Kanto (`kanto_league_trophy`) | 63 | 5 | 128 |
| 15 | `pegaso` | Pegaso | JOHTO | `lunaeternal:gimnasios` | `johto_valerio` | 14 | Céfiro (`johto_zephyr_badge`) | 64 | 4 | 128 |
| 16 | `anton` | Antón | JOHTO | `lunaeternal:gimnasios` | `johto_raffaello` | 15 | Colmena (`johto_hive_badge`) | 68 | 4 | 128 |
| 17 | `blanca` | Blanca | JOHTO | `lunaeternal:gimnasios` | `johto_chiara` | 16 | Llanura (`johto_plain_badge`) | 71 | 4 | 128 |
| 18 | `morti` | Morti | JOHTO | `lunaeternal:gimnasio_morti` | `johto_angelo` | 17 | Niebla (`johto_fog_badge`) | 75 | 4 | 512 |
| 19 | `anibal` | Aníbal | JOHTO | `lunaeternal:gimnasios` | `johto_furio` | 18 | Tormenta (`johto_storm_badge`) | 79 | 5 | 128 |
| 20 | `yasmina` | Yasmina | JOHTO | `lunaeternal:gimnasios` | `johto_jasmine` | 19 | Mineral (`johto_mineral_badge`) | 82 | 5 | 128 |
| 21 | `fredo` | Fredo | JOHTO | `lunaeternal:gimnasios` | `johto_alfredo` | 20 | Glaciar (`johto_glacier_badge`) | 86 | 5 | 128 |
| 22 | `debora` | Débora | JOHTO | `lunaeternal:gimnasios` | `johto_sandra` | 21 | Alzamiento (`johto_rising_badge`) | 90 | 5 | 128 |
| 23 | `campeon_johto` | Lance | JOHTO | `lunaeternal:gimnasios` | `johto_champion_lance` | 22 | Campeón de Johto (`johto_league_trophy`) | 100 | 5 | 128 |

---

## 3. COORDENADAS BASE Y FORMULACIÓN POR INSTANCIA

Para cualquier ranura $s \in [0, 7]$:

$$\text{Origen}_X = \text{salaEnDimension}(g) \times 1024$$
$$\text{Origen}_Y = 64$$
$$\text{Origen}_Z = s \times \text{pasoRanura}(g)$$

$$\text{Entrada} = (\text{Origen}_X + \Delta X_{\text{ent}}, \text{Origen}_Y + \Delta Y_{\text{ent}}, \text{Origen}_Z + \Delta Z_{\text{ent}})$$
$$\text{Líder} = (\text{Origen}_X + \Delta X_{\text{lid}}, \text{Origen}_Y + \Delta Y_{\text{lid}}, \text{Origen}_Z + \Delta Z_{\text{lid}})$$

### Detalle de Desfases Clave (Maestro):
* **Brock (`sala 0`):**
  * Entrada: $(48.0, 78.0, 17.26)$, Yaw: $0^\circ$
  * Líder: $(48.0, 72.0, 40.45)$, Yaw: $180^\circ$
* **Misty (`sala 1`):**
  * Entrada: $(1115.42, 91.0, 5.50)$, Yaw: $90^\circ$
  * Líder: $(1055.62, 71.0, 57.55)$, Yaw: $-90^\circ$
* **Surge (`sala 2`):**
  * Entrada: $(2055.459, 68.0, 90.470)$, Yaw: $-90^\circ$
  * Líder: $(2093.501, 68.0, 55.536)$, Yaw: $180^\circ$
* **Sabrina (`sala 5`):**
  * Entrada: $(5189.947, 95.0, 90.780 + s \cdot 128)$, Yaw: $180^\circ$
  * Líder: $(5161.603, 71.0, 52.524 + s \cdot 128)$, Yaw: $180^\circ$
* **Blue (`sala 13`):**
  * Entrada: $(13357.972, 68.0, 8.653 + s \cdot 128)$, Yaw: $0^\circ$
  * Líder: $(13358.023, 68.0, 54.711 + s \cdot 128)$, Yaw: $180^\circ$
* **Morti (`sala 17`, Dimensión Nocturna):**
  * Entrada: $(17409.456, 66.0, 35.714 + s \cdot 512)$, Yaw: $180^\circ$
  * Líder: $(17409.420, 78.0, -32.280 + s \cdot 512)$, Yaw: $0^\circ$
