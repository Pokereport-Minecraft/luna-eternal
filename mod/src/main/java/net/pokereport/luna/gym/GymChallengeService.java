package net.pokereport.luna.gym;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.pokereport.luna.world.LunaDimensions;
import net.pokereport.luna.world.TravelService;

/**
 * Servicio centralizado de seguimiento de desafíos de gimnasio, estado por jugador,
 * progresión de parkours/rutas, validación de acceso al líder y salida de emergencia.
 */
public final class GymChallengeService {

    public enum State {
        IN_PROGRESS,
        ROUTE_COMPLETED,
        IN_BATTLE,
        COMPLETED,
        FAILED
    }

    public record PlayerChallenge(
            UUID playerUuid,
            String gymId,
            int slot,
            long startedAt,
            boolean routeCompleted,
            int checkpoint,
            int failCount,
            State state
    ) {
        public PlayerChallenge withRouteCompleted(boolean completed) {
            return new PlayerChallenge(playerUuid, gymId, slot, startedAt, completed, checkpoint, failCount,
                    completed ? State.ROUTE_COMPLETED : state);
        }

        public PlayerChallenge withFail() {
            return new PlayerChallenge(playerUuid, gymId, slot, startedAt, false, checkpoint, failCount + 1, State.IN_PROGRESS);
        }

        public PlayerChallenge withState(State newState) {
            return new PlayerChallenge(playerUuid, gymId, slot, startedAt, routeCompleted, checkpoint, failCount, newState);
        }
    }

    private static final Map<UUID, PlayerChallenge> CHALLENGES = new ConcurrentHashMap<>();

    private GymChallengeService() {}

    public static void iniciarReto(ServerPlayerEntity player, Gimnasio.Gimnasio_ gym, int slot) {
        if (player == null || gym == null) return;
        boolean requiresRoute = "sabrina".equalsIgnoreCase(gym.id());
        CHALLENGES.put(player.getUuid(), new PlayerChallenge(
                player.getUuid(),
                gym.id(),
                slot,
                System.currentTimeMillis(),
                !requiresRoute,
                0,
                0,
                !requiresRoute ? State.ROUTE_COMPLETED : State.IN_PROGRESS
        ));
    }

    public static boolean esRutaCompletada(UUID playerUuid, String gymId) {
        if (playerUuid == null || gymId == null) return false;
        if (!"sabrina".equalsIgnoreCase(gymId)) {
            return true;
        }
        PlayerChallenge ch = CHALLENGES.get(playerUuid);
        if (ch == null) return false;
        return ch.gymId().equalsIgnoreCase(gymId) && ch.routeCompleted();
    }

    public static void completarRuta(UUID playerUuid, String gymId) {
        if (playerUuid == null || gymId == null) return;
        CHALLENGES.computeIfPresent(playerUuid, (k, old) -> {
            if (old.gymId().equalsIgnoreCase(gymId)) {
                return old.withRouteCompleted(true);
            }
            return old;
        });
    }

    public static void registrarFallo(UUID playerUuid, String gymId) {
        if (playerUuid == null || gymId == null) return;
        CHALLENGES.computeIfPresent(playerUuid, (k, old) -> {
            if (old.gymId().equalsIgnoreCase(gymId)) {
                return old.withFail();
            }
            return old;
        });
    }

    public static void finalizarReto(UUID playerUuid) {
        if (playerUuid != null) {
            CHALLENGES.remove(playerUuid);
            SabrinaParkourService.limpiar(playerUuid);
        }
    }

    public static void alSalir(UUID playerUuid) {
        finalizarReto(playerUuid);
    }

    public static PlayerChallenge obtenerEstado(UUID playerUuid) {
        if (playerUuid == null) return null;
        return CHALLENGES.get(playerUuid);
    }

    public static boolean abandonarReto(ServerPlayerEntity player) {
        if (player == null) return false;
        UUID uuid = player.getUuid();
        Ranuras.soltar(uuid);
        finalizarReto(uuid);
        TravelService.travel(player, LunaDimensions.CIUDADELA, "la Ciudadela");
        player.sendMessage(Text.literal("§6§lGIMNASIO §8» §7Has abandonado el reto del gimnasio de forma segura."), false);
        return true;
    }

    public static void resetAll() {
        CHALLENGES.clear();
    }

    public static void tick(MinecraftServer server) {
        if (server == null || server.getTicks() % 4 != 0) return;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (player.isSpectator()) continue;
            boolean isStaff = GymChallengePolicy.isStaffExempt(player);
            boolean isCreative = player.isCreative();

            if (!isCreative && !isStaff && GymChallengePolicy.isChallengeActive(player)) {
                // Bloqueo estricto de vuelo en supervivencia durante el reto
                if (player.getAbilities().flying) {
                    player.getAbilities().flying = false;
                    player.sendAbilitiesUpdate();
                }
                player.getAbilities().allowFlying = false;

                // Bloqueo estricto de monturas
                if (player.hasVehicle()) {
                    player.stopRiding();
                    player.sendMessage(Text.literal("§c§lGIMNASIO §8» §7No está permitido usar monturas dentro del gimnasio."), true);
                }
            }

            // Detección de llegada a la cámara del líder en Sabrina
            PlayerChallenge ch = CHALLENGES.get(player.getUuid());
            if (ch != null && "sabrina".equalsIgnoreCase(ch.gymId()) && !ch.routeCompleted()) {
                Gimnasio.Gimnasio_ gSabrina = Gimnasio.de("sabrina");
                if (gSabrina != null) {
                    Vec3d liderPos = Gimnasio.lider(gSabrina, ch.slot());
                    if (player.squaredDistanceTo(liderPos) <= 144.0 && Math.abs(player.getY() - liderPos.y) <= 4.0) {
                        completarRuta(player.getUuid(), "sabrina");
                        player.sendMessage(Text.literal("§a§l¡PARKOUR SUPERADO! §r§7Has alcanzado la cámara de Sabrina. ¡Habla con ella para combatir!"), false);
                    }
                }
            }
        }
    }
}
