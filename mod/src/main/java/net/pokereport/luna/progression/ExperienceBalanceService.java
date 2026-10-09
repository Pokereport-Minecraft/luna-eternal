package net.pokereport.luna.progression;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.ActorType;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.pokemon.ExperienceGainedEvent;
import com.cobblemon.mod.common.api.pokemon.experience.BattleExperienceSource;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import net.pokereport.luna.LunaEternal;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio de balance de experiencia (P0: Exploit Chansey/Blissey y retornos decrecientes en PvP).
 */
public final class ExperienceBalanceService {

    private ExperienceBalanceService() {}

    private static final Map<String, List<Long>> PVP_MATCH_HISTORY = new ConcurrentHashMap<>();
    private static final Map<String, List<Long>> BLISSEY_FAINT_HISTORY = new ConcurrentHashMap<>();
    private static final long WINDOW_24H_MS = 24 * 60 * 60 * 1000L;
    private static final long WINDOW_5M_MS = 5 * 60 * 1000L;

    public static void registrar() {
        CobblemonEvents.EXPERIENCE_GAINED_EVENT_PRE.subscribe(event -> {
            if (!ProgressionBalanceConfig.isEnabled()) return;
            procesarExperiencia(event);
        });
    }

    public static void procesarExperiencia(ExperienceGainedEvent.Pre event) {
        if (event == null || event.getExperience() <= 0) return;

        var source = event.getSource();
        if (!(source instanceof BattleExperienceSource bSource)) {
            return;
        }

        PokemonBattle battle = bSource.getBattle();
        if (battle == null) return;

        var cfg = ProgressionBalanceConfig.get().experience;

        // 1. COMBATE ENTRE JUGADORES (PvP)
        if (battle.isPvP()) {
            if (!cfg.pvpExperienceEnabled) {
                event.setExperience(0);
                event.cancel();
                return;
            }

            UUID winnerUuid = event.getPokemon().getOwnerUUID();
            UUID opponentUuid = null;
            if (winnerUuid != null) {
                for (BattleActor actor : battle.getActors()) {
                    if (actor.getType() == ActorType.PLAYER) {
                        boolean isWinnerActor = false;
                        for (UUID op : actor.getPlayerUUIDs()) {
                            if (winnerUuid.equals(op)) {
                                isWinnerActor = true;
                                break;
                            }
                        }
                        if (!isWinnerActor) {
                            for (UUID op : actor.getPlayerUUIDs()) {
                                opponentUuid = op;
                                break;
                            }
                        }
                    }
                }
            }

            if (winnerUuid != null && opponentUuid != null) {
                long now = System.currentTimeMillis();
                String pairKey = winnerUuid.compareTo(opponentUuid) < 0
                        ? winnerUuid + "_" + opponentUuid
                        : opponentUuid + "_" + winnerUuid;

                // Detección de abuso específico de Chansey / Blissey
                boolean tieneChanseyOBlissey = false;
                for (BattlePokemon bp : bSource.getFacedPokemon()) {
                    if (bp != null && bp.getOriginalPokemon() != null) {
                        String name = bp.getOriginalPokemon().getSpecies().getName().toLowerCase(Locale.ROOT);
                        if (name.equals("chansey") || name.equals("blissey")) {
                            tieneChanseyOBlissey = true;
                            break;
                        }
                    }
                }

                if (tieneChanseyOBlissey && esBlisseyBoosting(winnerUuid, opponentUuid, now)) {
                    LunaEternal.LOG.warn("[BOOSTING_ALERT] Bloqueado farmeo excesivo de Chansey/Blissey en PvP entre {} y {}",
                            winnerUuid, opponentUuid);
                    event.setExperience(0);
                    event.cancel();
                    return;
                }

                // Retornos decrecientes en PvP contra el mismo rival en 24 horas
                if (cfg.pvpDiminishingReturns) {
                    double factor = calcularFactorPvP(winnerUuid, opponentUuid, now);
                    if (factor <= 0.0) {
                        event.setExperience(0);
                        event.cancel();
                        return;
                    }
                    int adjusted = ajustarExperiencia(event.getExperience(), factor);
                    event.setExperience(adjusted);
                }
            }
            return;
        }

        // 2. COMBATE SALVAJE (PVE)
        boolean esSalvaje = false;
        for (BattleActor actor : battle.getActors()) {
            if (actor.getType() == ActorType.WILD) {
                esSalvaje = true;
                break;
            }
        }

        if (esSalvaje) {
            double wildMult = cfg.wildMultiplier;
            int adjusted = (int) Math.max(1, Math.round(event.getExperience() * wildMult));
            event.setExperience(adjusted);
            return;
        }

        // 3. COMBATE CONTRA ENTRENADOR NPC / GIMNASIO
        boolean esGym = false;
        for (BattleActor actor : battle.getActors()) {
            if (actor.getType() == ActorType.NPC) {
                String name = actor.getName().getString().toLowerCase(Locale.ROOT);
                if (name.contains("lider") || name.contains("gym") || name.contains("brock") || name.contains("misty")) {
                    esGym = true;
                    break;
                }
            }
        }

        double mult = esGym ? cfg.gymMultiplier : cfg.trainerMultiplier;
        if (mult != 1.0) {
            int adjusted = (int) Math.max(1, Math.round(event.getExperience() * mult));
            event.setExperience(adjusted);
        }
    }

    public static double calcularFactorPvP(UUID winnerUuid, UUID opponentUuid, long now) {
        var cfg = ProgressionBalanceConfig.get().experience;
        if (!cfg.pvpDiminishingReturns) return 1.0;
        String pairKey = winnerUuid.compareTo(opponentUuid) < 0
                ? winnerUuid + "_" + opponentUuid
                : opponentUuid + "_" + winnerUuid;
        var matches = PVP_MATCH_HISTORY.computeIfAbsent(pairKey, k -> new ArrayList<>());
        synchronized (matches) {
            matches.removeIf(t -> now - t > WINDOW_24H_MS);
            int count = matches.size();
            double factor;
            if (count == 0) factor = 1.00;
            else if (count == 1) factor = 0.50;
            else if (count == 2) factor = 0.20;
            else factor = 0.00;
            matches.add(now);
            return factor;
        }
    }

    public static boolean esBlisseyBoosting(UUID winnerUuid, UUID opponentUuid, long now) {
        var cfg = ProgressionBalanceConfig.get().experience;
        String pairKey = winnerUuid.compareTo(opponentUuid) < 0
                ? winnerUuid + "_" + opponentUuid
                : opponentUuid + "_" + winnerUuid;
        var faints = BLISSEY_FAINT_HISTORY.computeIfAbsent(pairKey, k -> new ArrayList<>());
        synchronized (faints) {
            faints.removeIf(t -> now - t > WINDOW_5M_MS);
            faints.add(now);
            return faints.size() >= cfg.blisseyBoostingThreshold;
        }
    }

    public static int ajustarExperiencia(int exp, double factor) {
        if (factor <= 0.0) return 0;
        return (int) Math.max(1, Math.round(exp * factor));
    }

    public static void limpiarHistorial() {
        PVP_MATCH_HISTORY.clear();
        BLISSEY_FAINT_HISTORY.clear();
    }
}
