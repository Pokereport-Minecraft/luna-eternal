# QA manual con Minecraft — candidato de preapertura

## Ejecución manual cuando falla la captura automática

El rechazo directo identificado es `This server requires you to connect with Velocity`: FabricProxy-Lite requiere forwarding moderno. El acceso de QA se corrige mediante un Velocity separado, ligado a `127.0.0.1:25592`, hacia el backend `127.0.0.1:25590`, con secreto exclusivo de staging. No desactivar EasyAuth ni FabricProxy-Lite para simular un login. El capturador tiene un fallo independiente (`FrameArrived timed out` / `window capture timed out`); esto no equivale a crash del servidor.

Operador del host, solo para levantar los entornos ya preparados:

```sh
docker start pokereport-prelaunch-20261002
docker start pokereport-prelaunch-proxy-20261002
python3 /tmp/pokereport-prelaunch-run.py list
```

Esperar `Done` en ambos contenedores antes de entrar. En un terminal local mantener este túnel, sin abrir puertos públicos:

```powershell
ssh -i C:/Users/JUAN/.ssh/pokereport_node01_ed25519 -o BatchMode=yes -o ExitOnForwardFailure=yes -o ServerAliveInterval=15 -N -L 127.0.0.1:25590:127.0.0.1:25592 root@15.235.16.131
```

Si el túnel de QA ya existe, usarlo; no arrancar otro sobre el mismo puerto. Lanzar exclusivamente el cliente preparado:

Verificar destino antes de probar: el último `Connecting to` del cliente debe apuntar a `127.0.0.1, 25590`, y `list` del backend QA debe incluir la cuenta. Registrarse en `play.pokereport.online:25565` no autentica la copia de staging ni valida el candidato. La lista `servers.dat` de la instancia QA se dejó con una única entrada de staging; su lista anterior quedó respaldada como `servers.before-staging-only.dat`, sin tocar la instancia principal.

```powershell
& 'C:/Users/JUAN/AppData/Local/Programs/FreesmLauncher/freesmlauncher.exe' --dir 'C:/Users/JUAN/.codex/prelaunch-qa/client-20261002' --launch 'Prelaunch QA' --offline PR_QA_Player --server 127.0.0.1:25590
```

La persona que prueba debe completar EasyAuth manualmente con una contraseña exclusiva de ensayo, usando `/register <password> <passwordAgain>` en primer acceso o `/login <password>` después. Hacerlo dentro del plazo; si aparece timeout, reconectar mediante conexión directa a `127.0.0.1:25590`. No fotografiar ni registrar contraseña, chat de autenticación o tokens. Si no llega a ese paso, registrar texto exacto del rechazo y logs; no marcar pruebas de protección. La captura F2 ya fue ejecutada e inspeccionada correctamente; el renderizado sí funciona.

Antes de probar Player, el operador confirma que `PR_QA_Player` no aparece en OP ni en grants. Puede retirar OP en staging mediante:

```sh
python3 /tmp/pokereport-prelaunch-run.py deop PR_QA_Player
python3 /tmp/pokereport-prelaunch-run.py gamemode survival PR_QA_Player
```

Usar portales/NPC reales para Ciudadela; ID real `lunaeternal:ciudadela`, no `pokereport:ciudadela`. Registrar la posición de la parcela de ensayo y conservar capturas antes/después. El operador entrega herramientas únicamente a esta cuenta en staging, por ejemplo:

```sh
python3 /tmp/pokereport-prelaunch-run.py give PR_QA_Player minecraft:diamond_pickaxe 1
python3 /tmp/pokereport-prelaunch-run.py give PR_QA_Player minecraft:stone 16
```

La persona debe intentar físicamente BREAK y PLACE, comprobar ausencia de cambio en servidor y reconectar para descartar bloques fantasma. Repetir después en Creative accidental sin grants, asignado exclusivamente desde consola de staging. Volver a Survival al terminar. Las pruebas de otros perfiles requieren grants de ensayo explícitos según la sección Perfiles; no usar OP de personas reales como sustituto.

