# POKEREPORT NETWORK — AUDITORÍA ECONÓMICA Y PLAN DE ENDURECIMIENTO

**Fecha:** 8 de Octubre de 2026  
**Ambiente:** Servidor de Producción (`15.235.16.131`, Base de Datos MariaDB `s11945_luna`)  
**Libro Contable:** `ledger_entry`, `player_economy`, `economy_snapshot`, `gts_listing`  
**Moneda Base:** Pokédólares (Plata) y LunaCoins (ReportCoins)

---

## 1. ESTADO ACTUAL DE LA MASA MONETARIA

### 1.1 Oferta Monetaria Activa (Snapshot Real de Producción)
* **Cuentas registradas en economía:** 20 jugadores activos.
* **Masa monetaria total en billeteras (`POKEDOLLAR`):** **136,582 Pokédólares**.
* **Saldo promedio:** **6,829 Pokédólares**.
* **Saldo máximo individual:** **22,732 Pokédólares** (jugador más rico).
* **Masa monetaria premium (`REPORTCOIN` / LunaCoins):** **5,600 monedas** distribuidas.

---

## 2. AUDITORÍA DE FLUJOS (ÚLTIMOS 7 DÍAS EN PRODUCCIÓN)

El libro diario (`ledger_entry`) de los últimos 7 días registra:
* **Total inyectado (FAUCETS):** **324,499 Pokédólares** en 1,285 transacciones.
* **Total destruido (SINKS):** **190,564 Pokédólares** en 257 transacciones.
* **Transferencias entre jugadores (TRANSFER):** **52,803 Pokédólares** en 24 transacciones.
* **Tasa de destrucción / inyección:** **58.7%** (Existe un superávit neto inflacionario de **+133,935 Pokédólares/semana**).

### 2.1 Ranking de Fuentes de Dinero (Faucets)

| Posición | Fuente (`reason`) | Transacciones | Dinero Inyectado | % de la Economía | Nivel de Riesgo |
|---|---|---|---|---|---|
| **1** | `quest_reward` (Misiones) | 246 | **224,078$** | **69.05%** | **CRÍTICO / ALTO** |
| **2** | `shop_sell` (Venta a tienda NPC) | 315 | **33,561$** | **10.34%** | MEDIO |
| **3** | `hunt_reward` (Cazas) | 21 | **26,160$** | **8.06%** | MEDIO |
| **4** | `gts_sale` (Venta GTS) | 12 | **25,725$** | **7.93%** | NORMAL |
| **5** | `lunita_regalo` (Evento Lunita) | 13 | **13,000$** | **4.01%** | CONTROLADO |
| **6** | `oficio_nivel` (Subida de nivel de oficio) | 36 | **12,550$** | **3.87%** | BAJO |
| **7** | `quien_es_ese_pokemon` (Trivia de chat) | 22 | **11,000$** | **3.39%** | CONTROLADO |
| **8** | `admin_grant` (Asignación manual) | 1 | **5,000$** | **1.54%** | PUNTUAL |
| **9** | `pokedex_nueva` (Registro Dex) | 630 | **3,150$** | **0.97%** | MUY SEGURO |
| **10** | `premio de cofre gacha_diario` | 2 | **1,000$** | **0.31%** | MUY SEGURO |

**Diagnóstico del Faucet:**
El **69% de toda la moneda generada en el servidor proviene de las recompensas de misiones (`quest_reward`)**. Las misiones diarias y repetibles pagan sumas fijas sin límite decreciente, lo que permite a jugadores veteranos farmear misiones básicas y acumular capital rápidamente.

### 2.2 Ranking de Sumideros de Dinero (Sinks)

| Posición | Sumidero (`reason`) | Transacciones | Dinero Destruido | % del Total Sinks |
|---|---|---|---|---|
| **1** | `shop_buy` (Compras en tienda NPC) | 161 | **157,690$** | **82.75%** |
| **2** | `cartas_sobre` (Sobres TCG) | 13 | **11,700$** | **6.14%** |
| **3** | `clan_fundar` (Creación de Clanes) | 2 | **10,000$** | **5.25%** |
| **4** | `gts_listing` (Comisión publicación GTS) | 80 | **6,174$** | **3.24%** |
| **5** | `santuario_alquiler` | 1 | **5,000$** | **2.62%** |

**Diagnóstico del Sink:**
La economía depende en un 82.7% de que los jugadores gasten voluntariamente en la tienda NPC. Fuera de las compras y los sobres de cartas, casi no existen sumideros estructurales obligatorios (las comisiones de GTS representan solo el 3.2%).

---

## 3. AUDITORÍA DEL MERCADO GTS (`GtsService.java`)

### 3.1 Datos del Mercado Live
* **Listados Activos (`ACTIVE`):** 49 publicaciones (por un valor combinado de 437,700 Pokédólares).
* **Ventas Concretadas (`SOLD`):** 12 ventas (por 27,078 Pokédólares).
* **Listados Cancelados (`CANCELLED`):** 19 publicaciones (152,501 Pokédólares).

### 3.2 Vulnerabilidades Detectadas
1. **Sin Límite de Ranuras Simultáneas por Jugador:** Un solo jugador gratuito puede tener 20 publicaciones activas simultáneamente en el mercado, acaparando el catálogo.
2. **Impuesto Progresivo:** Bien implementado con tramos (5%, 8%, 12%, 18%), pero la comisión de publicación fija de solo 1% (10 por mil) no desincentiva la especulación con precios astronómicos.

---

## 4. MEDIDAS DE HARDENING ECONÓMICO

### 4.1 Retornos Decrecientes en Ganancias Diarias (Diminishing Returns)
Se implementa una política contable sobre todas las entradas de Pokédólares para jugadores activos en una ventana rodante de 24 horas:

$$\text{Factor de Rendimiento} = \begin{cases} 
100\% & \text{si acumulado diario } \le 50,000\$ \\ 
80\% & \text{si } 50,000\$ < \text{acumulado} \le 150,000\$ \\ 
50\% & \text{si } 150,000\$ < \text{acumulado} \le 300,000\$ \\ 
25\% & \text{si acumulado } > 300,000\$ 
\end{cases}$$

### 4.2 Límite de Ranuras de Publicación en GTS por Rango
* **F2P (Entrenador):** Máximo **3 publicaciones activas**.
* **ÉLITE:** Máximo **5 publicaciones activas**.
* **CAMPEÓN:** Máximo **8 publicaciones activas**.
* **MAESTRO:** Máximo **12 publicaciones activas**.
* **LEYENDA:** Máximo **20 publicaciones activas**.

### 4.3 Nuevos Sumideros Estructurales (Sinks)
1. **Licencias de Entrenador:** Costo en Pokédólares por ascenso de licencia (Bronce: 5,000$, Plata: 20,000$, Oro: 50,000$, Maestro: 150,000$, Campeón: 500,000$).
2. **Ajuste de Precios de Crianza y Competitivo en Tienda:**
   * Lazo Destino: 15,000$ (antes 3,000$).
   * Objetos Recios (6): 5,000$ cada uno (antes 900$).
   * Piedra Eterna: 3,500$ (antes 900$).
3. **Retiro de Bayas Competitivas y de EVs de la Tienda Genérica:**
   * Solo 10 bayas de combate base permanecen a 200$.
   * Las 61 bayas restantes (reductoras de EVs, bayas raras) solo se obtienen mediante agricultura, biomas salvajes y recompensas de misiones.
