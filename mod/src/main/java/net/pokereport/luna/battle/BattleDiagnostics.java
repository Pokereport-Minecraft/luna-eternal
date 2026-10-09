package net.pokereport.luna.battle;

import net.pokereport.luna.LunaEternal;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Servicio de diagnóstico y telemetría end-to-end para combates de entrenadores y líderes de gimnasio.
 *
 * Traza de extremo a extremo:
 * PLAYER INTERACTION -> TRAINER ENTITY -> RCT INTERACTION -> TRAINER CONFIG ->
 * TEAM GENERATION -> ADAPTIVE TEAM -> POKÉMON CONVERSION -> BATTLE ACTORS ->
 * BATTLE REGISTRY -> SHOWDOWN -> REQUEST INSTRUCTION -> ACTION SELECT ->
 * TURN 1 -> TURN LOOP -> FAINT -> DOUBLE FAINT -> BATTLE END
 */
public final class BattleDiagnostics {

    private static boolean enabled = true;
    private static final AtomicLong SEQUENCE = new AtomicLong(1);
    private static final int MAX_HISTORY = 200;

    public record AttemptRecord(
            String battleAttemptId,
            long startTimestamp,
            UUID playerUuid,
            String trainerId,
            UUID trainerEntityUuid,
            String dimension,
            String trainerProfile,
            String teamSource,
            boolean adaptiveEnabled,
            List<String> generatedTeam,
            List<String> validationResults,
            boolean battleActorCreated,
            boolean battleRegistered,
            boolean showdownStarted,
            boolean firstRequestReceived,
            boolean firstChoiceMade,
            int currentTurn,
            long lastActionTimestamp,
            String failureStage,
            String rootCause,
            List<String> logTrace,
            boolean completedSuccessfully
    ) {}

    public static final class Builder {
        private final String battleAttemptId;
        private final long startTimestamp = System.currentTimeMillis();
        private final UUID playerUuid;
        private final String trainerId;
        private final UUID trainerEntityUuid;
        private final String dimension;

        private String trainerProfile = "UNKNOWN";
        private String teamSource = "DEFAULT";
        private boolean adaptiveEnabled = false;
        private final List<String> generatedTeam = new ArrayList<>();
        private final List<String> validationResults = new ArrayList<>();
        private boolean battleActorCreated = false;
        private boolean battleRegistered = false;
        private boolean showdownStarted = false;
        private boolean firstRequestReceived = false;
        private boolean firstChoiceMade = false;
        private int currentTurn = 0;
        private long lastActionTimestamp = System.currentTimeMillis();
        private String failureStage = null;
        private String rootCause = null;
        private final List<String> logTrace = new ArrayList<>();
        private boolean completedSuccessfully = false;

        public Builder(UUID playerUuid, String trainerId, UUID trainerEntityUuid, String dimension) {
            this.battleAttemptId = "btl-" + System.currentTimeMillis() + "-" + SEQUENCE.getAndIncrement();
            this.playerUuid = playerUuid;
            this.trainerId = trainerId;
            this.trainerEntityUuid = trainerEntityUuid;
            this.dimension = dimension;
        }

        public String getBattleAttemptId() { return battleAttemptId; }

        public Builder trainerProfile(String profile) { this.trainerProfile = profile; return this; }
        public Builder teamSource(String src) { this.teamSource = src; return this; }
        public Builder adaptiveEnabled(boolean val) { this.adaptiveEnabled = val; return this; }

        public Builder addPokemon(String species) {
            this.generatedTeam.add(species);
            return this;
        }

        public Builder addValidation(String val) {
            this.validationResults.add(val);
            return this;
        }

        public Builder markActorCreated() { this.battleActorCreated = true; return this; }
        public Builder markBattleRegistered() { this.battleRegistered = true; return this; }
        public Builder markShowdownStarted() { this.showdownStarted = true; return this; }
        public Builder markFirstRequest() { this.firstRequestReceived = true; return this; }
        public Builder markFirstChoice() { this.firstChoiceMade = true; return this; }

        public Builder setTurn(int turn) {
            this.currentTurn = turn;
            this.lastActionTimestamp = System.currentTimeMillis();
            return this;
        }

        public Builder trace(String msg) {
            long delta = System.currentTimeMillis() - startTimestamp;
            this.logTrace.add("+" + delta + "ms: " + msg);
            this.lastActionTimestamp = System.currentTimeMillis();
            return this;
        }

        public Builder fail(String stage, String cause) {
            this.failureStage = stage;
            this.rootCause = cause;
            trace("FAILURE at stage [" + stage + "]: " + cause);
            return this;
        }

