# Despliegue controlado — 2 de octubre de 2026

Autorizado por el usuario: «despliega todo ya». Servidor principal RUNNING, arranque y comprobaciones de consola PASS. Apertura NO CERTIFICADA: smoke con cliente posterior al despliegue, batallas, multiplayer, identidades de staff y revisión externa de Tebex siguen pendientes con el alcance de RELEASE_CANDIDATE.md. El aviso «¡Elige tu inicial!» sigue pendiente de corrección.

## Artefactos y publicación

Se instaló el candidato de `codex/prelaunch-world-hardening`, commit fuente 1161edab (último cambio Java 5443a030). Build: 186 PASS / 2 SKIPPED. Staging: 753 autotests y 21 ensayos de protección PASS, más QA humano documentado. No merge ni cambios en otros worktrees.

Build SHA256: `92bf8e95b6c6a3fc52fb1a022eff548d8d9a91a4d2fd9c28eaa8be5d5dba57dd`. JAR desplegado SHA256: `dd6568e7dfc18fe4b65f97eb7c6001508465500bdbc94d2089beacae2a37aa0e`. El ZIP de staging está reempaquetado; todos sus nombres y contenidos de entradas coinciden con el build limpio. Ver [recibo](prelaunch/production-deployment.json).

Launcher: publicado JAR del build limpio y manifiesto `manifest-b8ca83a88a.json`; puntero latest.json descargado y comprobado. Solo se sustituyó `mods/lunaeternal-0.1.0.jar`, conservando las demás entradas del manifiesto vigente. Fallback master actualizado en commit `fb66e71c872452a01ae16eef40869de3bfe06d42`. [Recibo de publicación](prelaunch/launcher-publication.json).

## Backup y reversión

Snapshot coordinado, con servidor detenido y sin otras conexiones al schema principal: `/var/backups/pokereport/releases/20261002T190203Z`. SQL + volumen completo, checksums e integridad gzip/zstd/SQLite PASS. Archivo extraído íntegramente y SQL restaurado en schema independiente; wallets idénticas. [Recibo de backup](prelaunch/production-backup.json). Los intentos anteriores que detectaron reinicio del servidor fueron rechazados y no son puntos válidos de rollback.

JAR/config anteriores y presencia original conservados en `rollback-files/rollback-manifest.json` dentro de ese snapshot. No se reemplazaron mundos ni DB; schema antes/después 56, ops.json intacto, sin personas asignadas. Staging y proxy QA detenidos, conector Tebex de QA desactivado.

**Usar el panel o API Wings para parar/iniciar el principal.** `docker stop` provoca que Wings lo reinicie automáticamente. Helper del host: `python3 /tmp/pokereport-wings-power.py stop` y `start`. Confirmar guardado y estado detenido antes de restaurar archivos. Durante esta intervención, la parada guardó dimensiones, cerró SQLite y Hikari, pero quedó un proceso residual; tras verificar esos cierres se terminó mediante Wings. No usar kill como parada habitual.

Ante regresión: parar mediante Wings, conservar evidencia, reponer solo JAR/config del manifest y comprobar compatibilidad de schema. El JAR anterior tiene el gap de protección: mantener mantenimiento si se revierte. No restaurar DB/mundos por un fallo visual. Para datos, aplicar DEPLOYMENT_ROLLBACK.md con snapshot coordinado y reconciliación Tebex.

## Smoke observado

Arranque `Done (4.333s)`; sin fallo fatal de servidor/mixins. Consola responde a list, catálogo de comandos y spark healthreport. 0 jugadores, TPS 20.0 en todas las ventanas; tick mediano 0.7 ms, p95 1.1 ms en 10 s; heap 3.4/14 GB. Esto mide reposo, no carga multiplayer. Persisten errores conocidos de registros/data fixers e ítems inválidos; SkinRestorer informó perfiles externos inexistentes. No se interpreta como arranque limpio de todas las integraciones.

Client smoke post-deploy: NOT TESTED. Reiniciar el cliente desde el launcher actualizado, conectar a play.pokereport.online, autenticar y comprobar entrada, PokePad/interacción y protección con cuenta normal. Registrar resultado y hora; no convertir las pruebas anteriores de staging en PASS de producción. La corrección P0 ya está instalada, pero el gate funcional de apertura permanece retenido hasta verificarla en cliente principal.
