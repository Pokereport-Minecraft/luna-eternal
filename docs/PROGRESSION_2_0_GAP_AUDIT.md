# POKEREPORT NETWORK — AUDITORÍA DIFERENCIAL DE BRECHAS PROGRESSION 2.0

**Fecha:** Octubre 2026  
**Entorno auditado:** Servidor de Producción (`15.235.16.131`, Contenedor `1f23bff0-bc4c-4c00-bbcf-2fe81ff666dd`) & Entorno Local (`D:\pokereportversionmejorada`)  
**Autor:** Auditoría de Arquitectura de Sistemas & Release Engineering  
**Estado de Despliegue:** PRE-DEPLOYMENT GATE · NO DESPLEGADO A PRODUCCIÓN

---

## 1. VERIFICACIÓN DEL RUNTIME REAL EN PRODUCCIÓN

Para evitar discrepancias entre el entorno de desarrollo y el servidor productivo activo, se realizó una inspección forense directa de los binarios, checksums SHA-256 y logs del contenedor de producción:

| Componente | Versión Declarada en Repo | Runtime Real Producción | Checksum SHA-256 Producción | Estado de Compatibilidad |
| :--- | :--- | :--- | :--- | :--- |
| **Minecraft** | `1.21.1` | `1.21.1` | N/A (Vanilla jar) | **COMPATIBLE** |
| **Fabric Loader** | `0.16.5` (baseline) | `0.18.4` | N/A (Fabric meta runtime) | **COMPATIBLE** |
| **Fabric API** | `0.116.15+1.21.1` | `0.116.15+1.21.1` | `a61a10f730ab8aa45ff42486ee65699e6e51c28a0c168deb161e7d4029473aa3` | **COMPATIBLE (MATCH)** |
| **Cobblemon** | `1.8.0+1.21.1` (`YgmyyFcs`) | `1.8.0+1.21.1` | `a6228f3291c70ed6348b9a47beadabc79cb241b6522312e7e2b56d428dc9ec31` | **COMPATIBLE (MATCH)** |
| **RCT (rctmod)**| `0.19.0-beta` | `0.19.0-beta` | `0cad7a16f47b0e58c2aff835d0bc75ef010ade9b1696a383f0f7315438f1ac27` | **COMPATIBLE (MATCH)** |
| **RCT API** | `0.16.0-beta` | `0.16.0-beta` | `2fe8643f28a60f9159138c2515253cc24ac34e04f44d937d52c87e480c688b75` | **COMPATIBLE (MATCH)** |
| **LunaEternal** | `0.1.0` | `0.1.0` | `b03fbf048c35634cb3781f0c277c7cb83392c82a6d9da19faa6337e9a6cced05` | **COMPATIBLE** |

> **Conclusión P0 Runtime:** Producción está corriendo Cobblemon 1.8.0+1.21.1 oficial con Fabric Loader 0.18.4. No existen divergencias de binarios incompatibles entre el cliente compilado y el servidor activo.

---

## 2. HALLAZGO CRÍTICO DE BASE DE DATOS Y RESOLUCIÓN V058 / V059

Durante la inspección de la base de datos de producción (`s11945_luna` en MariaDB), se auditó la tabla de control `version`:

```sql
SELECT version, description, applied_at, migration_file FROM version ORDER BY version DESC LIMIT 5;
```

**Resultado en Producción:**
```text
version: 58
description: confirmed hatch mission history
applied_at: 2026-10-05 14:02:46
migration_file: quest_hatch_event
```

### El Riesgo de Choque de Migraciones
Si hubiésemos aplicado `V058__progression_licenses.sql` sin auditar la base productiva:
1. El guardián de migraciones de `Database.migrate()` compara `version.description` con la descripción del fichero SQL local.
2. Al detectar un desfase (`progression licenses` vs `confirmed hatch mission history`), `Database.java` arroja una excepción fatal inmediata:
   ```text
   CHOQUE DE MIGRACIONES: la versión 58 en base de datos es 'confirmed hatch mission history',
   pero el fichero local dice 'progression licenses'.
   ```
3. El servidor de producción **se habría negado a arrancar**, causando una caída total no programada.

