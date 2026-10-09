# POKEREPORT NETWORK — AUDITORÍA Y HARDENING DEL SISTEMA DE CRIANZA

**Fecha:** 8 de Octubre de 2026  
**Módulo:** `mod/src/main/java/net/pokereport/luna/crianza/` (`CrianzaService.java`, `CrianzaReglas.java`)  
**Base de Datos:** Tabla `crianza_ranura`, `crianza_entrega_pendiente`  
**Interfaz:** Módulo de Crianza en el PokéPad

---

## 1. ESTADO ACTUAL DETECTADO (LÍNEA BASE)

### 1.1 Configuración de Ranuras
* **Total de Ranuras:** 7 ranuras máximas en el sistema.
* **Ranuras Base Gratuitas (F2P):** **2 ranuras** (Índices 0 y 1).
* **Ranuras Comprables con LunaCoins:** 3 ranuras (Índices 2, 3 y 4 a 150 LunaCoins cada una).
* **Ranuras Desbloqueadas por Rango:**
  * Ranura 5: Requiere rango **MAESTRO** (`escalon = 3`).
  * Ranura 6: Requiere rango **LEYENDA** (`escalon = 4`).

### 1.2 Tiempos de Incubación por Rango
* **F2P (Entrenador):** 25 minutos (`DURACION_BASE_MS = 25 * 60 * 1000L`).
* **Élite:** 20 minutos.
* **Campeón:** 15 minutos.
* **Maestro:** 10 minutos.
* **Leyenda:** 5 minutos.

### 1.3 Herencia Biológica y Objetos
* **Lazo Destino (`destiny_knot`):** Hereda 5 IVs de los progenitores (en lugar de los 3 estándar). Precio actual en tienda: solo 3,000 Pokédólares.
* **Piedra Eterna (`everstone`):** 100% de herencia de naturaleza. Precio actual en tienda: solo 900 Pokédólares.
* **Objetos Recios (Brazales de poder):** 100% de herencia del IV del stat asociado (`power_weight`, `power_bracer`, `power_belt`, `power_lens`, `power_band`, `power_anklet`). Precio actual en tienda: solo 900 Pokédólares.
* **Ditto + Ditto:** Totalmente funcional y restringido a bebés válidos de Gen 1 y 2 según `PoliticaFauna.getDittoPoolRandom()`.

### 1.4 Hallazgos Críticos de la Auditoría
1. **Acceso F2P Inicial Excesivamente Cómodo:** 2 ranuras simultáneas permiten a un jugador gratuito poner a criar dos parejas competitivas al mismo tiempo desde el minuto 1.
2. **Precios de Insumos Trivilializados:** Conseguir un equipo de 6 Pokémon con 5 IVs perfectos y naturalezas favorables toma menos de un fin de semana gracias al costo irrisorio de los objetos recios (900$) y el Lazo Destino (3,000$).
3. **Falta de Trazabilidad:** No se estaban registrando métricas de IVs perfectos creados ni tasa de eclosiones por rango.

---

## 2. REESTRUCTURACIÓN DE RANURAS Y RANGOS (HARDENING)

Siguiendo el mandato de reducir en **1 slot la base F2P** y establecer una curva progresiva:

### 2.1 Nueva Matriz de Ranuras

| Ranura | Índice | Requisito Previo | Nuevo Requisito | Justificación |
|---|---|---|---|---|
| **Ranura 1** | 0 | Base Gratuita (F2P) | **Base Gratuita (F2P)** | Ranura inicial única para todos los jugadores. |
| **Ranura 2** | 1 | Base Gratuita (F2P) | **Rango ÉLITE o Superior** | **-1 slot para F2P**. Los jugadores F2P se enfocan en una crianza estratégica por ciclo. |
| **Ranura 3** | 2 | 150 LunaCoins | 150 LunaCoins o Rango CAMPEÓN | Expansión de conveniencia o rango medio. |
| **Ranura 4** | 3 | 150 LunaCoins | 150 LunaCoins o Rango CAMPEÓN | Expansión comercial balanceada. |
| **Ranura 5** | 4 | 150 LunaCoins | 150 LunaCoins o Rango MAESTRO | Expansión comercial balanceada. |
| **Ranura 6** | 5 | Rango MAESTRO | Rango MAESTRO | Ventaja de conveniencia de rango alto. |
| **Ranura 7** | 6 | Rango LEYENDA | Rango LEYENDA | Máxima capacidad exclusiva de Leyenda. |

### 2.2 Resumen de Capacidad Efectiva por Estatus

* **F2P (Entrenador):** 1 ranura activa base (ampliable con LunaCoins a 2-4 si el jugador ahorra monedas de eventos).
* **ÉLITE:** 2 ranuras activas base (+1 respecto a F2P). Ciclo: 20 min.
* **CAMPEÓN:** 3 ranuras activas base (+2 respecto a F2P). Ciclo: 15 min.
* **MAESTRO:** 5 ranuras activas base (+4 respecto a F2P). Ciclo: 10 min.
* **LEYENDA:** 7 ranuras activas máximas. Ciclo: 5 min.

*Nota de Preservación:* La regla preserva compras históricas de ranuras (`desbloqueada_monedas = 1`) en la base de datos `crianza_ranura` para evitar cualquier afectación a jugadores que hayan desbloqueado ranuras previamente.

---

## 3. REBALANCEO DE OBJETOS DE CRIANZA EN TIENDA

Para evitar que la crianza de IVs perfectos sea inmediata:
* **Lazo Destino (`cobblemon:destiny_knot`):** Pasa de **3,000$** a **15,000$** Pokédólares.
* **Objetos Recios (`power_weight`, `power_bracer`, etc.):** Pasan de **900$** a **5,000$** Pokédólares cada uno.
* **Piedra Eterna (`cobblemon:everstone`):** Pasa de **900$** a **3,500$** Pokédólares.

---

## 4. TELEMETRÍA Y OBSERVABILIDAD

Se añade en `CrianzaService` el registro de eventos en el log estructurado:
* `crianza_iniciada`: `player_id`, `slot`, `madre`, `padre`, `rank`.
* `huevo_eclosionado`: `player_id`, `especie`, `iv_total`, `perfect_iv_count`, `shiny`.
* `alerta_iv_perfecto`: Si un huevo eclosiona con 5 o 6 IVs a 31, se genera un asiento de auditoría para trazabilidad genética.