Para cada paso usar F2 de Minecraft y recoger el archivo en `instances/Prelaunch QA/minecraft/screenshots`, con hora UTC; adjuntar solo capturas del juego sin secretos. Registrar líneas de `latest.log` cliente y logs del backend/proxy del mismo intervalo. Si el screenshot está negro, indicarlo como fallo de evidencia gráfica y pedir observación humana; no cambiar drivers ni desactivar seguridad de Windows como workaround.

Plantilla obligatoria:

```text
CASE_ID:
TIMESTAMP_UTC:
OPERADOR:
CUENTA / PERFIL / GRANTS:
DIMENSIÓN / POSICIÓN:
ACCIÓN FÍSICA:
ESPERADO:
OBSERVADO:
RESULTADO: PASS / FAIL / NOT TESTED / BLOCKED
SCREENSHOT: ruta de F2 o NO DISPONIBLE
LOG: archivo y líneas / intervalo UTC
ESTADO DB / INVENTARIO / BLOQUES ANTES Y DESPUÉS:
```

Sin ejecución humana y evidencia, conservar NOT TESTED. Al cerrar la sesión detener backend y proxy QA y terminar el túnel propio; preservar sus volúmenes. No desplegar ni certificar mientras sigan pendientes login, juego, multiplayer, staff y demás gates del candidato.

Solicitud adicional del usuario: ocultar el aviso «¡Elige tu inicial!» que muestra `key.keyboard.` literalmente. Corrección exclusivamente de cliente en [STARTER_TOAST_FIX.md](STARTER_TOAST_FIX.md), con build comprobado. Verificación visual al iniciar mediante el launcher actualizado: NOT TESTED; no mezclarla con resultados PASS de protección.

Estado inicial: NOT TESTED para los casos de juego. El cliente aislado arrancó, pero la primera conexión se cortó y la herramienta de captura falló dos veces; no se observaron sesiones jugables ni se certificaron interfaces. Ningún autotest sustituye los pasos siguientes.

Usar exclusivamente `/srv/pokereport-prelaunch-20261002`, DB `prelaunch_20261002`, conector Tebex desactivado y acceso por túnel localhost. Mantener la instancia original del launcher intacta. Copia de cliente: `C:/Users/JUAN/.codex/prelaunch-qa/client-20261002/instances/Prelaunch QA`. Comprobar que su JAR coincida con el candidato de `prelaunch/final-build.json` antes de probar; la primera sesión de arranque utilizó el candidato anterior. Nunca apuntar estas pruebas al principal.

Registrar cada caso: hora UTC, commit/JAR SHA256, cuenta de ensayo, perfil/grants, dimensión/posición, estado previo, acción, resultado esperado y observado, PASS/FAIL/NOT TESTED/BLOCKED, captura y líneas relevantes de logs. Inventario, party, saldo, recompensas y medallas requieren comparación antes/después. Para fixtures, usar identidades nuevas; no suplantar personas reales ni copiar tokens de launcher. Detener un caso si hay pérdida, dupe, crash o softlock y clasificar P0/P1.

## Perfiles

Preparar jugador sin OP ni grants, moderador OP1–3 sin grants, OP4 sin grants, y administrador OP4 con los dos grants de edición. No reutilizar cuentas reales de staff. La asignación de grants se hace por configuración y reinicio, conforme a ADMIN_ROLES.md.

| Perfil | Survival | Creative, EditMode OFF | Creative, EditMode ON |
|---|---|---|---|
| Player, incluso Creative accidental | NO edita | NO edita | No puede activar |
| Moderator sin grants | NO edita | NO edita | No puede activar |
| OP4 sin grants | NO edita | NO edita | No puede activar |
| Admin autorizado | NO edita | NO edita | SÍ edita |

Probar `/pradmin editmode on/off/status`, denegación de `/fill`, `/setblock`, `/execute` y comandos administrativos de Pokémon/cuentas sin el grant adicional. Probar comandos indirectos dirigidos desde Survival hacia una dimensión segura. Verificar activación/desactivación, rotura y desconexión en logs. No exigir auditoría de colocación como PASS: su cobertura sigue pendiente.

