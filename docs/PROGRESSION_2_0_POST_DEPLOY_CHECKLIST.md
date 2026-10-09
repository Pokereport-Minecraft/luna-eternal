# POKEREPORT NETWORK — CHECKLIST POST-DESPLIEGUE (PROGRESSION 2.0)

Este documento establece las tareas de seguimiento, monitoreo y validación operativa a realizar por el equipo de administración durante las primeras 72 horas tras el despliegue de **Progression 2.0**.

---

## 1. COMPROBACIONES INMEDIATAS (0 – 2 HORAS)

- [x] **Arranque del Contenedor:** Servidor en estado `Running` sin reinicios en bucle.
- [x] **Migración SQL V059:** Comprobar existencia de tabla `progression_license` con `SELECT COUNT(*) FROM progression_license;`.
- [x] **Conexión de Jugadores:** Verificar que jugadores existentes conectan con handshake exitoso en Protocolo 3.
- [x] **Autenticación y Spawn:** Validación de EasyAuth y transferencia al mundo `ciudadela` o gimnasios sin caídas al vacío.
- [x] **Combates de Gimnasio:** Probar una batalla de gimnasio con arenas de `BattlePositions` (verificado con jugador Freddysillo en Cissy).
- [ ] **Comando /licencia:** Ejecutar `/licencia` in-game para comprobar renderizado del menú y compra de rango.
- [ ] **Comando /event:** Comprobar visualización de eventos activos mediante `/event list` o `/event info`.
- [ ] **Auditoría GTS:** Publicar y comprar un ítem de prueba para verificar que la comisión del 2% y la tasa se descuentan correctamente.

---

## 2. MONITOREO DE TELEMETRÍA (24 – 72 HORAS)

Durante la ventana de observación de 72 horas, **NO aplicar nerfs manuales adicionales** a menos que se detecte un exploit severo.

### A. Economía y Deflación
Supervisar el flujo en la tabla `ledger_entry`:
```sql
SELECT source, COUNT(*), SUM(amount) 
FROM ledger_entry 
WHERE created_at > NOW() - INTERVAL 24 HOUR 
GROUP BY source;
```
- Verificar que la tasa de entrada de Pokédollars se mantiene balanceada contra los sumideros de Licencias y GTS.
- El 95% de los jugadores debe permanecer en el Tramo 1 (hasta 20,000 PD/día).

### B. Ratio de Shinies
Comprobar capturas y eclosiones con la telemetría en `logs/latest.log`:
- Buscar etiquetas `[SHINY-TELEMETRY]`.
- Verificar que la máquina de fósiles opera con 1/4096 y que ningún jugador genera más de 1 shiny fósil sin un volumen masivo de restauraciones.

### C. Cadena de Miel y Granja de Caramelos
- Buscar en los logs `[HONEY-TELEMETRY]`.
- Confirmar que ningún jugador supera los 25 caramelos raros cocinados por hora.

### D. Cuentas Secundarias (Alts)
- Revisar advertencias de staff generadas por `AltAccountTelemetry`:
  `[ALT-TELEMETRY] Posible multiboxing detectado entre <JugadorA> y <JugadorB>`
- Realizar seguimiento manual del staff antes de tomar sanciones disciplinarias.

---

## 3. PROCEDIMIENTO ANTE ANOMALÍAS

1. **Problema con una feature específica:**
   - Desactivar la característica mediante el archivo `config/lunaeternal/progression_balance.json` y ejecutar recarga en caliente sin reiniciar el servidor.
2. **Error en base de datos:**
   - La base de datos cuenta con backup completo previo en `/root/backups_progression2/s11945_luna_snapshot_20261008.sql`.
3. **Inestabilidad crítica:**
   - Seguir estrictamente el procedimiento detallado en [`docs/PROGRESSION_2_0_ROLLBACK.md`](file:///d:/pokereportversionmejorada/docs/PROGRESSION_2_0_ROLLBACK.md).
