# Guía de Recuperación y Resiliencia ante Fallos

## 1. Filosofía de Recuperación

El actualizador de **PokeReport Network** está diseñado para ser **completamente resistente a fallos abruptos** (cortes de energía, reinicios repentinos de Windows, cierre forzado de procesos o pérdida de red).

En ningún escenario el juego debe quedar en un estado roto, incompleto o a medias entre dos versiones.

---

## 2. El Journal de Transacción (`.pokereport/update-journal.json`)

El archivo `.pokereport/update-journal.json` registra en tiempo real la fase exacta de la actualización:

| Fase | Significado | Comportamiento en caso de Crash |
|---|---|---|
| `DOWNLOADING` | Descargando archivos a `.pokereport/update-staging/`. La carpeta `mods/` no ha sido tocada. | Al reabrir, se purga la carpeta temporal de staging y el journal. La instalación original queda intacta. |
| `VERIFIED` | Todos los archivos en staging pasaron el check SHA-256. | Staging limpio, ningún archivo del juego alterado. |
| `PREPARED` | Se creó el backup en `.pokereport/backups/` y se prepararon archivos `.new`. | Se eliminan los archivos `.new` y se limpia el staging. La versión activa sigue intacta. |
| `COMMITTING` | Proceso de intercambio atómico de archivos en ejecución. | El updater detecta la interrupción y ejecuta **Rollback automático** desde `.pokereport/backups/<prevBundle>/` hacia la instancia. |
| `COMMITTED` | Los archivos nuevos están en su lugar pero falta la validación post-commit. | Se valida la integridad de los archivos. Si alguno está corrupto, se restaura desde el backup; si todos son válidos, se finaliza el journal. |
| `VALIDATED` | Verificación post-commit superada. Solo resta limpiar temporales. | Se eliminan los temporales y se cierra el journal normalmente. |

---

## 3. Política de Backup y Rollback

### Creación del Backup
Antes de realizar el commit de cualquier archivo, los archivos administrados existentes son respaldados en:
```text
.pokereport/backups/<version-anterior>/
```
Solo se respaldan los archivos que van a ser modificados, evitando copias masivas de datos innecesarios.

### Rollback Automático
Si durante la validación post-commit se detecta que un archivo no existe o su hash no coincide:
1. El updater emite un aviso de emergencia en `.pokereport/logs/updater.log`.
2. Restaura de forma inmediata cada archivo respaldado en `.pokereport/backups/<version-anterior>/` hacia su ubicación original.
3. Elimina cualquier archivo `.new` o temporal residual.
4. Muestra un mensaje amigable al jugador informando que la actualización no pudo verificarse y que su versión anterior sigue 100% operativa.

---

## 4. Detección de Bloqueos de Archivos en Windows (File Locking)

En sistemas operativos Windows, el motor JVM mantiene un bloqueo exclusivo sobre los archivos JAR cargados en memoria. Intentar sobrescribir un JAR con Minecraft abierto causa un error `ERROR_SHARING_VIOLATION`.

### Detección Preventiva
Antes de tocar cualquier archivo, el updater ejecuta:
```java
try (FileChannel ch = FileChannel.open(targetPath, StandardOpenOption.WRITE)) {
    // Archivo no bloqueado
} catch (Exception e) {
    // Archivo bloqueado por otro proceso
}
```
Si detecta que el archivo está en uso:
- **NO fuerza la terminación de procesos Java** (evita cerrar IDEs, otros servidores o programas en ejecución).
- Presenta el mensaje claro:
  ```text
  Minecraft está abierto.
  Cierra el juego antes de actualizar PokeReport.
  ```
- Aborta la transacción limpiamente sin alterar ningún archivo.
