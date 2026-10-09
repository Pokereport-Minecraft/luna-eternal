# POKEREPORT NETWORK — INFORME DE BALANCE DE LA ECONOMÍA DE MIEL Y CARAMELOS

**Fecha:** Octubre 2026  
**Componentes Auditados:** Recetas de Olla de Fogata Cobblemon (Campfire Pot), Granjas de Abejas Vanilla, `HoneyTelemetry.java`, `HoneyEconomyTest.java`  
**Estado:** LOOP DE GENERACIÓN ILIMITADA MITIGADO · TELEMETRÍA ACTIVA

---

## 1. DESCRIPCIÓN DEL SISTEMA Y MECÁNICAS DE JUEGO

Cobblemon implementa una mecánica de cocina interactiva mediante la **Olla de Fogata (Campfire Pot)**, la cual permite colocar ingredientes sobre un fuego para elaborar consumibles Pokémon.

Entre estos ingredientes, los productos apícolas de Minecraft vanilla (`minecraft:honeycomb` y `minecraft:honey_bottle`) ocupan un lugar central:

| Ingrediente | Recetas Producidas en Olla de Fogata Cobblemon | Impacto en la Jugabilidad |
| :--- | :--- | :--- |
| **Panal de Miel** (`honeycomb`) | • `cobblemon:exp_candy_xs` (Caramelos de EXP)<br>• Todos los caramelos de EVs (`courage_candy`, `health_candy`, `mighty_candy`, `quick_candy`, `smart_candy`, `tough_candy`)<br>• `cobblemon:metal_coat` (Revestimiento Metálico) | Salta el entrenamiento de EVs y la subida de nivel de Pokémon. |
| **Botella de Miel** (`honey_bottle`) | • Todos los Mochis de Kitakami (incluyendo `fresh_start_mochi` de reseteo total de EVs)<br>• Mochis de aumento de EVs (+10 EVs)<br>• Pociones y Revivir | Salta la recolección de bayas reductoras y objetos de reset. |

---

## 2. EL PROBLEMA: GRANJAS AUTOMÁTICAS DE ABEJAS VANILLA

1. **Facilidad de Automatización:** En Minecraft 1.21.1, una colmena con dispensadores automáticos, tijeras y botellas conectada a un reloj de redstone produce **más de 120 panales y botellas por hora de forma pasiva y sin costes**.
2. **El Bucle de Generación:**
   $$\text{Granja de Abejas} \longrightarrow \text{Panales Ilimitados} \longrightarrow \text{Olla de Fogata} \longrightarrow \text{Caramelos de EXP y EVs Infinitos}$$
3. **Evasión de Progresión:** Los jugadores novatos no requerían luchar contra Pokémon salvajes, entrenadores ni líderes de gimnasio para optimizar las estadísticas de sus Pokémon, erosionando el reto del servidor.

---

## 3. MEDIDAS DE BALANCE IMPLEMENTADAS

Para preservar la viabilidad de la apicultura como actividad legítima sin que eclipse el entrenamiento:

### A. Multiplicador de Rendimiento de Cocina (`candyCookMultiplier`)
- Configurado centralmente en `ProgressionBalanceConfig.HoneyBalance`.
- Valor calibrado: **`0.50`** (50% de rendimiento por lote de cocción en fogata).
- Reduce la inyección masiva de caramelos a la mitad, forzando al jugador a depender de combates y misiones para su entrenamiento principal.

### B. Control del Valor de Reventa NPC
- `baseHoneyValue`: 250 PokéDólares por botella.
- Regulado activamente por la curva de saturación de [`DynamicPricing.java`](file:///d:/pokereportversionmejorada/mod/src/main/java/net/pokereport/luna/shop/DynamicPricing.java): si un jugador intenta liquidar 2.000 botellas de miel al banco, el precio de compra del NPC cae dinámicamente hasta su suelo mínimo (25 PD), anulando el arbitraje pasivo.

### C. Telemetría de Apicultura (`HoneyTelemetry.java`)
Monitoreo continuo de:
- `honeyBottlesGenerated` y `honeycombsGenerated` (Cosechas)
- `honeyBottlesConsumed` y `honeycombsConsumed` (Consumos)
- `expCandiesCooked`, `evCandiesCooked` y `mochisCooked` (Producción de cocina)

Validado con éxito en [`HoneyEconomyTest.java`](file:///d:/pokereportversionmejorada/mod/src/test/java/net/pokereport/luna/progression/HoneyEconomyTest.java) (PASS).
