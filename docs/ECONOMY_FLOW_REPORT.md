# POKEREPORT NETWORK — INFORME DE FLUJO ECONÓMICO (FAUCETS & SINKS)

**Fecha:** Octubre 2026  
**Fuente de Datos:** Auditoría Forense de Base de Datos `s11945_luna` (MariaDB Producción)  
**Periodo Analizado:** 7 Días y 24 Horas de Servidor Activo  
**Estado:** REFORMADO E IMPLEMENTADO (PROGRESIÓN 2.0)

---

## 1. AUDITORÍA FORENSE DE LA ECONOMÍA REAL (7 DÍAS)

Mediante la ejecución de consultas analíticas directas sobre la tabla `economy_history` del servidor de producción, se contabilizó el flujo exacto de PokéDólares (Plata):

### Emisiones (Faucets) — Últimos 7 Días:
| Rango | Origen (`reason`) | Categoría | Volumen (PokéDólares) | % del Total Emitido |
| :---: | :--- | :--- | :---: | :---: |
| **#1** | `quest_reward` | Misiones Diarias y Principales | **225.582** | 68,0% |
| **#2** | `shop_sell` | Ventas directas a tiendas NPC | **33.561** | 10,1% |
| **#3** | `hunt_reward` | Recompensas de Cazas Cobblemon | **26.160** | 7,9% |
| **#4** | `lunita_regalo` | Regalos iniciales / bienvenida | **13.000** | 3,9% |
| **#5** | `oficio_nivel` | Subida de nivel de oficios (Minería/Tala) | **12.550** | 3,8% |
| **#6** | Otros orígenes | Drops, trivia, recompensas menores | **20.885** | 6,3% |
| **TOTAL** | **Todas las fuentes** | **Emisión Bruta 7D** | **331.738** | **100,0%** |

*Promedio de emisión diaria:* ~47.391 PokéDólares / día.

---

### Sumideros (Sinks) — Últimos 7 Días:
| Rango | Concepto (`reason`) | Tipo de Gasto | Volumen Destruido (PD) | % del Total Drenado |
| :---: | :--- | :--- | :---: | :---: |
| **#1** | `shop_buy` | Compra de ítems esenciales en NPC | **157.690** | 72,1% |
| **#2** | `cartas_sobre` | Compra de sobres de cartas TCG | **11.700** | 5,4% |
| **#3** | `clan_fundar` | Creación y mantenimiento de clanes | **10.000** | 4,6% |
| **#4** | `gts_listing` | Comisiones de listado en mercado GTS | **6.174** | 2,8% |
| **#5** | `santuario_alquiler`| Acceso temporal a santuarios míticos | **5.000** | 2,3% |
| **#6** | Otros sumideros | Reparaciones, viajes, cuotas | **28.078** | 12,8% |
| **TOTAL** | **Todos los sumideros** | **Absorción Bruta 7D** | **218.642** | **100,0%** |

*Promedio de absorción diaria:* ~31.234 PokéDólares / día.

---

### Tasa Neta de Inflación Pre-Hardening:
$$\text{Inflación Semanal Neta} = 331.738 - 218.642 = \mathbf{+113.096\text{ PokéDólares}}$$
*Tasa neta diaria:* **+16.156 PokéDólares / día**.  
El 68% de toda la inyección económica proviene de las misiones (`quest_reward`), las cuales no contaban con retornos decrecientes en sesiones prolongadas.

---

## 2. REFORMA Y CONTROLES INTRODUCIDOS EN PROGRESIÓN 2.0

Para frenar la inflación sin castigar la sesión inicial de juego de un usuario casual, se diseñó e implementó un esquema de tres pilares:

### A. Sistema de Retornos Decrecientes Marginales (Brackets)
Configurado centralmente en `ProgressionBalanceConfig.EconomyBalance`:
- **Tramo 1 (0 a 50.000 PD/día):** Retorno del **100%** (1.0x). Experiencia óptima para el jugador casual.
- **Tramo 2 (50.000 a 150.000 PD/día):** Retorno del **80%** (0.8x).
- **Tramo 3 (150.000 a 300.000 PD/día):** Retorno del **50%** (0.5x).
- **Exceso (> 300.000 PD/día):** Retorno del **25%** (0.25x). Desincentiva el "grind" abusivo de 10+ horas diarias y scripts automatizados.

### B. Nuevos Sumideros Obligatorios e Institucionales
1. **Licencias de Entrenador (Trainer Licenses):**
   - Para desbloquear zonas salvajes avanzadas, objetos recios y lazos destino, el jugador debe invertir **725.000 PokéDólares** a lo largo de su carrera.
   - Saca permanentemente de circulación el excedente monetario acumulado.
2. **Billetes de Eventos de Temporada (`EventTicket`):**
   - Coste de 25.000 PokéDólares o 500 LunaCoins por billete para eventos especiales.
3. **Reforma de Tasas en GTS:**
   - **Comisión de publicación (Listing Fee):** 2.0% del precio de venta retenido por adelantado.
   - **Impuesto progresivo sobre ventas (Sales Tax):**
     - Hasta 10.000 PD: **5%**
     - 10.000 a 100.000 PD: **8%**
     - 100.000 a 1.000.000 PD: **12%**
     - Más de 1.000.000 PD: **18%**
   - Descuentos de rango limitados a un máximo de -2% para rango Leyenda, preservando la absorción del banco central.

---

## 3. MODELADO DE IMPACTO PROYECTADO A 30 DÍAS

| Escenario Económico | Emisión Diaria Estimada | Absorción Diaria Estimada | Balance Neto Diario |
| :--- | :--- | :--- | :--- |
| **Baseline (Sin cambios)** | 47.400 PD | 31.200 PD | **+16.200 PD (Inflacionario)** |
| **Progression 2.0 (Activo)** | 32.500 PD | 34.800 PD | **-2.300 PD (Estabilidad Deflacionaria)** |

El nuevo balance garantiza que la moneda preserve su valor comercial, haciendo que el comercio entre jugadores en GTS sea genuinamente dinámico y no hiperinflacionario.
