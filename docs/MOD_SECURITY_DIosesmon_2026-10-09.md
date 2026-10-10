# Revisión de mods y cliente — 2026-10-09

Repositorios descargados como datos y leídos; no se ejecutaron sus builds ni se instalaron sus JAR. CobblemonPatches commit `6e3dd6975e69ae448153121a51cc62066f3cbe93`; AsyncCatcher commit `a7015ea182067670daabd0aa450b565f65d6ddba`.

## CobblemonPatches

Optimiza spawn, entidades, PC/party, guardados y skins y modifica batallas. Compila contra 1.8.1 y exige >=1.8.1; incompatibilidad directa con nuestro 1.8.0. Configuración no permite desactivar individualmente mixins; required=true/defaultRequire=1. Cambiar a 1.8.1 no valida automáticamente su convivencia con Luna/RCT.

Hallazgos en código:

- `AIBattleActorMixin.fallbackPass()` usa `PassActionResponse.INSTANCE`. Su catch(Throwable) llama de nuevo a ese fallback fuera de una segunda contención: una clase ya fallida puede volver a lanzar LinkageError. No constituye solución garantizada al crash original.
- `PokemonBattleMixin.checkInactivityTimeout()` cuenta ticks mientras cualquier actor mustChoose; no identifica por sí solo un deadlock. Superados 2400 ticks termina la batalla; también puede acabar una elección lenta válida. No equivale siempre a 120 segundos reales bajo TPS bajo.
- `FileBackedPokemonStoreFactoryMixin`: si save lanza excepción, retira el store de dirtyStores. Riesgo de perder el reintento de persistencia; no es prueba de pérdida observada.
- `NPCEntityMixin.downloadTextureBytes`: URI arbitraria, timeout de 5 segundos, readAllBytes sin límite de tamaño ni allowlist. Riesgo condicional de consumo de memoria/acceso a URLs locales si un actor no confiable pudiera controlar esa URI; no se demostró que los jugadores tengan esa capacidad.
- Mixins async actuales principalmente registran violaciones mediante PatchesUtil; el changelog reconoce retirada del marshaller de eliminación de entidades. No convierten todas las operaciones en seguras.
- Metadata Fabric declara All-Rights-Reserved mientras LICENSE/properties indican GPL: discrepancia de empaquetado pendiente de aclaración para distribución.

No se identificaron mecanismos explícitos de robo de credenciales, ejecución de programas arbitrarios en runtime o subida de datos en las rutas revisadas. Hay tráfico de skins/perfiles y escritura de configuración esperables. El workflow publica artefactos/avisos desde CI con secrets del autor; no es una llamada del runtime al Discord del jugador. Revisión estática no certifica el binario distribuido ni sus dependencias. Decisión: no incorporarlo íntegro en producción; estudiar fixes concretos y probarlos de forma separada.

## AsyncCatcher

Solo servidor (metadata environment=server), MC ~1.21.1, Java >=21. Build usa Loader0.19.5 pero metadata permite cualquier loader: esa permisividad no demuestra compatibilidad con nuestro runtime0.18.4.

Siete archivos mixin y tres clases internas añaden guardas al acceso de entidades, tickets, bloques, teleports y comandos. `catchOp` verifica hilo y lanza IllegalStateException. No reprograma la operación en server.execute, no repara el mod que la originó y no mejora FPS del cliente. No hay modo de solo diagnóstico configurable. Una excepción sin contener puede interrumpir una operación/tick; usarlo en staging para identificar infractores, no prometer que evita crashes.

No hay red, lectura de credenciales ni ejecución de procesos en el código Java revisado. No se analizó un binario particular descargado. Decisión: útil como detector en entorno aislado, no instalar directamente en producción con jugadores.

## Diosesmon

Instancia original conservada: Fabric0.19.2, Minecraft1.21.1, Cobblemon1.7.3, 90 JAR principales. Se leyó metadata interna y hashes, no solo nombres. Nuestro manifiesto real vigente tiene164 ficheros y ya contiene Sodium0.8.13, ImmediatelyFast1.6.13, Lithium0.15.4, EntityCulling1.10.5, MoreCulling1.0.10, ModernFix5.25.1, FerriteCore7.0.3 y BadOptimizations2.4.1. No retroceder a los JAR más antiguos de Diosesmon.

Candidatos visuales iniciales descargados oficialmente y verificados contra Modrinth: Chat Heads0.15.1 (MPL2.0) y ChatAnimation1.1.3 (CC0), cliente únicamente. Falling Leaves podría ser opcional; ya hay Particular/ParticleRain/ParticleCore, evitar sumar efectos redundantes. No copiar su mega_showdown, SimpleTMs, Additions o mod propietario a PokeReport. Bobby retiene chunks adicionales y puede elevar RAM/disco. ItemBorders declara NC y ND: no trasladarlo al pack comercial sin aclarar permiso del autor. ImmersiveUI requiere revisión específica de compatibilidad con Poképad y sus animaciones.

Configuración local PokeReport observada: shaders ComplementaryUnbound activos, render12, simulation12, FPS260 y partículas completas. Puede explicar carga gráfica, pero no se midieron FPS/frametimes ni VRAM: no afirmar ganancia porcentual. Para un perfil ligero, probar sin shaders primero y comparar igual escena; limitar FPS y ajustar sombras/partículas antes de sumar más mods. No se sobrescribieron preferencias del usuario.
