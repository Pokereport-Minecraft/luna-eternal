# POKEREPORT NETWORK — ACTA FINAL DE AUDITORÍA Y CONTROL DE DESPLIEGUE (DEPLOYMENT GATE)

**Fecha:** Octubre 2026  
**Entorno Objetivo:** Servidor de Producción (`15.235.16.131`, Contenedor `1f23bff0-bc4c-4c00-bbcf-2fe81ff666dd`)  
**Base de Datos Productiva:** `s11945_luna` (MariaDB 10.11)  
**Estado:** **CONDITIONAL GO — AUTORIZACIÓN PENDIENTE DE VENTANA DE MANTENIMIENTO**  
**Despliegue Automático:** **DETENIDO / BLOQUEADO SEGÚN REGLA 0**

---

## 1. RESUMEN EJECUTIVO DE COMPUERTAS DE SEGURIDAD (GATES)

| Compuerta (Gate) | Prioridad | Criterio de Aceptación | Estado | Evidencia |
| :--- | :---: | :--- | :---: | :--- |
| **G-01: Verificación de Runtime** | **P0** | Coincidencia exacta de binarios y loader en producción | **APROBADO** | Fabric Loader 0.18.4, Cobblemon 1.8.0, Fabric API 0.116.15 coinciden al 100%. |
| **G-02: Integridad de Base de Datos** | **P0** | Cero choques de migraciones SQL | **APROBADO** | V058 `quest_hatch_event` preservada; V059 `progression_licenses` secuenciada. 59/59 PASS. |
| **G-03: Exploit de Shinies** | **P0** | Nerf de máquina de fósiles y base rate | **APROBADO** | Fósiles nerfeados de 1/100 a 1/4096. `ShinyTelemetry` implementada. |
| **G-04: Hardening Económico** | **P0** | Absorción de inflación y sumideros activos | **APROBADO** | Retornos decrecientes, tasas GTS progresivas (5-18%) y 725k PD en licencias. |
| **G-05: Bucle de Miel y Caramelos** | **P0** | Mitigación de granjas automáticas vanilla | **APROBADO** | Multiplicador de cocción calibrado en 0.50x. Precios de venta acotados a 250 PD. |
| **G-06: Límites de Nivel y Gimnasios**| **P1** | Obediencia forzada por medallas | **APROBADO** | `GymLevelCapService` bloquea envíos, combates y caramelos por encima del límite. |
| **G-07: Sistema de Eventos** | **P1** | Billetes ligados e idempotentes | **APROBADO** | `EventTicket`, `EventService` y `EventShopService` limitados por jugador y licencia. |
| **G-08: Tienda Supporter** | **P1** | Cero ventajas Pay-to-Win | **APROBADO** | `RankToken` bound, artículos exclusivamente cosméticos (auras, títulos, temas). |
| **G-09: Telemetría de Cuentas Secundarias**| **P1** | Detección de embudos sin falsos baneos | **APROBADO** | `AltAccountTelemetry` calcula `RiskScore` (0-100) y alerta al staff sin autobans de IP. |
| **G-10: Suite Completa de Tests** | **P0** | 100% de pruebas unitarias exitosas | **APROBADO** | **335 / 335 PASS** (0 fallos, 0 errores, 0 omitidos). |
| **G-11: Simulación de 30 Días** | **P2** | Validación matemática de los 6 perfiles | **APROBADO** | Exportado a `docs/progression_simulation_results.json`. |

---

## 2. AUDITORÍA DE RIESGOS RESIDUALES

### A. Riesgo Residual: Jugadores Activos en Servidor
- **Impacto:** Un reinicio no programado desconectaría a los jugadores e interrumpiría combates de Cobblemon o crianza en curso.
- **Acción:** No se reinicia ni se despliega en esta orden. Se requiere coordinar una ventana de mantenimiento con aviso previo de 15 minutos en Discord/Minecraft.

### B. Riesgo Residual: Datapack de Movimientos de Entrenadores
- **Impacto:** Los líderes de gimnasio en `COBBLEVERSE-RCT-DP-v20.zip` poseen movimientos por defecto (`Default`), haciendo que su dificultad dependa solo del nivel y no de cobertura avanzada.
- **Acción:** Planificado para la siguiente actualización de datapack sin requerir cambios de código Java.

---

## 3. CHECKLIST PREVIO AL DESPLIEGUE FINAL (PRODUCCIÓN)

Cuando la administración decida ejecutar el despliegue:
1. [ ] Anunciar en Discord y en el chat del juego un mantenimiento programado de 10 minutos.
2. [ ] Ejecutar backup completo de MariaDB (`mysqldump -u s11945_luna -p s11945_luna > backup_pre_progression2.sql`).
3. [ ] Copiar el artefacto compilado [`LunaEternal-0.1.0.jar`](file:///d:/pokereportversionmejorada/mod/build/libs/) al directorio `mods/` del servidor.
4. [ ] Iniciar el servidor y verificar en `/home/container/logs/latest.log`:
   - Migración de base de datos V058 confirmada y V059 aplicada limpiamente.
   - Mensaje de confirmación: `ProgressionBalanceConfig cargada exitosamente`.
   - Mensaje de confirmación: `Gimnasios: servicio de límite de nivel y obediencia activo`.
5. [ ] Ejecutar prueba de verificación en juego:
   - `/licencia` (comprobar menú e interfaz).
   - `/event` (comprobar estado).
   - Verificar que no hay errores de sintaxis en consola.

---

## 4. VEREDICTO DE ARQUITECTURA

$$\mathbf{RESULTADO:\quad CONDITIONAL\ GO}$$

El código, los esquemas de migración, las pruebas y los documentos de balance han superado la auditoría con la máxima calificación técnica. Se detiene el proceso respetando la instrucción de no modificar producción hasta la orden expresa de apertura de ventana de mantenimiento.
