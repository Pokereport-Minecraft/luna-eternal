package net.pokereport.luna.progression;

import net.pokereport.luna.LunaEternal;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Telemetría y detección heurística de cuentas secundarias (Alts) y traspaso indebido de recursos.
 *
 * <p>Calcula un RiskScore (0 a 100) y genera alertas para el staff sin aplicar sanciones
 * automatizadas por IP (evitando falsos positivos en familias o redes compartidas).
 */
public final class AltAccountTelemetry {

    private AltAccountTelemetry() {}

    public record TransferRecord(UUID sender, UUID receiver, long amount, long timestamp) {}

    private static final Map<UUID, List<TransferRecord>> RECENT_TRANSFERS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> KIT_REDEMPTION_COUNTS = new ConcurrentHashMap<>();
    private static final Map<String, Set<UUID>> IP_ACCOUNT_MAP = new ConcurrentHashMap<>();

    public static void recordIpConnection(UUID uuid, String ip) {
        if (ip == null || ip.isBlank()) return;
        IP_ACCOUNT_MAP.computeIfAbsent(ip, k -> ConcurrentHashMap.newKeySet()).add(uuid);
    }

    public static void recordMoneyTransfer(UUID sender, UUID receiver, long amount) {
        long now = System.currentTimeMillis();
        TransferRecord rec = new TransferRecord(sender, receiver, amount, now);
        RECENT_TRANSFERS.computeIfAbsent(sender, k -> new ArrayList<>()).add(rec);
        RECENT_TRANSFERS.computeIfAbsent(receiver, k -> new ArrayList<>()).add(rec);
    }

    public static void recordKitRedemption(UUID uuid) {
        KIT_REDEMPTION_COUNTS.merge(uuid, 1, Integer::sum);
    }

    public static int calculateRiskScore(UUID uuid, String ip) {
        int score = 0;

        // 1. Número de cuentas compartiendo IP (>2 añade 25 pts, >4 añade 50 pts)
        if (ip != null && IP_ACCOUNT_MAP.containsKey(ip)) {
            int accounts = IP_ACCOUNT_MAP.get(ip).size();
            if (accounts > 4) {
                score += 50;
            } else if (accounts > 2) {
                score += 25;
            }
        }

        // 2. Transferencias de dinero anómalas
        var transfers = RECENT_TRANSFERS.get(uuid);
        if (transfers != null) {
            long totalReceived = 0;
            for (TransferRecord t : transfers) {
                if (uuid.equals(t.receiver())) {
                    totalReceived += t.amount();
                }
            }
            if (totalReceived > 100_000) {
                score += 35;
            } else if (totalReceived > 25_000) {
                score += 15;
            }
        }

        // 3. Ráfaga de redención de kits
        int kits = KIT_REDEMPTION_COUNTS.getOrDefault(uuid, 0);
        if (kits > 10) {
            score += 25;
        } else if (kits > 5) {
            score += 10;
        }

        int finalScore = Math.min(100, score);
        var cfg = ProgressionBalanceConfig.get().alts;
        if (cfg.enabled && finalScore >= cfg.riskScoreAlertThreshold) {
            try {
                LunaEternal.LOG.warn("[ALT_ALERT] Jugador {} en IP {} presenta RiskScore={} (Posible granja/cuenta secundaria)",
                        uuid, ip, finalScore);
            } catch (Throwable ignored) {}
        }
        return finalScore;
    }

    public static void reset() {
        RECENT_TRANSFERS.clear();
        KIT_REDEMPTION_COUNTS.clear();
        IP_ACCOUNT_MAP.clear();
    }
}
