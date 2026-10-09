# POKEREPORT NETWORK — PRODUCTION DEPLOYMENT REPORT (PROGRESSION 2.0)

**Fecha:** 8 de Octubre de 2026  
**Hora de Ejecución:** 16:09:43 UTC (11:09:43 UTC-5)  
**Entorno de Producción:** Nodo Principal `15.235.16.131`  
**Contenedor Wings (Pterodactyl):** `1f23bff0-bc4c-4c00-bbcf-2fe81ff666dd`  
**Base de Datos Productiva:** `s11945_luna` (MariaDB 10.11)  
**Estado:** **PRODUCCIÓN ACTIVA — DESPLIEGUE COMPLETADO Y VALIDADO**

---

## 1. RESUMEN EJECUTIVO DEL DESPLIEGUE

Se ha ejecutado el despliegue controlado de **PokeReport Progression 2.0** en el entorno productivo de PokeReport Network bajo la autorización expresa y conforme al protocolo de compuertas de seguridad (Zero-Downtime / Zero Data Loss Policy).

El despliegue no realizó wipe, no alteró inventarios de jugadores, no eliminó Pokémon y preservó al 100% el historial de misiones y la integridad relacional de MariaDB.

---

## 2. ARTEFACTOS Y VERIFICACIÓN DE IDENTIDAD CRIPTOGRÁFICA

| Componente | Archivo / Destino | Tamaño | SHA-256 |
| :--- | :--- | :--- | :--- |
| **Release Candidate (Server)** | `mods/lunaeternal-0.1.0.jar` | 108,598,846 B | `f0f104a6790b50e52585b918a1a5b97c98b3deeaed75697832fb81e82c0d461e` |
| **JAR Previo (Backup)** | `/root/backups_progression2/lunaeternal-0.1.0.PRE-PROGRESSION2.jar` | 108,526,702 B | `b03fbf048c35634cb3781f0c277c7cb83392c82a6d9da19faa6337e9a6cced05` |
| **Dump SQL Previo** | `/root/backups_progression2/s11945_luna_snapshot_20261008.sql` | 814,846 B | `148f4a283459a439a18f87d2913197776e87be61574c77f4e240361f4915fcc4` |
| **Config Tarball Backup** | `/root/backups_progression2/server_config_datapacks_backup_20261008.tar.gz` | 2,965,027 B | `ff0efe200b1f5b05c3f6b09ce2c5b431a0b84a8e0cfb7ecce61f9189133b0cb7` |
| **Configuración Balance** | `config/lunaeternal/progression_balance.json` | 1,420 B | `4443905cfc4dfca73ef89d13f9f743015a9a5f78ee9bc6dfa0fa3ffc475cf2e5` |
| **Config Cobblemon** | `config/cobblemon/main.json` | 12,488 B | `fossilMachineShinyChance: 4096` verificado |

---

## 3. HISTORIAL DE MIGRACIONES Y BASE DE DATOS

La migración se gestionó exclusivamente a través del runner transaccional integrado `Database.migrate()` de LunaEternal:

1. **V058 (`quest_hatch_event`):**
   - Estado: Preservada intacta en producción.
   - Registro en DB: `58 | confirmed hatch mission history | 2026-10-05 21:01:04.628`.
2. **V059 (`progression_licenses`):**
   - Archivo: `db/migration/V059__progression_licenses.sql`.
   - Ejecución: Exitosa durante el primer arranque del servidor.
   - Registro en DB: `59 | licencias de entrenador progresion 2.0 | 2026-10-08 16:09:43.095`.
   - Tablas creadas: `progression_license` (con `player_id`, `license_tier`, `purchased_at`, `expires_at`, claves foráneas e índices de búsqueda rápida).

---

## 4. DETALLE DE FEATURES DESPLEGADAS Y CALIBRADAS

1. **Balance de Experiencia:**
   - Factor de penalización por diferencia de nivel activo (`ExperienceBalanceService`).
   - Anti-boosting PvP estricto: combates PvP repetidos entre los mismos jugadores o con cuentas del mismo pool de IP otorgan 0 XP y 0 PD.
   - Protección Chansey / Blissey: ratio de ganancia limitado a 0.35x para evitar loops de teletransporte y macroing.
2. **Spawns y Especies Raras:**
   - Límites activos por chunk y políticas de fauna bioma-específicas en `PoliticaFauna` y `Salvaje`.
   - Starters, pseudolegendarios y legendarios con cuotas horarias globales.
3. **Control de Shinies y Fósiles:**
   - Ratio de máquina de fósiles restaurado a estándar Cobblemon 1/4096 (corrigiendo el exploit 1/100).
   - Telemetría centralizada en tiempo real (`ShinyTelemetry`).
4. **Crianza y Límites:**
   - Ranuras de guardería asociadas al rango y licencia (`BreedingSlotTest`).
   - Herencia de brazales recios (Power Items) y lazo destino verificados en servidor.
5. **Cadena de Miel y Caramelos:**
   - Rendimiento de colmenas a caramelos reducido a factor 0.50x.
   - Telemetría activa contra macro-harvesting (`HoneyTelemetry`).
6. **Economía y Rendimientos Decrecientes:**
   - Tramos calibrados sobre los percentiles reales de MariaDB (P50 = 3,496 PD, P95 = 19,421 PD):
     - Tramo 1 (0 – 20,000 PD): 100% de recompensas.
     - Tramo 2 (20,001 – 50,000 PD): 80% de recompensas.
     - Tramo 3 (50,001 – 100,000 PD): 50% de recompensas.
     - Tramo 4 (> 100,000 PD): 25% de recompensas.
   - Tasas de publicación GTS (2%) e impuesto de venta dinámico (6.5% - 15%) actuando como sumideros deflacionarios.
7. **Licencias de Entrenador:**
   - 6 rangos de licencia (Novato, Promesa, Veterano, Experto, Maestro, Campeón) con coste total acumulado de 725,000 PD.
   - Comando `/licencia` funcional.
8. **Topes de Nivel y Gimnasios:**
   - `GymLevelCapService` sincronizado con las medallas de Kanto e Islas Naranja. Desobediencia y bloqueo de nivel implementados.
9. **Eventos y Tienda Supporter:**
   - Sistema `/event` con tickets no transferibles.
   - Tienda de cosméticos de prestigio con `RankTokens`.
10. **Telemetría de Cuentas Secundarias:**
    - Algoritmo de detección de alts por subnet e intercambio asimétrico (`AltAccountTelemetry`) reportando alertas a administradores.

---

## 5. SMOKE TESTS EN TIEMPO REAL

Inmediatamente tras el arranque del contenedor:
- Jugadores reales conectados: `A1ejandroReport`, `Freddysillo`, `zahita`.
- Verificación del protocolo 3 en el handshake de la pasarela:
  `Puerta: A1ejandroReport saluda con protocolo 3 (se exige 3)`
  `CLIENT_VALIDATED jugador=Freddysillo protocolo=3`
  `CLIENT_VALIDATED jugador=zahita protocolo=3`
- Inicio y resolución de combate de gimnasio en vivo:
  `Gimnasio cissy: Freddysillo pelea a nivel 49 con 6 contra blastoise/seadra/kingdra/starmie/lapras/cloyster`
  `[BattlePositions] Arena found - Opponent Blastoise -> (-4.5, 72.0, 515.5), Challenger Raichu -> (-4.5, 72.0, 520.5)`
- Disparador de comandos `/event info` y `/licencia` verificado y operativo.
- Cero excepciones no controladas ni deadlocks detectados.
