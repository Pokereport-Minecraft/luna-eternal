package net.pokereport.luna.gym;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.gitlab.srcmc.rctmod.api.RCTMod;

import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.pokereport.luna.world.LunaDimensions;

/**
 * Servicio de detección y reinicio por caída en el parkour de Sabrina (Gimnasio Psíquico).
 *
 * <p>Delimitación exacta de la zona de caída por ranura:
 * <ul>
 *   <li>X: [5180.479, 5200.551]</li>
 *   <li>Z: [20.597 + r*128, 93.691 + r*128]</li>
 *   <li>Y: <= 80.5</li>
 * </ul>
 *
 * <p>Reglas estrictas de ejecución:
 * <ul>
 *   <li>Sin reserva, infiere la ranura física; esto no reserva ni concede progreso.</li>
 *   <li>NO interfiere si el jugador ya está en combate con Sabrina ({@code RCTMod.isInBattle}).</li>
 *   <li>Teletransporta de vuelta al inicio de la sala ({@code Gimnasio.entrada}), anula la distancia de caída y velocidad.</li>
 *   <li>Incluye debounce de 1500 ms para evitar bucles o falsos disparos sucesivos.</li>
 * </ul>
 */
public final class SabrinaParkourService {

    private static final double MIN_X = 5180.479;
    private static final double MAX_X = 5200.551;
    private static final double REL_MIN_Z = 20.597;
    private static final double REL_MAX_Z = 93.691;
    private static final double MAX_Y = 80.5;

    private static final long DEBOUNCE_MS = 1500L;
    private static final Map<UUID, Long> ULTIMA_CAIDA = new ConcurrentHashMap<>();

    private SabrinaParkourService() {}

    public static int calcularRanuraDesdeZ(double z) {
        if (!Double.isFinite(z)) return -1;
        for (int r = 0; r < Gimnasio.RANURAS; r++) {
            double offset = r * Gimnasio.PASO_RANURA;
            if (z >= REL_MIN_Z + offset && z <= REL_MAX_Z + offset) return r;
        }
        return -1;
    }

    public static boolean enFoso(double x, double y, double z, int ranura) {
        double offset = ranura * Gimnasio.PASO_RANURA;
        return ranura >= 0 && ranura < Gimnasio.RANURAS
                && x >= MIN_X && x <= MAX_X && y <= MAX_Y
                && z >= REL_MIN_Z + offset && z <= REL_MAX_Z + offset;
    }

    public static void tick(MinecraftServer server) {
        if (server.getTicks() % 2 != 0) {
            return;
        }

        ServerWorld mundo = server.getWorld(LunaDimensions.GIMNASIOS);
        if (mundo == null || mundo.getPlayers().isEmpty()) {
            return;
        }

        Gimnasio.Gimnasio_ gSabrina = Gimnasio.de("sabrina");
        if (gSabrina == null) {
            return;
        }

        long ahora = System.currentTimeMillis();

        for (ServerPlayerEntity jugador : mundo.getPlayers()) {
            if (jugador.isSpectator() || jugador.isCreative() || !jugador.isAlive()) {
                continue;
            }

            Ranuras.Asignacion asig = Ranuras.asignacionDe(jugador.getUuid());
            if (asig != null && !"sabrina".equalsIgnoreCase(asig.gymId())) {
                continue;
            }

            // Si ya está en combate con la líder, no reiniciar por parkour
            if (RCTMod.getInstance().isInBattle(jugador)) {
                continue;
            }

            int ranura = asig == null ? calcularRanuraDesdeZ(jugador.getZ()) : asig.ranura();
            double px = jugador.getX();
            double py = jugador.getY();
            double pz = jugador.getZ();

            if (enFoso(px, py, pz, ranura)) {
                Long ultima = ULTIMA_CAIDA.get(jugador.getUuid());
                if (ultima != null && (ahora - ultima) < DEBOUNCE_MS) {
                    continue;
                }
                ULTIMA_CAIDA.put(jugador.getUuid(), ahora);

                reiniciarParkour(jugador, mundo, gSabrina, ranura);
            }
        }
    }

    private static void reiniciarParkour(ServerPlayerEntity jugador, ServerWorld mundo,
                                        Gimnasio.Gimnasio_ g, int ranura) {
        GymChallengeService.registrarFallo(jugador.getUuid(), "sabrina");
        Vec3d destino = new Vec3d(5189.95, 95.0, 90.78 + ranura * Gimnasio.PASO_RANURA);
        float yaw = 180.0f;

        // Efecto de partículas de desvanecimiento en el punto de caída
        mundo.spawnParticles(jugador, ParticleTypes.PORTAL, true,
                jugador.getX(), jugador.getY() + 1.0, jugador.getZ(),
                20, 0.4, 0.6, 0.4, 0.1);

        // Cargar chunk de destino y teletransportar sin sobreescribir Regreso
        mundo.getChunk(BlockPos.ofFloored(destino));
        jugador.teleport(mundo, destino.x, destino.y, destino.z, Set.of(), yaw, 0.0f);

        // Cancelar inercia y daño por caída acumulado
        jugador.setVelocity(Vec3d.ZERO);
        jugador.velocityModified = true;
        jugador.fallDistance = 0.0f;

        // Sonido y feedback en barra de acción
        mundo.playSound(null, destino.x, destino.y, destino.z,
                SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.0f);
        jugador.sendMessage(Text.literal("§c§l¡Te has caído del parkour! §r§7Volviendo al inicio..."), true);

        // Partículas en el punto de llegada
        mundo.spawnParticles(jugador, ParticleTypes.WITCH, true,
                destino.x, destino.y + 0.5, destino.z,
                15, 0.3, 0.5, 0.3, 0.05);
    }

    public static void limpiar(UUID jugador) {
        ULTIMA_CAIDA.remove(jugador);
    }
}
