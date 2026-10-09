# Resumen Técnico: Reestructuración de la Economía (Fases 1 a 17)
**Proyecto:** PokeReport Network - `lunaeternal` mod
**Fecha de Implementación:** Octubre 2026

Este documento contiene el desglose técnico detallado de todas las modificaciones realizadas en el código y en la base de datos para transformar la economía del servidor de un modelo estático a un modelo macroeconómico dinámico tipo MMORPG, cumpliendo con el prompt original de 16 Fases.

---

## 1. Modificaciones de Arquitectura y Base de Datos (Core)

### A. Migraciones SQL Implementadas
1.  **`V055__minero_anti_exploit.sql`**: Se introdujo la tabla `placed_ore (x, y, z, dim, placed_at)` para registrar asíncronamente qué minerales fueron colocados artificialmente por los jugadores.
2.  **`V054__economia_observable.sql`** *(Preexistente adaptada)*: Se comprobó la existencia de la tabla `npc_trade_daily` (la cual guarda el `sold_qty` y `bought_qty` histórico diario) y la tabla `ledger_entry` para el libro mayor económico.

### B. Telemetría de Jugadores (Fase 18: Valor de 1 Hora de Juego)
*   **`PlayerService.java` y `LunaEternal.java`**: Se integró un hook al evento `ServerPlayConnectionEvents.DISCONNECT`. Ahora, cuando un jugador sale del servidor, se rescata el estadístico nativo de Minecraft (`Stats.PLAY_TIME`) y se guarda asíncronamente en la columna `play_ticks` de la tabla `player`. Esto permite analizar el ingreso de Plata vs. Tiempo Real de Juego, detectando bots o *farming* automatizado.

---

## 2. Prevención de Exploits y Generación Infinita (Fases 4 y 5)

### A. Trabajo: Minero (Anti-Silk Touch Exploit)
*   **Vulnerabilidad Cerrada:** Los jugadores usaban Silk Touch para picar un mineral, colocarlo y volverlo a picar, obteniendo XP (y consecuentemente, niveles y Plata) infinita.
*   **Solución (Lag Cero):** Se inyectó `MixinBlockItem.java`. Cuando un jugador coloca un bloque de la etiqueta `*_ORES`, se inserta en `placed_ore` de fondo. En `OficiosListener.java`, cuando se rompe un mineral, se hace una consulta y borrado asíncrono. Si el bloque estaba en la tabla (fue puesto por un jugador), se otorga **0 XP y 0 Plata**.

---

## 3. Modelo de Precios Dinámicos (Fases 3 y 16)

Se eliminaron los precios estáticos que permitían granjas infinitas de dinero (como la macroagricultura de Bayas o farmeo de Muebles).

*   **`DynamicPricing.java`**: Nuevo actuador económico en memoria. Realiza un *pooling* asíncrono de la tabla `npc_trade_daily` para cachear la "Saturación del Mercado".
*   **Saturación de Recompra (NPC Sells):** Método `getDynamicSellPrice()`. Si la oferta de un ítem supera el límite de saturación (ej. 5,000 unidades diarias globales), su precio de recompra cae linealmente hasta un piso del 10%. Esto obliga a la diversificación de cultivos/farmeo.
*   **Integración UI (`Red.java`):** El *payload* de red `Tienda` (Pokepad) ahora lee y envía el precio dinámico actual en lugar del estático, para que el jugador sepa exactamente cuánto se le pagará.

---

## 4. Controlador Macroeconómico (El "Banco Central") (Fases 11, 12 y 15)

Para cumplir la exigencia de "No simular inflación añadiendo un +5% artificial", se construyó un Banco Central que lee los datos orgánicos del mercado entre jugadores (GTS).

*   **`EconomicController.java`**: Servicio asíncrono programado (`ScheduledExecutorService`) que arranca con el servidor.
*   **PPI (PokeReport Price Index):** Calcula la **mediana** de los precios de venta reales (`state = 'SOLD'`) de los últimos 7 días en el `gts_listing` de una canasta básica representativa (Ej: Poké Balls, Ultra Balls, Caramelos Raros, Diamantes).
*   **Cálculo de Inflación Real:** Compara el valor actual de la canasta contra un `PPI_BASELINE_VALUE` fijo. Esto produce una variable `currentInflationRate` (Ej: 1.25x indica 25% de inflación).

---

## 5. Drenaje de Dinero Dinámico (Sinks) (Fases 6, 13 y 14)

El GTS se convirtió en el principal amortiguador de la inflación mediante impuestos variables basados en la riqueza y la economía global.

*   **`GtsService.java`**: Se modificaron las funciones `taxFor()` y `listingFee()`. 
*   **Impuestos de Mercado Dinámicos:** Los impuestos estáticos por tramos (5%, 8%, 12%, 18%) ahora se **multiplican por la Tasa de Inflación** del `EconomicController`.
    *   *Si hay inflación (Exceso de Plata):* Los impuestos suben automáticamente para destruir más dinero circulante.
    *   *Si hay deflación (Falta de Plata):* Los impuestos bajan para dar liquidez.
*   **Precios de Compra NPC:** Se añadió `getDynamicBuyPrice()` en `DynamicPricing`. Los objetos que los jugadores compran al NPC (Poké balls, pociones) subirán de precio si hay inflación macroeconómica.
*   **Límites de Seguridad (Guardrails):** Los modificadores dinámicos tienen un límite duro por código (Mínimo 0.5x, Máximo 2.0x, y el impuesto del GTS nunca supera el 50% absoluto) para asegurar que la economía sea predecible y no se destruya el juego en picos de mercado.

---

## 6. Balance Macroeconómico y Auditoría (Fases 7, 9 y 17)

### A. Dashboard Administrativo (`EconomyReport.java`)
*   Se actualizó el comando nativo `/eco report` para incluir la telemetría del `EconomicController`. 
*   Muestra en tiempo real la Masa Monetaria, el Ratio de Faucets vs Sinks (creación vs destrucción), la distribución de riqueza (P50 vs P99), la **Inflación Actual del PPI** y el **Multiplicador de GTS Taxes**.

### B. Clasificación de Ítems (`ITEM_TIERS.md`)
*   Se generó un documento de conceptualización auditando el catálogo del servidor en 6 niveles (Tier 0 al Tier 5) basándose en su Costo de Oportunidad para guiar futuros ajustes de `silverForLevel` en los trabajos.

---
**Conclusión de la Auditoría:**
La economía del servidor ya no depende de precios codificados eternamente. Funciona bajo un ciclo cerrado donde la inyección desmedida de un ítem devalúa su propio precio de venta, y la inflación global encarece los costos de vida (compras e impuestos) para drenar la Plata sobrante. Todos los *Money Faucets* tienen contrapesos orgánicos en los *Money Sinks*.
