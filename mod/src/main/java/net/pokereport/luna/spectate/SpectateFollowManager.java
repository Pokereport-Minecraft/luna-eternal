package net.pokereport.luna.spectate;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import net.pokereport.luna.LunaEternal;
import net.pokereport.luna.world.StaffRoles;
import net.pokereport.luna.world.WorldProtectionManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Gestor del sistema de cámara de espectador automático para streaming 24/7 (/spectatefollow).
 */
public final class SpectateFollowManager {

    private static final SpectateFollowConfig CONFIG = new SpectateFollowConfig();
    private static final Map<UUID, SpectateSession> SESSIONS = new ConcurrentHashMap<>();
    private static volatile boolean initialized = false;

    private SpectateFollowManager() {}

    public static SpectateFollowConfig getConfig() {
        return CONFIG;
    }

    public static Map<UUID, SpectateSession> getSessions() {
        return Collections.unmodifiableMap(SESSIONS);
    }

    public static SpectateSession getSession(UUID spectatorUuid) {
        return SESSIONS.get(spectatorUuid);
    }

    /**
     * Registra los eventos de Fabric para el ciclo de vida del espectador.
     */
    public static synchronized void registrar() {
        if (initialized) return;
        initialized = true;

        CONFIG.load();

        // 1. Tick del servidor para reconciliación y rotación
        ServerTickEvents.END_SERVER_TICK.register(SpectateFollowManager::onServerTick);

        // 2. Cambio de dimensión del objetivo (cross-dimension inmediato)
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
            onPlayerChangeWorld(player, destination);
        });

        // 3. Reaparición del objetivo tras morir
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            onPlayerRespawn(oldPlayer, newPlayer);
        });

        // 4. Desconexión de jugadores (espectador o target)
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            if (handler.player != null) {
                onPlayerDisconnect(handler.player, server);
            }
        });

        // 5. Conexión de jugadores (recuperación desde WAITING_FOR_PLAYER)
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (handler.player != null) {
                onPlayerJoin(handler.player, server);
            }
        });

        LunaEternal.LOG.info("[SPECTATE-FOLLOW] Sistema de cámara de streaming registrado correctamente.");
    }

    // =========================================================================
    // COMANDOS / ACCIONES DEL USUARIO
    // =========================================================================

    public static void startManual(ServerPlayerEntity spectator, ServerPlayerEntity target) {
        if (spectator == null || target == null) return;
        if (spectator.getUuid().equals(target.getUuid())) {
            spectator.sendMessage(Text.literal("§cNo puedes seguirte a ti mismo como cámara."), false);
            return;
        }

        SpectateSession session = SESSIONS.computeIfAbsent(spectator.getUuid(), uuid ->
                new SpectateSession(
                        spectator.getUuid(),
                        spectator.getGameProfile().getName(),
                        spectator.interactionManager.getGameMode(),
                        spectator.getServerWorld().getRegistryKey(),
                        spectator.getPos(),
                        spectator.getYaw(),
                        spectator.getPitch(),
                        SpectateMode.MANUAL
                )
        );

        session.setBaseMode(SpectateMode.MANUAL);
        session.setMode(SpectateMode.MANUAL);
        session.setPreferredTarget(target.getUuid(), target.getGameProfile().getName());
        attachCamera(spectator, target, session);
        spectator.sendMessage(Text.literal("§a[Cámara] Siguiendo a §e" + target.getGameProfile().getName() + " §a(modo manual)."), false);
    }

    public static void startAuto(ServerPlayerEntity spectator) {
        if (spectator == null) return;

        SpectateSession session = SESSIONS.computeIfAbsent(spectator.getUuid(), uuid ->
                new SpectateSession(
                        spectator.getUuid(),
                        spectator.getGameProfile().getName(),
                        spectator.interactionManager.getGameMode(),
                        spectator.getServerWorld().getRegistryKey(),
                        spectator.getPos(),
                        spectator.getYaw(),
                        spectator.getPitch(),
                        SpectateMode.AUTO
                )
        );

        session.setBaseMode(SpectateMode.AUTO);
        session.setMode(SpectateMode.AUTO);
        session.setPreferredTarget(null, null);

        ServerPlayerEntity candidate = findCandidate(spectator.getServer(), spectator.getUuid(), null);
        if (candidate != null) {
            attachCamera(spectator, candidate, session);
            spectator.sendMessage(Text.literal("§a[Cámara] Modo automático activado. Siguiendo a §e" + candidate.getGameProfile().getName() + "§a."), false);
        } else {
            session.clearTarget();
            session.setMode(SpectateMode.WAITING_FOR_PLAYER);
            if (spectator.interactionManager.getGameMode() != GameMode.SPECTATOR) {
                spectator.changeGameMode(GameMode.SPECTATOR);
            }
            spectator.sendMessage(Text.literal("§e[Cámara] Modo automático activado. Esperando jugadores disponibles en línea..."), false);
        }
    }

    public static void nextTarget(ServerPlayerEntity spectator) {
        if (spectator == null) return;

        SpectateSession session = SESSIONS.get(spectator.getUuid());
        if (session == null) {
            startAuto(spectator);
            return;
        }

        ServerPlayerEntity candidate = findCandidate(spectator.getServer(), spectator.getUuid(), session.getCurrentTargetUuid());
        if (candidate != null) {
            attachCamera(spectator, candidate, session);
            spectator.sendMessage(Text.literal("§a[Cámara] Siguiente objetivo: §e" + candidate.getGameProfile().getName() + "§a."), false);
        } else {
            session.clearTarget();
            session.setMode(SpectateMode.WAITING_FOR_PLAYER);
            spectator.sendMessage(Text.literal("§e[Cámara] No hay más objetivos disponibles; en espera de jugadores..."), false);
        }
    }

    public static void previousTarget(ServerPlayerEntity spectator) {
        if (spectator == null) return;

        SpectateSession session = SESSIONS.get(spectator.getUuid());
        if (session == null) {
            spectator.sendMessage(Text.literal("§cNo tienes una sesión de cámara activa."), false);
            return;
        }

        MinecraftServer server = spectator.getServer();
        if (server == null) return;

        UUID prevUuid;
        ServerPlayerEntity prevPlayer = null;
        while ((prevUuid = session.popPreviousTarget()) != null) {
            ServerPlayerEntity candidate = server.getPlayerManager().getPlayer(prevUuid);
            if (candidate != null && isPlayerEligible(candidate, spectator.getUuid())) {
                prevPlayer = candidate;
                break;
            }
        }

        if (prevPlayer != null) {
            attachCamera(spectator, prevPlayer, session);
            spectator.sendMessage(Text.literal("§a[Cámara] Regresando al objetivo anterior: §e" + prevPlayer.getGameProfile().getName() + "§a."), false);
        } else {
            spectator.sendMessage(Text.literal("§c[Cámara] No hay objetivos anteriores válidos en el historial."), false);
        }
    }

    public static void stopSpectating(ServerPlayerEntity spectator) {
        if (spectator == null) return;

        SpectateSession session = SESSIONS.remove(spectator.getUuid());
        if (session == null) {
            spectator.sendMessage(Text.literal("§cNo tienes una sesión de cámara activa."), false);
            return;
        }

        session.setMode(SpectateMode.OFF);
        session.clearTarget();
        session.clearPendingJoin();

        // 1. Restaurar entidad de cámara al propio jugador
        spectator.setCameraEntity(spectator);

        // 2. Restaurar mundo y posición originales
        MinecraftServer server = spectator.getServer();
        if (server != null && session.getOriginalWorldKey() != null) {
            ServerWorld originalWorld = server.getWorld(session.getOriginalWorldKey());
            if (originalWorld != null) {
                Vec3d pos = session.getOriginalPos();
                spectator.teleport(
                        originalWorld,
                        pos.x, pos.y, pos.z,
                        Set.of(),
                        session.getOriginalYaw(),
                        session.getOriginalPitch()
                );
            }
        }

        // 3. Restaurar modo de juego original
        spectator.changeGameMode(session.getOriginalGameMode());

        spectator.sendMessage(Text.literal("§e[Cámara] Modo espectador desactivado. Modo de juego y posición restaurados."), false);
    }

    public static void setRotation(ServerPlayerEntity spectator, int minutes) {
        if (spectator == null) return;

        SpectateSession session = SESSIONS.get(spectator.getUuid());
        if (session == null) {
            spectator.sendMessage(Text.literal("§cNo tienes una sesión de cámara activa."), false);
            return;
        }

        session.setRotationMinutesOverride(minutes);
        if (minutes > 0) {
            spectator.sendMessage(Text.literal("§a[Cámara] Rotación automática configurada a §e" + minutes + " §aminuto(s)."), false);
        } else {
            spectator.sendMessage(Text.literal("§e[Cámara] Rotación automática desactivada para esta sesión."), false);
        }
    }

    public static Text getStatusText(ServerPlayerEntity spectator) {
        SpectateSession session = SESSIONS.get(spectator.getUuid());
        if (session == null) {
            return Text.literal("§7No hay ninguna sesión de cámara de streaming activa para ti.");
        }

        long now = System.currentTimeMillis();
        long totalSeconds = (now - session.getSessionStartMs()) / 1000L;
        long targetSeconds = (now - session.getCurrentTargetStartMs()) / 1000L;

        int rotMin = session.getEffectiveRotationMinutes(CONFIG);
        String rotText;
        if (rotMin > 0 && session.getMode() == SpectateMode.AUTO) {
            long remainingSec = Math.max(0, (rotMin * 60L) - targetSeconds);
            rotText = String.format("%d min (restan %02d:%02d)", rotMin, remainingSec / 60, remainingSec % 60);
        } else {
            rotText = "Desactivada";
        }

        String targetStr = session.getCurrentTargetName() != null
                ? session.getCurrentTargetName() + " (" + session.getCurrentTargetUuid() + ")"
                : (session.getMode() == SpectateMode.WAITING_FOR_PLAYER ? "Ninguno (En espera)" : "Ninguno");

        String worldStr = session.getCurrentTargetWorldKey() != null
                ? session.getCurrentTargetWorldKey().getValue().toString()
                : spectator.getServerWorld().getRegistryKey().getValue().toString();

        Vec3d pos = spectator.getPos();

        return Text.literal(
                "§6=== Estado de Cámara de Streaming ===\n" +
                "§eModo: §f" + session.getMode().name() + "\n" +
                "§eObjetivo: §f" + targetStr + "\n" +
                "§eDimensión actual: §f" + worldStr + "\n" +
                "§eCoordenadas cámara: §f[" + (int)pos.x + ", " + (int)pos.y + ", " + (int)pos.z + "]\n" +
                "§eRotación: §f" + rotText + "\n" +
                "§eTiempo en objetivo: §f" + formatDuration(targetSeconds) + "\n" +
                "§eDuración de sesión: §f" + formatDuration(totalSeconds) + "\n" +
                "§eCámaras activas en total: §f" + SESSIONS.size() + "\n" +
                "§eJugadores excluidos en blacklist: §f" + CONFIG.getExcluded().size()
        );
    }

    private static String formatDuration(long seconds) {
        long m = seconds / 60L;
        long s = seconds % 60L;
        long h = m / 60L;
        m = m % 60L;
        if (h > 0) {
            return String.format("%02d:%02d:%02d", h, m, s);
        }
        return String.format("%02d:%02d", m, s);
    }

    // =========================================================================
    // SELECCIÓN Y VALIDACIÓN DE JUGADORES
    // =========================================================================

    public static boolean isPlayerEligible(ServerPlayerEntity player, UUID currentSpectatorUuid) {
        if (player == null || player.isRemoved() || player.isDisconnected()) {
            return false;
        }

        UUID uuid = player.getUuid();

        // 1. Nunca seguir al propio espectador
        if (uuid.equals(currentSpectatorUuid)) {
            return false;
        }

        // 2. Nunca seguir a otra cámara de espectador activa
        if (SESSIONS.containsKey(uuid)) {
            return false;
        }

        // 3. Verificar lista de exclusión (por UUID o nombre)
        String name = player.getGameProfile().getName();
        if (CONFIG.isExcluded(uuid, name)) {
            return false;
        }

        // 4. Exclusión de Staff por defecto si includeStaff es false
        if (!CONFIG.isIncludeStaff() && isStaff(player)) {
            return false;
        }

        return true;
    }

    public static boolean isStaff(ServerPlayerEntity player) {
        if (player == null) return false;
        UUID uuid = player.getUuid();
        if (StaffRoles.display(uuid) != null) return true;
        if (player.hasPermissionLevel(2)) return true;
        return WorldProtectionManager.hasAdministrativeEditAccess(player);
    }

    public record CandidateInfo(
            UUID uuid,
            String name,
            boolean isStaff,
            boolean isRemovedOrDisconnected
    ) {}

    public static UUID selectCandidateFromList(
            List<CandidateInfo> candidates,
            UUID spectatorUuid,
            UUID avoidUuid,
            Map<UUID, SpectateSession> sessions,
            SpectateFollowConfig config
    ) {
        if (candidates == null || candidates.isEmpty()) return null;

        List<CandidateInfo> eligible = candidates.stream()
                .filter(c -> c != null && !c.isRemovedOrDisconnected())
                .filter(c -> !c.uuid().equals(spectatorUuid))
                .filter(c -> !sessions.containsKey(c.uuid()))
                .filter(c -> !config.isExcluded(c.uuid(), c.name()))
                .filter(c -> config.isIncludeStaff() || !c.isStaff())
                .toList();

        if (eligible.isEmpty()) return null;

        Set<UUID> targetsOfOtherSessions = sessions.values().stream()
                .filter(s -> !s.getSpectatorUuid().equals(spectatorUuid))
                .map(SpectateSession::getCurrentTargetUuid)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Prioridad 1: No seguido por otra cámara Y distinto de avoidUuid
        for (CandidateInfo c : eligible) {
            if (!targetsOfOtherSessions.contains(c.uuid()) && (avoidUuid == null || !c.uuid().equals(avoidUuid))) {
                return c.uuid();
            }
        }

        // Prioridad 2: No seguido por otra cámara (aunque sea avoidUuid)
        for (CandidateInfo c : eligible) {
            if (!targetsOfOtherSessions.contains(c.uuid())) {
                return c.uuid();
            }
        }

        // Prioridad 3: Distinto de avoidUuid
        for (CandidateInfo c : eligible) {
            if (avoidUuid == null || !c.uuid().equals(avoidUuid)) {
                return c.uuid();
            }
        }

        // Prioridad 4: Cualquier elegible
        return eligible.get(0).uuid();
    }

    public static ServerPlayerEntity findCandidate(
            MinecraftServer server,
            UUID spectatorUuid,
            UUID avoidUuid
    ) {
        if (server == null) return null;

        List<ServerPlayerEntity> online = server.getPlayerManager().getPlayerList();
        List<CandidateInfo> candidates = online.stream()
                .map(p -> new CandidateInfo(
                        p.getUuid(),
                        p.getGameProfile().getName(),
                        isStaff(p),
                        p.isRemoved() || p.isDisconnected()
                ))
                .toList();

        UUID selected = selectCandidateFromList(candidates, spectatorUuid, avoidUuid, SESSIONS, CONFIG);
        return selected != null ? server.getPlayerManager().getPlayer(selected) : null;
    }

    public static void registerSession(SpectateSession session) {
        if (session != null) {
            SESSIONS.put(session.getSpectatorUuid(), session);
        }
    }

    public static void clearSessions() {
        SESSIONS.clear();
    }

    public static void processTargetDisconnect(
            SpectateSession session,
            UUID disconnectingUuid,
            List<CandidateInfo> candidates
    ) {
        if (session == null) return;
        if (disconnectingUuid.equals(session.getCurrentTargetUuid())) {
            if (session.getMode() == SpectateMode.AUTO || CONFIG.isAutoSwitchOnDisconnect()) {
                UUID nextUuid = selectCandidateFromList(candidates, session.getSpectatorUuid(), disconnectingUuid, SESSIONS, CONFIG);
                if (nextUuid != null) {
                    CandidateInfo next = candidates.stream().filter(c -> c.uuid().equals(nextUuid)).findFirst().orElse(null);
                    session.setTarget(nextUuid, next != null ? next.name() : nextUuid.toString(), session.getCurrentTargetWorldKey());
                    session.setMode(session.getBaseMode());
                } else {
                    session.clearTarget();
                    session.setMode(SpectateMode.WAITING_FOR_PLAYER);
                }
            } else {
                session.clearTarget();
                session.setMode(SpectateMode.WAITING_FOR_PLAYER);
            }
        }
    }

    public static boolean checkRotationDue(
            SpectateSession session,
            boolean inBattle,
            long currentTimeMs
    ) {
        if (session == null || session.getMode() != SpectateMode.AUTO) return false;
        int rotMin = session.getEffectiveRotationMinutes(CONFIG);
        if (rotMin <= 0) return false;

        long elapsed = currentTimeMs - session.getCurrentTargetStartMs();
        if (elapsed >= rotMin * 60L * 1000L) {
            if (inBattle) {
                // Postponer por combate Pokémon
                return false;
            }
            return true;
        }
        return false;
    }

    // =========================================================================
    // VINCULACIÓN Y TELETRANSPORTE DE CÁMARA
    // =========================================================================

    public static void attachCamera(ServerPlayerEntity spectator, ServerPlayerEntity target, SpectateSession session) {
        if (spectator == null || target == null || session == null) return;
        if (target.isRemoved() || target.isDisconnected()) return;

        // 1. Asegurar modo espectador
        if (spectator.interactionManager.getGameMode() != GameMode.SPECTATOR) {
            spectator.changeGameMode(GameMode.SPECTATOR);
        }

        // 2. Mover a la dimensión y posición del objetivo
        ServerWorld targetWorld = target.getServerWorld();
        spectator.teleport(
                targetWorld,
                target.getX(), target.getY(), target.getZ(),
                Set.of(),
                target.getYaw(), target.getPitch()
        );

        // 3. Fijar cámara vanilla en el objetivo
        spectator.setCameraEntity(target);

        // 4. Actualizar sesión
        session.setTarget(target.getUuid(), target.getGameProfile().getName(), targetWorld.getRegistryKey());
    }

    public static void syncCamera(ServerPlayerEntity spectator, ServerPlayerEntity target, SpectateSession session) {
        if (spectator == null || target == null || session == null) return;
        if (target.isRemoved() || target.isDisconnected()) return;

        boolean worldMismatch = spectator.getServerWorld() != target.getServerWorld();
        boolean cameraDetached = spectator.getCameraEntity() != target;
        double distSq = spectator.squaredDistanceTo(target);

        if (worldMismatch || cameraDetached || distSq > 2500.0) {
            ServerWorld targetWorld = target.getServerWorld();
            spectator.teleport(
                    targetWorld,
                    target.getX(), target.getY(), target.getZ(),
                    Set.of(),
                    target.getYaw(), target.getPitch()
            );
            spectator.setCameraEntity(target);
            session.setCurrentTargetWorldKey(targetWorld.getRegistryKey());
        }
    }

    public static boolean isInBattle(ServerPlayerEntity player) {
        if (player == null) return false;
        try {
            return com.cobblemon.mod.common.Cobblemon.INSTANCE
                    .getBattleRegistry()
                    .getBattleByParticipatingPlayer(player) != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    // =========================================================================
    // CICLO DE VIDA / EVENT LISTENERS
    // =========================================================================

    public static void onServerTick(MinecraftServer server) {
        if (SESSIONS.isEmpty() || server == null) return;

        boolean checkSync = (server.getTicks() % 10 == 0);

        for (SpectateSession session : SESSIONS.values()) {
            ServerPlayerEntity spectator = server.getPlayerManager().getPlayer(session.getSpectatorUuid());
            if (spectator == null || spectator.isRemoved() || spectator.isDisconnected()) {
                // Espectador desconectado
                SESSIONS.remove(session.getSpectatorUuid());
                continue;
            }

            // Manejo de entrada con retraso (join delay)
            if ((session.getMode() == SpectateMode.WAITING_FOR_PLAYER || CONFIG.isReturnToPreferredTarget())
                    && session.getPendingJoinPlayerUuid() != null) {
                if (session.decrementPendingJoinTicks() <= 0) {
                    UUID pendingUuid = session.getPendingJoinPlayerUuid();
                    session.clearPendingJoin();

                    ServerPlayerEntity joinedPlayer = server.getPlayerManager().getPlayer(pendingUuid);
                    if (joinedPlayer != null && isPlayerEligible(joinedPlayer, spectator.getUuid())) {
                        session.setMode(session.getBaseMode());
                        attachCamera(spectator, joinedPlayer, session);
                        spectator.sendMessage(Text.literal("§a[Cámara] Jugador disponible detectado: siguiendo a §e" + joinedPlayer.getGameProfile().getName() + "§a."), false);
                    } else {
                        // Reintentar buscar cualquier candidato libre si estábamos en espera
                        if (session.getMode() == SpectateMode.WAITING_FOR_PLAYER) {
                            ServerPlayerEntity fallback = findCandidate(server, spectator.getUuid(), null);
                            if (fallback != null) {
                                session.setMode(session.getBaseMode());
                                attachCamera(spectator, fallback, session);
                                spectator.sendMessage(Text.literal("§a[Cámara] Siguiendo a §e" + fallback.getGameProfile().getName() + "§a."), false);
                            }
                        }
                    }
                }
                continue;
            }

            // Si está en espera sin pending player, verificar cada 10 ticks si ya hay alguien elegible
            if (session.getMode() == SpectateMode.WAITING_FOR_PLAYER) {
                if (checkSync) {
                    ServerPlayerEntity candidate = findCandidate(server, spectator.getUuid(), null);
                    if (candidate != null) {
                        session.setMode(session.getBaseMode());
                        attachCamera(spectator, candidate, session);
                        spectator.sendMessage(Text.literal("§a[Cámara] Jugador disponible detectado: siguiendo a §e" + candidate.getGameProfile().getName() + "§a."), false);
                    }
                }
                continue;
            }

            // Sesiones con objetivo activo (MANUAL o AUTO)
            UUID targetUuid = session.getCurrentTargetUuid();
            if (targetUuid == null) {
                session.setMode(SpectateMode.WAITING_FOR_PLAYER);
                continue;
            }

            ServerPlayerEntity target = server.getPlayerManager().getPlayer(targetUuid);
            if (target == null || target.isRemoved() || target.isDisconnected() || !isPlayerEligible(target, spectator.getUuid())) {
                // El objetivo ya no está disponible
                handleTargetUnavailable(session, spectator, server);
                continue;
            }

            // Validación periódica de posición/dimensión/cámara
            if (checkSync) {
                syncCamera(spectator, target, session);

                // Rotación periódica en modo AUTO
                if (session.getMode() == SpectateMode.AUTO) {
                    int rotMin = session.getEffectiveRotationMinutes(CONFIG);
                    if (rotMin > 0) {
                        long elapsed = System.currentTimeMillis() - session.getCurrentTargetStartMs();
                        if (elapsed >= rotMin * 60L * 1000L) {
                            // Si el objetivo está en combate Pokémon, posponer rotación hasta que termine
                            if (isInBattle(target)) {
                                continue;
                            }
                            nextTarget(spectator);
                        }
                    }
                }
            }
        }
    }

    private static void handleTargetUnavailable(SpectateSession session, ServerPlayerEntity spectator, MinecraftServer server) {
        if (session.getMode() == SpectateMode.AUTO || CONFIG.isAutoSwitchOnDisconnect()) {
            ServerPlayerEntity next = findCandidate(server, spectator.getUuid(), session.getCurrentTargetUuid());
            if (next != null) {
                attachCamera(spectator, next, session);
                session.setMode(session.getBaseMode());
                spectator.sendMessage(Text.literal("§e[Cámara] Objetivo no disponible. Cambiando automáticamente a §a" + next.getGameProfile().getName() + "§e."), false);
            } else {
                session.clearTarget();
                session.setMode(SpectateMode.WAITING_FOR_PLAYER);
                spectator.sendMessage(Text.literal("§e[Cámara] Objetivo no disponible. Esperando nuevos jugadores en línea..."), false);
            }
        } else {
            session.clearTarget();
            session.setMode(SpectateMode.WAITING_FOR_PLAYER);
            spectator.sendMessage(Text.literal("§e[Cámara] Objetivo no disponible. En espera..."), false);
        }
    }

    public static void onPlayerChangeWorld(ServerPlayerEntity player, ServerWorld destination) {
        if (player == null || destination == null) return;
        if (player.isRemoved() || player.isDisconnected()) return;
        UUID playerUuid = player.getUuid();

        for (SpectateSession session : SESSIONS.values()) {
            if (playerUuid.equals(session.getCurrentTargetUuid())) {
                MinecraftServer server = destination.getServer();
                ServerPlayerEntity spectator = server.getPlayerManager().getPlayer(session.getSpectatorUuid());
                if (spectator != null && !spectator.isRemoved() && !spectator.isDisconnected()) {
                    spectator.teleport(
                            destination,
                            player.getX(), player.getY(), player.getZ(),
                            Set.of(),
                            player.getYaw(), player.getPitch()
                    );
                    spectator.setCameraEntity(player);
                    session.setCurrentTargetWorldKey(destination.getRegistryKey());
                }
            }
        }
    }

    public static void onPlayerRespawn(ServerPlayerEntity oldPlayer, ServerPlayerEntity newPlayer) {
        if (oldPlayer == null || newPlayer == null) return;
        if (newPlayer.isRemoved() || newPlayer.isDisconnected()) return;
        UUID targetUuid = oldPlayer.getUuid();

        for (SpectateSession session : SESSIONS.values()) {
            if (targetUuid.equals(session.getCurrentTargetUuid())) {
                MinecraftServer server = newPlayer.getServer();
                if (server == null) continue;
                ServerPlayerEntity spectator = server.getPlayerManager().getPlayer(session.getSpectatorUuid());
                if (spectator != null && !spectator.isRemoved() && !spectator.isDisconnected()) {
                    ServerWorld world = newPlayer.getServerWorld();
                    spectator.teleport(
                            world,
                            newPlayer.getX(), newPlayer.getY(), newPlayer.getZ(),
                            Set.of(),
                            newPlayer.getYaw(), newPlayer.getPitch()
                    );
                    spectator.setCameraEntity(newPlayer);
                    session.setCurrentTargetWorldKey(world.getRegistryKey());
                }
            }
        }
    }

    public static void onPlayerDisconnect(ServerPlayerEntity player, MinecraftServer server) {
        if (player == null) return;
        UUID uuid = player.getUuid();

        // 1. Si era un espectador, remover sesión
        SESSIONS.remove(uuid);

        // 2. Si alguna sesión tenía a este jugador pendiente de unirse, cancelar pending
        for (SpectateSession session : SESSIONS.values()) {
            if (uuid.equals(session.getPendingJoinPlayerUuid())) {
                session.clearPendingJoin();
            }
        }

        // 3. Si era el objetivo de alguna sesión activa
        for (SpectateSession session : SESSIONS.values()) {
            if (uuid.equals(session.getCurrentTargetUuid())) {
                ServerPlayerEntity spectator = server.getPlayerManager().getPlayer(session.getSpectatorUuid());
                if (spectator != null) {
                    handleTargetUnavailable(session, spectator, server);
                } else {
                    session.clearTarget();
                    session.setMode(SpectateMode.WAITING_FOR_PLAYER);
                }
            }
        }
    }

    public static void onPlayerJoin(ServerPlayerEntity player, MinecraftServer server) {
        if (player == null || server == null) return;

        for (SpectateSession session : SESSIONS.values()) {
            if (session.getMode() == SpectateMode.WAITING_FOR_PLAYER) {
                if (isPlayerEligible(player, session.getSpectatorUuid())) {
                    // Programar retraso configurado para permitir la carga de chunks del jugador
                    int delayTicks = CONFIG.getJoinDelaySeconds() * 20;
                    session.setPendingJoin(player.getUuid(), delayTicks);
                }
            } else if (CONFIG.isReturnToPreferredTarget()
                    && session.getPreferredTargetUuid() != null
                    && session.getPreferredTargetUuid().equals(player.getUuid())) {
                if (isPlayerEligible(player, session.getSpectatorUuid())) {
                    int delayTicks = CONFIG.getJoinDelaySeconds() * 20;
                    session.setPendingJoin(player.getUuid(), delayTicks);
                }
            }
        }
    }

    public static void onExclusionAdded(String nameOrUuid) {
        for (SpectateSession session : SESSIONS.values()) {
            if (session.getCurrentTargetName() != null && session.getCurrentTargetName().equalsIgnoreCase(nameOrUuid)) {
                // Rotar inmediatamente fuera de este objetivo
                MinecraftServer server = LunaEternal.server();
                if (server != null) {
                    ServerPlayerEntity spectator = server.getPlayerManager().getPlayer(session.getSpectatorUuid());
                    if (spectator != null) {
                        nextTarget(spectator);
                    }
                }
            }
        }
    }
}
