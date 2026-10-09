# POKEREPORT NETWORK — POLÍTICA DE SEGURIDAD Y ANTI-BYPASS EN GIMNASIOS

**Fecha de corte:** 08/10/2026  
**Documento:** Política Centralizada de Retos de Gimnasio  
**Clases Clave:** `GymChallengePolicy`, `GymChallengeService`, `GymArenaProteccion`, `LobbyNetworkGuardMixin`

---

## 1. OBJETIVO DE LA POLÍTICA

Garantizar la integridad deportiva de los 23 gimnasios y retos de la red, impidiendo que los jugadores utilicen atajos, comandos de teletransporte externo, monturas voladoras o terrestres, objetos de evasión física (perlas de ender, frutas del coro) o modificación de bloques para eludir las mecánicas diseñadas.

---

## 2. FILTRADO DE COMANDOS DURANTE EL RETO

Cuando un jugador se encuentra dentro de una instancia de gimnasio o posee una ranura activa asignada (`GymChallengePolicy.isChallengeActive`), su ejecutor de comandos queda regulado por una lista blanca estricta con filtrado proactivo.

### Comandos Bloqueados Expresamente
* **Hogares y spawn:** `/home`, `/homes`, `/sethome`, `/delhome`, `/spawn`, `/hogar`, `/salvaje`.
* **Teletransporte de regreso:** `/back`.
* **Peticiones entre jugadores:** `/tpa`, `/tpahere`, `/tpaccept`, `/tpdeny`.
* **Teletransporte aleatorio y zonas:** `/rtp`, `/wild`.
* **Teletransportes públicos y privados:** `/warp`, `/warps`, `/pwarp`, `/pwarps`.
* **Bases de clan:** `/clan home`, `/clan sethome`.

### Comandos Permitidos
* **Abandono seguro de la instancia:** `/gym leave`, `/gym salir`, `/gimnasio leave`, `/gimnasio salir`.
* **Mensajería directa:** `/msg`, `/tell`, `/w`, `/r`.
* **Canales de chat:** `/c` (clan), `/g` (global), `/l` (local), `/canal`, `/channel`, `/tr` (comercio), `/sc` (staff).
* **Asistencia e información:** `/help`, `/ayuda`, `/ticket`, `/rules`, `/ping`, `/suicide`.

Cualquier comando no contemplado que intente escapar de la instancia es interceptado en `LobbyNetworkGuardMixin` con el mensaje:
> `§c§lGIMNASIO §8» §7No puedes usar ese comando durante el reto. Usa §e/gym leave§7 para salir.`

---

## 3. RESTRICCIONES DE OBJETOS E ÍTEMS DE TELETRANSPORTE

Durante el reto y en todas las dimensiones de gimnasio, los siguientes objetos están completamente inhabilitados mediante `UseItemCallback` y `UseBlockCallback` en `GymArenaProteccion`:
* **Perlas de Ender (`minecraft:ender_pearl`):** Se cancela el lanzamiento en el aire o contra bloques para evitar teletransporte por encima de fosos o a través de barreras.
* **Frutas del Coro (`minecraft:chorus_fruit`):** Se bloquea el consumo para evitar teletransporte aleatorio hacia zonas cerradas del esquema.

---

## 4. RESTRICCIONES DE MOVIMIENTO Y MECÁNICAS DE VUELO

En cada tick del servidor (`GymChallengeService.tick`):
1. **Bloqueo de Vuelo:**
   * En supervivencia y aventura, `player.getAbilities().allowFlying` se fuerza a `false`.
   * Si el jugador se encontraba volando (`flying = true`), se cancela inmediatamente y se envía `sendAbilitiesUpdate()`.
2. **Bloqueo de Monturas:**
   * No se permite montar Pokémon, barcos, vagonetas ni caballos dentro de las instancias.
   * Si el jugador se monta en una entidad, se ejecuta `player.stopRiding()` y se emite notificación en la barra de acción:
     > `§c§lGIMNASIO §8» §7No está permitido usar monturas dentro del gimnasio.`

---

## 5. COMANDO DE SALIDA DE EMERGENCIA: `/gym leave`

Si un jugador queda atascado o desea retirarse de un gimnasio sin penalización:
1. Ejecuta `/gym leave` o `/gym salir`.
2. Se liberan sus ranuras reservadas (`Ranuras.soltar(uuid)`).
3. Se limpia su estado de reto (`GymChallengeService.finalizarReto(uuid)`).
4. Se teletransporta de regreso a la Ciudadela mediante `TravelService.travel`.
5. Se muestra confirmación en chat:
   > `§6§lGIMNASIO §8» §7Has abandonado el reto del gimnasio de forma segura.`
