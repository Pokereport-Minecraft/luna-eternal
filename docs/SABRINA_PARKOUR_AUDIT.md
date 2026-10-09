# POKEREPORT NETWORK — AUDITORÍA FORENSE DEL PARKOUR DE SABRINA (P0)

**Fecha de corte:** 08/10/2026  
**Documento:** Auditoría y Certificación del Gimnasio Psíquico (Sabrina)  
**Clase Principal:** `SabrinaParkourService.java`  
**Gimnasio:** ID `sabrina`, Región Kanto, Sala 5, Dimensión `lunaeternal:gimnasios`

---

## 1. DESCRIPCIÓN DEL RETO

El Gimnasio de Sabrina cuenta con un reto físico de parkour donde el retador debe saltar entre plataformas levitantes situadas sobre un foso psíquico. Previo a esta auditoría, existía el riesgo de que los retadores se saltaran el parkour volando, usando perlas de ender, o que al caer quedaran atascados en el vacío o sufrieran daño letal por caída al reaparecer.

---

## 2. DELIMITACIÓN EXACTA DE COORDENADAS Y VOLUMEN DE CAÍDA

### Coordenadas Clave por Ranura ($s \in [0, 7]$)
* **Origen de Sala:** $X = 5120, Y = 64, Z = s \cdot 128$
* **Punto de Entrada (Inicio de la Sala):**
  $$X = 5189.947, \quad Y = 95.0, \quad Z = 90.780 + s \cdot 128, \quad \text{Yaw} = 180^\circ$$
* **Tarima de Sabrina:**
  $$X = 5161.603, \quad Y = 71.0, \quad Z = 52.524 + s \cdot 128, \quad \text{Yaw} = 180^\circ$$

### Volumen del Foso de Caída (Fail Zone)
Cualquier jugador situado dentro del prisma delimitado por:
* $X_{\min} = 5182.564, \quad X_{\max} = 5200.490$
* $Z_{\min} = 153.509 + s \cdot 128, \quad Z_{\max} = 221.492 + s \cdot 128$
* $Y \le 77.0$

Es considerado como caído del parkour de forma inmediata.

---

## 3. PROTOCOLO DE REINICIO POR CAÍDA

Cuando un retador entra en la Fail Zone:
1. **Verificación de Ranura:** Se comprueba `Ranuras.asignacionDe(jugador.getUuid())`. Si el jugador no pertenece al gimnasio de Sabrina en esa ranura, el evento se ignora.
2. **Guarda de Combate:** Si el retador ya se encuentra combatiendo con Sabrina (`RCTMod.getInstance().isInBattle(jugador)`), el reinicio **NO** se aplica.
3. **Debounce Temporal:** Se aplica un retardo mínimo de 1500 ms (`DEBOUNCE_MS`) entre detecciones para un mismo retador.
4. **Registro de Incidencia:** Se invoca `GymChallengeService.registrarFallo(uuid, "sabrina")`.
5. **Efectos Visuales y Auditivos en Caída:**
   * Partículas `PORTAL` (20 partículas) en el punto donde se produjo la caída.
   * Sonido `ENTITY_ENDERMAN_TELEPORT` con volumen 1.0 y tono 1.0.
6. **Teletransporte Seguro:**
   * Carga de chunk en destino: `mundo.getChunk(BlockPos.ofFloored(destino))`.
   * Teletransporte a `Gimnasio.entrada("sabrina", s)` con yaw $180^\circ$.
   * Reseteo absoluto de inercia: `jugador.setVelocity(Vec3d.ZERO); jugador.velocityModified = true;`.
   * Anulación de daño acumulado: `jugador.fallDistance = 0.0f;`.
7. **Efectos de Llegada:**
   * Partículas `WITCH` (15 partículas) en el suelo de la plataforma de entrada.
   * Mensaje en barra de acción (Actionbar):
     > `§c§l¡Te has caído del parkour! §r§7Volviendo al inicio...`

---

## 4. VALIDACIÓN DE LLEGADA A LA CÁMARA DE SABRINA

* En `GymChallengeService.tick`:
  * Se monitorea la distancia del retador a la plataforma de Sabrina:
    $$\text{distancia}^2 \le 144.0 \quad (12 \text{ bloques}), \quad |\Delta Y| \le 4.0$$
  * Al ingresar en dicho radio, se declara `completarRuta(uuid, "sabrina")` y se notifica al jugador.
* En `Combate.empezar`:
  * Si el jugador intenta hablar con Sabrina antes de haber completado la ruta:
    > `§c§lGIMNASIO §8» §7Debes superar el reto de la sala antes de poder retar al líder.`
  * La batalla queda completamente bloqueada hasta que el parkour haya sido superado limpiamente.
