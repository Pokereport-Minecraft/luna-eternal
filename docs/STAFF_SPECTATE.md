# POKEREPORT NETWORK — SISTEMA STAFF STEALTH SPECTATE

**Fecha:** 08/10/2026  
**Módulos:** `net.pokereport.luna.spectate.StaffSpectateService`, `net.pokereport.luna.command.StaffSpectateCommand`, `net.pokereport.luna.world.StaffSurvivalPolicy`  
**Objetivo:** Supervisión invisible, silenciosa y no detectable de jugadores por parte del equipo de moderación.

---

## 1. ESPECIFICACIÓN DEL MODO SIGILO (STEALTH)

El comando `/staffspectate <jugador>` permite a administradores y moderadores monitorear en tiempo real a cualquier usuario sin alertarlo por canales de chat, efectos visuales o física del juego.

### Características Clave:
1. **Invisibilidad Real y Sin Colisiones:**
   - Modo de juego configurado a `GameMode.SPECTATOR`.
   - Efecto de invisibilidad sin partículas vanilla aplicadas.
   - Colisiones con entidades y bloques desactivadas.
2. **Silenciamiento Completo y No Recogida de Ítems:**
   - La entidad staff no emite sonidos de pasos, caídas ni golpes.
   - Bloqueo total de la absorción de ítems en el suelo (`noPickup`), impidiendo vaciar granjas de los jugadores o alterar inventarios accidentalmente.
3. **Seguimiento Interdimensional Automático:**
   - Registrado en el evento `ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD`.
   - Si el jugador objetivo viaja a otra dimensión (p. ej. de `minecraft:overworld` a `lunaeternal:salvaje` o `lunaeternal:ciudadela`), el staff en sesión activa es teletransportado automáticamente a la nueva dimensión junto al objetivo.
4. **Restauración Atómica de Estado:**
   - Al salir con `/staffspectate off` o desconectarse, el sistema restaura:
     - Coordenadas de origen anteriores a la sesión.
     - Dimensión previa.
     - Modo de juego anterior (`SURVIVAL` / `CREATIVE`).
     - Inventario y efectos de pociones intactos.
5. **Auditoría Persistente en Base de Datos:**
   - Cada inicio y cierre de sesión de spectate se registra en la tabla `staff_spectate_log` (migración `V060`):
     - `staff_uuid`, `target_uuid`, `start_time`, `end_time`, `start_pos`, `end_pos`.

---

## 2. COMANDOS DISPONIBLES

| Comando | Permiso | Descripción |
| :--- | :--- | :--- |
| `/staffspectate <jugador>` | Nivel 2 (Staff) | Inicia el modo de vigilancia espectador sobre el jugador indicado. |
| `/staffspectate off` | Nivel 2 (Staff) | Finaliza la vigilancia y regresa al punto de origen previo. |
| `/staffmode` | Nivel 2 (Staff) | Alterna el modo de servicio administrativo general con invisibilidad. |

---

## 3. AUDITORÍA DE SUPERVIVENCIA STAFF (`StaffSurvivalPolicy.java`)

Se implementó la política de supervivencia para garantizar que:
- Los miembros del staff en modo de juego supervivencia normal no utilicen privilegios indebidos en economías de juego (granjas, drops de bosses).
- Al entrar en modo spectate, se audite que no se transmitan ventajas a terceras cuentas.

---

## 4. TESTS Y VALIDACIÓN

- `StaffSpectateTest.java`: Verifica el cambio de gamemode, el seguimiento interdimensional y la restauración de coordenadas.
- `StaffSurvivalPolicyTest.java`: Verifica las restricciones de inventario y políticas de juego limpio.
- **Resultado:** Aprobado en la suite de 420/420 tests.
