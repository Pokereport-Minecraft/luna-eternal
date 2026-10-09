# Guía del Pipeline de Release y Publicación para Administradores

## 1. Visión General

El pipeline de distribución del cliente de **PokeReport Network** automatiza completamente el ciclo de preparación, verificación, firma criptográfica y publicación del modpack y los paquetes incrementales.

Se rige por el principio de **dos comandos estrictamente separados**:
1. `releasePokeReportClient`: Compila, audita, firma y empaqueta en un Release Candidate local. **Nunca publica automáticamente**.
2. `publishPokeReportClient`: Toma **únicamente** el Release Candidate probado y lo publica en los canales de distribución.

---

## 2. Comandos para el Administrador

### Paso 1: Generar y Auditar el Release Candidate
```powershell
.\gradlew.bat releasePokeReportClient
```
*(o en PowerShell directamente: `.\release-client.ps1`)*

**Qué hace este comando automáticamente:**
1. **Preflight**: Comprueba la rama activa, commit Git y estado del repositorio.
2. **Tests Unitarios**: Ejecuta `./gradlew test` en `mod/` para verificar la compilación y lógica de Kotlin/Java.
3. **Matriz de Actualización**: Ejecuta la suite de 25 pruebas del actualizador (`test_updater.py`), verificando firmas, atomicidad, rollbacks, exclusiones y bloqueos.
4. **Verificación de JARs**: Comprueba la presencia y frescura de `lunaeternal-0.1.0.jar` y `lunaneon-0.1.0.jar`.
5. **Sincronización Packwiz**: Actualiza la estructura de `client-pack/` y refresca el índice.
6. **Empaquetado Base Pack**: Genera `PokeReport-Base-0.2.0.mrpack` utilizando `packwiz`.
7. **Auditoría Anti-Leak**: Valida el archivo `.mrpack` verificando que **cero** mods exclusivos del servidor (`spark`, `easyauth`, `claimblocks`, `geyser`, etc.) o archivos sensibles (`.env`, `.db`, `auth.json`) se encuentren en el paquete.
8. **Manifest y Firma Ed25519**: Genera `client-latest.json` (schema v2) y lo firma digitalmente con la clave privada Ed25519 (`client-latest.json.sig`).
9. **Informes de Provenance**: Produce `release-info.json`, `build-report.txt` y los hashes `.sha256`.
10. **Prueba de Instalación Simulada**: Extrae el `.mrpack` en un entorno temporal aislado para confirmar que Prism Launcher o Modrinth App pueden leerlo e instalarlo sin fallos.

---

### Paso 2: Publicar el Release Candidate Aprobado
```powershell
.\gradlew.bat publishPokeReportClient
```
*(o en PowerShell directamente: `.\publish-client.ps1`)*

**Qué hace este comando automáticamente:**
1. **Verificación Estricta**: Comprueba que el Release Candidate existe y que sus hashes SHA-256 coinciden exactamente con lo registrado en el build report.
2. **Verificación Criptográfica**: Valida la firma Ed25519 con la clave pública antes de permitir la publicación.
3. **Idempotencia**: Si la versión ya fue publicada, lo detecta y no duplica ni altera artefactos existentes.
4. **Publicación Web**: Publica los metadatos globales (`build/client-release/latest.json`, `client-latest.json`, `client-latest.json.sig`).
5. **Anuncio para Discord**: Genera el payload de notificación `discord-announcement.json` listo para enviar al webhook de la comunidad.

---

## 3. Estructura de Salida (`build/client-release/`)

```text
build/client-release/
├── latest.json                             <- Metadatos web globales
├── client-latest.json                      <- Manifest firmado de la última versión
├── client-latest.json.sig                  <- Firma Ed25519 del manifest
└── 0.2.0/                                  <- Directorio inmutable del release
    ├── PokeReport-Base-0.2.0.mrpack        <- Modpack base para nuevos jugadores
    ├── PokeReport-Base-0.2.0.mrpack.sha256 <- Hash SHA-256 oficial del mrpack
    ├── client-latest.json                  <- Copia archivada del manifest
    ├── client-latest.json.sig              <- Copia archivada de la firma
    ├── release-info.json                   <- Trazabilidad y procedencia Git/Java
    ├── build-report.txt                    <- Resumen de compilación y pruebas
    └── discord-announcement.json           <- Payload formateado para Discord
```
