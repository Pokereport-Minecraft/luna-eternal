# POKEREPORT NETWORK — DIFICULTAD ADAPTATIVA DE GIMNASIOS & BENCHMARK

**Fecha:** 08/10/2026  
**Módulos:** `net.pokereport.luna.gym.Adaptador`, `net.pokereport.luna.gym.GymAdaptiveTeamService`, `net.pokereport.luna.gym.Gimnasio`  
**Objetivo:** Adaptabilidad de líderes de gimnasio a nivel del retador manteniendo el canon e inmutabilidad de la party.

---

## 1. PRINCIPIOS DE DISEÑO Y REGLAS FUNDAMENTALES

1. **Benchmark Inmutable de Brock:**
   - Brock es el punto de referencia de inicio del recorrido de gimnasios en Kanto.
   - Su nivel base es **Nivel 15**.
   - Su tipo principal es **Roca / Tierra**.
   - Sus Pokémon insignia (**Geodude** y **Onix**) no pueden ser sustituidos por especies no canónicas ni tener un nivel inferior al base.
2. **Inmutabilidad de la Party del Jugador:**
   - La adaptación **NUNCA** modifica la estructura, EVs, IVs, movimientos ni datos NBT del equipo del jugador retador.
   - Toda adaptación se realiza en la instancia o clon temporal del equipo del líder NPC (`PokemonModel` en RCT).
3. **Escalamiento por Rango de Líderes:**
   - Los líderes cuentan con niveles canónicos de referencia:
     - Brock: Nivel 15
     - Misty: Nivel 22
     - Lt. Surge: Nivel 28
     - Erika: Nivel 35
     - Koga: Nivel 43
     - Sabrina: Nivel 50
     - Blaine: Nivel 58
     - Giovanni: Nivel 65
   - Si un jugador entra con un equipo de nivel superior o inferior dentro de los márgenes de cap, el líder adapta dinámicamente sus movimientos de cobertura y orden táctico según la composición elemental del retador sin romper la temática del gimnasio.

---

## 2. ALGORITMO ADAPTATIVO (`Adaptador.java`)

El método `Adaptador.prepararEquipo(Gimnasio gimnasio, List<PokemonModel> retador)` ejecuta los siguientes pasos:

1. **Análisis del Retador:**
   - Se calcula el nivel promedio y el nivel máximo del retador.
   - Se auditan las debilidades y resistencias elementales predominantes en el equipo del retador.
2. **Filtrado y Preservación del Núcleo:**
   - Los dos primeros Pokémon del líder (ej. Geodude y Onix para Brock) se mantienen obligatoriamente.
   - Para gimnasios avanzados (Erika en adelante), los miembros secundarios seleccionan variantes de cobertura táctica (`coberturaNueva`) extraídas de un repertorio pre-validado de Cobblemon.
3. **Orden Táctico (`ordenarEquipo`):**
   - El líder sitúa como primer combatiente a la criatura que tenga mayor ventaja de tipos frente al primer Pokémon del retador.
   - Los tanques defensivos se priorizan para el juego intermedio y los atacantes con velocidad alta o counters directos se sitúan al final.
4. **Respeto de Nivel Mínimo:**
   - Si el nivel del retador es inferior al mínimo del gimnasio, el líder **no baja** de su nivel benchmark (ej. Brock nunca bajará de nivel 15).

---

## 3. SERVICIO DE INTEGRACIÓN (`GymAdaptiveTeamService.java`)

Para aislar las batallas de RCT y evitar que los cambios del líder persistan entre combates:
- `GymAdaptiveTeamService.crearEquipoAdaptado(gimnasio, retador)` genera una copia desacoplada del equipo del líder.
- Tras la finalización del combate (victoria, derrota o desconexión), `GymAdaptiveTeamService.limpiar(sp)` descarta el estado temporal sin tocar el almacenamiento de entrenadores del servidor.

---

## 4. SUITE DE TESTS

Se implementaron y ejecutaron las siguientes pruebas:
- `GymAdaptiveTeamTest.java`: Verifica que el nivel de Brock es exactamente 15 y que su equipo contiene a Geodude y Onix.
- `GymDifficultyPolicyTest.java`: Verifica los límites de nivel por medalla y cap de entrenamiento.
- `GymFairnessTest.java`: Verifica que ningún líder de gimnasio pueda tener un nivel que supere el cap de la medalla en disputa.
- `GymTeamPersistenceTest.java`: Verifica que la party del jugador permanece 100% inalterada antes, durante y después del combate.

**Resultado:** 100% de tests aprobados.
