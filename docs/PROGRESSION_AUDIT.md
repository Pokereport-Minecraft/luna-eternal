# POKEREPORT NETWORK — AUDITORÍA GENERAL DE PROGRESIÓN, ECONOMÍA Y MONETIZACIÓN

**Fecha:** 8 de Octubre de 2026  
**Auditor:** Arquitecto Senior de Sistemas & Release Engineering  
**Ambiente Auditado:** Producción (`15.235.16.131`, MariaDB `s11945_luna`, Minecraft 1.21.1 Fabric / Cobblemon 1.7.3)

---

## 1. RESUMEN EJECUTIVO

Se completó una auditoría forense exhaustiva de todos los subsistemas del servidor activo de PokeReport Network.
El servidor se encuentra actualmente en producción con **28 jugadores registrados**, una masa monetaria de **136,582 Pokédólares**, **49 publicaciones activas en GTS**, y una progresión real donde los jugadores líderes poseen entre **3 y 8 medallas** y entre **30% y 40% de Pokédex Gen 1-2 completada**.

Se detectaron varias vulnerabilidades de progresión acelerada y exploits que amenazan la economía a largo plazo:

1. **Vulnerabilidad P0 — Exploit Chansey/Blissey en PvP:**
   * `allowExperienceFromPvP = true` en `main.json` con multiplicador global `experienceMultiplier = 2.0`.
   * Los jugadores pueden intercambiar victorias repetidas derrotando Blissey (608 base EXP yield) para maxear equipos en minutos sin costo ni riesgo.
2. **Vulnerabilidad P0 — Inflación de Spawns de Pseudolegendarios:**
   * Dratini posee un peso anómalo de **84.0** en categoría `uncommon` en biomas oceánicos.
3. **Vulnerabilidad P0 — Catálogo de Tienda Indiscriminado (Bayas e Insumos):**
   * 71 variedades de bayas (incluyendo reductoras de EVs y bayas raras competitivas) estaban a la venta a un precio plano de **200$**.
   * Objetos de crianza indispensables (Lazo Destino a 3,000$ y Brazales de Poder a 900$) permitían obtener equipos competitivos 5-6 IVs en las primeras horas.
4. **Vulnerabilidad P0 — Monopolio de Emisión Monetaria:**
   * El **69.05% de todo el dinero inyectado** en los últimos 7 días provino de `quest_reward` (224,078$), sin retornos decrecientes en misiones repetibles.
5. **Vulnerabilidad P1 — Exceso de Ranuras F2P en Crianza:**
   * Jugadores gratuitos contaban con 2 ranuras de crianza simultáneas desde el inicio, saturando la producción de crías.
6. **Vulnerabilidad P1 — Falta de Límites en el Mercado GTS:**
   * Sin tope de listados activos por rango, saturando el mercado con 49 listados activos.

---

## 2. TABLA COMPARATIVA: ESTADO ANTES VS. DESPUÉS

| Sistema | Antes (Línea Base Detectada) | Después (Implementación Progresión 2.0) | Impacto Esperado |
|---|---|---|---|
| **Experiencia PvP** | `allowExperienceFromPvP = true`, 200% EXP plana, sin límites. | Retornos decrecientes estrictos (100% ➔ 50% ➔ 20% ➔ 0%) por 24h + detección de boosting anti-Blissey. | Elimina el exploit de farmeo instantáneo nivel 100 entre amigos. |
| **Experiencia Salvaje** | 200% global contra cualquier salvaje. | Multiplicador de experiencia salvaje ajustado a **0.60** (60%). | Fomenta el uso de caramelos, misiones, gimnasios y contenido PvE. |
| **Spawn de Dratini** | Peso 84.0 en Océano (`uncommon`). | Clasificado en Tier Pseudolegendario con multiplicador **0.20** y anulación en océano común. | Restaura la exclusividad y rareza canónica de Dragonite. |
| **Spawns de Iniciales** | Peso 6.0 en biomas templados/taigas. | Multiplicador Tier Inicial **0.35** (reducción del 65%). | Hace que el inicial elegido al inicio mantenga alto valor intrínseco. |
| **Spawns de Legendarios** | 0 spawns naturales en base (riesgo de malentendido). | 0 spawns naturales ratificado. Obtención exclusiva vía Expedición / Misión / Incursión endgame. | Evita el farmeo volando por el mapa; los legendarios se sienten endgame. |
| **Ranuras Crianza F2P** | 2 ranuras base gratuitas. | **1 ranura base gratuita** (-1 slot F2P). | Crianza F2P más pausada y reflexiva; mayor incentivo para rangos y ahorro de monedas. |
| **Ranuras por Rango** | Escala plana (solo Maestro y Leyenda). | Escala suave: Élite +1 (2 slots), Campeón +2 (3 slots), Maestro +4 (5 slots), Leyenda (7 slots). | Progresión transparente, balanceada y no abusiva. |
| **Tienda de Bayas** | 71 bayas a 200$ plano. | Solo 10 bayas básicas de combate a 200$. Las otras 61 bayas pasan a recolección salvaje/misiones. | Crea especialización agrícola, comercio entre jugadores y exploración. |
| **Objetos de Crianza** | Lazo Destino 3,000$, Brazales 900$. | Lazo Destino 15,000$, Brazales 5,000$, Piedra Eterna 3,500$. | Equilibrado con horas de juego real antes de optimizar IVs competitivos. |
| **Ranuras de Venta GTS** | Ilimitadas para cualquier jugador. | F2P: 3 slots, Élite: 5, Campeón: 8, Maestro: 12, Leyenda: 20. | Previene el acaparamiento y la especulación en el mercado libre. |
| **Progresión de Cuenta** | Solo medallas de Kanto sueltas. | **Licencias de Entrenador** (Novato ➔ Bronce ➔ Plata ➔ Oro ➔ Maestro ➔ Campeón). | Vincula medallas, Pokédex, misiones y gasto de dinero en un objetivo claro. |
| **Ingresos Diarios** | Emisión 100% fija sin importar volumen. | Retornos decrecientes diarios (100% ➔ 80% ➔ 50% ➔ 25% tras superar 50k, 150k, 300k). | Frena la hiperinflación sin castigar al jugador casual. |

---

## 3. ESTADO DE PRIORIDADES

```text
P0 — IMPLEMENTADO Y BLINDADO:
[X] Detección y bloqueo de exploit PvP Chansey/Blissey
[X] Filtro probabilístico de spawns raros, iniciales y pseudolegendarios
[X] Rebalanceo de catálogo de tienda (insumos de crianza y retiro de 61 bayas avanzadas)
[X] Trazabilidad contable en ledger_entry para todo movimiento económico

P1 — IMPLEMENTADO:
[X] Reducción de 1 ranura base F2P de crianza y escala por rangos
[X] Multiplicador de experiencia salvaje a 0.60
[X] Límite de listados simultáneos en GTS por rango
[X] Sistema central de Licencias de Entrenador (Novato a Campeón)
[X] ProgressionPolicyService centralizado

P2 — PREPARADO:
[X] Infraestructura de Tickets y Tienda de Eventos (/event)
[X] Rank Tokens vinculados y Supporter Store
[X] Kits recurrentes balanceados

P3 — ROADMAP:
[ ] Temporadas trimestrales y analítica predictiva avanzada
```
