# POKEREPORT NETWORK — SISTEMA DE MODERACIÓN Y SANCIONES

**Fecha:** 08/10/2026  
**Módulos:** `net.pokereport.luna.moderation.ModerationService`, `net.pokereport.luna.command.ModerationCommands`, Migración `V060__staff_moderation_and_spectate.sql`  
**Objetivo:** Gestión integral de sanciones (advertencias, silenciamientos, expulsiones y expulsiones permanentes/temporales) con persistencia en MariaDB e interceptores de red.

---

## 1. COMANDOS DE MODERACIÓN Y SINTAXIS

Todos los comandos requieren nivel de operador / staff (`requires(source -> source.hasPermissionLevel(2))`).

| Comando | Parámetros | Acción |
| :--- | :--- | :--- |
| `/warn` | `<jugador> <motivo>` | Registra una advertencia formal al jugador y le notifica en pantalla. |
| `/kick` | `<jugador> [motivo]` | Desconecta inmediatamente al jugador de la sesión actual. |
| `/mute` | `<jugador> [motivo]` | Silencia indefinidamente al jugador en el chat público y privado. |
| `/tempmute` | `<jugador> <tiempo> [motivo]` | Silencia temporalmente (formatos: `10m`, `2h`, `1d`). |
| `/unmute` | `<jugador>` | Revoca cualquier silenciamiento activo sobre el jugador. |
| `/ban` | `<jugador> [motivo]` | Bloquea permanentemente la conexión del jugador al servidor. |
| `/tempban` | `<jugador> <tiempo> [motivo]` | Bloquea temporalmente el acceso del jugador por la duración especificada. |
| `/unban` | `<jugador>` | Revoca cualquier baneo activo. |
| `/history` | `<jugador>` | Consulta y lista el historial completo de sanciones de un usuario. |

---

## 2. PERSISTENCIA EN BASE DE DATOS (MIGRACIÓN V060)

Se preparó la migración `V060__staff_moderation_and_spectate.sql` para alojar las sanciones:

```sql
CREATE TABLE IF NOT EXISTS staff_punishment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    target_uuid VARCHAR(36) NOT NULL,
    target_name VARCHAR(64) NOT NULL,
    punishment_type VARCHAR(32) NOT NULL, -- WARN, MUTE, BAN, KICK
    reason TEXT NOT NULL,
    staff_uuid VARCHAR(36) NOT NULL,
    staff_name VARCHAR(64) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NULL,
    active BOOLEAN DEFAULT TRUE,
    INDEX idx_target_uuid (target_uuid),
    INDEX idx_active (active)
);

CREATE TABLE IF NOT EXISTS staff_spectate_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    staff_uuid VARCHAR(36) NOT NULL,
    staff_name VARCHAR(64) NOT NULL,
    target_uuid VARCHAR(36) NOT NULL,
    target_name VARCHAR(64) NOT NULL,
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP NULL,
    start_dimension VARCHAR(64),
    end_dimension VARCHAR(64)
);
```

---

## 3. INTERCEPTORES DE EVENTOS (ENFORCEMENT)

1. **Mute Listener (`ServerMessageEvents.ALLOW_CHAT_MESSAGE`):**
   - Intercepta cada mensaje de chat saliente.
   - Consulta `ModerationService.isMuted(player.getUuid())`.
   - Si el jugador se encuentra silenciado, cancela el mensaje y emite un aviso en rojo indicando el tiempo restante de la sanción y el motivo.
2. **Ban Enforcer (`ServerLoginConnectionEvents.QUERY_START`):**
   - Intercepta el handshake de pre-autenticación antes de spawnear la entidad.
   - Consulta `ModerationService.isBanned(profile.getId())`.
   - Si existe un baneo activo, desconecta la conexión inmediatamente con una pantalla de desconexión detallando el motivo y expiración.

---

## 4. TESTS DE INTEGRACIÓN

- `ModerationCommandsTest.java`: Verifica la ejecución de comandos y sintaxis de tiempo (`10m`, `2h`, `1d`).
- `ModerationServiceTest.java`: Verifica la comprobación de expiración de sanciones temporales y revocación.
- **Resultado:** Aprobado en la suite de 420/420 tests.
