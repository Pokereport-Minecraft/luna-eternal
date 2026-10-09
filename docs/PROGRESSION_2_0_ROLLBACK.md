# POKEREPORT NETWORK — PROTOCOLO DE ROLLBACK (PROGRESSION 2.0)

**Fecha:** 8 de Octubre de 2026  
**Entorno:** Producción (`15.235.16.131`, Contenedor `1f23bff0-bc4c-4c00-bbcf-2fe81ff666dd`)  
**Base de Datos:** `s11945_luna`  

Este documento define el procedimiento de reversión inmediata (Rollback Plan) en caso de contingencia crítica en el entorno de producción.

---

## 1. INVENTARIO DE ARTEFACTOS DE RESPALDO (BACKUPS VERIFICADOS)

Todos los artefactos de respaldo fueron generados antes del despliegue y están garantizados en el host remoto y en copia local:

| Artefacto de Respaldo | Ruta en Servidor (`15.235.16.131`) | Tamaño | SHA-256 |
| :--- | :--- | :--- | :--- |
| **Snapshot de Base de Datos** | `/root/backups_progression2/s11945_luna_snapshot_20261008.sql` | 814,846 B | `148f4a283459a439a18f87d2913197776e87be61574c77f4e240361f4915fcc4` |
| **Copia Local de Base de Datos** | `build/s11945_luna_snapshot_20261008.sql` | 814,846 B | `148f4a283459a439a18f87d2913197776e87be61574c77f4e240361f4915fcc4` |
| **JAR Servidor Anterior** | `/root/backups_progression2/lunaeternal-0.1.0.PRE-PROGRESSION2.jar` | 108,526,702 B | `b03fbf048c35634cb3781f0c277c7cb83392c82a6d9da19faa6337e9a6cced05` |
| **Tarball de Configs y Datapacks** | `/root/backups_progression2/server_config_datapacks_backup_20261008.tar.gz` | 2,965,027 B | `ff0efe200b1f5b05c3f6b09ce2c5b431a0b84a8e0cfb7ecce61f9189133b0cb7` |

---

## 2. PROCEDIMIENTO PASO A PASO DE REVERSIÓN

### Paso 1: Notificación y Parada Limpia del Servidor
1. Notificar a los jugadores en el juego con mensaje de mantenimiento preventivo.
2. Forzar guardado del mundo:
   ```bash
   /usr/local/sbin/pokereport-server-cmd "save-all flush"
   ```
3. Detener el contenedor mediante la API de Wings (evitando reinicio automático del supervisor):
   ```bash
   curl -k -X POST https://127.0.0.1:8080/api/servers/1f23bff0-bc4c-4c00-bbcf-2fe81ff666dd/power \
     -H "Authorization: Bearer <TOKEN>" \
     -H "Content-Type: application/json" \
     -d '{"action": "stop"}'
   ```
4. Esperar hasta que el estado del contenedor sea `offline`.

### Paso 2: Restauración del Binario JAR Anterior
Reemplazar el JAR desplegado por la versión previa verificada:
```bash
cp /root/backups_progression2/lunaeternal-0.1.0.PRE-PROGRESSION2.jar \
   /var/lib/pterodactyl/volumes/1f23bff0-bc4c-4c00-bbcf-2fe81ff666dd/mods/lunaeternal-0.1.0.jar
```

### Paso 3: Reversión de Configuraciones
1. Eliminar o archivar la configuración de progresión:
   ```bash
   mv /var/lib/pterodactyl/volumes/1f23bff0-bc4c-4c00-bbcf-2fe81ff666dd/config/lunaeternal/progression_balance.json \
      /var/lib/pterodactyl/volumes/1f23bff0-bc4c-4c00-bbcf-2fe81ff666dd/config/lunaeternal/progression_balance.json.bak
   ```
2. (Opcional) Revertir `config/cobblemon/main.json` si se desea restaurar el ratio anterior.

### Paso 4: Política respecto a Base de Datos (MariaDB)
> [!NOTE]
> La migración V059 es **estrictamente aditiva** (crea la tabla `progression_license` sin modificar tablas existentes como `player`, `pokemon`, `economy`, o `quest`).
> Por lo tanto, el JAR previo simplemente ignorará la tabla `progression_license` y funcionará con normalidad.
> **NO restaurar el dump de base de datos salvo que se haya producido corrupción de datos**, para evitar perder el progreso reciente de los jugadores.

Si un rollback completo de base de datos fuera estrictamente necesario:
```bash
mariadb s11945_luna < /root/backups_progression2/s11945_luna_snapshot_20261008.sql
```

### Paso 5: Rearranque del Servidor
Iniciar el contenedor mediante la API de Wings:
```bash
curl -k -X POST https://127.0.0.1:8080/api/servers/1f23bff0-bc4c-4c00-bbcf-2fe81ff666dd/power \
   -H "Authorization: Bearer <TOKEN>" \
   -H "Content-Type: application/json" \
   -d '{"action": "start"}'
```

### Paso 6: Verificación de Recuperación
Revisar `logs/latest.log` para confirmar:
- Arranque completado sin errores.
- Jugadores capaces de conectar e interactuar con normalidad.
