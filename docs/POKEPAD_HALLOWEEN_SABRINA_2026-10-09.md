# Poképad completo Halloween, premio 100 y pruebas de Sabrina

**Corrección de la verificación de producción:** la revisión en vivo posterior encontró un segundo JAR Luna de respaldo en `mods`, que Fabric estaba seleccionando con el mismo id/versión. El hash del archivo instalado y los tests de staging eran correctos, pero no demostraban el código ejecutado en producción. El respaldo fue conservado fuera de `mods` y la recarga posterior confirmó el mensaje nuevo de bordes. Evidencia y estado jugable: [LIVE_GYM_RUNTIME_AUDIT_2026-10-09.md](LIVE_GYM_RUNTIME_AUDIT_2026-10-09.md).

## Base y cambios

Continuación de `codex/production-gym-battle-hardening`, desde el código ya desplegado SHA256 `88778cfda70eed9fde86299de28a332f4bd2befaef63946cc4f988739be5acbc`. Se conserva el checkout principal y su cambio local de Sabrina. No se restaura ninguna versión antigua, mundo, base de datos, claim ni configuración.

La reparación previa de permisos, NPCs y contención de excepciones de combate se mantiene. Este incremento corrige el caso concreto que quedó fuera del detector de Sabrina: sesiones `/luna probar` en Creativo. Se reemplaza la exclusión por modo de juego por una excepción explícita para **editmode activo**, en Survival o Creative. Una prueba siempre activa la detección aunque editmode esté habilitado. Visitas directas sin editmode se detectan también en Creative; Spectator y jugadores muertos siguen excluidos.

Foso por ranura: X 5180.479..5200.551; Z 20.597..93.691 + 128r; Y <= 80.5; r = 0..7. Sin reserva se infiere únicamente un foso físico válido. Inicio (5189.95, 95, 90.78 + 128r), yaw 180; velocidad y caída canceladas; debounce 1500 ms. Una asignación de otro gimnasio o un combate activo no se modifica. No se otorga una reserva ni una victoria por caer.

El nivel 1 conserva el Charizard shiny mecha original, nivel 50 y aspecto forzado `luna_mecha`. El 100 entrega un **Kit de Maestría: 3 Master Balls, 3 parches de habilidad y 16 caramelos raros**, sin otro Charizard. La tarjeta muestra el contenido del catálogo real. Se separan las líneas VARIOCOLOR y nivel para evitar el desbordamiento mostrado en la captura. Claims ya cobrados conservan su idempotencia; no se fuerzan entregas retroactivas.

Halloween aporta 72 PNG nuevos para toda la interfaz, incluidos 52 del directorio Poképad y 20 de sus pantallas complementarias. Son variantes de los originales, con dos iconos alternativos para Cartas y Cosméticos porque el generador rechazó sus ediciones. Los 125 archivos originales de GUI se conservan byte a byte y en una copia con SHA256. Cambio automático octubre/noviembre en America/Bogota. Tipografía Atkinson Hyperlegible con licencia OFL, medición/dibujo/centrado consistentes, etiquetas sin el contorno de cuatro copias y contraste mejorado. Detalles y prompts: [ui/HALLOWEEN_ASSETS.md](ui/HALLOWEEN_ASSETS.md).

El empaquetado normaliza los finales de línea SQL a LF para que Windows no cambie el artefacto por CRLF. Las 60 migraciones conservan exactamente el contenido al normalizar exclusivamente finales de línea. No se actualizan los checksums registrados ni se vuelve a ejecutar una migración aplicada.

## Validación y distribución

Estado de este documento antes de publicación: candidato local. La evidencia final se añade tras build, QA aislado, publicación y arranque.

Se exige la misma validación del incremento previo: build JDK 21; suite JUnit; validación de 72 PNG, tamaños, metadatos y hashes; arranque de staging con Cobblemon 1.8/RCT y DB `prelaunch_20261002`; autotest y protección. Los comandos que escriben datos de ensayo se ejecutan solo en staging.

La distribución cambia únicamente Luna: conserva 163 archivos del launcher, nueve entradas del bundle firmado y 147 dependencias/otros overrides del mrpack. El pack conserva base 0.2.0 y protocolo 3; nueva edición firmada 0.1.2. El cliente importado desde otro launcher sin actualizador debe recibir el JAR nuevo o importar el mrpack actualizado.

La carga en producción requiere aviso, guardado, cierre de dimensiones y del pool SQL, y reemplazo atómico con el proceso detenido. SIGTERM solo después del guardado si quedan hilos residuales; nunca SIGKILL. No se afirma un combate humano completo ni QA gráfico de Minecraft a partir de la maqueta web.

## Avisos de colmenas y rendimiento (00:42â€“00:44 UTC del 10 de octubre)

