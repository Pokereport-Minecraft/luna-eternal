# POKEREPORT NETWORK — INFORME DE DIFICULTAD Y PROGRESIÓN DE GIMNASIOS

**Fecha:** Octubre 2026  
**Componentes Auditados:** `Gimnasio.java`, `GymLevelCapService.java`, `COBBLEVERSE-RCT-DP-v20.zip`, `GymDifficultyPolicyTest.java`  
**Estado:** LÍMITES DE NIVEL Y OBEDIENCIA ACTIVOS · AUDITORÍA DE DATAPACK RCT COMPLETADA

---

## 1. ESTRUCTURA DE LA LIGA Y FRONTERAS REGIONALES (GEN 1 Y GEN 2)

La arquitectura de combates de gimnasio de PokeReport Network comprende exactamente **23 retos oficiales** instanciados en dimensiones aisladas, respetando rigurosamente las fronteras de las Generaciones 1 y 2:

| Región | Gimnasios Registrados | Campeón / Reto Final | Total Retos | Bitmask Medallas |
| :--- | :---: | :---: | :---: | :---: |
| **Kanto** | 8 Líderes (Brock a Giovanni) | Campeón de Kanto | 9 | Bits 0 al 8 |
| **Liga Naranja** | 4 Líderes (Cissy a Luana) | Drake (Trofeo Naranja) | 5 | Bits 9 al 13 |
| **Johto** | 8 Líderes (Pegaso a Débora) | Campeón de Johto | 9 | Bits 14 al 22 |
| **TOTAL** | **20 Líderes** | **3 Campeones** | **23** | **23 Bits Únicos** |

---

## 2. POLÍTICA DE LÍMITES DE NIVEL (LEVEL CAPS) Y OBEDIENCIA

El servicio [`GymLevelCapService.java`](file:///d:/pokereportversionmejorada/mod/src/main/java/net/pokereport/luna/gym/GymLevelCapService.java) impone una curva de progreso obligatoria:

```text
0 Medallas  ──> Tope Nv. 15 (Brock)
1 Medalla   ──> Tope Nv. 19 (Misty)
2 Medallas  ──> Tope Nv. 24 (Teniente Surge)
3 Medallas  ──> Tope Nv. 28 (Erika)
4 Medallas  ──> Tope Nv. 32 (Koga)
5 Medallas  ──> Tope Nv. 37 (Sabrina)
6 Medallas  ──> Tope Nv. 42 (Blaine)
7 Medallas  ──> Tope Nv. 50 (Giovanni)
...
23 Medallas ──> Tope Nv. 100
```

### Reglas de Ejecución:
1. **Captura Libre:** Un jugador novato puede capturar un Pokémon de nivel alto si es capaz de atraparlo.
2. **Obediencia y Combate Bloqueados:** Si el nivel del Pokémon supera el tope permitido por sus medallas:
   - Se bloquea lanzarlo de su Pokéball al mundo (`POKEMON_SENT_PRE`).
   - Se bloquea iniciar combates PvE con ese Pokémon en el equipo (`BATTLE_STARTED_PRE`).
   - Se cancela la interacción o montura con él (`UseEntityCallback`).
3. **Bloqueo de EXP y Caramelos:**
   - La ganancia de experiencia en combates se cancela (`EXPERIENCE_GAINED_EVENT_PRE`).
   - El uso de caramelos raros y caramelos de EXP se cancela (`EXPERIENCE_CANDY_USE_PRE`).

---

## 3. AUDITORÍA DEL DATAPACK DE ENTRENADORES (RCT)

Mediante el script analítico [`tools/audit_gym_leaders.py`](file:///d:/pokereportversionmejorada/tools/audit_gym_leaders.py) se examinaron los archivos JSON de los líderes en `build/COBBLEVERSE-RCT-DP-v20.zip`:

### Hallazgos del Datapack:
- **Identificadores Verificados:**
  - Kanto utiliza nombres directos (ej. `kanto_brock`, `kanto_misty`, `kanto_ltsurge` *sin guion*).
  - Johto utiliza identificadores de origen italiano asignados por RCT (`valerio`, `raffaello`, `chiara`, `angelo`, `furio`, `jasmine`, `alfredo`, `sandra`).
- **Configuración de Movimientos (`moves`):**
  - Actualmente, la mayoría de los equipos en el datapack tienen `moves = Default`.
  - Cobblemon autogenera movimientos por nivel, lo que resulta en combinaciones subóptimas y falta de cobertura de tipos en gimnasios avanzados (ej. Koga, Sabrina o Giovanni).
- **Pericia de la IA:**
  - Todos los líderes en código tienen declarada `pericia >= 3` (IA avanzada de Showdown con cambios y predicción de debilidades).

---

## 4. RECOMENDACIONES DE MEJORA COMPETITIVA

1. Mantener los niveles de Kanto de 15 a 50 y la Liga Naranja como puente hacia Johto (50 a 65).
2. Para futuras iteraciones de datapack, actualizar los archivos JSON de RCT con sets competitivos personalizados (IVs 31, EVs distribuidos y objetos equipados competitivos como Banda Focus, Restos y Vidasfera).
3. Todo el comportamiento de límites y obediencia está respaldado por [`GymDifficultyPolicyTest.java`](file:///d:/pokereportversionmejorada/mod/src/test/java/net/pokereport/luna/gym/GymDifficultyPolicyTest.java) (PASS).