## Ciudadela: jugador normal

- [ ] No rompe ni coloca bloques; comprobar cliente y estado real del servidor.
- [ ] No usa agua/lava para modificar, no genera fuego ni propaga incendio.
- [ ] No destruye item frames, glow item frames, armor stands, paintings ni decoración.
- [ ] No modifica cultivos con bonemeal o pisoteo; no hace grief con pistones ni explosiones.
- [ ] No recibe daño vanilla por golpes, caída, lava, fuego o vacío; no pierde hambre; no hace PvP.
- [ ] Recuperación del vacío funciona: cancelar daño no demuestra teleport de recuperación.
- [ ] Puede interactuar con NPC, PokePad, tiendas y botones autorizados, y entrar a gimnasios.
- [ ] PC, healer, puertas, comercio y contenedores públicos funcionan; los administrativos permanecen cerrados.
- [ ] No aparecen bloques fantasma, consumo indebido de items ni desincronización tras reconectar.

Repetir con pickaxe, axe, shovel, shears, bucket, lava bucket, water bucket, flint and steel, fire charge, TNT, creeper, end crystal, bed, respawn anchor, piston, sticky piston, slime, honey, boat, minecart, armor stand, painting, item frame, glow item frame, bonemeal y trampling. Probar herramientas de mods realmente instalados, incluyendo Axiom/WorldEdit si están disponibles. Preparar mecanismos de pistones/explosiones mediante operador solo en una parcela de ensayo de staging; no construir trampas en producción. Comparar snapshot de bloques y entidades antes/después. Probar daño y hambre vanilla sin cancelar los eventos de combate Pokémon.

## Todas las dimensiones

Los nombres `pokereport:*` del encargo no son los IDs instalados. Johto se distribuye en `lunaeternal:gimnasios` y Morti tiene dimensión propia; Torre Comercial es una ubicación en Ciudadela. Torre de Batalla usa `lunaeternal:torre`.

| ID real o ubicación | Política esperada |
|---|---|
| `lunaeternal:ciudadela` (incluye Torre Comercial) | Segura |
| `lunaeternal:lobby` | Segura |
| `lunaeternal:gimnasios` (incluye gimnasios Johto) | Segura |
| `lunaeternal:gimnasio_morti` | Segura |
| `lunaeternal:islas_naranja` | Segura |
| `lunaeternal:torre` | Segura |
| `minecraft:overworld` (Hogar) | Survival según reglas originales |
| `lunaeternal:salvaje`, `salvaje2`, `salvaje3`, `salvaje4`, `salvaje5`, `salvaje6` | Survival según reglas originales |
| `minecraft:the_nether`, `minecraft:the_end` | Verificar disponibilidad y reglas originales |
| `cobblemonraiddens:raid_dimension` | Reglas del mod externo; no incluida automáticamente en zonas seguras |
| `lumymon:origin`, `lumymon:nightmare` | Reglas del mod externo; no incluidas automáticamente |

Para CADA ID, sin combinar resultados, completar:

```text
ID / ubicación:
Perfil / permisos:
GameMode:
Block Break:
Block Place:
Damage:
Hunger:
PvP:
Interaction (NPC, PC, healer, tienda, contenedor):
Respawn:
Void Recovery:
Evidencia / resultado:
```

En Hogar y cada Salvaje verificar Survival, hambre, daño, muerte, rotura, colocación e interacción conforme a reglas originales; comprobar inventario y respawn según keepInventory/reglas existentes. No afirmar que una dimensión externa permite construir sin verificar su propia protección.

## Teleports y reconexión

Probar ambos sentidos: Ciudadela–Hogar, Ciudadela–cada Salvaje, Ciudadela–gimnasios, Ciudadela–Morti/Johto, Ciudadela–Islas Naranja, Ciudadela–Torre de Batalla; recorrer Torre Comercial dentro de Ciudadela. Añadir lobby y accesos de mods externos disponibles. Usar los portales/NPC/menús reales, no solo `/tp` del operador.

