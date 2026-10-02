# Protección de mundos — candidato prelaunch

| Dimensión real | Política |
|---|---|
| minecraft:overworld (Hogar) | Survival existente |
| lunaeternal:salvaje, salvaje2, salvaje3, salvaje4, salvaje5, salvaje6 | Survival existente |
| lunaeternal:ciudadela | Segura |
| lunaeternal:lobby | Segura + autenticación existente |
| lunaeternal:gimnasios | Segura |
| lunaeternal:gimnasio_morti | Segura |
| lunaeternal:torre | Segura |
| lunaeternal:islas_naranja | Segura |
| Dimensiones externas LumyMon / Raid Dens | Política original; fuera de esta lista |

Torre Comercial es una ubicación dentro de Ciudadela; no existe una dimensión `pokereport:torre_comercial`. La selección central está en WorldProtectionManager; no se reemplazaron JSON de dimensiones ni se regeneraron mundos.

En zonas seguras se deniegan rotura y colocación normales, cubos, ignición, entidades colocables, herramientas que alteran bloques, fertilizante y huevos. Se protegen decoraciones, farmland, rotura ambiental, terreno de explosiones, propagación de fuego, fluidos y acciones de bloques de Enderman. Los controles usan eventos Fabric y mixins en rutas del servidor. Axiom tiene barrera para payloads de su namespace; no equivale a auditoría de cada protocolo de terceros.

Daño vanilla a ServerPlayerEntity y agotamiento se cancelan; HungerManager conserva comida 20. Existe defensa ante muerte directa y recuperación bajo el suelo mediante posición segura reciente o spawn existente. Estos controles no cancelan el sistema de batalla Pokémon. Las pruebas no cubren cada fuente de daño de todos los mods. La cancelación de explosiones/fluidos también se aplica a administradores en edición: editar no convierte el mapa en un mundo de destrucción ambiental.

Puertas, trampillas, puertas de valla, botones y palancas conservan sus manejadores. Los callbacks establecidos de tienda y memorial siguen antes de la política de contenedores sensibles. Los contenedores Inventory quedan cerrados salvo coordenadas públicas explícitas; command blocks/structure/jigsaw quedan cerrados para jugadores normales.

Config: `config/lunaeternal/interactions.properties`. En Java Properties hay que escapar el separador de namespace: `lunaeternal\:ciudadela@10,70,20=true`. No comodines. No habilitar cofres de staff. Revisar en cliente PC, tiendas, PokePad y memorial antes de desplegar: el ensayo sintético no demuestra compatibilidad de todas las interfaces de terceros.

Execute/function/schedule/clone y los constructores de mapas Luna exigen sesión de edición también al invocarse desde un mundo Survival, para impedir destinos protegidos indirectos. Los comandos directos de terreno en Survival conservan el gate OP4+commands.

La excepción de edición exige OP4 + nodos edit y editmode + Creative + sesión activada. Las antiguas excepciones OP2 Creative en GymArenaProteccion/SantuarioProteccion se sustituyeron por esa decisión central. Hogar y Salvajes no reciben esta barrera de mapa; continúan sujetos a claims y protecciones previas. No hay conversión automática de gamemode al entrar/salir; el modo de salida de editmode sí se establece explícitamente.

Validación ejecutada: 21 ensayos cargando los mixins reales, 0 FAIL. Incluye rotura, colocación directa, lava/caída/vacío, hambre, armor stand, farmland, explosión, cofre, puerta y niveles OP. Pendientes: griefing desde cliente modificado, portales, reconexión, pistones entre áreas, interacciones completas y viajes reales con Pokémon. No afirmar cobertura universal de todos los bloques/mods.
