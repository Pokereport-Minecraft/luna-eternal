package net.pokereport.luna.spectate;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import net.pokereport.luna.LunaEternal;
import net.pokereport.luna.world.StaffRoles;
import net.pokereport.luna.world.WorldProtectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio de modo espectador sigiloso (Stealth Spectate) para Moderación y Staff.
 * Garantiza:
 * - Invisibilidad y sin colisiones / recogida de objetos.
 * - Seguimiento cross-dimension inmediato.
 * - Registro persistente en staff_spectate_log.
 * - Restauración completa del estado (posición, mundo, modo de juego) al salir o desconectarse.
 */
public final class StaffSpectateService {

    public record SessionState(
            UUID staffUuid,
            String staffName,
            GameMode originalGameMode,
            RegistryKey<World> originalWorld,
            Vec3d originalPos,
            float originalYaw,
            float originalPitch,
            UUID targetUuid,
            String targetName,
            long startedAt
    ) {}

    private static final Map<UUID, SessionState> SESSIONS = new ConcurrentHashMap<>();
    private static final Map<UUID, SessionState> DISCONNECTED_STAFF = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> STAFF_MODE_ACTIVE = new ConcurrentHashMap<>();
    private static volatile boolean initialized = false;

    private StaffSpectateService() {}

    public static void registrar() {
        if (initialized) return;
        initialized = true;

        // Seguimiento entre dimensiones
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
            onTargetChangeWorld(player, destination);
        });

        // Desconexión de jugadores (staff o target)
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            if (handler.player != null) {
                onPlayerDisconnect(handler.player);
            }
        });

        // Reconexión de staff para restauración de estado si se desconectó espectando
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (handler.player != null) {
                onPlayerJoin(handler.player);
            }
        });
    }

    public static boolean isAuthorized(ServerPlayerEntity player) {
        if (player == null) return false;
        if (player.hasPermissionLevel(2)) return true;
        if (StaffRoles.display(player.getUuid()) != null) return true;
        return WorldProtectionManager.hasAdministrativeEditAccess(player);
    }

    public static boolean isSpectating(UUID staffUuid) {
        return SESSIONS.containsKey(staffUuid);
    }

    public static SessionState getSession(UUID staffUuid) {
        return SESSIONS.get(staffUuid);
    }

    public static Map<UUID, SessionState> getActiveSessions() {
        return Collections.unmodifiableMap(SESSIONS);
    }

    public static boolean isStaffMode(UUID uuid) {
        return STAFF_MODE_ACTIVE.getOrDefault(uuid, false);
    }

    public static boolean toggleStaffMode(ServerPlayerEntity staff) {
        if (!isAuthorized(staff)) return false;
        boolean nowActive = !isStaffMode(staff.getUuid());
        STAFF_MODE_ACTIVE.put(staff.getUuid(), nowActive);

        if (nowActive) {
            staff.getAbilities().allowFlying = true;
            staff.getAbilities().invulnerable = true;
            staff.sendAbilitiesUpdate();
            staff.sendMessage(Text.literal("§6[Staff] §aModo Staff activado (vuelo e invulnerabilidad habilitados)."), false);
        } else {
            if (staff.interactionManager.getGameMode() == GameMode.SURVIVAL) {
                staff.getAbilities().allowFlying = false;
                staff.getAbilities().flying = false;
                staff.getAbilities().invulnerable = false;
                staff.sendAbilitiesUpdate();
            }
            staff.sendMessage(Text.literal("§6[Staff] §cModo Staff desactivado."), false);
        }
        return nowActive;
    }

    public static boolean startSpectate(ServerPlayerEntity staff, ServerPlayerEntity target, String reason) {
        if (staff == null || target == null) return false;
        if (!isAuthorized(staff)) {
            staff.sendMessage(Text.literal("§cNo tienes autorización para usar el modo espectador de staff."), false);
            return false;
        }
        if (staff.getUuid().equals(target.getUuid())) {
            staff.sendMessage(Text.literal("§cNo puedes espectarte a ti mismo."), false);
            return false;
        }

        // Si ya estaba en una sesión previa, restaurar primero
        if (SESSIONS.containsKey(staff.getUuid())) {
            stopSpectate(staff, false);
        }

        SessionState state = new SessionState(
                staff.getUuid(),
                staff.getGameProfile().getName(),
                staff.interactionManager.getGameMode(),
                staff.getServerWorld().getRegistryKey(),
                staff.getPos(),
                staff.getYaw(),
                staff.getPitch(),
                target.getUuid(),
                target.getGameProfile().getName(),
                System.currentTimeMillis()
        );
        SESSIONS.put(staff.getUuid(), state);

        // Cambiar a modo espectador
        staff.changeGameMode(GameMode.SPECTATOR);

        // Teletransportar al mundo y posición del objetivo
        ServerWorld targetWorld = target.getServerWorld();
        if (staff.getServerWorld() != targetWorld) {
            staff.teleport(targetWorld, target.getX(), target.getY(), target.getZ(), target.getYaw(), target.getPitch());
        } else {
            staff.requestTeleport(target.getX(), target.getY(), target.getZ());
        }

        // Anclar la cámara al jugador objetivo
        staff.setCameraEntity(target);

        // Registro de auditoría asíncrono
        final String logReason = (reason == null || reason.isBlank()) ? "Auditoría / Vigilancia" : reason.trim();
        LunaEternal.submit(() -> {
            logSpectateStart(state.staffUuid(), state.staffName(), state.targetUuid(), state.targetName(), state.startedAt(), logReason);
        });

        staff.sendMessage(Text.literal("§6[Staff Spectate] §aObservando a §e" + target.getGameProfile().getName()
                + " §aen modo sigiloso. Usa §e/staffspectate off §apara volver."), false);
        return true;
    }

    public static boolean stopSpectate(ServerPlayerEntity staff) {
        return stopSpectate(staff, true);
    }

    public static boolean stopSpectate(ServerPlayerEntity staff, boolean notify) {
        if (staff == null) return false;
        SessionState state = SESSIONS.remove(staff.getUuid());
        if (state == null) {
            if (notify) staff.sendMessage(Text.literal("§cNo estás en modo espectador de staff."), false);
            return false;
        }

        // Desvincular cámara
        staff.setCameraEntity(staff);

        // Restaurar mundo y posición original
        ServerWorld origWorld = staff.getServer().getWorld(state.originalWorld());
        if (origWorld != null) {
            staff.teleport(origWorld, state.originalPos().x, state.originalPos().y, state.originalPos().z,
                    state.originalYaw(), state.originalPitch());
        }

        // Restaurar modo de juego
        staff.changeGameMode(state.originalGameMode());

        // Actualizar registro de auditoría en base de datos
        final long endedAt = System.currentTimeMillis();
        LunaEternal.submit(() -> {
            logSpectateEnd(state.staffUuid(), endedAt);
        });

        if (notify) {
            staff.sendMessage(Text.literal("§6[Staff Spectate] §aModo espectador finalizado. Posición y modo restaurados."), false);
        }
        return true;
    }

    private static void onTargetChangeWorld(ServerPlayerEntity target, ServerWorld destination) {
        for (Map.Entry<UUID, SessionState> entry : SESSIONS.entrySet()) {
            SessionState session = entry.getValue();
            if (session.targetUuid().equals(target.getUuid())) {
                ServerPlayerEntity staff = target.getServer().getPlayerManager().getPlayer(session.staffUuid());
                if (staff != null && staff.isAlive()) {
                    staff.teleport(destination, target.getX(), target.getY(), target.getZ(), target.getYaw(), target.getPitch());
                    staff.setCameraEntity(target);
                    staff.sendMessage(Text.literal("§7[Staff] El objetivo cambió de dimensión a "
                            + destination.getRegistryKey().getValue().getPath() + "."), false);
                }
            }
        }
    }

    private static void onPlayerDisconnect(ServerPlayerEntity player) {
        // Caso 1: El staff se desconecta mientras espectaba
        SessionState state = SESSIONS.remove(player.getUuid());
        if (state != null) {
            DISCONNECTED_STAFF.put(player.getUuid(), state);
            long endedAt = System.currentTimeMillis();
            LunaEternal.submit(() -> logSpectateEnd(state.staffUuid(), endedAt));
        }

        // Caso 2: El objetivo observado se desconecta
        for (Map.Entry<UUID, SessionState> entry : SESSIONS.entrySet()) {
            if (entry.getValue().targetUuid().equals(player.getUuid())) {
                ServerPlayerEntity staff = player.getServer().getPlayerManager().getPlayer(entry.getKey());
                if (staff != null) {
                    staff.setCameraEntity(staff);
                    staff.sendMessage(Text.literal("§c[Staff] El jugador " + player.getGameProfile().getName()
                            + " se ha desconectado. Usa /staffspectate off para volver a tu posición."), false);
                }
            }
        }
    }

    private static void onPlayerJoin(ServerPlayerEntity player) {
        // Restaurar al staff si se cayó durante un spectate
        SessionState state = DISCONNECTED_STAFF.remove(player.getUuid());
        if (state != null) {
            ServerWorld origWorld = player.getServer().getWorld(state.originalWorld());
            if (origWorld != null) {
                player.teleport(origWorld, state.originalPos().x, state.originalPos().y, state.originalPos().z,
                        state.originalYaw(), state.originalPitch());
            }
            player.changeGameMode(state.originalGameMode());
            player.sendMessage(Text.literal("§6[Staff] §eSe restauró tu ubicación y modo de juego previos al espectador."), false);
        }
    }

    private static void logSpectateStart(UUID staffUuid, String staffName, UUID targetUuid, String targetName, long startedAt, String reason) {
        String sql = "INSERT INTO staff_spectate_log (staff_uuid, staff_name, target_uuid, target_name, started_at, reason) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = LunaEternal.database().connection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, staffUuid.toString());
            ps.setString(2, staffName);
            ps.setString(3, targetUuid.toString());
            ps.setString(4, targetName);
            ps.setLong(5, startedAt);
            ps.setString(6, reason);
            ps.executeUpdate();
        } catch (Exception e) {
            LunaEternal.LOG.error("Error al registrar inicio de staff_spectate_log", e);
        }
    }

    private static void logSpectateEnd(UUID staffUuid, long endedAt) {
        String sql = "UPDATE staff_spectate_log SET ended_at = ? WHERE staff_uuid = ? AND ended_at IS NULL ORDER BY id DESC LIMIT 1";
        try (Connection c = LunaEternal.database().connection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, endedAt);
            ps.setString(2, staffUuid.toString());
            ps.executeUpdate();
        } catch (Exception e) {
            LunaEternal.LOG.error("Error al registrar fin de staff_spectate_log", e);
        }
    }
}
