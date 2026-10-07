# CLIENT-QA-BLOCKER — diagnóstico localizado

2026-10-02, candidato base `5443a030`, SHA256 cliente `92bf8e95b6c6a3fc52fb1a022eff548d8d9a91a4d2fd9c28eaa8be5d5dba57dd`. Sin cambios de Java, economía, contenido ni producción en esta fase.

```text
Cliente inicia: YES
Cliente conecta: YES
Servidor acepta login: YES (login técnico; EasyAuth aún no autenticado)
Jugador entra al mundo: YES (lobby previo a autenticación)
Ventana renderiza: YES
Captura funciona: YES (F2 Minecraft); NO (capturador de ventanas)
Automatización controla cliente: YES (tecla F2 comprobada);
  interacción jugable completa: NOT TESTED
```

## Componentes y causas comprobadas

1. Cliente directo → backend: FabricProxy-Lite rechazó `PR_QA_Player` con `This server requires you to connect with Velocity` a las 17:57:54 UTC. TCP/status Minecraft funcionaron al finalizar el arranque. No era crash ni mismatch de mods.
2. Capturador de ventanas: `FrameArrived timed out: timed out waiting on channel` y, después de renovar selección, `window capture timed out: timed out waiting on channel`. La herramienta encuentra ventana/foco y permite F2, pero no devuelve frames. La causa interna del capturador no está determinada; no atribuirla al servidor ni cambiar drivers/seguridad sin evidencia.
3. Proxy QA añadido: Velocity existente copiado a `/srv/pokereport-prelaunch-proxy-20261002`, backend `127.0.0.1:25590`, listener `127.0.0.1:25592`, forwarding moderno y secreto exclusivo de QA. Proxy principal/configuración intactos. Contenedor QA limitado a 0.5 CPU/512 MB, heap 256 MB. Sin plugins externos copiados. Primera ejecución no pudo leer el JAR por permisos del directorio; se recreó únicamente ese contenedor con usuario explícito, manteniendo archivos restringidos.
4. Whitelist: el primer grant por nombre generó la identidad minúscula; Velocity offline transmitió `PR_QA_Player` con UUID sensible a mayúsculas. Backend rechazó la entrada a las 18:02:34 UTC. Se corrigió exclusivamente su entrada de whitelist en staging y se recargó. UUID de fixture: `518899f0-7b05-332d-95e9-85416a628eb3`; no es OP ni tiene grants explícitos. No se desactivó whitelist.
5. Conexión lograda: proxy aceptó y conectó al backend a las 18:04:42; servidor registró login a las 18:04:45 y PLAYER_CONNECT/LOBBY_READY a las 18:04:47 con auth pendiente; handshake Puerta protocolo 3 aceptado a las 18:04:49. Cliente registró carga de mundo a las 18:04:53 y prompt de registro a las 18:04:54.
6. Timeout EasyAuth: al no registrar la cuenta dentro del plazo, el servidor la expulsó. La captura F2 a las 18:05:58 UTC muestra ese mensaje. Es denegación de autenticación esperada, no prueba fallida de protección ni crash.

## Render, mods y evidencia

GPU AMD Radeon 780M Graphics, driver 32.0.31041.1004, escritorio 1920×1080, cliente 1024×700. OpenGL 3.2 inicializado y screenshot F2 correcto. No hubo nuevo crash report. Shader/resource packs locales cargaron; staging no exige resource pack externo. No se cambiaron resolución, GPU, shaderpacks ni drivers como workaround.

Comparación de metadata Fabric: 135 mods cliente identificables; Cobblemon 1.8.0 y candidato Luna coinciden. Diferencia compartida detectada: Fabric API cliente 0.116.17 frente a servidor 0.116.15. No se cambió: la conexión y carga de mundo se completaron. Esto no certifica todas las mecánicas/mods. Fabric Loader cliente 0.19.3 frente a staging 0.18.4 queda registrado como diferencia de entorno sin rechazo observado.

Logs fuente: `C:/Users/JUAN/.codex/prelaunch-qa/client-20261002/instances/Prelaunch QA/minecraft/logs/latest.log`, logs Docker de backend/proxy. No publicar logs del launcher: pueden incluir entorno y credenciales. Recibos sanitizados: `prelaunch/client-mod-diagnostics.json`, `qa-player-profile.json` y `client-status-probe.json`.

Screenshot inspeccionado: `C:/Users/JUAN/.codex/prelaunch-qa/client-20261002/instances/Prelaunch QA/minecraft/screenshots/2026-10-02_13.05.58.png`. Hora local Colombia UTC−5. Imagen original conservada fuera del repositorio; no incluye credenciales.

## Siguiente paso requerido

Una persona debe reconectar a `127.0.0.1:25590` por el túnel QA y completar `/register` con contraseña exclusiva de ensayo, sin compartirla aquí. La skill computer-use prohíbe automatizar diálogos de autenticación. No desactivar EasyAuth ni marcar la cuenta como premium para eludir el paso.

Después, ejecutar físicamente BREAK/PLACE y el checklist de MANUAL_CLIENT_QA.md. Con F2 + logs + timestamps se dispone de evidencia manual verificable; el fallo del capturador ya no impide preparar el QA. Casos jugables, batallas y multiplayer: **NOT TESTED** hasta ejecutarse. Staff real: pendiente de identidades/UUID confirmados. Backup coordinado y despliegue: retenidos hasta cerrar gates. No certificar apertura.
