# PokeVoice — candidato y QA

## Estado actual — 3 de octubre de 2026

**Desplegado en el servidor principal por petición expresa del usuario.** Cliente publicado para instalación automática: manifest-ffb7b92111.json. Motor oficial SVC 2.6.22 Release, Flashback Constructor 0.39.8 Release. Producción UDP 24591, allocation Pterodactyl 22; mapping de Wings verificado, backend privado preservado. Arranque, migración 57, caché y ping externo PASS (113 ms). Build 230 PASS/2 SKIPPED; staging 763 PASS/0 FAIL. Backup coordinado privado de 16 GB y SQL, restauración SQL (68 tablas), JAR/config seleccionados PASS; restauración completa de mundo NOT RUN. Audio real y nueva captura de interfaz NOT RUN: despliegue no equivale a certificación de apertura. Evidencia production-deployment.json y production-backup-verification.json.

Las fases siguientes conservan antecedentes de staging; cualquier referencia antigua a producción intacta o publicación pendiente queda supersedida por este estado.


Propósito: consentimiento para voz de proximidad, integrado en el PokéPad existente.
Estado: candidato de staging; producción sin cambios. Rama `codex/pokevoice`, base `a74d626d`.

## Motor y alcance

Simple Voice Chat Fabric 1.21.1 **2.6.22**, API **2.6.20**, Java 21. Motor oficial, sin fork ni implementación propia de audio.
Fuente: https://modrinth.com/plugin/simple-voice-chat/version/VS5X7jZd
API: https://modrepo.de/minecraft/voicechat/api/getting_started
SHA-512 del motor: `c44e69400ac7349ad6d4699263bde4d87359e03233c31a872eb02542658cfc73a185b46e4361c54cb1ac029a2991bd8eb4d4ea59b1c3e3b43f234087520b325a`.

El filtro interviene exclusivamente en SOURCE_PROXIMITY. GROUP, PLUGIN y SPECTATOR conservan su comportamiento nativo. Los bloqueos descritos aquí protegen proximidad e invitaciones; no prometen aislamiento dentro de grupos nativos compartidos. Las pantallas de micrófono, altavoces, volumen y grupos se abren mediante sus controles nativos, respetando las teclas configuradas.

## Autoridad y persistencia

UUID como identidad. Invitaciones a un máximo de 12 bloques, misma dimensión, ambos autenticados y vivos. Caducidad 30 s, rechazo 60 s, máximo 3 pendientes; solicitudes del cliente limitadas a 4/s y cola de control limitada a 128. Cada aceptación revalida nonce, destinatario, presencia y consentimiento. Un bloqueo domina sobre la relación; desbloquear no restaura consentimiento.

V057 registra relaciones y preferencias en MariaDB. Invitaciones efímeras; relaciones conocidas sobreviven al reinicio y al cambio de dimensión. El trabajador de control hace SQL; el filtro de audio consulta una instantánea en memoria. Un error de carga o escritura cierra proximidad. No se graba audio.

C2S `lunaeternal:pokevoice_request_v1`: acción, UUID objetivo, nonce, booleano y página; 36 bytes. Sin posición, identidad del actor ni estado autorizado del cliente. S2C `pokevoice_snapshot_v1`: JSON acotado a 32768 caracteres y 50 compañeros por página, visible solo para su destinatario.

Configuración servidor: `config/pokevoice.properties`. Predeterminados: enabled=true, require_relationship_for_proximity=true, invite_distance=12, invite_expiration_seconds=30, reject_cooldown_seconds=60, max_pending_invitations=3, show_nearby_hud=true. Consultar VoiceConfig.java para los nombres exactos y límites. Deshabilitar la función mantiene la proximidad cerrada. Configuración de transporte independiente: `config/voicechat/voicechat-server.properties`.

## Validación automática

Build: **PASS**, 226 aprobadas, 2 omitidas, 228 descubiertas. Pruebas de relaciones, fraude de aceptación, replay, caducidad, bloqueo, consentimiento, concurrencia y fallo de persistencia; adaptador de eventos conserva fuentes no proximidad. Las pruebas omitidas heredadas no se cuentan como PASS. Staging autotest: **PASS**, 763 aprobadas, 0 fallos a las 15:22:14 UTC. Últimas correcciones compiladas: SHA256 `fc4eb59b97c4603622113376aa4a406796154357d52eed7ec41e355032d01015`; reinicio y autotest del JAR final PASS; migración 57 verificada en MariaDB.

Microbenchmark: `filter-benchmark.csv`, dos millones de decisiones por tamaño 2/10/25/50, 18–25 ns por decisión en esta ejecución. Cero llamadas al repositorio durante el audio. Existe asignación pequeña de pares; el delta de heap no equivale a memoria retenida. **No es una prueba de TPS, paquetes UDP ni audio real.**

## Prueba real con dos equipos

Usuario confirmó disponibilidad de dos equipos. Falta acordar segundo nombre, IPs públicas, acceso restringido y perfiles de cliente separados. No publicar el candidato en el manifest de producción.

Registrar por fila: PASS / FAIL / BLOCKED / NOT RUN, UTC, nombres, dimensión/coordenadas, captura y extracto de log. Confirmar audio en ambos sentidos; no basta con ver el icono del micrófono.

