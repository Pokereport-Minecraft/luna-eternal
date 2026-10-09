# POKEREPORT NETWORK — INFORME DEL SISTEMA DE EVENTOS Y TIENDA DE COLABORADORES

**Fecha:** Octubre 2026  
**Servicios Auditados:** `EventTicket.java`, `EventService.java`, `EventShopService.java`, `SupporterShopService.java`, `RankToken.java`  
**Estado:** IMPLEMENTADO · 100% COMPROBADO EN TEST SUITE

---

## 1. ARQUITECTURA DE BILLETES DE EVENTO (`EventTicket` & `EventService`)

Para coordinar eventos estacionales (ej. Halloween, Festivales Lunares, Torneos de Temporada) sin desbalancear la economía del servidor:

### Reglas de Diseño de `EventTicket`:
1. **Permanentemente Ligado (BOUND):**
   - El billete está asociado de forma inmutable al `UUID` del jugador.
   - `canTrade() == false` y `canSell() == false`. Imposible comerciarlo en GTS, soltarlo al suelo o transferirlo entre cuentas.
2. **Expiración Temporal:**
   - Cada billete cuenta con un `issuedAt` y `expiresAt`. Si el evento termina o el plazo expira, `isValid()` devuelve `false`.
3. **Consumo Idempotente:**
   - La función sincronizada `consume(String reason, Instant now)` registra la fecha y motivo del consumo.
   - Cualquier intento de reutilización posterior falla de forma segura, previniendo duplicación o ataques de repetición.

### Coste y Límite por Jugador (`EventService`):
- **Opciones de compra:** 25.000 PokéDólares (sumidero F2P) o 500 LunaCoins (moneda de apoyo).
- **Límite activo:** 1 billete por jugador por defecto (`maxTicketsPerPlayer = 1`), configurable en `ProgressionBalanceConfig.EventConfig`.

---

## 2. TIENDA ESTACIONAL DE EVENTOS (`EventShopService`)

La tienda de eventos ofrece recompensas temáticas limitadas sin permitir que cuentas secundarias o nuevos usuarios acaparen el inventario:

### Medidas Anti-Abuso:
1. **Requisito de Licencia de Entrenador:**
   - Artículos de alto impacto (ej. Caramelos Raros o Piedras Noche) requieren Licencia `BRONCE` o `PLATA`. Jugadores con cuentas secundarias recién creadas (`NOVATO`) no pueden adquirirlos.
2. **Existencias Limitadas por Jugador (`limitPerPlayer`):**
   - Cada jugador dispone de una cuota fija de compra por temporada (ej. máximo 3 Caramelos Raros, 2 Piedras Noche).
3. **Interruptor de Temporada:**
   - Si `ProgressionBalanceConfig.get().events.active` es falso, la tienda rechaza transacciones automáticamente.

---

## 3. TIENDA DE COLABORADORES SUPPORTER (`SupporterShopService`)

Permite recompensar a los donadores de rangos mediante fichas honoríficas (`RankToken`) garantizando **CERO ventajas competitivas**:

### Características de `RankToken`:
- Moneda de agradecimiento vinculada permanentemente al jugador (`isBound() == true`).
- No vendible a NPCs ni en GTS.
- No convertible a PokéDólares ni a LunaCoins.

### Catálogo de la Supporter Shop:
Todos los artículos son 100% cosméticos y de identidad:
- **Auras de Partículas:** Aura de Aurora Polar (5 tokens), Aura Carmesí Volcánica (5 tokens).
- **Títulos Honoríficos:** `[Pionero de Luna]` (3 tokens), `[Veterano]` (3 tokens).
- **Temas de PokéPad:** PokéPad Neón Cyber (4 tokens), PokéPad Vintage Dorado (4 tokens).
- **Prohibición Estricta:** Ningún artículo otorga estadísticas, EVs, IVs, niveles ni ventajas en combate Pokémon.

---

## 4. VALIDACIÓN DE PRUEBAS UNITARIAS

- [`EventTicketTest.java`](file:///d:/pokereportversionmejorada/mod/src/test/java/net/pokereport/luna/event/EventTicketTest.java): PASS.
- [`EventShopTest.java`](file:///d:/pokereportversionmejorada/mod/src/test/java/net/pokereport/luna/event/EventShopTest.java): PASS.
- [`RankKitTest.java`](file:///d:/pokereportversionmejorada/mod/src/test/java/net/pokereport/luna/rank/RankKitTest.java): PASS.
