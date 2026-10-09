# PokeReport Network — Arquitectura de Actualización Atómica y Distribución del Cliente

## 1. Visión General y Principio Fundamental

La distribución del cliente de **PokeReport Network** resuelve el problema clásico de los modpacks de Minecraft: **el ciclo continuo de reinstalación de instancias**.

Tradicionalmente, cada vez que un servidor actualiza su mod principal o corrige un bug (p. ej., en batallas de gimnasio o crianza), el jugador se ve forzado a:
1. Descargar un nuevo archivo `.mrpack` o `.zip`.
2. Crear una nueva instancia en su launcher (Prism, Modrinth, CurseForge).
3. Reconfigurar sus opciones gráficas, controles y mods personales.
4. O manipular archivos JAR manualmente en la carpeta `mods/`.

### El Nuevo Paradigma de PokeReport
```text
               POKEREPORT BASE PACK
                    (.mrpack)
                       │
                       │ Instalación UNA SOLA VEZ
                       ▼
              INSTANCIA POKEREPORT
                       │
         ┌─────────────┴─────────────┐
         │                           │
         ▼                           ▼
   CLIENT BUNDLE                USER FILES
   (Administrado)               (Protegidos)
         │
         │ Actualización Atómica
         ▼
    PokeReport Updater
         │
         ├─ Firma Ed25519
         ├─ Verificación SHA-256
         ├─ Staging transaccional
         ├─ Journal y Rollback
         └─ Sustitución atómica (.new)
         │
         ▼
     CLIENTE LISTO (Handshake Protocolo 3)
```

---

## 2. Separación de Responsabilidades: Base Pack vs. Client Bundle

### A. Base Pack (`PokeReport-Base-X.Y.mrpack`)
El Base Pack representa la infraestructura base, pesada y de baja frecuencia de cambio:
- **Minecraft**: 1.21.1
- **Fabric Loader**: 0.19.5
- **Cobblemon**: 1.8.0 Fabric
- **RCT / RCT API**: 0.19.0 / 0.16.0
- **Fabric API**: 0.116.17+1.21.1
- **Mods externos de rendimiento**: Sodium, Iris, FerriteCore, Lithium, ImmediatelyFast, EntityCulling, ModernFix, etc.
- **Resourcepacks grandes y shaders base**.

**Cuándo se emite un nuevo Base Pack:**
- Actualización de versión mayor de Minecraft o Fabric Loader.
- Actualización mayor de Cobblemon (ej. 1.8.0 -> 1.9.0) o Fabric API.
- Reemplazo estructural de mods de rendimiento que requiera nueva base.

### B. Client Bundle (`client-latest.json`)
El Client Bundle representa la lógica propietaria de PokeReport, que evoluciona rápidamente:
- **LunaEternal** (`lunaeternal-0.1.0.jar`): PokePad, tienda, misiones, gimnasios, crianza, economía, networking.
- **LunaNeon** (`lunaneon-0.1.0.jar`): Bloques de neón para Ciudadela, modelos Bedrock cosméticos y resolvers Cobblemon.
- **Cobblemon Cards, Additions, Battle Positions y Armaduras de Rango**.
- **Configuraciones de cliente administradas** (`config/cobblemon-cards.json`, etc.).
- **Filtros y metadatos de sincronización**.

**Cuándo se emite un nuevo Client Bundle:**
- Correcciones de batallas de NPC/entrenadores.
- Nuevas misiones, objetos de crianza o tiendas del PokePad.
- Nuevos modelos cosméticos o bloques decorativos.
- Parches de seguridad o estabilidad.

---

## 3. Versionado Independiente

Para evitar inconsistencias, cada componente mantiene su propio versionado desacoplado:

| Propiedad | Valor Actual | Propósito |
|---|---|---|
| `basePackVersion` | `0.2.0` | Versión del modpack base `.mrpack` |
| `clientBundleVersion` | `0.1.0` | Versión del paquete incremental administrado |
| `protocolVersion` | `3` | Número de protocolo de red entre cliente y servidor (`Puerta.java`) |
| `updaterVersion` | `1.0.0` | Versión del motor actualizador atómico |
| `manifestSchemaVersion` | `2` | Esquema del manifest criptográfico |
| `minecraftVersion` | `1.21.1` | Versión de Minecraft requerida |
| `fabricLoaderVersion` | `0.19.5` | Versión de Fabric Loader |

---

## 4. Ciclo Transaccional de Actualización (All-or-Nothing)

El actualizador implementa una máquina de estados estrictamente transaccional. Ningún archivo existente se modifica hasta que todo el lote ha sido descargado, verificado y preparado:

1. **CHECK**: Descarga `client-latest.json` y `client-latest.json.sig`. Verifica la firma criptográfica Ed25519 con la clave pública embebida. Si el hash local coincide con el remoto, aborta inmediatamente con estado `NOOP`.
2. **LOCK CHECK**: Comprueba si Minecraft o Java mantiene abiertos los archivos JAR en Windows (`FileChannel.open(..., WRITE)`). Si Minecraft está abierto, notifica al usuario y aborta sin alterar ningún archivo.
3. **SPACE CHECK**: Comprueba que el espacio libre en disco cubra al menos $3\times$ el tamaño del release más 50 MB de margen.
4. **DOWNLOADING**: Descarga los archivos administrados en `.pokereport/update-staging/<releaseId>/`. Escribe el journal de transacción con fase `DOWNLOADING`.
5. **VERIFY**: Calcula tamaño exacto y hash SHA-256 de cada archivo en staging. Si uno solo falla, la transacción se aborta y se limpia el staging.
6. **PREPARE**: Copia los archivos administrados actuales a `.pokereport/backups/<prevBundle>/` y genera archivos `.new` listos para intercambio.
7. **COMMIT**: Sustitución atómica de los archivos administrados y retiro controlado de archivos obsoletos.
8. **VALIDATE**: Comprueba que los archivos finales en la instancia coinciden con los hashes esperados. Si la validación falla, ejecuta rollback inmediato desde el backup.
9. **CLEANUP**: Elimina staging, archivos `.new` y el archivo de journal. Actualiza `.pokereport/installed.json`.

---

## 5. Experiencia del Usuario

### Jugador Nuevo
1. Descarga `PokeReport-Base-0.2.0.mrpack` desde `pokereport.online`.
2. Lo importa en su launcher favorito (Prism Launcher, Modrinth App, etc.).
3. Inicia el juego y se conecta directamente a `play.pokereport.online`.

### Jugador Existente
1. Ejecuta `ACTUALIZAR POKEREPORT` (mediante `actualizar-pokereport.bat` o a través del launcher).
2. El actualizador procesa el cambio en menos de 3 segundos.
3. Inicia Minecraft sin haber perdido configuraciones, mapas ni mundos locales.