En cada transición registrar antes/después: modo, hambre, salud, dimensión/posición, inventario, party, permisos y protección. Repetir con admin EditMode OFF/ON y desconexión/reconexión; la sesión se revoca al desconectar. Probar muerte/respawn, salir durante teleport y volver a entrar. Confirmar UI de carga y que el usuario no quede inmóvil o atrapado.

## Gimnasios y combates completos

Inventariar todos los gimnasios/NPC instalados y repetir por cada uno, incluyendo Johto, Morti y líderes de Naranja. No certificar un gimnasio por haber iniciado batalla:

- [ ] NPC visible e interacción; ambos Pokémon visibles.
- [ ] Inicio, selección de ataque, cambio, faint y doble faint.
- [ ] Victoria y derrota completas; cierre y salida de arena.
- [ ] Recompensa/medalla exacta una sola vez, cooldown y segundo intento.
- [ ] Desconexión a mitad, reconexión, abandono y limpieza de clones/ranuras.
- [ ] Dos combates simultáneos sin cruces de arena, actores ni recompensas.
- [ ] Cynthia usa movimientos válidos y el combate termina.

Buscar en cliente y servidor: softlock, battle freeze, `RequestInstruction`, `mustChoose`, RCT mixins, `BattleActorMixin`, `PokemonBattleMixin`, `PokemonModelConverter`. Registrar ID de batalla, secuencia y logs; no continuar otorgando premios manualmente para ocultar un fallo. Preparar equipos de ensayo en staging para victoria, derrota y doble faint reproducibles.

## Multiplayer y carga

Ejecutar con cinco clientes reales y cuentas distintas; arrancar gradualmente y limitar memoria para no agotar el host. El servidor QA limita a cinco jugadores. Mantener grabación/horas y revisar wallets, ledger, recompensas, inventarios y sesiones al terminar.

| Escenario | Acción mínima | Criterio |
|---|---|---|
| A | Cinco jugadores en Ciudadela durante 10 min | Protección y sesiones correctas |
| B | Interacciones simultáneas con NPC/tiendas/PokePad | Respuestas independientes, sin dupe |
| C | Varios combates simultáneos y cierre | Sin softlock, cruces ni doble recompensa |
| D | Teleports simultáneos de ida/vuelta | Posición, modo y protección correctos |
| E | Misiones/trabajos/economía concurrentes | Saldos y ledger consistentes |
| F | Logout/reconnect rápido, también durante combate | Sesiones y reservas liberadas, sin dupe |

Medir antes/durante/después TPS, MSPT p50/p95/max, heap, GC, CPU, threads, entidades, chunks y latencia DB p50/p95. Registrar duración, clientes, acciones/s, límites del contenedor y hora. Usar Spark en staging y consulta SQL de solo lectura en su schema; no publicar perfiles externos con datos del servidor sin revisar contenido. Anotar saturación/errores/spam. Ensayos de backend y baseline sin jugadores no equivalen a A–F.

## Tebex

El drill `tools/prelaunch_tebex_drill.py` usa el comando real de entrega y catálogo copiado, fixtures nuevos y conector externo desactivado; `--replay` reutiliza la misma transacción después de reinicio. Verificar un único ledger credit, saldo, rango y fila de fulfillment. Username desconocido debe quedar pendiente sin crédito; completar primer login/reconnect con cliente y confirmar una sola resolución.

Compra externa, cola real, reconocimiento del conector, reconnect y kits requieren ensayo sandbox autorizado en Tebex o revisión de órdenes existentes sin volver a ejecutarlas. No realizar compras reales innecesarias ni cambiar productos/precios. El catálogo implementa LUNACOINS, RANK y RANK_UPGRADE; kits se compran/reclaman en juego según moneda/rango: probar saldo, inventario, requisitos y cooldown. No inventar un producto KIT. Revisar el contacto previo del primer staging con Tebex antes de cerrar OPEN-05.
