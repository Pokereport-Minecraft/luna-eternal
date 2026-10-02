# Resultados manuales de cliente — staging

Candidato cliente: SHA256 `92bf8e95b6c6a3fc52fb1a022eff548d8d9a91a4d2fd9c28eaa8be5d5dba57dd`. Cuenta de ensayo `PR_QA_Player`, UUID `518899f0-7b05-332d-95e9-85416a628eb3`, sin OP ni grants de edición según comprobación de perfil. Destino QA verificado mediante backend y cliente; no son pruebas del principal.

| Caso | Resultado | Observación | Evidencia |
|---|---|---|---|
| Login / registro EasyAuth | PASS | Registro humano aceptado, sesión conectada | Backend 2026-10-02 18:11:40 UTC; no copiar hash de contraseña |
| Entrada a Ciudadela | PASS | Dimensión `lunaeternal:ciudadela`, modo 0 (Survival) | RCON confirmado 2026-10-02 18:19:26 UTC |
| Player Survival BREAK | PASS manual | Usuario: al romper aparece que no puede romperlos | Declaración directa del usuario, recibida antes de 18:19:26 UTC |
| Player Survival PLACE | PASS manual | Usuario: al poner bloques el servidor los quita | Declaración directa del usuario, recibida antes de 18:19:26 UTC |
| Player Creative BREAK | PASS manual | Usuario confirma la misma denegación que en Survival | Declaración directa; dimensión Ciudadela y modo 1 verificados 18:21:27 UTC |
| Player Creative PLACE | PASS manual | Usuario confirma la misma reversión que en Survival | Declaración directa; sin elevación de OP ni grants |
| Player Survival WATER | PASS manual | Ante la prueba de agua/lava/mechero, usuario confirma «no deja» | Declaración directa; no screenshot ni snapshot de fluidos |
| Player Survival LAVA | PASS manual | Usuario confirma que no permite dejar lava | Mismo ensayo manual en Ciudadela |
| Player Survival FIRE | PASS manual | Usuario confirma que no permite dejar fuego | Mismo ensayo manual en Ciudadela |

Interacciones PokePad/NPC/tienda y reconexión: usuario respondió «funciona todo bien creo». Se registra observación favorable provisional, sin afirmar PASS individual de cada flujo ni comparar inventario/estado persistente. Hace falta confirmación específica o evidencia por caso.

Confirmación posterior: tras solicitar sprint/saltos, PokePad, NPC, tienda y reconexión sin bloques/líquidos fantasma, el usuario respondió «sí funciona todo bien». PokePad, NPC, tienda y ausencia de fantasmas tras reconexión se registran PASS manual reportado; no se identificó NPC/producto concreto ni se verificaron compras, saldo/inventario o todas las pantallas. La respuesta sobre hambre no indica explícitamente el valor observado: conservar la lectura del servidor y no inferir un test exhaustivo de hambre únicamente de esa frase.

Lectura posterior del backend a las 18:31:09 UTC: Ciudadela, FoodLevel=20, foodExhaustionLevel=0.0, Health=20. Es consistente con la protección tras la secuencia solicitada; no se midió distancia/duración efectiva de sprint ni hambre en dimensiones Survival.

Viaje a Hogar o Salvaje, construcción fuera de spawn/claims y retorno a Ciudadela con denegación: usuario declara «ya probé todo y funcionó todo» tras pedir esa secuencia. PASS manual reportado para la secuencia; destino exacto pendiente de identificar. No extender esa declaración a ambos mundos, seis Salvajes, muerte/respawn, combates o multiplayer.

Siguiente caso preparado: OP temporal de la cuenta de ensayo y Creative, sin añadir grants. Objetivo: verificar que OP por sí solo no permita editar Ciudadela. Revertir OP/Creative después del caso; no asignar privilegios a personas reales.

Resultado OP + Creative sin grants: PASS manual reportado de rotura/colocación denegadas y comando administrativo denegado. Tras pedir específicamente BREAK, PLACE y `/give @s minecraft:diamond 1`, el usuario confirmó que todo estaba bien y adjuntó una captura con «Esta acción requiere autorización administrativa explícita y, para editar mapas, modo edición». La imagen respalda el mensaje de barrera; no muestra el comando completo ni inventario, por lo que no se afirma comprobación visual de cada entrada. Captura fuente: `C:/Users/JUAN/AppData/Local/Temp/codex-clipboard-29e9a4f8-5e22-4064-9231-15fb6d5b4387.png`. OP retirado y Survival restaurado después del ensayo; no se concedieron grants.

Daño por caída: PASS con cliente real conectado, comprobación RCON en Ciudadela/Survival. A las 18:27 UTC se elevó el fixture de Y68 a Y74 y volvió a Y68 con Health=20. Para evitar que la regeneración ocultase daño, se repitió con `naturalRegeneration=false`: aterrizó en Y68 con Health=20. Valor original true restaurado y verificado inmediatamente, solo en staging. Esto certifica esa caída, no lava, PvP, vacío ni combate Pokémon. FoodLevel=20 fue una medición puntual; sprint/hambre sostenida sigue pendiente.

La hora anotada corresponde al registro/verificación, no a una hora exacta de clic. Screenshot de BREAK/PLACE no aportado; no se comparó snapshot de bloques ni inventario para estos dos casos. No afirmar ausencia de consumo/desincronización tras reconnect hasta probarlo. No confundir colocación visible breve y posterior reversión con permiso de construcción persistente.

Tras Creative se repuso Survival y se entregaron únicamente al fixture QA cubos de agua/lava y flint and steel. Denegación manual de esos tres usos confirmada por el usuario. Persistencia/desincronización tras reconexión, entidades/decoración, hambre/daño/PvP, interacciones, dimensiones/teleports, batallas y multiplayer siguen NOT TESTED. P0/P1 no se cierran por estas siete pruebas. Computer Use permanece detenido por Escape del usuario; pruebas físicas realizadas por la persona.
