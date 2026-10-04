> Actualización: la excepción de OP/staff para el PC remoto fue eliminada por petición del usuario. La política vigente está en REMOTE_PC_COMMERCIAL_RANK.md: Campeón/Maestro/Leyenda exclusivamente.

# PC remoto, ajustes iniciales y Angry Alphas — 2026-10-03

## Acceso al PC

Los PCs físicos de Ciudadela siguen disponibles para todos. La tecla **P** (`key.lumymon.access_pc`) y **/pc** requieren **Campeón, Maestro o Leyenda**. Se preservan los accesos existentes del staff y operadores. El servidor controla el paquete de LumyMon, por lo que cambiar teclas o enviar el paquete directamente no evita el control. También se respeta el bloqueo de acceso inicial y no se abre el PC durante combates. El enlace PCLink autoriza correctamente las operaciones del PC remoto.

## Ajustes de jugadores

`launcher-defaults/options.txt` contiene una instantánea de los ajustes actuales del usuario, con las teclas conservadas. Se retiran el enlace y recurso de Music Notification, y se desactiva la pestaña de objetos de operador en la plantilla pública. El launcher recibe `options.txt` con **once=true**: únicamente lo descarga si no existe; no compara ni restaura los cambios personales, tampoco en modo Reparar.

`defaultoptions.journal.json` también es inicial y marca los recursos visuales como aplicados: evita que Default Options sustituya la selección de recursos recién entregada. Default Options 21.1.8 lee las teclas presentes en options.txt y no reemplaza esas asignaciones. No se distribuyen sesiones, credenciales ni archivos personales de servidores.

Music Notification se retira del manifiesto. Ese componente proporcionaba Open Jukebox y las notificaciones de pistas; no se elimina la música del juego. La limpieza administrada del launcher retira el JAR en la próxima sincronización. Todas las entradas del pack ajenas al JAR propio, Music Notification y estos dos archivos iniciales se conservan idénticas.

## Angry Alphas

Se fija **W’s Angry Alphas 1.1.0**, versión Modrinth `9chEWhXr`, compatible con Minecraft 1.21–1.21.1. Archivo oficial intacto, SHA1 `d2b5f740966c7d010583afdf3c682955aadf9994`; Cobblemon 1.8.0 dispone de Pokémon alfa. Se instala como datapack del servidor, no como mod del launcher.

El archivo original dirige su tick al overworld. `pokereport-angry-alphas-worlds.zip` añade llamadas a la función oficial en Nether, End, los seis Salvajes e Islas Naranja. No modifica sus reglas de agresividad ni los spawns. Se puede reproducir con `python tools/prepare_angry_alphas.py`. Las funciones y los datapacks cargaron en staging sin errores de sintaxis.

## Validación y despliegue

- Build: 239 pruebas, **237 PASS**, 2 SKIPPED, 0 errores.
- Staging: **779 PASS**, 0 FAIL.
- Launcher LunaSync: **15 PASS**, incluido conservar archivos once modificados.
- Plantilla: todas las teclas existentes conservadas excepto Jukebox; todos sus recursos visuales disponibles en el manifiesto o su archivo de recursos.
- Backup coordinado antes del cambio: `/var/backups/pokereport/preopen-fixes/20261003T233132Z`, incluye mundo, configuraciones, JAR anterior y SQL.
- JAR de producción SHA256: `5fb69284c8b3e4086d82f56b30478aa8cfea7a883d93d950c1e66059fc0b21fa`.
- Voice Chat y EasyAuth: configuraciones idénticas antes/después; esquema SQL 57.

La validación jugable de P con cuentas de cada rango, una instalación nueva y el encuentro real con un alfa todavía requieren cliente real. Las comprobaciones automáticas no se presentan como ese QA ni como certificación de apertura.

Producción: arranque confirmado `Done (4.887s)` a las 23:33:41 UTC; Voice Chat activo en UDP 24591. Ambos datapacks figuran habilitados y la función de dimensiones respondió. Publicación del cliente verificada por descarga y SHA1; puntero actual `38a9240999e7410561fb5992f7f9ea9bcc75ed75` (ver player-defaults-publication.json).
