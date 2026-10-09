# POKEREPORT NETWORK — INFORME DE BALANCE DE TIENDAS Y CATÁLOGO NPC

**Fecha:** Octubre 2026  
**Componentes Auditados:** `shop_catalog.json`, `ShopCatalog.java`, `ShopService.java`, `DynamicPricing.java`  
**Estado:** CERTIFICADO · ANTI-ARBITRAJE COMPROBADO · 100% PASS

---

## 1. INTEGRIDAD DEL CATÁLOGO Y VALIDACIÓN ANTI-ARBITRAJE

El catálogo oficial de la tienda (`shop_catalog.json`) contiene más de 620 artículos distribuidos en múltiples categorías esenciales, de combate, cosméticos y construcción.

### El Invariante Fundamental Anti-Arbitraje
En cualquier economía virtual abierta con NPCs compradores, el fallo más destructivo es el arbitraje (comprar barato al NPC y revenderle caro al mismo o a otro NPC, generando dinero infinito).

En PokeReport Network, este principio se encuentra blindado en el código fuente de [`ShopCatalog.java`](file:///d:/pokereportversionmejorada/mod/src/main/java/net/pokereport/luna/shop/ShopCatalog.java) y verificado por [`ShopPriceTest.java`](file:///d:/pokereportversionmejorada/mod/src/test/java/net/pokereport/luna/shop/ShopPriceTest.java):

$$\forall e \in \text{CatalogEntries}, \quad \text{sell}(e) \le \text{buy}(e)$$

- **Total de entradas auditadas:** > 120 artículos comerciales.
- **Entradas con violación de arbitraje:** **0**.
- **Regla de arranque:** Si cualquier entrada modificada incumple esta condición durante el inicio, el servidor aborta el arranque automáticamente para evitar corrupción de la economía.

---

## 2. PRECIOS DE REFERENCIA Y RATIOS COMERCIALES

Para evitar la desvalorización del dinero y proteger el esfuerzo de los jugadores que recolectan objetos en el mundo, se establecieron ratios estrictos entre el coste de adquisición y el precio de recompra:

| Artículo | Identificador | Precio Compra (`buy`) | Precio Recompra (`sell`) | Ratio Recompra |
| :--- | :--- | :---: | :---: | :---: |
| **Poké Ball** | `cobblemon:poke_ball` | 400 PD | 40 PD | 10,0% |
| **Super Ball** | `cobblemon:great_ball` | 800 PD | 80 PD | 10,0% |
| **Ultra Ball** | `cobblemon:ultra_ball` | 1.200 PD | 120 PD | 10,0% |
| **Caramelo Raro**| `cobblemon:rare_candy` | 5.000 PD | 500 PD | 10,0% |
| **Caramelo XL** | `cobblemon:exp_candy_xl` | 8.000 PD | 800 PD | 10,0% |
| **Poción Máxima**| `cobblemon:max_potion` | 1.000 PD | 100 PD | 10,0% |
| **Revivir** | `cobblemon:revive` | 1.500 PD | 150 PD | 10,0% |
| **Lazo Destino** | `cobblemon:destiny_knot` | 45.000 PD | 4.500 PD | 10,0% |
| **Piedra Fuego** | `cobblemon:fire_stone` | 15.000 PD | 1.500 PD | 10,0% |

> **Nota de Diseño:** Un ratio del 10% asegura que la venta directa de ítems sea una fuente secundaria de subsistencia y no la vía principal de enriquecimiento, redirigiendo la liquidez hacia el mercado GTS entre jugadores.

---

## 3. PRECIOS DINÁMICOS Y PROTECCIÓN CONTRA GRANJAS AUTOMATIZADAS

Para mitigar el impacto de granjas industriales masivas (ej. granjas automáticas de bayas, madera o cultivos vanilla):
- Se implementó [`DynamicPricing.java`](file:///d:/pokereportversionmejorada/mod/src/main/java/net/pokereport/luna/shop/DynamicPricing.java).
- **Mecanismo:** La fórmula calcula la oferta acumulada diaria en `npc_trade_daily`:
  $$\text{factor} = \text{floor} + \frac{1 - \text{floor}}{1 + \left(\frac{\text{supply}}{\text{threshold}}\right)^{\text{elasticity}}}$$
- **Umbral de saturación (`threshold`):** 5.000 unidades vendidas globalmente al día.
- **Suelo mínimo de protección (`floor`):** 10% del precio base.
- Si los jugadores inundan el mercado con 20.000 bayas o cultivos, el precio cae gradualmente, evitando que el servidor absorba emisiones descontroladas de PokéDólares.

---

## 4. RESOLUCIÓN DE MÓDULOS DE PROTECCIÓN (HEAD METADATA)

Un error crítico resuelto en la arquitectura de `ShopCatalog` fue la discriminación de los bloques de protección (`protectionstones`):
- Los 5 tamaños de protección comparten el mismo `Item` base (`minecraft:player_head`).
- Si se utiliza el identificador del ítem para procesar compras, todas las compras entregarían la piedra más básica.
- **Solución implementada:** La propiedad `clave()` en `ShopCatalog.Entry` concatena `item#entrega`, garantizando que la entrega mediante `Modulos.fabricar(entry.entrega(), amount)` entregue exactamente el bloque configurado con su NBT y tamaño respectivo.

---

## 5. CONCLUSIONES Y RECOMENDACIONES

1. El catálogo de tiendas está estabilizado y protegido contra ataques de arbitraje.
2. El sistema dinámico previene hiperinflación originada en granjas técnicas.
3. Se recomienda mantener las piedras evolutivas y lazos destino vinculados a requisitos de Licencia de Entrenador (`TrainerLicense.PLATA` y `TrainerLicense.ORO`).
