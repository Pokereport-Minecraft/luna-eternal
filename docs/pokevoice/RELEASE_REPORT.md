# PokeVoice — informe del candidato

## Estado actual — 3 de octubre de 2026

**Desplegado en el servidor principal por petición expresa del usuario.** Cliente publicado para instalación automática: manifest-ffb7b92111.json. Motor oficial SVC 2.6.22 Release, Flashback Constructor 0.39.8 Release. Producción UDP 24591, allocation Pterodactyl 22; mapping de Wings verificado, backend privado preservado. Arranque, migración 57, caché y ping externo PASS (113 ms). Build 230 PASS/2 SKIPPED; staging 763 PASS/0 FAIL. Backup coordinado privado de 16 GB y SQL, restauración SQL (68 tablas), JAR/config seleccionados PASS; restauración completa de mundo NOT RUN. Audio real y nueva captura de interfaz NOT RUN: despliegue no equivale a certificación de apertura. Evidencia production-deployment.json y production-backup-verification.json.

Las fases siguientes conservan antecedentes de staging; cualquier referencia antigua a producción intacta o publicación pendiente queda supersedida por este estado.


1. **Versión:** motor oficial Simple Voice Chat 2.6.22 para Fabric 1.21.1; API 2.6.20 incorporada por ese motor. Release fijada, sin beta.
2. **Fuente:** Modrinth VS5X7jZd y documentación oficial maxhenkel; checksum completo en POKEVOICE_QA.md.
3. **Base preservada:** worktree independiente pokevoice, rama codex/pokevoice, base a74d626d. Otros worktrees conservados. Sin merge.
4. **Stack:** compilación Loader 0.16.5/Fabric API 0.115.0; staging existente Loader 0.18.4/API 0.116.15. Java 21.0.12; motor arranca con el stack real.
5. **Archivos:** núcleo main/voice, cliente client/voice, migración V057, entrypoints Fabric, registro Red, CatalogoPad/Apps, icono propio, traducciones y pines en herramientas de modpack. No se regeneró el manifest publicado.
6. **Configuración:** defaults y límites en VoiceConfig, archivo pokevoice.properties; transporte independiente voicechat-server.properties. UDP staging inicial 24590 en loopback.
7. **Arquitectura:** motor nativo para Opus, transporte y espacialización; PokeVoice para consentimiento, relaciones, privacidad y UI.
8. **Persistencia:** dos tablas MariaDB, UUID, pares canónicos, commit previo a consentimiento; caché de lectura. Invitaciones no durables.
9. **Interfaz:** app adicional del PokéPad conserva carcasa y orden previo; Cercanos/Conocidos/Grupos/Ajustes/Privacidad. Controles nativos abren configuración y grupos. Capturas y legibilidad cliente: NOT RUN.
10. **Protocolo:** acciones acotadas sin actor ni coordenadas del cliente; nonce de servidor, validación de destinatario y máximo de página; snapshot privado acotado.
11. **Permisos:** autenticación EasyAuth, presencia viva, misma dimensión y distancia calculada por servidor. Consentimiento no exige OP ni rol de staff.
12. **Filtro:** SOURCE_PROXIMITY por destinatario; caché inmutable, sin SQL en audio. Deniega ante datos ausentes/error; bloqueos y preferencias dominan.
13. **Grupos:** motor nativo, sin filtro de relaciones sobre GROUP/PLUGIN/SPECTATOR. No prometer privacidad de proximidad dentro de un grupo compartido.
14. **Pruebas automáticas:** build PASS 226 y 2 SKIPPED; staging 763 PASS, 0 FAIL. Reinicio y autotest del último JAR PASS, evidencia en evidence.json.
15. **Audio real:** NOT RUN. Usuario confirmó dos equipos. Pendientes nombre/IP/acceso y matriz documentada. No simular aprobación.
16. **Rendimiento:** microbenchmark de filtro 2/10/25/50, 2M decisiones, CSV adjunto, 0 llamadas a repositorio. TPS/carga UDP/voces simultáneas/latencia extremo a extremo: NOT RUN.
17. **Conflictos y riesgos:** sin motor VoIP previo. Backend offline produce advertencia nativa de cifrado; forwarding/EasyAuth y acceso restringido requieren QA. Errores preexistentes de items/UUID en mundo staging no se silencian. API de interfaz nativa no expone apertura directa: se usa el KeyBinding registrado, pendiente QA. Pequeñas asignaciones de Pair medidas, no se afirma cero allocation.
18. **Promoción:** BLOCKED. Producción intacta; backup previo de staging privado. QA real, carga y backup coordinado de producción obligatorios antes de desplegar y ejecutar smoke test. No certificación de apertura.

Documentación: POKEVOICE_QA.md, evidence.json y filter-benchmark.csv.

## Actualización automática autorizada

Publicado `manifest-f83d7644f3.json` y actualizado el puntero compartido del launcher. Solo cambia el JAR propio y agrega Simple Voice Chat; las otras 159 entradas y cabecera se preservan. Descargas y hashes remotos PASS. Esto publica el cliente para instalación automática; no despliega PokeVoice al servidor Minecraft principal. QA de audio real NOT RUN. Cerrar Minecraft y pulsar Jugar en launcher, después conectar a `15.235.16.131:25595`. Puntero anterior y evidencias en launcher-publication.json.
