# Backup y recuperación — evidencia 2026-10-02

Se preservó Git completo en `C:/Users/JUAN/.codex/prelaunch-backups/2026-10-02/history.bundle`, cambios pendientes de la copia principal en `primary-pending.patch` y archivos individuales. Otros worktrees permanecen intactos.

Servidor: scripts existentes `/usr/local/sbin/pokereport-db-backup`, `pokereport-files-backup`, `pokereport-offsite-backup`; timers observados activos y ejecuciones anteriores correctas. Base principal `s11945_luna`. Backups en `/var/backups/pokereport/{database,files}`. El original de files-backup quedó en `/var/backups/pokereport/maintenance/pokereport-files-backup.before-prelaunch-20261002`.

Defecto reparado: exclusión global de ZIP eliminaba datapacks empaquetados. Ahora incluye ZIP, archivos de lanzamiento/libraries/versions, listas de acceso, configuración y scripts operativos. Reparación mínima verificada con bash -n. Archivo generado: `/var/backups/pokereport/files/cobblemon_2026-10-02_152009.tar.zst`, 11,473,418,673 bytes; checksum PASS; EasyAuth SQLite integrity PASS. save-on restaurado. La retención normal eliminó el backup más antiguo según el script existente.

Restauración ejecutada: archivos 04:30 y SQL 04:00 en `/srv/pokereport-prelaunch-20261002` y schema `prelaunch_20261002`; usuario exclusivo de ese schema. Debido al defecto del backup antiguo se suplementaron ZIP/launcher desde live. Arrancó el servidor y pasó QA; esto NO certifica recuperación autosuficiente del nuevo archivo ni un snapshot coordinado entre SQL y mundo.

## Recuperación segura reproducible

1. Verificar checksums antes de extraer. Crear directorio NUEVO, nunca sobrescribir el volumen activo.
2. Extraer archivos con tar --zstd; revisar inventario de mods, config, datapacks, mundos, permisos y launcher. Los scripts del host quedan bajo `usr/local/sbin` en el directorio de recuperación; revisarlos antes de reponerlos al host.
3. Crear schema SQL y usuario NUEVOS; restaurar dump allí. Cambiar db.name/user/password de la copia. Prohibir acceso a schema de producción.
4. Desactivar conectores externos con credenciales reales antes del arranque: al menos Tebex; revisar restantes. Desactivar builder key. Bind localhost y puertos independientes, RCON secreto independiente, whitelist y límites recursos.
5. Arrancar copia, revisar migraciones/tablas, EasyAuth, datos jugador, party, inventario y saldos. Ejecutar autotest solo en copia: modifica fixtures.
6. Alterar un dato de prueba en la copia; detenerla; restaurar de nuevo a otro destino nuevo y comprobar que el dato volvió al snapshot. Este último ensayo del nuevo backup está PENDING.
7. Para rollback de producción, detener y preservar también el estado actual, acordar punto SQL/mundo coordinado, restaurar versiones compatibles y verificar antes de abrir acceso. No revertir automáticamente solo el JAR tras migraciones nuevas.

Incidente QA: el primer arranque restaurado permitió que el mod Tebex contactara la tienda real. Se detuvo y se desactivó exclusivamente en staging; el setup ahora lo desactiva antes de iniciar. No se probó una compra real. Revisar entregas/cola en Tebex para descartar consumo externo; no inferir inocuidad solo porque había cero jugadores.

El staging y su schema se conservan para reproducción; no borrar mundos ni cuentas reales. Copias de config contienen secretos: permisos restringidos, no añadir al repositorio ni publicar su contenido.

## Segundo ensayo ejecutado: backup corregido

Se restauró íntegro el archivo 15:20 en `/srv/pokereport-prelaunch-recovery-20261002`, sin copiar ZIP ni launcher desde live. Schema `prelaunch_recovery_20261002`, cuenta exclusiva, Tebex desactivado antes de iniciar. Arranque y RCON list PASS (0 jugadores). Se alteró un saldo únicamente en la copia; un segundo schema restaurado `prelaunch_recovery_verify_20261002` recuperó exactamente el snapshot de wallets (SHA256 422063815fdbb9d0ebd7896fd96a6140be255e4e40b6bcb56448700fc0de5bc2). Se repuso el dato alterado de la primera copia. No se modificó DB principal. Se usó SQL 04:00 y archivos 15:20: recuperabilidad de archivos/SQL comprobada; coordinación temporal entre ambos no certificada.
