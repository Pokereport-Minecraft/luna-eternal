# POKEREPORT NETWORK — PLAN DE DESPLIEGUE A PRODUCCIÓN

**Versión Destino:** LunaEternal 0.1.0 (`commit 63640845` con hotfixes auditados)  
**Entorno Destino:** Servidor Dedicado PokeReport (`15.235.16.131`)  
**Condición Obligatoria:** **ESTRICTAMENTE CONDICIONADO A AUTORIZACIÓN EXPRESA DEL PROPIETARIO.**

---

## 1. PRE-FLIGHT CHECKS (FASE PREVIA)

Antes de abrir la ventana de mantenimiento:
1. **Comprobación de Tests:**
   - Confirmar que `mod/gradlew test` arroja **420/420 PASS (0 failures, 0 skipped)**.
2. **Integridad del JAR:**
   - Comprobar que el archivo `build/release-candidate/lunaeternal-0.1.0-63640845.jar` posee el SHA-256:
     `b16a8bef26b4cb3844ca0f8afb54015a27979c67c030691908a872c5e7efd250`
3. **Validación del Cliente:**
   - Comprobar que `build/release-candidate/client/client-latest.json` y su firma `.sig` se encuentran validados.

---

## 2. PROCEDIMIENTO PASO A PASO (ROLLOUT GATES)

### GATE 0 — RESPALDO FORENSE PREVIO (T-10m)
1. **Base de Datos MariaDB:**
   - Ejecutar dump completo de la base de datos `s11945_luna`:
     ```bash
     mysqldump -u <user> -p s11945_luna > backup_pre_deploy_20261008.sql
     ```
   - Calcular SHA-256 del dump y verificar tamaño no nulo.
2. **Servidor Minecraft:**
   - Respaldar el JAR actual remoto:
     ```bash
     cp mods/lunaeternal-0.1.0.jar mods/lunaeternal-0.1.0.jar.bak_20261008
     ```

### GATE 1 — NOTIFICACIÓN Y VENTANA DE MANTENIMIENTO (T-5m)
1. Anunciar a los jugadores en el servidor mediante broadcast:
   ```text
   /broadcast §6[PokeReport] §eEl servidor entrará en mantenimiento programado de 3 minutos. Guardando datos...
   ```
2. Ejecutar `/save-all` para sincronizar mundos y datos NBT a disco.
3. Detener el servidor de forma ordenada mediante `/stop`.

### GATE 2 — APLICACIÓN DE MIGRACIÓN SQL V060 (T-2m)
1. Aplicar la migración incremental en MariaDB `s11945_luna`:
   - `V060__staff_moderation_and_spectate.sql`
2. Verificar la creación exitosa de las tablas:
   - `staff_punishment`
   - `staff_spectate_log`

### GATE 3 — REEMPLAZO ATÓMICO DEL JAR DE SERVIDOR (T-1m)
1. Transferir `build/release-candidate/lunaeternal-0.1.0-63640845.jar` al directorio `mods/lunaeternal-0.1.0.jar` del servidor.
2. Verificar el hash del archivo en el servidor remoto mediante `sha256sum mods/lunaeternal-0.1.0.jar`.

### GATE 4 — ARRANQUE DEL SERVIDOR Y VERIFICACIÓN DE LOGS (T+0)
1. Iniciar el servicio del servidor.
2. Monitorear el archivo `logs/latest.log`:
   - Verificar carga de Fabric Loader y Fabric API.
   - Verificar conexión exitosa a MariaDB y confirmación de Schema version 60.
   - Confirmar inicialización de `CardsNpc` en Ciudadela.
   - Confirmar arranque del thread pool de `SanctuaryImagePipeline`.
   - Cero excepciones de mixins ni advertencias críticas.

### GATE 5 — PUBLICACIÓN SINCRONIZADA DEL CLIENTE / LAUNCHER (T+5m)
1. Subir `PokeReport-Base-0.2.0.mrpack` y los JARs gestionados al repositorio de distribución.
2. Publicar `client-latest.json` y `client-latest.json.sig` en el servidor web del launcher.
3. El Launcher detectará automáticamente la versión atómica y actualizará a los clientes.

### GATE 6 — SMOKE TESTS POST-DESPLIEGUE (T+10m)
1. Iniciar sesión con cuenta de pruebas.
2. Visitar el NPC de cartas en Ciudadela (`/luna npc cartas`) e interactuar.
3. Comprobar recompensas del Pase de Batalla (Nivel 1 y 100).
4. Comprobar benchmark de gimnasio de Brock (Nivel 15).
5. Comprobar comando `/staffspectate`.
6. Abrir acceso público a los jugadores.
