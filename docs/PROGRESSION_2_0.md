# POKEREPORT NETWORK — ARQUITECTURA DE PROGRESIÓN 2.0

**Versión:** 2.0.0-PROD-READY  
**Fecha:** 8 de Octubre de 2026  
**Módulos Core:** `net.pokereport.luna.progression`, `crianza`, `gym`, `economy`, `world`

---

## 1. VISIÓN DEL BUCLE DE JUEGO (GAME LOOP)

El ciclo de progresión del jugador se reestructura desde un esquema plano hacia un embudo escalonado y satisfactorio:

```text
    ┌───────────────────────────────┐
    │       1. ENTRAR AL MUNDO      │
    └───────────────┬───────────────┘
                    ▼
    ┌───────────────────────────────┐
    │     2. EXPLORACIÓN & DEX      │  (Capturas Gen 1/2 en salvajes)
    └───────────────┬───────────────┘
                    ▼
    ┌───────────────────────────────┐
    │    3. MISIONES & OFICIOS      │  (Economía base con retornos decrecientes)
    └───────────────┬───────────────┘
                    ▼
    ┌───────────────────────────────┐
    │    4. GIMNASIOS DE KANTO      │  (Combate adaptativo, tope de nivel)
    └───────────────┬───────────────┘
                    ▼
    ┌───────────────────────────────┐
    │ 5. LICENCIAS DE ENTRENADOR    │  (Novato ➔ Bronce ➔ Plata ➔ Oro ➔ Maestro ➔ Campeón)
    └───────────────┬───────────────┘
                    ▼
    ┌───────────────────────────────┐
    │ 6. CONTENIDO RARO & EXPEDICIÓN│  (Desbloqueo de tiendas de élite y áreas salvajes)
    └───────────────┬───────────────┘
                    ▼
    ┌───────────────────────────────┐
    │ 7. CRIANZA COMPETITIVA & BOSS │  (1 slot base F2P, herencia regulada)
    └───────────────┬───────────────┘
                    ▼
    ┌───────────────────────────────┐
    │ 8. LIGA POKÉMON & ENDGAME     │  (Encuentros legendarios por mérito)
    └───────────────────────────────┘
```

---

## 2. SISTEMA DE LICENCIAS DE ENTRENADOR (`TrainerLicenseService`)

La Licencia de Entrenador es la columna vertebral de la progresión de la cuenta. No se compra con dinero real: requiere hitos reales de juego.

| Licencia | Requisito Medallas | Requisito Pokédex (Gen 1-2) | Misiones / Hitos | Costo Pokédólares | Desbloqueos en el Servidor |
|---|---|---|---|---|---|
| **NOVATO** | 0 medallas | 0% (0 Pokémon) | Inicial seleccionado | Gratuito | Acceso a Mundo Hogar y Salvaje 1. |
| **BRONCE** | 2 medallas | 15% (38 Pokémon) | 5 misiones completadas | 5,000$ | Tienda de Pociones intermedias y Salvaje 2. |
| **PLATA** | 4 medallas | 30% (75 Pokémon) | 15 misiones completadas | 20,000$ | Objetos Recios en tienda y Salvaje 3. |
| **ORO** | 6 medallas | 50% (125 Pokémon) | 30 misiones completadas | 50,000$ | Lazo Destino, Incursiones y Salvaje 4. |
| **MAESTRO** | 8 medallas (Kanto) | 70% (175 Pokémon) | 50 misiones completadas | 150,000$ | Expediciones Legendarias y Salvaje 5-6. |
| **CAMPEÓN** | 12 medallas (Liga) | 85% (213 Pokémon) | Alto Mando derrotado | 500,000$ | Título de Campeón y Santuarios Míticos. |

---

## 3. SOLUCIÓN INTEGRAL AL EXPLOIT DE CHANSEY / BLISSEY / PVP EXP

### 3.1 Causa Raíz
* Cobblemon utilizaba el rendimiento base de experiencia de Blissey (608) y Chansey (395) multiplicado por el `experienceMultiplier = 2.0` y bonos de entrenador ajeno.
* `allowExperienceFromPvP = true` permitía a dos jugadores coordinar derrotas infinitas sin costo.

### 3.2 Corrección Implementada
1. **Filtro Transaccional de PvP en `CobblemonEvents.EXPERIENCE_GAINED_EVENT_PRE`:**
   * En cualquier combate clasificado como PvP (`battle.isPvP()`):
     * **1ª victoria en 24h contra el mismo jugador:** 100% de la experiencia base.
     * **2ª victoria en 24h contra el mismo jugador:** 50% de la experiencia base.
     * **3ª victoria en 24h contra el mismo jugador:** 20% de la experiencia base.
     * **4ª victoria o más:** **0% de experiencia** (`event.setExperience(0); event.cancel();`).
2. **Detección de Boosting:** Si se detectan 3 o más noqueos de Chansey/Blissey en un lapso menor a 5 minutos entre los mismos dos jugadores, el sistema genera una alerta en la bitácora de seguridad y anula la experiencia de la sesión.
3. **Multiplicadores de Experiencia PvE:**
   * Experiencia salvaje (`wild_multiplier`): **0.60** (Reduce el grindeo desmedido matando salvajes).
   * Experiencia entrenadores NPC (`trainer_multiplier`): **1.00**.
   * Experiencia líderes de gimnasio (`gym_multiplier`): **1.10**.

---

## 4. ESTIMACIÓN DE TIEMPOS OBJETIVO (BALANCE FINAL)

* **2 Medallas:** 2 a 4 horas de juego activo.
* **4 Medallas:** 8 a 12 horas.
* **8 Medallas:** 25 a 35 horas.
* **50% de Pokédex Gen 1-2:** 20 a 30 horas.
* **80% de Pokédex Gen 1-2:** 40 a 60 horas.
* **Primer Legendario por Expedición:** 15 a 25 horas.
* **Primer Equipo Competitivo Criado:** 30 a 50 horas.
* **Endgame Completo (Liga + Campeón):** 60+ horas.