        public Builder success() {
            this.completedSuccessfully = true;
            trace("COMPLETED successfully");
            return this;
        }

        public AttemptRecord build() {
            return new AttemptRecord(
                    battleAttemptId, startTimestamp, playerUuid, trainerId, trainerEntityUuid,
                    dimension, trainerProfile, teamSource, adaptiveEnabled,
                    List.copyOf(generatedTeam), List.copyOf(validationResults),
                    battleActorCreated, battleRegistered, showdownStarted,
                    firstRequestReceived, firstChoiceMade, currentTurn,
                    lastActionTimestamp, failureStage, rootCause, List.copyOf(logTrace),
                    completedSuccessfully
            );
        }
    }

    private static final Map<String, Builder> ACTIVE_ATTEMPTS = new ConcurrentHashMap<>();
    private static final Map<UUID, String> PLAYER_ACTIVE_ATTEMPTS = new ConcurrentHashMap<>();
    private static final ConcurrentLinkedDeque<AttemptRecord> HISTORY = new ConcurrentLinkedDeque<>();

    private BattleDiagnostics() {}

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static Builder startAttempt(UUID playerUuid, String trainerId, UUID trainerEntityUuid, String dimension) {
        Builder b = new Builder(playerUuid, trainerId, trainerEntityUuid, dimension);
        if (!enabled) return b;

        b.trace("START battle attempt for player=" + playerUuid + " trainer=" + trainerId + " dim=" + dimension);
        ACTIVE_ATTEMPTS.put(b.getBattleAttemptId(), b);
        if (playerUuid != null) {
            PLAYER_ACTIVE_ATTEMPTS.put(playerUuid, b.getBattleAttemptId());
        }
        LunaEternal.LOG.info("[BATTLE-DIAG] Initialized attempt {} (player={}, trainer={})",
                b.getBattleAttemptId(), playerUuid, trainerId);
        return b;
    }

    public static Builder getActive(String battleAttemptId) {
        if (battleAttemptId == null) return null;
        return ACTIVE_ATTEMPTS.get(battleAttemptId);
    }

    public static Builder getActiveForPlayer(UUID playerUuid) {
        if (playerUuid == null) return null;
        String id = PLAYER_ACTIVE_ATTEMPTS.get(playerUuid);
        return id != null ? ACTIVE_ATTEMPTS.get(id) : null;
    }

    public static void recordStep(String battleAttemptId, String step) {
        if (!enabled || battleAttemptId == null) return;
        Builder b = ACTIVE_ATTEMPTS.get(battleAttemptId);
        if (b != null) {
            b.trace(step);
        }
    }

    public static void recordTurn(String battleAttemptId, int turn) {
        if (!enabled || battleAttemptId == null) return;
        Builder b = ACTIVE_ATTEMPTS.get(battleAttemptId);
        if (b != null) {
            b.setTurn(turn);
            b.trace("Turn advanced to " + turn);
        }
    }

    public static void recordFailure(String battleAttemptId, String stage, String cause, Throwable t) {
        if (battleAttemptId == null) return;
        Builder b = ACTIVE_ATTEMPTS.remove(battleAttemptId);
        if (b != null) {
            b.fail(stage, cause + (t != null ? " (" + t.getMessage() + ")" : ""));
            AttemptRecord rec = b.build();
            archive(rec);
            PLAYER_ACTIVE_ATTEMPTS.remove(rec.playerUuid());
            LunaEternal.LOG.error("[BATTLE-DIAG] Battle attempt {} FAILED at stage {}: {} (rootCause={})",
                    battleAttemptId, stage, cause, rec.rootCause());
        }
    }

    public static void recordSuccess(String battleAttemptId) {
        if (battleAttemptId == null) return;
        Builder b = ACTIVE_ATTEMPTS.remove(battleAttemptId);
        if (b != null) {
            b.success();
            AttemptRecord rec = b.build();
            archive(rec);
            PLAYER_ACTIVE_ATTEMPTS.remove(rec.playerUuid());
            LunaEternal.LOG.info("[BATTLE-DIAG] Battle attempt {} COMPLETED successfully", battleAttemptId);
        }
    }

    private static void archive(AttemptRecord rec) {
        HISTORY.addLast(rec);
        while (HISTORY.size() > MAX_HISTORY) {
            HISTORY.pollFirst();
        }
    }

    public static List<AttemptRecord> getRecentHistory() {
        return List.copyOf(HISTORY);
    }

    public static int getActiveCount() {
        return ACTIVE_ATTEMPTS.size();
    }
}
