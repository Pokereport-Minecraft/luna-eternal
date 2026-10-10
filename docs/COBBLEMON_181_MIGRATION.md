# Cobblemon 1.8.1 — migración dirigida, 2026-10-10

## Conservación y compatibilidad

Base cliente0.3.0 / bundle0.1.4, Minecraft1.21.1, Java21. Cliente Fabric0.19.5; servidor mantiene0.18.4. Se parte del manifiesto REAL del launcher en release pack-manifest, SHA1 `0573817c70d2163ffb0c6777ac853a310f730c7f`; latest.json de master era obsoleto.

Servidor: únicamente Cobblemon1.8.0→1.8.1 y TMCraft1.4.19+1.8.0→1.4.19+1.8.1. Luna conserva SHA256 `ed83825868a26ed50d333cb9fc9c33e9ccfb0098959b1a34a3df40d38174a18e`. RCT/MegaShowdown/ZaMega, mundos, configuración y SQL conservados. No rollback de datos.

| Cliente | Versión | Modrinth ID |
|---|---|---|
| Cobblemon Fabric | 1.8.1+1.21.1 | gBW3vLC7 |
| TMCraft | 1.4.19+1.8.1 | cP1BvBXr |
| Tim Core | 1.8.1-fabric-1.32.1 | hnix66jw |
| Capture XP | 1.8.1-fabric-1.3.0 | FOUiJc3h |
| Chat Heads | 0.15.1 | Xxx6dqjP |
| ChatAnimation | 1.1.3 | mezhyzfP |

TMCraft, Tim Core y Capture XP anteriores requieren EXACTAMENTE Cobblemon1.8.0. La prueba de cliente descubrió los dos últimos: no saltarse el resolutor alterando metadata. Tim Core1.32.1 corrige refmap del port sin adoptar handlers nuevos de1.33.x. Tim/Capture no estaban en el servidor.

Launcher164→166 archivos:160 entradas ajenas idénticas. Los10 elementos del manifiesto nativo conservados; cambia base/bundle/notas. Los177 overrides del MRPACK se conservan byte por byte: solo cambia el índice, con cuatro sustituciones y dos adiciones. Luna cliente conserva SHA256 `5044dc8bc5446977b0a226d0c69aa95d4535f3516a4e71ce55fec8d9096fa3e6`. Poképad, menú/cargas Halloween, regreso en noviembre y premios conservados.

## Validación real

- Compilación contra1.8.1:518 tests,0fail,2skipped. Build completo conserva60 SQL idénticos, pero contiene el parche adicional de combate y **NO se promociona**.
- Staging `/srv/pokereport-prelaunch-20261002`, DB `prelaunch_20261002`:836 PASS con candidato completo y836 PASS con el Luna EXACTO de producción. Segunda ejecución: `/root/pokehardening-luna-1791608345.txt`, log04:59:03UTC.
- Tres pruebas de barrera/orden de batalla PASS en candidato completo. No certifican un combate humano contra Blaine; ese trabajo sigue pendiente por petición del usuario.
- Cliente completo aislado `/srv/pokereport-client181-qa`, Java21/Fabric0.19.5, Xvfb/Mesa, sin credenciales ni conexión a producción: arranque52s, recursos/mixins PASS, sin errores críticos de inyección/LinkageError. `qa-result.json` identifica siete versiones/hashes reales. Shaders/resolución reducidos SOLO en QA; preferencias de jugadores intactas. No medición de FPS/VRAM ni prueba multijugador real.
- Errores de red/auth/telemetría/audio esperables en entorno sin red/tarjeta. Avisos de sombreros inexistentes y nombres inválidos están en assets anteriores sin cambios; quedan pendientes y no se presentan como resueltos.
- Bot v4:14 tests PASS. manualUrl independiente del directorio de Luna permite migración de base conservando su JAR.

## Distribución y operación

Preparación sin promoción de punteros en `https://pokereport.online/client/0.3.0-cobblemon181-20261010/`.
MRPACK SHA256 `8d21de2ce7bdd8d9601c111b82350755ac4f83f2c7019dda39aeb0c2321031fa`.
ZIP manual SHA256 `5313c8dc43b9ced9ba0e947eada38255ce8b378ccd4d70d8493ed4ac550152ce`.

ZIP contiene Luna y LEEME; cuatro mods obligatorios y dos nuevos se descargan directamente de sus autores con hashes. **Reemplazar únicamente Luna NO migra la base.** MRPACK importa completo; launcher oficial sincroniza y elimina JAR gestionados anteriores. No prometer actualización automática en otros launchers.

`tools/releases/deploy_cobblemon181.py` valida hashes vivos/QA/staging, avisa60s, save-all flush y parada Wings. SIGTERM únicamente después de guardado de dimensiones/cierre SQL. Dos JAR anteriores salen fuera de mods; restantes verifican hash idéntico. No SIGKILL ni restauración de mundos/DB. Punteros se promocionan después del arranque comprobado.

Canal opcional manual `1558346924715675658`, sin ampliar cinco canales iniciales. Bot publica versiones una sola vez sin ping en ese canal. Anuncio actual con imagen y @everyone/@Entrenador solicitado expresamente; permiso comprobado en anuncios. Chats/tickets conservados.

## Estado de despliegue

Producción migrada: arranque real `Done (3.722s)` a05:31UTC del10octubre (00:31Bogotá), Cobblemon1.8.1+1.21.1 y TMCraft1.4.19+1.8.1 confirmados en log.70 JAR ajenos idénticos, Luna conserva hash. Respaldo de binarios fuera de mods: `/root/backups_predeploy/cobblemon181-20261010`. No se reinició de nuevo para completar la publicación.

Launcher/publicación web completados tras reanudar la tarea; manifiesto SHA1 `213990cf8e480737dd88bccb626133c958a69598`, fallback de master idéntico. Descarga HTTPS INTEGRA del MRPACK validada por SHA256 y ZIP; firma Ed25519 pública válida; botones de entrada apuntan a0.3.0. Las versiones anteriores siguen como artefactos históricos; ninguna restauración de datos.

Bot v4 activo,14 tests PASS, gateway/tickets/DB sanos; tarjeta cliente0.1.4 y tarjeta manual con cinco botones, incluido JAR directo. Anuncio único `1558390719260004445` con imagen, mention_everyone=true y rol Entrenador `1555769436378759268`; no se duplicó al recuperar el envío. Link: https://discord.com/channels/1537115743898832906/1537117626927554682/1558390719260004445.

Muestra08:11:44UTC (03:11Bogotá):0 jugadores,20TPS en5s/10s/1m/5m/15m, mediana0.7ms, p95 de tick1.1ms; memoria de contenedor7.702GiB, CPU Docker10.19% de un core y Spark1% de capacidad total. No comparar directamente con la muestra anterior de7 jugadores ni atribuir la diferencia de RAM a una optimización demostrada. Prueba de combate humano y medición GPU/FPS siguen pendientes.
