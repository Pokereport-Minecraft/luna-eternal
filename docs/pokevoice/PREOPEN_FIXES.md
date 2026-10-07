# Correcciones de preapertura — 2026-10-03

Se conserva el candidato 165cdfe4 y todos los cambios anteriores en el worktree codex/pokevoice. No se modifica economía, precios, mundo o permisos de staff.

## Inventario
Los tres buffs gestionados de constructor (velocidad IV, visión nocturna y supersalto III) se filtran solamente en la lista del inventario. Deben coincidir tipo, nivel, duración infinita, ambient, sin partículas y sin icono. Siguen activos; las pociones normales permanecen visibles. El redirect apunta al método de Minecraft 1.21.1 verificado en el bytecode mapeado. Verificación visual en cliente real: PENDIENTE después de actualizar.

## Ciudadela
Se permite operar el PC y curador de Cobblemon y sillas, bancos y sofás de Handcrafted, incluso llevando bloques/herramientas sin agacharse. La colocación continúa bloqueada en BlockItem.place; no se abren cofres ni contenedores administrativos. El modo aventura puede mantenerse. Uso físico de PC y silla con cuenta normal: PENDIENTE.

## PokeVoice: revisión de controles
| Control | Cambio o cobertura | Prueba cliente real |
|---|---|---|
| Inicio y pestañas | Navegación preservada, selección visible | PENDIENTE |
| Anterior / Siguiente | Deshabilitados al llegar al límite; color distingue estado | PENDIENTE |
| Solicitar / Cancelar | Identidad, distancia, nonce y ciclo cubiertos en pruebas de relaciones | PENDIENTE |
| Aceptar / Rechazar | Validación y caducidad cubiertas; diálogo evita blur posterior | PENDIENTE |
| Revocar / Bloquear / Desbloquear | Bloqueo y persistencia cubiertos; desbloquear no concede consentimiento | PENDIENTE |
| Solicitudes / Audio de conocidos | REFRESH separado del límite de controles, evitando que el refresco consuma el clic | PENDIENTE |
| Grupos | Activa exclusivamente la acción nativa registrada; no genera una tecla compartida por otros mods | PENDIENTE |
| Micrófono y audio / Volumen individual | Invoca acción aunque la tecla esté sin asignar, conserva interfaz y asistente inicial oficiales | PENDIENTE |
| HUD | Preferencia local persistida, sin SQL/audio añadido | PENDIENTE |

No se certifica audio real entre equipos a partir del estado conectado. La primera apertura nativa puede iniciar el asistente de Simple Voice Chat.

## Launcher 0.2.4
Fuente independiente D:/luna-launcher, rama codex/preopen-player-ui, commit fcf6447b3. El perfil público solo expone Jugador. Constructor almacenado se resuelve a Jugador cuando la habilitación de staff no está presente, y la sincronización retira los cinco mods administrados de construcción. Acceso local privado del propietario: C:/Users/JUAN/Desktop/PokeReport Staff.lnk. Cerrar el launcher antes de abrir ese acceso; después seleccionar Constructor.

La habilitación local de herramientas no autentica un rol de Minecraft. Los permisos efectivos siguen siendo impuestos por el servidor; un cliente modificado no concede world.edit ni editmode. No se modifica el entorno global de Windows.

Build Release Windows con LTO, dependencias Qt/MSVC y NSIS. Artefacto del actualizador: luna-launcher-win-x64-Setup.exe. Los ejecutables de prueba no se distribuyen. El aviso Qt6Test.dll ocurrió al ejecutar la prueba interna sin su entorno Qt; corregido con PATH solo en el proceso de pruebas.

## Validación
- Mod: 234 pruebas descubiertas, 232 PASS, 2 SKIPPED; 0 fallos.
- Staging: Autotest final passed=768 failed=0 a las 21:28:08 UTC.
- Launcher configuración: 7 PASS; sincronización: 15 PASS, incluida retirada de herramientas al cambiar perfil.
- Manifest publicado: manifest-f4445a12b8.json; únicamente cambia el mod propio y se verifican hashes remotos.
- JAR SHA256: a253b389819236431e1d058851f8779748110ea3f470c679bad13c42fefb9728.
- Registro de producción y backup coordinado: preopen-deployment.json.

No se ejecutan autotests con fixtures en producción. QA visual y audio real: PENDIENTE, no interpretados como PASS.

Smoke de producción: servidor RUNNING y Done 21:30:49 UTC; VoiceChatServerThread escucha 24591 y cache lista. Ping UDP externo 117 ms, primer intento. Launcher 0.2.4 instalado localmente con NSIS /S, salida 0 y versión de binario 0.2.4.0. No se sobrescribe la instancia Minecraft en ejecución: recibirá el mod al siguiente inicio mediante el launcher.
