# Resultados manuales de cliente — staging

Candidato cliente: SHA256 `92bf8e95b6c6a3fc52fb1a022eff548d8d9a91a4d2fd9c28eaa8be5d5dba57dd`. Cuenta de ensayo `PR_QA_Player`, UUID `518899f0-7b05-332d-95e9-85416a628eb3`, sin OP ni grants de edición según comprobación de perfil. Destino QA verificado mediante backend y cliente; no son pruebas del principal.

| Caso | Resultado | Observación | Evidencia |
|---|---|---|---|
| Login / registro EasyAuth | PASS | Registro humano aceptado, sesión conectada | Backend 2026-10-02 18:11:40 UTC; no copiar hash de contraseña |
| Entrada a Ciudadela | PASS | Dimensión `lunaeternal:ciudadela`, modo 0 (Survival) | RCON confirmado 2026-10-02 18:19:26 UTC |
| Player Survival BREAK | PASS manual | Usuario: al romper aparece que no puede romperlos | Declaración directa del usuario, recibida antes de 18:19:26 UTC |
| Player Survival PLACE | PASS manual | Usuario: al poner bloques el servidor los quita | Declaración directa del usuario, recibida antes de 18:19:26 UTC |

La hora anotada corresponde al registro/verificación, no a una hora exacta de clic. Screenshot de BREAK/PLACE no aportado; no se comparó snapshot de bloques ni inventario para estos dos casos. No afirmar ausencia de consumo/desincronización tras reconnect hasta probarlo. No confundir colocación visible breve y posterior reversión con permiso de construcción persistente.

Player Creative sin grants, fluidos/fuego, entidades/decoración, hambre/daño/PvP, interacciones, dimensiones/teleports, batallas y multiplayer siguen NOT TESTED. P0/P1 no se cierran por estas dos pruebas. Computer Use permanece detenido por Escape del usuario; pruebas físicas realizadas por la persona.
