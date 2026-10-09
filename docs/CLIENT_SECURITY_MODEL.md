# Modelo de Seguridad del Cliente y Actualizador

## 1. Fundamentos de Seguridad

El sistema de actualización del cliente de **PokeReport Network** implementa un modelo de seguridad por capas con defensa en profundidad para proteger al jugador de modificaciones no autorizadas, ataques de intermediarios (MITM), inyecciones de rutas y daños accidentales a sus datos personales.

---

## 2. Firma Criptográfica Asimétrica (Ed25519)

### Arquitectura de Claves
- **Algoritmo**: Ed25519 (estándar RFC 8032) sobre curva elíptica Curve25519.
- **Clave Privada (`pokereport_release_ed25519_private.der`)**:
  - Reside **exclusivamente** en el entorno seguro de compilación y empaquetado del equipo de desarrollo (`tools/keys/`).
  - **NUNCA** se distribuye al cliente ni se incluye en repositorios públicos.
- **Clave Pública (`pokereport_release_ed25519_public.der`)**:
  - Embebida de forma estática en el código del `PokeReportUpdater` (`EMBEDDED_PUBLIC_KEY_B64`).
  - Valor: `MCowBQYDK2VwAyEAKI58PNfegf7pWop3OTa6ttewQ1CrkVQMU1dgAXLzP/s=`

### Procedimiento de Verificación
1. El cliente descarga `client-latest.json` y `client-latest.json.sig`.
2. Antes de interpretar cualquier campo o URL, el updater ejecuta:
   ```java
   sig.initVerify(publicKey);
   sig.update(manifestBytes);
   if (!sig.verify(signatureBytes)) {
       throw new SecurityException("Firma del manifest inválida");
   }
   ```
3. Si la firma no coincide o el manifest ha sido alterado aunque sea por un solo bit, la ejecución **aborta inmediatamente** sin descargar ningún archivo ni tocar la instalación.

---

## 3. Integridad Criptográfica de Archivos (SHA-256)

- Cada entrada en el manifest contiene su hash SHA-256 precalculado y su tamaño en bytes exacto.
- Durante la fase `VERIFY`:
  - Se valida el tamaño exacto del archivo descargado en staging.
  - Se computa el digest SHA-256 contra el flujo completo de bytes.
  - No se confía en cabeceras HTTP `Content-Length`.

---

## 4. Seguridad de Rutas y Prevención de Inyecciones

Para evitar ataques de tipo Zip Slip, Path Traversal o escrituras fuera de la carpeta del juego:
- Se validan todas las rutas relativas declaradas en el manifest.
- **Prohibido terminantemente**:
  - Rutas que contengan secuencias `../` o `..\`.
  - Rutas absolutas (`C:\...`, `/etc/...`).
  - Rutas con caracteres de escape UNC o comodines.
- Cada ruta se normaliza y se valida mediante `path.startsWith(instanceRoot)`.

---

## 5. Política Estricta de URLs y Dominios

- Solo se aceptan conexiones que utilicen protocolo **HTTPS** (`https://`). Conexiones `http://`, `file://` o `ftp://` son rechazadas en entornos remotos.
- Los dominios remotos deben pertenecer a la allowlist explícita:
  - `pokereport.online` (y subdominios)
  - `github.com`
  - `githubusercontent.com`
  - `objects.githubusercontent.com`
  - `modrinth.com`
  - `cdn.modrinth.com`

---

## 6. Protección de Archivos y Privacidad del Usuario

El updater tiene prohibido modificar o eliminar cualquier archivo que no esté marcado con `managed=true` en `.pokereport/installed.json`.

### Deny-List Absoluta
Bajo ninguna circunstancia el updater toca:
- `screenshots/` (capturas de pantalla del jugador).
- `saves/` (mundos individuales locales).
- `logs/` y `crash-reports/`.
- `options.txt` (sensibilidad, controles y ajustes de vídeo).
- `servers.dat` (excepto en la instalación inicial si el archivo no existe).
- Mods personales no administrados colocados por el jugador en `mods/`.
- Credenciales, cuentas de Microsoft o tokens de inicio de sesión.
