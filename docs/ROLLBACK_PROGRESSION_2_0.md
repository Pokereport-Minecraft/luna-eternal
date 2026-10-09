# POKEREPORT NETWORK — PLAN DE REVERSIÓN Y ROLLBACK (PROGRESIÓN 2.0)

**Fecha:** 8 de Octubre de 2026  
**Objetivo:** Garantizar que ante cualquier contingencia, el sistema pueda volver a su estado anterior en menos de 60 segundos sin pérdida de datos de jugadores.

---

## 1. MECANISMOS DE REVERSIÓN INMEDIATA (EN CALIENTE)

Toda la lógica de Progresión 2.0 está desacoplada mediante un conmutador de características (Feature Flag) en `config/lunaeternal/progression_balance.json`:

### 1.1 Apagado Total sin Reinicio (Hot-Disable)
Para deshabilitar Progresión 2.0 en caliente:
1. Abrir `config/lunaeternal/progression_balance.json`.
2. Establecer `"enabled": false`.
3. Ejecutar en consola del servidor:
   ```text
   /luna reload
   ```
4. **Efecto instantáneo:**
   * Todos los multiplicadores de spawn vuelven a 1.0 (vanilla).
   * Los filtros de experiencia salvaje y PvP vuelven al comportamiento nativo.
   * Las restricciones de ranuras de GTS y crianza vuelven a los valores por defecto históricos.

---

## 2. REVERSIÓN DE BASE DE DATOS (SQL ROLLBACK)

Si se ejecutó la migración `V058__progression_licenses.sql`, la reversión es estrictamente no destructiva:

```sql
-- Reversión de Progresión 2.0 (idempotente)
DROP TABLE IF EXISTS player_license;
DROP TABLE IF EXISTS pvp_battle_history;
DELETE FROM schema_version WHERE version = 58;
```

*Nota:* Los inventarios, las economías (`player_economy`), los Pokémon capturados y las ranuras de crianza existentes (`crianza_ranura`) **NO se tocan ni se alteran**, preservando el 100% de la integridad de los datos de los jugadores.

---

## 3. CONTACTOS Y PROCEDIMIENTO DE ESCALADO
1. Si un jugador experimenta una discrepancia en el nivel de su licencia, el comando administrativo `/licencia set <jugador> <licencia>` permite sincronización manual inmediata.
2. Si un spawn en particular requiere ajuste fino, se edita individualmente su multiplicador en `progression_balance.json` y se recarga en segundos.
