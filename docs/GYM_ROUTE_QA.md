# POKEREPORT NETWORK — CONTROL DE CALIDAD DE RUTAS, PARKOURS Y PUZZLES

**Fecha de corte:** 08/10/2026  
**Documento:** QA y Estado de Rutas de Gimnasios  
**Componente:** `GymChallengeService` / `SabrinaParkourService` / `Combate`

---

## 1. SEPARACIÓN CONCEPTUAL: GYM BATTLE STATUS VS GYM ROUTE STATUS

Para garantizar que ningún jugador pueda enfrentarse a un líder saltándose la ruta, puzzle o parkour previo, se implementa una separación estricta en el ciclo de vida del desafío:

1. **Estado de Ruta (`GymChallengeService.esRutaCompletada`):**
   * Evalúa si el jugador ha superado la navegación física de la instancia.
   * En gimnasios sin parkour restrictivo (Brock, Misty, etc.), la ruta se considera superada al teleportarse a la entrada.
   * En gimnasios con parkour activo (**Sabrina**), el estado inicial es `IN_PROGRESS` (`routeCompleted = false`).
2. **Acceso al Combate (`Combate.empezar`):**
   * Antes de iniciar la batalla vía RCT Mod, se verifica `GymChallengeService.esRutaCompletada(uuid, gymId)`.
   * Si la ruta no está completada, la batalla se cancela con mensaje de advertencia.
3. **Estado de Batalla (`RCTMod.isInBattle`):**
   * Durante el combate, los detectores de caída de parkour quedan suspendidos para evitar interrupciones o teletransportes erróneos mientras la cámara y el Pokémon combaten.

---

## 2. MATRIZ DE ESTADO DE RUTAS POR GIMNASIO

| ID | Líder | Tipo de Recorrido / Puzzle | Estado de Ruta | Requisito Previo de Batalla |
|---|---|---|---|---|
| `brock` | Brock | Pasillo de roca hacia arena | Directo | Entrada a la instancia |
| `misty` | Misty | Plataforma de agua / piscina | Directo | Entrada a la instancia |
| `surge` | Surge | Sala de interruptores eléctricos | Directo | Entrada a la instancia |
| `erika` | Erika | Jardín botánico / camino vegetal | Directo | Entrada a la instancia |
| `koga` | Koga | Laberinto invisible + partículas veneno | Ambiental | Entrada a la instancia |
| `sabrina` | **Sabrina** | **Parkour de plataformas sobre foso** | **OBLIGATORIO** | **Llegada física a tarima de Sabrina** |
| `blaine` | Blaine | Pasillo volcánico / cuestionario | Directo | Entrada a la instancia |
| `giovanni` | Giovanni | Almacén Rocket / suelo guiado | Directo | Entrada a la instancia |
| `cissy` | Cissy | Plataforma costera | Directo | Entrada a la instancia |
| `danny` | Danny | Plataforma montañosa | Directo | Entrada a la instancia |
| `rudy` | Rudy | Plataforma insular | Directo | Entrada a la instancia |
| `luana` | Luana | Arena de hotel / combate doble | Directo | Entrada a la instancia |
| `drake` | Drake | Estadio naranja principal | Directo | Entrada a la instancia |
| `campeon_kanto`| Blue | Sala de Campeón de Kanto | Directo | Entrada a la instancia |
| `pegaso` | Pegaso | Torre alta / corriente aérea | Directo | Entrada a la instancia |
| `anton` | Antón | Vía de bicho | Directo | Entrada a la instancia |
| `blanca` | Blanca | Pasillo recto de gimnasio normal | Directo | Entrada a la instancia |
| `morti` | Morti | Dimensión nocturna / puente espectral | Directo | Entrada a la instancia |
| `anibal` | Aníbal | Sala de entrenamiento de combate | Directo | Entrada a la instancia |
| `yasmina` | Yasmina | Faro / sala de acero | Directo | Entrada a la instancia |
| `fredo` | Fredo | Sala de suelo helado | Directo | Entrada a la instancia |
| `debora` | Débora | Caverna de dragones / lava | Directo | Entrada a la instancia |
| `campeon_johto`| Lance | Sala de Campeón de Johto | Directo | Entrada a la instancia |

---

## 3. AUDITORÍA DETALLADA DEL PARKOUR DE SABRINA (P0)

### Mecánica de Navegación
* El retador aparece en la entrada elevada: $X = 5189.947, Y = 95.0, Z = 90.780 + s \cdot 128$.
* Debe sortear una serie de plataformas suspendidas sobre el foso hasta alcanzar la plataforma de Sabrina: $X = 5161.603, Y = 71.0, Z = 52.524 + s \cdot 128$.

### Reglas de Detección de Caída
* **Bounding Box de Caída:**
  $$X \in [5182.564, 5200.490]$$
  $$Z \in [153.509 + s \cdot 128, 221.492 + s \cdot 128]$$
  $$Y \le 77.0$$
* Al entrar en la zona de caída:
  1. Se invoca `GymChallengeService.registrarFallo(uuid, "sabrina")`.
  2. Teletransporte instantáneo a la entrada exacta: `Gimnasio.entrada("sabrina", slot)`.
  3. Reseteo total de velocidad: `jugador.setVelocity(Vec3d.ZERO)`.
  4. Reseteo de distancia de caída: `jugador.fallDistance = 0.0f` (anula daño de impacto).
  5. Partículas `PORTAL` en punto de caída y `WITCH` en punto de reaparición.
  6. Sonido `ENTITY_ENDERMAN_TELEPORT`.
  7. Debounce de 1500 ms por jugador para prevenir bucles de teletransporte.

### Detección de Superación de Ruta
* Al aproximarse a la plataforma de Sabrina dentro de un radio $\le 12$ bloques ($144.0$ en distancia al cuadrado) y $|\Delta Y| \le 4.0$:
  1. `GymChallengeService.completarRuta(uuid, "sabrina")`.
  2. Mensaje en chat al retador: `§a§l¡PARKOUR SUPERADO! §r§7Has alcanzado la cámara de Sabrina. ¡Habla con ella para combatir!`
  3. Habilitación del diálogo de combate en `Combate.empezar`.
