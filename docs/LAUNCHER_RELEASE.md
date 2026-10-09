# POKEREPORT NETWORK — INFORME DE INTEGRIDAD DEL LAUNCHER Y ACTUALIZADOR

**Fecha:** 8 de Octubre de 2026  
**Versión de Actualizador (Updater):** `1.0.0`  
**Esquema de Manifest:** Schema v2  
**Manifest Activo:** `client-latest.json`  
**Firma Criptográfica:** Ed25519 (`client-latest.json.sig`)  
**Estado:** **CERTIFICADO — 25/25 PRUEBAS DE ACTUALIZADOR APROBADAS (100% PASS)**

---

## 1. RESULTADOS DE LA SUITE DE PRUEBAS DEL ACTUALIZADOR (U01 – U25)

Se ha ejecutado la matriz completa de pruebas de seguridad y robustez transaccional del actualizador:

| Test ID | Caso de Prueba | Resultado | Descripción |
| :---: | :--- | :---: | :--- |
| **U01** | Instalación al día -> NOOP | **PASS** | No descarga si los archivos ya coinciden. |
| **U02** | Actualización de un JAR | **PASS** | Reemplazo atómico individual con verificación SHA-256. |
| **U03** | Actualización múltiple de JARs | **PASS** | Descarga e instalación por lotes sin inconsistencias. |
| **U04** | Fallo en descarga secundaria | **PASS** | 0 cambios aplicados; rollback limpio si falla 1 archivo. |
| **U05** | Hash SHA-256 corrupto | **PASS** | Aborta la instalación de forma preventiva. |
| **U06** | Manifest sin firma válida | **PASS** | Rechaza actualización sin firma criptográfica. |
| **U07** | Manifest adulterado tras firma | **PASS** | Detección inmediata de alteración Ed25519. |
| **U08** | Corte o timeout de red | **PASS** | La versión previa permanece intacta y jugable. |
| **U09** | Caída durante fase DOWNLOAD | **PASS** | Limpieza automática de temporales; sin archivos corruptos. |
| **U10** | Caída durante fase COMMIT | **PASS** | El journal transaccional recupera el estado previo. |
| **U11** | Minecraft en ejecución / bloqueo de archivos | **PASS** | No corrompe archivos en uso; emite aviso claro. |
| **U12** | Mods personales del usuario en `mods/` | **PASS** | **Nunca se eliminan mods añadidos por el usuario.** |
| **U13** | Opciones y configs personales | **PASS** | Preserva opciones de vídeo, controles y shaders. |
| **U14** | JAR administrado obsoleto | **PASS** | Retiro limpio del JAR antiguo tras éxito total. |
| **U15** | Modpack base incompatible | **PASS** | Bloqueo preventivo si la versión base no coincide. |
| **U16** | Versión de updater obsoleta | **PASS** | Exige actualización del binario del updater. |
| **U17** | Incompatibilidad de protocolo | **PASS** | Mensaje descriptivo al usuario impidiendo arranque erróneo. |
| **U18** | Host no autorizado (Anti-MITM) | **PASS** | Solo permite dominios oficiales del repositorio. |
| **U19** | Intento de Path Traversal (`../`) | **PASS** | Bloqueo estricto de escape de directorio. |
| **U20** | Enlaces simbólicos / Junction escape | **PASS** | Validación estricta del árbol de directorios. |
| **U21** | Ejecución duplicada | **PASS** | Idempotencia total sin operaciones redundantes. |
| **U22** | Cache remoto de manifest | **PASS** | Headers de Cache-Control aplicados correctamente. |
| **U23** | Reinicio antes de limpieza | **PASS** | Detección persistente del estado de instalación. |
| **U24** | Simulación de disco lleno | **PASS** | Comprobación preventiva de cuota de almacenamiento. |
| **U25** | Permisos de escritura insuficientes | **PASS** | Notificación legible sin crasheo de proceso. |

---

## 2. COMPORTAMIENTO PARA EL JUGADOR (PLAYER ACTION)

Dado que Progression 2.0 es una actualización clasificada como **CASE A — SERVER ONLY**, el manifest del cliente (`client-latest.json`) en producción permanece en la versión estable `0.1.0`.

- **¿Los jugadores tienen que reiniciar el launcher?**
  **NO — NO REQUIERE ACTUALIZACIÓN DEL LAUNCHER.**
- Los jugadores activos y nuevos pueden acceder directamente al servidor sin descargas adicionales.
