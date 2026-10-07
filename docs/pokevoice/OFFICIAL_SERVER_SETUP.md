# PokeVoice — integración oficial de servidor

## Estado actual — 3 de octubre de 2026

**Desplegado en el servidor principal por petición expresa del usuario.** Cliente publicado para instalación automática: manifest-ffb7b92111.json. Motor oficial SVC 2.6.22 Release, Flashback Constructor 0.39.8 Release. Producción UDP 24591, allocation Pterodactyl 22; mapping de Wings verificado, backend privado preservado. Arranque, migración 57, caché y ping externo PASS (113 ms). Build 230 PASS/2 SKIPPED; staging 763 PASS/0 FAIL. Backup coordinado privado de 16 GB y SQL, restauración SQL (68 tablas), JAR/config seleccionados PASS; restauración completa de mundo NOT RUN. Audio real y nueva captura de interfaz NOT RUN: despliegue no equivale a certificación de apertura. Evidencia production-deployment.json y production-backup-verification.json.

Las fases siguientes conservan antecedentes de staging; cualquier referencia antigua a producción intacta o publicación pendiente queda supersedida por este estado.


Propósito: documentar la adaptación al stack real sin fork del motor ni confundir transporte con QA de audio.

## Fuentes consultadas

- https://modrepo.de/minecraft/voicechat/wiki/server_setup
- https://modrepo.de/minecraft/voicechat/wiki/proxy_setup
- https://modrepo.de/minecraft/voicechat/wiki/server_config
- https://github.com/henkelmax/simple-voice-chat
- https://github.com/henkelmax/simple-voice-chat/blob/1.21.1/common/src/main/java/de/maxhenkel/voicechat/voice/server/Server.java
- https://github.com/henkelmax/simple-voice-chat/blob/1.21.1/common/src/main/java/de/maxhenkel/voicechat/voice/server/ServerVoiceEvents.java
- https://github.com/henkelmax/simple-voice-chat/blob/1.21.1/common-client/src/main/java/de/maxhenkel/voicechat/voice/client/ClientManager.java
- https://github.com/henkelmax/svc-cli-utils

El branch remoto 1.21.1 puede avanzar: no sustituir el binario Release fijado por un build de ese branch. La documentación sirve para verificar flujo de configuración, handshake, fuentes de audio y autenticación, no para atribuir un checksum a código remoto mutable.

## Stack actual

Fabric mod en cliente y servidor; Simple Voice Chat Release 2.6.22 con API 2.6.20. PokeVoice se registra como plugin oficial. No se recompila ni modifica SVC. Opus, micrófono, cifrado nativo, atenuación, PTT, grupos y UDP permanecen en SVC.

Staging usa Docker network=host, backend TCP 127.0.0.1:25590 y Velocity público 25595. Elegida la alternativa oficial sin plugin SVC en el proxy: cada backend anuncia su propio endpoint público UDP. No se necesita allocation Pterodactyl para este contenedor independiente con red host.

Configuración efectiva staging:
```
port=24590
bind_address=0.0.0.0
voice_host=15.235.16.131:24590
max_voice_distance=32.0
whisper_distance=8.0
enable_groups=true
force_voice_chat=false
```

UFW permite 24590/udp y 25595/tcp. La prueba oficial `svc ping 15.235.16.131:24590 --attempts 3` desde el equipo del usuario recibió respuesta en 87 ms: **PASS transporte UDP público**. No acredita audio bidireccional, permisos de micrófono ni grupos.

## EasyAuth y conexión inicial

Logs cliente: envía request_secret al entrar, sin respuesta. Logs servidor: ausencia de Received secret request. EasyAuth extended.conf bloquea custom payload antes del login. Código oficial ClientManager envía handshake al entrar y no reintenta tras EasyAuth; ServerVoiceEvents necesita ese payload para registrar compatibilidad y entregar el secreto.

Adaptación mínima en staging: permitir solo `voicechat:request_secret` en allowed-custom-packets, sin permitir todo voicechat ni allow-all-packets. Evento oficial MicrophonePacketEvent cancela audio de usuarios sin autenticación/presencia viva usando caché servidor. Las acciones de grupos siguen bloqueadas por EasyAuth antes del login; el filtro de relaciones continúa limitado a SOURCE_PROXIMITY. Auth gate no impone KNOWN a grupos nativos. No se escribe SQL desde el hilo de audio.

## Pterodactyl en producción

El contenedor principal usa bridge y publica solo 25567 TCP/UDP en 172.18.0.1. Añadir una allocation al panel es necesario para persistir el puerto extra; abrir UFW por sí solo no publica un puerto ausente del contenedor.

Plan de promoción directa, preservando Velocity actual:
1. Elegir puerto distinto al de staging, por ejemplo 24591 si libre.
2. Crear/asignar allocation del nodo al servidor principal mediante Panel/Application API; no modificar solamente Docker a mano, Wings sobrescribiría el mapping.
3. Configurar SVC `port=24591`, `bind_address=*`, `voice_host=15.235.16.131:24591` dentro del volumen principal.
4. Verificar mapping UDP host→contenedor después del reinicio gestionado por Wings y ruta/firewall; backend Minecraft y RCON siguen privados.
5. Instalar el mismo motor Release y JAR propio, aplicar adaptación de EasyAuth verificada y conservar backup coordinado.
6. Ejecutar svc ping y prueba real de dos equipos. En juego, `/voicechat test <nombre>` exige ejecutor jugador; consola RCON devuelve rechazo y no se considera PASS.

Alternativa oficial multi-backend: plugin SVC de Velocity, UDP público en proxy y puertos privados distintos por backend. No mezclar esta alternativa con voice_host directo: el plugin ignora voice_host del backend. Si se adopta, instalar y validar en todos los backends participantes.

Estado: infraestructura de voz activa en staging; producción principal aún no modificada. Transporte PASS, handshake tras ajuste y audio real pendientes de reconexión de ambos clientes.

## Interfaz

Corrección del blur: override renderBackground vacío evita que Screen.render aplique desenfoque después de dibujar el PokéPad. Se conserva textura existente; botones propios carbón/dorado, tab activo, foco/hover y filas delimitadas. Estado de relación conocido separado de conexión UDP real. Build verificado; captura nueva cliente pendiente.