| Caso | Procedimiento y resultado esperado | Estado |
|---|---|---|
| Transporte | Conectar ambos al staging, autenticar EasyAuth, comprobar conexión SVC UDP | NOT RUN |
| Desconocidos | A y B cerca: ambos hablan, ninguno oye proximidad del otro | NOT RUN |
| Invitación | PokéPad → PokeVoice → Cercanos → Solicitar; B acepta antes de 30 s | NOT RUN |
| Conocidos | Ambos oyen voz espacial, PTT y activación; volumen y susurro nativos funcionan | NOT RUN |
| Distancia | Alejarse fuera del rango nativo: desaparece audio, volver lo recupera | NOT RUN |
| Dimensiones | Ciudadela, overworld, nether, end y las otras dos dimensiones habilitadas: relación persiste, voz nunca cruza mundos | NOT RUN |
| Rechazo | Rechazar; nueva invitación durante cooldown se deniega | NOT RUN |
| Bloqueo | Bloquear: cesa proximidad bidireccional y no admite invitaciones; desbloquear requiere nueva aceptación | NOT RUN |
| Preferencias | Desactivar solicitudes y luego voz de conocidos: verificar denegación respectiva | NOT RUN |
| Grupos | Crear/unirse mediante interfaz nativa; voz de grupo funciona independientemente de la proximidad | NOT RUN |
| Interfaz | Inventario, chat, PokéPad y batalla no se reemplazan por una invitación; avatar, HUD y controles legibles | NOT RUN |
| Reconexión | Desconectar/reconectar, morir y teletransportarse: pendientes invalidan, sin consentimiento nuevo accidental | NOT RUN |
| Reinicio | Reiniciar únicamente staging: conocidos/bloqueos/preferencias persisten, pendientes desaparecen | NOT RUN |
| UDP perdido | Cerrar transporte en cliente: indica desconexión, no concede voz; restaurar sin estado obsoleto | NOT RUN |
| Carga real | 2/10/25/50 usuarios y varias voces: TPS, CPU, heap, GC, tráfico, latencia p95/p99 | NOT RUN |

## Staging, backup y rollback

Servidor aislado `/srv/pokereport-prelaunch-20261002`; DB `prelaunch_20261002`. TCP backend 127.0.0.1:25590 y UDP inicial 127.0.0.1:24590. El bind local no demuestra acceso externo. No se ha abierto UDP público ni modificado production.
Backup previo: `/var/backups/pokereport/pokevoice/20261003T150620Z`, JAR anterior, dump SQL y configuraciones; contiene secretos y permanece privado. Falló el primer arranque por una copia parcial del JAR: corregida con copia completa y hash validado. Arranque posterior PASS: Done a las 15:18:22 UTC; motor UDP iniciado a las 15:18:25 UTC; caché cargada a las 15:18:31 UTC. Bind verificado con ss. El motor advierte modo offline (backend existente detrás de Velocity/EasyAuth): no equivale a autenticación premium ni permite afirmar seguridad criptográfica completa; validar forwarding, autenticación y acceso restringido antes de promoción. Hay avisos de entidades/items existentes ajenos a PokeVoice; conservarlos en evidencia.

Rollback: detener únicamente staging, conservar evidencia, restaurar su JAR/configuración desde ese backup y retirar el motor nuevo. Restaurar DB solo con servicio detenido y tras revisar el dump; nunca ejecutar este procedimiento sobre producción.

## Condiciones de promoción

**BLOCKED para despliegue:** falta QA real de dos clientes y benchmark de carga real. No merge ni producción. Antes de promover: actualizar JAR con las últimas correcciones, repetir build y autotest en staging, validar cliente/UDP/interfaz, reinicio y matriz de audio; backup coordinado independiente de producción; smoke test y evidencia explícita. Ningún NOT RUN se convierte en PASS por ausencia de errores.

## Acceso de prueba autorizado

Usuario autorizó acceso sin restricción por IP. Proxy de staging público `15.235.16.131:25595`, UDP `24590`, whitelist TheJuanCE/Freddysillo. Backend y RCON permanecen en loopback. Reglas UFW exclusivas 25595/tcp y 24590/udp; producción sin cambios. Configuración anterior de Velocity respaldada junto al backup. Kit de cliente local: `C:/Users/JUAN/.codex/pokevoice-artifacts/client-candidate`; instalar solo en copias independientes del perfil existente. Audio real sigue NOT RUN.

## Actualización automática autorizada

Publicado `manifest-f83d7644f3.json` y actualizado el puntero compartido del launcher. Solo cambia el JAR propio y agrega Simple Voice Chat; las otras 159 entradas y cabecera se preservan. Descargas y hashes remotos PASS. Esto publica el cliente para instalación automática; no despliega PokeVoice al servidor Minecraft principal. QA de audio real NOT RUN. Cerrar Minecraft y pulsar Jugar en launcher, después conectar a `15.235.16.131:25595`. Puntero anterior y evidencias en launcher-publication.json.

## Corrección de compatibilidad del perfil Constructor

La primera publicación falló en cliente: Flashback 0.39.9 declara breaks voicechat <2.6.23. Revisadas versiones oficiales de Modrinth y metadatos internos. Motor 2.6.23/24 son beta; mantener 2.6.22 Release. Flashback 0.39.8 Release admite MC >=1.21 <1.21.2 y no declara conflicto voicechat/API. Publicado manifest-357c9ecd0e.json, solo reemplaza Flashback del perfil Constructor; mantiene 160 entradas restantes. Hash descargado y puntero remoto PASS; arranque real de cliente NOT RUN. No se borran grabaciones de usuario; reproducción/grabación de Flashback requiere QA. Evidencia flashback-compatibility-fix.json.

## Integración oficial y corrección visual

Ver OFFICIAL_SERVER_SETUP.md. Build actualizado: 230 PASS, 2 SKIPPED, 232 descubiertas. Ping externo oficial PASS, 87 ms. Causa del estado sin UDP: handshake inicial bloqueado por EasyAuth antes del login; autorizar únicamente request_secret y proteger micrófono con evento oficial autenticado. Carcasa conserva textura original, override evita blur sobre contenido, controles propios y estado de relación separado de conexión real. Captura y audio tras actualizar: NOT RUN.