### Resolución Aplicada
1. Se recuperó y formalizó [`V058__quest_hatch_event.sql`](file:///d:/pokereportversionmejorada/mod/src/main/resources/db/migration/V058__quest_hatch_event.sql) preservando la tabla `quest_hatch_event`.
2. Se reubicó la nueva migración de licencias en [`V059__progression_licenses.sql`](file:///d:/pokereportversionmejorada/mod/src/main/resources/db/migration/V059__progression_licenses.sql).
3. Se actualizó el registro secuencial en [`Database.java`](file:///d:/pokereportversionmejorada/mod/src/main/java/net/pokereport/luna/db/Database.java).
4. La prueba unitaria [`DatabaseMigrationTest.java`](file:///d:/pokereportversionmejorada/mod/src/test/java/net/pokereport/luna/db/DatabaseMigrationTest.java) valida las 59 migraciones en orden correlativo estricto sin colisiones (59/59 PASS).

---

## 3. AUDITORÍA DIFERENCIAL DE COMPONENTES DE PROGRESIÓN 2.0

Se respetó la regla absoluta de **no sobrescribir** los componentes existentes, extendiéndolos conforme a las necesidades de hardening:

### A. ExperienceBalanceService & Experiencia
- **Baseline antiguo:** Multiplicador global de 2.0x, subiendo Pokémon a Nivel 100 en apenas 2.5 horas de juego sin esfuerzo.
- **Progression 2.0:**
  - Multiplicador de Pokémon salvajes: `0.60x`.
  - Entrenadores NPC: `1.00x`.
  - Gimnasios oficiales: `1.10x`.
  - Batallas repetitivas contra Blissey (Blissey Boosting): Retornos decrecientes activados tras 3 combates diarios (`blisseyBoostingThreshold = 3`).
  - Límite de nivel de obediencia estricto por medallas de gimnasio (`GymLevelCapService`).

### B. ProgressionBalanceConfig & Control en Caliente
- Archivo de configuración: `config/lunaeternal/progression_balance.json`.
- Kill-switch global: `enabled: true/false`.
- Subsecciones independientes: `spawn`, `shiny`, `honey`, `experience`, `breeding`, `gts`, `economy`, `events`, `alts`.
- Métodos `load(Path)` y `reload()` para recarga en caliente sin reiniciar el servidor.

### C. TrainerLicense & TrainerLicenseService
- Sistema institucional de méritos y sumideros de dinero (PokéDólares):
  - **Novato:** Nv. base, 0 requisitos, 0 coste.
  - **Bronce:** 2 medallas, 15% Dex, 5 misiones, 5.000 PokéDólares.
  - **Plata:** 4 medallas, 30% Dex, 15 misiones, 20.000 PokéDólares.
  - **Oro:** 6 medallas, 50% Dex, 30 misiones, 50.000 PokéDólares.
  - **Maestro:** 8 medallas, 70% Dex, 50 misiones, 150.000 PokéDólares.
  - **Campeón:** 12 medallas, 85% Dex, 75 misiones, 500.000 PokéDólares.
- Sinks totales por jugador para completar licencias: **725.000 PokéDólares**.

### D. Control de Cuentas Secundarias (Alts)
- Implementado en [`AltAccountTelemetry.java`](file:///d:/pokereportversionmejorada/mod/src/main/java/net/pokereport/luna/progression/AltAccountTelemetry.java):
  - Detección de múltiples cuentas en la misma IP (>2 cuentas: +25 pts; >4 cuentas: +50 pts).
  - Monitoreo de embudos de transferencias de dinero (>25k: +15 pts; >100k: +35 pts).
  - Detección de ráfagas de redención de kits (>5 kits: +10 pts; >10 kits: +25 pts).
  - Cálculo de `RiskScore` (0 a 100). Emisión de alertas para el equipo de moderación (`[ALT_ALERT]`) sin autobans que generen falsos positivos en familias.

---

## 4. IMPACTO DEL HARDENING (30 DÍAS)

| Métrica Clave | Perfil F2P Casual (1.5h/d) | Perfil F2P Hardcore (5h/d) | Donador Campeón (3.5h/d) | Donador Leyenda (5h/d) |
| :--- | :--- | :--- | :--- | :--- |
| **Nivel 100 Obtenidos** | 5 (antes 18) | 17 (antes 60) | 17 (antes 42) | 33 (antes 60) |
| **Pokédex Completada** | 61 / 251 | 251 / 251 | 181 / 251 | 251 / 251 |
| **Licencia Alcanzada** | Bronce | Maestro | Maestro | Campeón |
| **Shinies Obtenidos** | 0 (antes 1) | 1 (antes 5) | 1 (antes 3) | 1 (antes 5) |
| **Riqueza Neta Acumulada** | 107.000 PD | 364.000 PD | 253.600 PD | 320.000 PD |

---

## 5. RESUMEN DE COBERTURA Y PRUEBAS

- Total pruebas unitarias ejecutadas: **335**.
- Fallos: **0**.
- Omitidas: **0**.
- Tasa de éxito: **100%**.
