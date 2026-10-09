# Clasificación de Ítems (Tiers) - PokeReport Network

Como parte de la **Fase 17 de la Auditoría Económica**, se han inspeccionado conceptualmente todos los objetos comercializables en el servidor (incluyendo consumibles de Cobblemon, Pokedolls, materiales de Minecraft y objetos de crianza). 

Se ha estructurado la economía en un modelo de 6 Tiers (Niveles) basados en el **Costo de Oportunidad** y el **Valor de una Hora de Juego (VHJ)**. Esta estructura servirá como mapa de ruta para balanceos futuros y para la base de datos del Índice de Precios PPI.

## Modelo Tiers Económicos

### TIER 0: Recursos Infinitos / Agricultura (Ej. Bayas, Cultivos Básicos)
*   **Obtención:** Granjas pasivas o de poco esfuerzo.
*   **Riesgo Económico:** Muy alto (potencial hiperinflacionario).
*   **Balanceo Asignado:** Tienen un precio de NPC base bajo y están estrictamente controlados por la **Presión de Mercado (DynamicPricing)**. Nunca deben superar las 2-5 Plata reales.
*   **Ejemplos:** Berries comunes, Trigo, Madera común, Piedra natural.

### TIER 1: Consumibles Regulares (Ej. Poké Balls, Pociones)
*   **Obtención:** Crafteo estándar o compra en tienda NPC (Sinks de Plata).
*   **Riesgo Económico:** Bajo.
*   **Balanceo Asignado:** Su función es actuar como estabilizadores. Su precio de mercado no suele fluctuar más allá de márgenes predecibles. Son el ancla del **PPI (Price Index)**.
*   **Ejemplos:** Poké Balls (400 Plata), Super Balls, Pociones.

### TIER 2: Materiales de Farmeo Activo (Ej. Minerales Básicos, Drop Común)
*   **Obtención:** Trabajos activos (Minero, Pescador) sin exploits. Requieren tiempo real de jugador (Acciones por Minuto controladas por `JobsConfig`).
*   **Riesgo Económico:** Medio (Requieren topes y control de acciones/minuto).
*   **Balanceo Asignado:** Generan el principal ingreso neto de Plata a la economía.
*   **Ejemplos:** Carbón, Hierro, Cobre.

### TIER 3: Recursos Escasos & Competitivos Iniciales (Ej. Ultra Balls, EV Items)
*   **Obtención:** Crafteo complejo, recompensas de Quests o misiones diarias.
*   **Riesgo Económico:** Medio.
*   **Balanceo Asignado:** Objetos de gran liquidez en el GTS. Al ser útiles y limitados, su impuesto de venta en GTS (listing fee + tax) retira un gran porcentaje de dinero circulante.
*   **Ejemplos:** Ultra Balls, Caramelos Raros, Diamantes, Piedras Evolutivas.

### TIER 4: Cosméticos y Estética Exclusiva (Ej. Pokedolls, Muebles Premium)
*   **Obtención:** Compra con LunaCoins (Moneda Premium), o farmeo intensivo de eventos.
*   **Riesgo Económico:** Nulo en Plata (si no son recomprados por NPCs).
*   **Balanceo Asignado:** Sinks definitivos. Al no tener valor competitivo, sirven como drenaje estético de riqueza de los jugadores más ricos.
*   **Ejemplos:** `pokeblocks:pokedoll_*`, Sombreros, Títulos, Partículas.

### TIER 5: Endgame & Legendarios (Ej. Master Balls, Objetos de Crianza Perfecta)
*   **Obtención:** Eventos únicos, finalización total de Pokedex o Santuario.
*   **Riesgo Económico:** Muy alto (Desbalancean el juego si son comerciables).
*   **Balanceo Asignado:** Generalmente ligados al jugador (Soulbound) o sujetos a los impuestos del tramo máximo de GTS (18%+ con multiplicador inflacionario).
*   **Ejemplos:** Master Balls, Orbes Legendarios, Destiny Knots.

---

### Métrica Base: El Valor de una Hora de Juego (VHJ)
A partir de la nueva implementación de `play_ticks`, definiremos el VHJ objetivo. Un jugador activo promedio no debería poder generar Plata suficiente para vaciar el Tier 3 en menos de 10 horas de juego efectivo. Esta métrica alimentará los rebalanceos de `silverForLevel` en las próximas iteraciones.
