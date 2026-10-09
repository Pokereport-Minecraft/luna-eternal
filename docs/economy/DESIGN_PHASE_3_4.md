# Diseño de Fases 3 y 4: Anti-Exploits y Precios Dinámicos

Este documento define la arquitectura técnica para corregir las vulnerabilidades halladas en la Fase 1 y establecer las bases de la economía dinámica para PokeReport.

## 1. Anti-Exploit de Trabajos (Minero)

**Vulnerabilidad Actual:**
El trabajo de `Minero` (en `OficiosListener.java`) recompensa al jugador por destruir bloques específicos (`BlockTags.DIAMOND_ORES`, etc.). Un jugador puede minar un mineral con un pico de *Silk Touch*, colocar el bloque de mineral en el suelo y volver a minarlo, generando un bucle infinito de Plata y XP.

**Solución Diseñada: Trackeo de "Bloques Contaminados" (Placed Blocks)**
No podemos impedir que los jugadores coloquen bloques de mineral (es una mecánica válida de construcción). En cambio, el servidor "recordará" qué bloques fueron colocados por entidades que no son el generador del mundo natural.

1.  **Modelo de Datos (`V055__minero_anti_exploit.sql`):**
    Crearemos una tabla `placed_block` optimizada:
    ```sql
    CREATE TABLE placed_block (
        x INT NOT NULL,
        y INT NOT NULL,
        z INT NOT NULL,
        dim VARCHAR(64) NOT NULL,
        placed_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
        PRIMARY KEY (x, y, z, dim)
    ) ENGINE=InnoDB;
    ```

2.  **Lógica del Listener (`OficiosListener.java` / `BlockPlacementListener.java`):**
    *   **On Block Place:** Si el bloque colocado pertenece a las etiquetas de minerales (`BlockTags.*_ORES`), insertamos de forma asíncrona sus coordenadas en `placed_block`.
    *   **On Block Break:** Antes de pagar al Minero, consultamos (y borramos simultáneamente) la coordenada en la tabla `placed_block`. Si la fila existía, el bloque fue puesto artificialmente $\rightarrow$ **Recompensa = 0**. Si no existía $\rightarrow$ **Recompensa Normal**.
    *   *Nota de Performance:* Para evitar saturar el pool de base de datos en minería masiva con herramientas 3x3, las consultas de eliminación se enviarán en lote o se usará una caché LRU en memoria respaldada por la BD si la carga lo amerita. Por ahora, un `DELETE` directo por bloque es rápido en MariaDB con HikariCP.

## 2. Telemetría de Ingresos: Plata por Hora

**Vulnerabilidad Actual:**
No hay forma de calcular cuánto gana realmente un jugador por hora activa, pues los KPIs económicos se diluyen por ausencias (AFK).

**Solución Diseñada:**
Utilizar la tabla `player_economy` existente para registrar la verdadera actividad. Implementar una métrica de "Tiempo Económico Activo". 

## 3. Precios Dinámicos NPC (Buy Prices)

**Vulnerabilidad Actual:**
Las Bayas (cosechables cada pocas horas) y Muebles básicos tienen precios de recompra fijos. Generaron +115,200 Plata en las pruebas. Un jugador puede causar hiperinflación.

**Solución Diseñada: `MarketPressure`**
El sistema introducirá el concepto de "Presión de Oferta" (Supply Pressure).

1.  **Fórmula Dinámica Base:**
    Cada ítem tendrá un `basePrice` y una "canasta diaria" (ej. el NPC espera comprar 500 Bayas Aranja al día al precio base). 
    ```
    PrecioActual = PrecioBase * ( 1 - (UnidadesVendidasHoy / LimiteSaturacion) )
    ```
    *   `PrecioMínimo`: Evitaremos que el precio llegue a cero. El piso (floor) será el 10% del valor base.
2.  **Saturación:**
    Si el servidor le inyecta al NPC 5,000 bayas en un día, el precio caerá rápidamente a su valor de saturación (ej. 1 Plata) desincentivando el cultivo industrial infinito y obligando al jugador a buscar otro "trabajo" o mercado (GTS).
3.  **Implementación:**
    Esto requiere modificar el controlador del mercado NPC (`ShopService.java` o similar) para registrar en base de datos la cantidad total global vendida por artículo en ventanas de 24 horas (`shop_global_velocity`).

---
*Este diseño ha sido trazado para ser implementado en la iteración actual (Fase 3/4).*
