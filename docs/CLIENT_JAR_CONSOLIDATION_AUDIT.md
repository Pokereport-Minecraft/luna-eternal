# Auditoría Técnica de Consolidación de Artefactos Propios

## 1. Alcance y Metodología

Esta auditoría técnica evalúa la viabilidad y los riesgos de consolidar los distintos módulos y JARs propietarios de **PokeReport Network** en un único artefacto (fat-JAR/shadow) frente a mantenerlos separados pero actualizados de forma atómica.

Se aplicaron los principios de:
- Cero regresiones en registros de Fabric y Minecraft.
- Cero colisiones de refmaps o prioridades de Mixins.
- Cero pérdida de compatibilidad con mundos y chunks guardados en producción.
- Trazabilidad y tiempos de compilación independientes.

---

## 2. Inventario de Componentes Propios

| Artefacto | modId | Proyecto / Ruta | Side | Dependencias Principales | Clasificación |
|---|---|---|---|---|---|
| `lunaeternal-0.1.0.jar` | `lunaeternal` | `mod/` | BOTH | Fabric API, GeckoLib, SimpleVoiceChat, Cobblemon, RCT API | `CLIENT_REQUIRED` |
| `lunaneon-0.1.0.jar` | `lunaneon` | `neon/` | BOTH | Fabric API, Cobblemon | `CLIENT_REQUIRED` |
| `armaduraspokereport-kits-completo-1.0.0.jar` | `armaduraspokereport` | `tools/armaduras_export/` | BOTH | Fabric API, GeckoLib | `CLIENT_REQUIRED` |
| `cobblemon-cards-1.0.5-luna1.jar` | `cobblemon_cards` | `build/pack/overrides/mods/` | BOTH | Cobblemon, Fabric API | `CLIENT_REQUIRED` |
| `cobblemon-battle-positions-1.1.3.jar` | `cobblemon_battle_positions` | `build/pack/overrides/mods/` | CLIENT | Cobblemon | `CLIENT_REQUIRED` |
| `cobblemon-additions-4.3.0-luna1.jar` | `cobblemon_additions` | `build/pack/` | BOTH | Cobblemon | `CLIENT_REQUIRED` |
| `magikarp-animated-armor-1.0.0.jar` | `magikarp_armor` | `build/pack/` | CLIENT | GeckoLib, Fabric API | `CLIENT_REQUIRED` |
| `pikachu-animated-armor-1.0.0.jar` | `pikachu_armor` | `build/pack/` | CLIENT | GeckoLib, Fabric API | `CLIENT_REQUIRED` |
| `eeveelution-armor-1.0.0.jar` | `eeveelution_armor` | `build/pack/` | CLIENT | GeckoLib, Fabric API | `CLIENT_REQUIRED` |

---

## 3. Matriz de Decisión de Consolidación

| JAR Propio | Decisión | Motivo Técnico | Riesgo Si Se Unifica | Resultado Final |
|---|---|---|---|---|
| **LunaEternal** | `KEEP_SEPARATE` | Es el núcleo central de sistemas (progreso, economía, PokePad, combate, networking). | Riesgo severo de colisión de refmaps con Loom y sobrecarga de tiempos de build. | Mantenido separado con release independiente. |
| **LunaNeon** | `KEEP_SEPARATE` | Registra bloques específicos (`lunaneon:*`) y empaqueta recursos Bedrock cosméticos para Cobblemon. | **Corrupción de chunks**: los bloques ya colocados en el mundo con ID `lunaneon:*` se convertirían en bloques desconocidos o aire si se fusiona el modId. | Mantenido separado. El actualizador atómico lo actualiza en conjunto. |
| **Armaduras de Rango** | `KEEP_SEPARATE` | Módulo cosmético de armaduras animadas con modelos y texturas GeckoLib. | Desacopla la estética de rangos de la lógica crítica del servidor. | Mantenido separado. |
| **Cobblemon Cards** | `KEEP_SEPARATE` | Mod de cartas coleccionables con recetas y pantallas propias. | Rompería la compatibilidad con el formato de cartas serializadas. | Mantenido separado. |
| **Cobblemon Battle Positions** | `KEEP_SEPARATE` | Utilidad puramente de cliente para posiciones de cámara en combate. | Mod de terceros compilado; alterar su metadata causaría desajustes con Cobblemon upstream. | Mantenido separado. |
| **Cobblemon Additions** | `KEEP_SEPARATE` | Extensión de herramientas Cobblemon. | Riesgo de romper dependencias transitivas. | Mantenido separado. |
| **Armaduras Animadas (Magikarp, Pikachu, Eevee)** | `KEEP_SEPARATE` | Assets cosméticos de cliente. | Riesgo de sobrecargar el JAR de LunaEternal con más de 100 MB adicionales innecesariamente. | Mantenido separado. |

---

## 4. Conclusión Técnica de la Auditoría

> **Principio Clave**: *No confundir un solo JAR con una sola actualización.*

Unificar forzadamente múltiples JARs en un "Fat-JAR" mediante ShadowJar en un entorno Fabric 1.21.1 introduce riesgos inaceptables:
1. **Pérdida de namespaces de bloques**: bloques con ID `lunaneon:*` desaparecerían de las construcciones de Ciudadela en el servidor y en el cliente.
2. **Colisiones de Mixins y Refmaps**: Fabric Loom genera tablas de mapeo refmap asociadas a cada modId. Fusionar clases con anotaciones Mixin bajo distintos paquetes pero un solo contenedor requiere remapeos complejos que han generado en el pasado fallos `InvalidMixinException`.
3. **Bloqueo de desarrollo**: un desarrollador que solo modifica un modelo en Neon tendría que recompilar los 108 MB de LunaEternal.

La solución de ingeniería superior es la adoptada por PokeReport: **conservar los módulos físicamente desacoplados en el compilador, pero gestionarlos como una sola unidad transaccional e indivisible para el jugador a través del PokeReport Updater**.
