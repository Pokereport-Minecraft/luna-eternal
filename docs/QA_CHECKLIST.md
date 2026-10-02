# QA y cierre de lanzamiento

Estados: PASS = evidencia ejecutada; PENDING = no ejecutado; FAIL = reproducción negativa. No convertir revisión de código en PASS funcional.

| Prueba | Estado | Evidencia |
|---|---|---|
| Build limpio JDK21 | PASS | Gradle build; 187 tests, 0 fallos |
| Arranque con Fabric/Cobblemon/RCT reales | PASS | Staging localhost, DB separada |
| Autotest | PASS | 753 PASS / 0 FAIL |
| Protección real de servidor | PASS | 21 PASS / 0 FAIL |
| Exportación Brigadier | PASS | 2247 usos con Tebex; 2236 al aislar conector |
| Backup corregido checksum | PASS | Archivo 2026-10-02_152009 |
| Restauración archivos/SQL antigua | PASS parcial | Arranque con ZIP/launcher suplementados |
| Restauración íntegra del backup corregido | PENDING | No confundir con la restauración antigua |
| Batallas con clientes / dos cuentas | PENDING | Procedimiento abajo |
| Transiciones / reconexión | PENDING | Procedimiento abajo |
| UI / resource pack / PokePad | PENDING | Cliente real necesario |
| Compra externa Tebex | PENDING | No ejecutar en staging con credenciales reales |
| Carga multiplayer | PENDING | La prueba sin jugadores no certifica carga |

## Guion de griefing

En staging aislado, conectar cuenta normal Survival, misma cuenta Creative, OP2 Creative y superior OP4 con nodos. Recorrer Ciudadela, Torre Comercial, Gimnasios, Morti, Torre e Islas. Intentar romper/colocar; cubo lava/agua; TNT/cama/cristal; fuego; fertilizante; arado; cambio de camino; cuadros/item frames/armor stands; command/structure/jigsaw; WorldEdit/Axiom; `/fill`, `/execute`, aliases con namespace. Normal y OP2 deben fallar sin cambiar mapa. Superior solo edita tras `editmode on`, Creative y nodos. Probar revocación por logout. Comparar archivos/regiones con snapshot; revisar logs. No realizar ataques en producción.

## Guion de viajes y autenticación

Nueva cuenta en Lobby: registro/login, tutorial/guardián. Viajar a Ciudadela, cada gimnasio, Morti, Hogar y cada Salvaje; volver, morir en Survival, desconectar durante traslado y reconectar. Anotar posición, dimensión, salud, hambre, gamemode, inventario, party y permisos. En safe no daño vanilla ni hambre; en Survival comprobar rotura, colocación, comida, daño y claims. Intentar suplantación de OP sin autenticación: ninguna acción administrativa permitida. Verificar portales y caer al vacío; sin softlock ni duplicación.

## Guion de gimnasio y concurrencia

Dos jugadores: entrar simultáneamente, interactuar con líderes, iniciar batalla, ganar/perder/huir, desconectar dentro, reconectar, repetir jefe. Comprobar ranuras distintas, clon sin superposición, cleanup, salida, una sola recompensa/medalla por evento. Ejecutar Johto y Morti con offsets reales. Validar Cynthia tras corregir o justificar los movimientos inválidos que aparecen en arranque. Registrar ID batalla, UUID, hora, logs y estado DB antes/después. El clon y premios unitarios no certifican el combate completo.

## Guion UI/economía

PokePad, PC, NPC, tiendas, memorial, tutorial, eventos, teleports, GTS y mercado con dos cuentas. Compra/venta/cancelación/expiración/reconexión, saldo insuficiente, límites, doble clic, repetición de mensaje. Plata/ReportCoin conservan ledger y balances. MARK retirada: no se puede acreditar/intercambiar. Tebex: sandbox sin producción, pago válido, callback duplicado, reintento, offline y fallo DB; comprobar entrega exactamente una vez. Revisar el intento de conexión externa del primer arranque de QA en panel Tebex.

## Rendimiento

Capturar Spark con carga acordada y cuentas reales: TPS/MSPT, heap/GC, consultas lentas/pool, tareas por tick, colas gimnasios, carga chunks y mixins. Escenarios 10/25/50 jugadores solo si capacidad y objetivo reales lo justifican. Definir umbrales antes de aprobar; no usar el baseline de 0 jugadores como certificación.
