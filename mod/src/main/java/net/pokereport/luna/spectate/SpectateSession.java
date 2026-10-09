package net.pokereport.luna.spectate;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.UUID;

/**
 * Representa el estado de una sesión activa de espectador de streaming.
 */
public final class SpectateSession {

    private final UUID spectatorUuid;
    private final String spectatorName;

    // Estado original del jugador antes de entrar al modo espectador
    private final GameMode originalGameMode;
    private final RegistryKey<World> originalWorldKey;
    private final Vec3d originalPos;
    private final float originalYaw;
    private final float originalPitch;

    // Estado dinámico de la sesión
    private volatile SpectateMode baseMode;
    private volatile SpectateMode mode;
    private volatile UUID currentTargetUuid;
    private volatile String currentTargetName;
    private volatile RegistryKey<World> currentTargetWorldKey;

    // Objetivo manual preferido para posible retorno
    private volatile UUID preferredTargetUuid;
    private volatile String preferredTargetName;

    // Historial para navegación previa
    private final Deque<UUID> targetHistory = new ArrayDeque<>();

    // Tiempos
    private final long sessionStartMs;
    private volatile long currentTargetStartMs;

    // Sobrescritura de rotación por sesión (-1 = usar configuración global)
    private volatile int rotationMinutesOverride = -1;

    // Control de espera al entrar un jugador
    private volatile UUID pendingJoinPlayerUuid;
    private volatile int pendingJoinDelayTicks = 0;

    public SpectateSession(
            UUID spectatorUuid,
            String spectatorName,
            GameMode originalGameMode,
            RegistryKey<World> originalWorldKey,
            Vec3d originalPos,
            float originalYaw,
            float originalPitch,
            SpectateMode initialMode
    ) {
        this.spectatorUuid = Objects.requireNonNull(spectatorUuid, "spectatorUuid");
        this.spectatorName = spectatorName != null ? spectatorName : spectatorUuid.toString();
        this.originalGameMode = originalGameMode != null ? originalGameMode : GameMode.SURVIVAL;
        this.originalWorldKey = originalWorldKey;
        this.originalPos = originalPos != null ? originalPos : Vec3d.ZERO;
        this.originalYaw = originalYaw;
        this.originalPitch = originalPitch;
        this.baseMode = initialMode != null ? initialMode : SpectateMode.AUTO;
        this.mode = this.baseMode;
        this.sessionStartMs = System.currentTimeMillis();
        this.currentTargetStartMs = this.sessionStartMs;
    }

    public UUID getSpectatorUuid() {
        return spectatorUuid;
    }

    public String getSpectatorName() {
        return spectatorName;
    }

    public GameMode getOriginalGameMode() {
        return originalGameMode;
    }

    public RegistryKey<World> getOriginalWorldKey() {
        return originalWorldKey;
    }

    public Vec3d getOriginalPos() {
        return originalPos;
    }

    public float getOriginalYaw() {
        return originalYaw;
    }

    public float getOriginalPitch() {
        return originalPitch;
    }

    public SpectateMode getMode() {
        return mode;
    }

    public void setMode(SpectateMode mode) {
        this.mode = mode;
    }

    public SpectateMode getBaseMode() {
        return baseMode;
    }

    public void setBaseMode(SpectateMode baseMode) {
        this.baseMode = baseMode;
    }

    public UUID getPreferredTargetUuid() {
        return preferredTargetUuid;
    }

    public String getPreferredTargetName() {
        return preferredTargetName;
    }

    public void setPreferredTarget(UUID uuid, String name) {
        this.preferredTargetUuid = uuid;
        this.preferredTargetName = name;
    }

    public UUID getCurrentTargetUuid() {
        return currentTargetUuid;
    }

    public String getCurrentTargetName() {
        return currentTargetName;
    }

    public RegistryKey<World> getCurrentTargetWorldKey() {
        return currentTargetWorldKey;
    }

    public void setCurrentTargetWorldKey(RegistryKey<World> worldKey) {
        this.currentTargetWorldKey = worldKey;
    }

    public synchronized void setTarget(UUID targetUuid, String targetName, RegistryKey<World> worldKey) {
        if (this.currentTargetUuid != null && !this.currentTargetUuid.equals(targetUuid)) {
            // Guardar en historial (máximo 20 elementos)
            if (targetHistory.size() >= 20) {
                targetHistory.removeLast();
            }
            targetHistory.push(this.currentTargetUuid);
        }
        this.currentTargetUuid = targetUuid;
        this.currentTargetName = targetName;
        this.currentTargetWorldKey = worldKey;
        this.currentTargetStartMs = System.currentTimeMillis();
        this.pendingJoinPlayerUuid = null;
        this.pendingJoinDelayTicks = 0;
    }

    public synchronized UUID popPreviousTarget() {
        return targetHistory.pollFirst();
    }

    public synchronized void clearTarget() {
        if (this.currentTargetUuid != null) {
            if (targetHistory.size() >= 20) {
                targetHistory.removeLast();
            }
            targetHistory.push(this.currentTargetUuid);
        }
        this.currentTargetUuid = null;
        this.currentTargetName = null;
        this.currentTargetWorldKey = null;
        this.currentTargetStartMs = System.currentTimeMillis();
    }

    public long getSessionStartMs() {
        return sessionStartMs;
    }

    public long getCurrentTargetStartMs() {
        return currentTargetStartMs;
    }

    public int getRotationMinutesOverride() {
        return rotationMinutesOverride;
    }

    public void setRotationMinutesOverride(int rotationMinutesOverride) {
        this.rotationMinutesOverride = rotationMinutesOverride;
    }

    public int getEffectiveRotationMinutes(SpectateFollowConfig config) {
        if (rotationMinutesOverride >= 0) {
            return rotationMinutesOverride;
        }
        return config.getRotationMinutes();
    }

    public UUID getPendingJoinPlayerUuid() {
        return pendingJoinPlayerUuid;
    }

    public int getPendingJoinDelayTicks() {
        return pendingJoinDelayTicks;
    }

    public void setPendingJoin(UUID playerUuid, int delayTicks) {
        this.pendingJoinPlayerUuid = playerUuid;
        this.pendingJoinDelayTicks = delayTicks;
    }

    public void clearPendingJoin() {
        this.pendingJoinPlayerUuid = null;
        this.pendingJoinDelayTicks = 0;
    }

    public int decrementPendingJoinTicks() {
        if (this.pendingJoinDelayTicks > 0) {
            this.pendingJoinDelayTicks--;
        }
        return this.pendingJoinDelayTicks;
    }
}