El identificador `cobblemon_pokemon_entity_placeholder` es un marcador válido de los nidos saccharine, no un Pokémon de jugador perdido. El bytecode de Cobblemon 1.8.0 confirma: `SaccharineTreeFeature.populateBeeNest` almacena el marcador con `pokemonProperties`; `BeeOccupantMixin` convierte el resultado nulo de `BeeData.loadEntity` en un Combee y aplica posición, edad y néctar. Vanilla intenta primero deserializar el marcador no registrado y emite el aviso en cada intento de salida de la colmena. `SaccharineHivePlaceholderMixin` evita exclusivamente esa deserialización en la colmena y mantiene el handler RETURN original de Cobblemon. No elimina NBT, no convierte Pokémon guardados, no desactiva avisos generales y no toca chunks existentes.

Medición de producción antes del incremento: Docker 9,911 GiB de 14,7 GiB, CPU 28,58% (100% equivale a un núcleo); heap G1 comprometido 8,26 GiB y usado 4,85 GiB. El perfil de un minuto [Spark](https://spark.lucko.me/PlxOHAE1Ao) registra 6 jugadores al finalizar, 20 TPS, MSPT mediano 8,17 y p95 12,2; máximo aislado 441 ms. CPU del proceso 4,42% sobre los 8 hilos disponibles (~35% de un núcleo). GC joven promedio histórico 27,7 ms, ninguna colección Old. Spark contabiliza 231 entidades y 92 chunks con entidades, incluidos 67 Pokémon y 84 text_display; no confundir este contador con todos los chunks residentes.

La cifra del panel no equivale a objetos vivos: Java conserva heap comprometido/RSS además de metaspace y memoria nativa. Esta muestra no demuestra una fuga, ni garantiza ausencia de crecimiento posterior. Se evita forzar GC, limpiar entidades o bajar distancias a ciegas. `-Xmx14336M` deja poco margen si se ocupa el máximo: debe considerarse un límite de heap menor en una ventana posterior, conservando el límite del contenedor, tras medir memoria nativa y carga pico. No se ha cambiado la configuración de Wings ni los mundos por esta medición.

## Publicación y cierre de producción

Bundle firmado 0.1.2 publicado y firma Ed25519 verificada descargando manifest y firma públicos. Manifest de launcher `c1b0bb75133ddc91b417764d77704578667349ab`, con los 163 archivos restantes intactos y fallback sincronizado. MRpack SHA256 `5d41b11b0993558bec6c3c732a44b59cb5e56fd948b69b17c3b663186fd67a6b`: únicamente cambia `overrides/mods/lunaeternal-0.1.0.jar`.

Durante la parada del proceso previo se detectó `ConcurrentModificationException` en `ServerEntityManager.unload` al descargar pasajeros. El guardado `save-all flush` de todas las dimensiones terminó a 00:54:57 UTC; la fase de guardado de jugadores regresó antes del fallo de cierre de mundos a 00:54:59. El instalador retuvo el JAR hasta que terminó el proceso. Se confirmó que los workers luna-io estaban en WAITING, se conservó el log en `/root/backups_predeploy/halloween-20261009/stop-exception.log`, y se completó el cierre con SIGTERM normal, sin SIGKILL. El pool SQL no alcanzó su mensaje normal de cierre en ese proceso; no afirmar lo contrario. No se restauraron mundos ni DB. Este fallo adicional de descarga de pasajeros queda identificado y requiere una reproducción específica; no está cubierto por la contención del tick de batallas.

Reemplazo atómico offline del JAR final y arranque solicitado vía Wings a 00:56 UTC aproximadamente. Se añade el resultado del arranque a continuación.

Arranque de producción confirmado: `2026-10-10T00:56:51Z`, `Done` a 00:57:14; protección ENABLED y comando `spark tps` responden. Sin errores de aplicación de mixins, sin nuevas migraciones aplicadas y sin el NoClassDefFoundError indicado en el arranque revisado. Hash instalado coincide con el público y con staging. Las 60 migraciones conservan el contenido; V060 empaquetada vuelve al hash LF original `9ae1a242ead8ad9a90e96609e00f37bf0d84a7091cf88efbd3155de8704dd769`, sin actualizar la tabla de checksums. Staging guardado y detenido.

La lectura inmediatamente posterior al arranque (sin la carga de jugadores anterior) muestra 20 TPS y p95 de 3,5 ms; no debe compararse como una mejora de rendimiento bajo igual carga. La referencia válida con jugadores es el perfil previo enlazado arriba. El checkout principal conserva exclusivamente su cambio local original de Sabrina en archivos rastreados; los nuevos cambios se hicieron y publicaron desde el worktree verificado.
