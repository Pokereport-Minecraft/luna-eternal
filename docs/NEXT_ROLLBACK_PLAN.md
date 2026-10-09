# POKEREPORT NETWORK — PLAN DE CONTINGENCIA Y ROLLBACK

**Fecha:** 08/10/2026  
**Objetivo:** Protocolo de reversión rápida y segura en caso de anomalía crítica (P0) post-despliegue.

---

## 1. CRITERIOS DE DISPARO DEL ROLLBACK (TRIGGERS P0)

Se ejecutará un rollback inmediato si durante o tras el despliegue ocurre cualquiera de las siguientes condiciones:
1. **Corrupción de Datos:** Pérdida de inventarios, estadísticas de Pokémon o niveles de jugadores.
2. **Crash Loop del Servidor:** Fallo en el inicio del servidor causado por incompatibilidad de mixins o dependencias.
3. **Bloqueo Masivo de Conexión:** Imposibilidad general de los jugadores para ingresar a través del Launcher o cliente.
4. **Fallo Crítico de Base de Datos:** Deadlocks irrecuperables en transacciones de MariaDB.

---

## 2. PROCEDIMIENTO DE REVERSIÓN PASO A PASO

### PASO 1 — DETENCIÓN INMEDIATA DEL SERVIDOR
```bash
/stop
```
Si el servidor no responde por consola, forzar la detención desde el panel de control o señal `kill -15` (SIGTERM).

### PASO 2 — RESTAURACIÓN DEL JAR ANTERIOR
1. Eliminar el JAR problemático:
   ```bash
   rm mods/lunaeternal-0.1.0.jar
   ```
2. Restaurar el backup verificado del commit base `63640845` previo a los cambios:
   ```bash
   cp mods/lunaeternal-0.1.0.jar.bak_20261008 mods/lunaeternal-0.1.0.jar
   ```

### PASO 3 — ROLLBACK DEL MANIFIESTO DEL LAUNCHER
1. Revertir `client-latest.json` en el servidor web a la versión anterior estable.
2. Los clientes que hayan descargado la actualización se mantendrán compatibles gracias al diseño retrocompatible del protocolo, o el actualizador restaurará los archivos correspondientes.

### PASO 4 — CONTINGENCIA DE BASE DE DATOS
- **Nota sobre V060:** La migración `V060` es puramente aditiva (crea `staff_punishment` y `staff_spectate_log`). Por tanto, **NO rompe** la retrocompatibilidad con la versión anterior de LunaEternal si el JAR anterior se vuelve a iniciar.
- Solo en caso de corrupción demostrada de tablas de jugadores, restaurar el dump:
  ```bash
  mysql -u <user> -p s11945_luna < backup_pre_deploy_20261008.sql
  ```

### PASO 5 — REINICIO Y VERIFICACIÓN
1. Iniciar el servidor con el JAR anterior.
2. Confirmar que los jugadores pueden conectarse y que su estado está intacto.
3. Declarar el entorno en estabilización e iniciar el análisis de causa raíz (post-mortem).
