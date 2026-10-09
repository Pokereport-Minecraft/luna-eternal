package net.pokereport.luna.progression;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.pokereport.luna.LunaEternal;
import net.pokereport.luna.db.Database;
import net.pokereport.luna.economy.Currency;
import net.pokereport.luna.gym.MedallaService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio de Licencias de Entrenador (Progresión 2.0).
 * Vincula medallas de gimnasio, % de Pokédex, misiones completadas y Pokédólares.
 */
public final class TrainerLicenseService {

    private final Database db;
    private static final Map<UUID, TrainerLicense> CACHE = new ConcurrentHashMap<>();
    public static final int TOTAL_POKEDEX_GEN12 = 251;

    public TrainerLicenseService(Database db) {
        this.db = db;
    }

    public static TrainerLicense getLicense(UUID uuid) {
        return CACHE.getOrDefault(uuid, TrainerLicense.NOVATO);
    }

    public static void setLicenseCache(UUID uuid, TrainerLicense license) {
        CACHE.put(uuid, license != null ? license : TrainerLicense.NOVATO);
    }

    public static void clearCache(UUID uuid) {
        CACHE.remove(uuid);
    }

    public void load(UUID uuid, long playerId) {
        try (Connection c = db.connection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT license FROM player_license WHERE player_id = ?")) {
            ps.setLong(1, playerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    CACHE.put(uuid, TrainerLicense.de(rs.getString("license")));
                    return;
                }
            }
        } catch (Exception e) {
            LunaEternal.LOG.warn("No se pudo cargar la licencia para player_id={}: {}", playerId, e.getMessage());
        }
        CACHE.put(uuid, TrainerLicense.NOVATO);
    }

    public void save(long playerId, TrainerLicense license) throws SQLException {
        try (Connection c = db.connection();
             PreparedStatement ps = c.prepareStatement("""
                 INSERT INTO player_license (player_id, license, upgraded_at)
                 VALUES (?, ?, CURRENT_TIMESTAMP(3))
                 ON DUPLICATE KEY UPDATE license = VALUES(license), upgraded_at = CURRENT_TIMESTAMP(3)
                 """)) {
            ps.setLong(1, playerId);
            ps.setString(2, license.name());
            ps.executeUpdate();
        }
    }

    public record UpgradeCheck(
            boolean canUpgrade,
            TrainerLicense currentLicense,
            TrainerLicense nextLicense,
            int badgesHave,
            int badgesNeed,
            int dexHave,
            int dexNeed,
            int questsHave,
            int questsNeed,
            long moneyHave,
            long moneyNeed,
            String failReason
    ) {}

    public UpgradeCheck checkUpgrade(ServerPlayerEntity player, long playerId) {
        UUID uuid = player.getUuid();
        TrainerLicense current = getLicense(uuid);
        TrainerLicense next = current.next();

        if (next == null) {
            return new UpgradeCheck(false, current, null, 0, 0, 0, 0, 0, 0, 0, 0,
                    "§a¡Ya posees la licencia máxima (" + current.formatted + "§a)!");
        }

        int badgesHave = Integer.bitCount(MedallaService.enCache(uuid));
        int badgesNeed = next.requiredBadges;

        int dexHave = 0;
        try {
            if (LunaEternal.pokedex() != null) {
                dexHave = LunaEternal.pokedex().summary(playerId).caught();
            }
        } catch (Exception ignored) {}
        int dexNeed = (int) Math.ceil(next.requiredDexPct * TOTAL_POKEDEX_GEN12);

        int questsHave = 0;
        try {
            if (LunaEternal.quests() != null) {
                questsHave = LunaEternal.quests().completadas(playerId);
            }
        } catch (Exception ignored) {}
        int questsNeed = next.requiredQuests;

        long moneyHave = 0;
        try {
            if (LunaEternal.economy() != null) {
                moneyHave = LunaEternal.economy().balance(playerId, Currency.POKEDOLLAR);
            }
        } catch (Exception ignored) {}
        long moneyNeed = next.upgradeFee;

        if (badgesHave < badgesNeed) {
            return new UpgradeCheck(false, current, next, badgesHave, badgesNeed, dexHave, dexNeed,
                    questsHave, questsNeed, moneyHave, moneyNeed,
                    "§cTe faltan medallas de gimnasio (§e" + badgesHave + "§7/§e" + badgesNeed + "§c).");
        }
        if (dexHave < dexNeed) {
            return new UpgradeCheck(false, current, next, badgesHave, badgesNeed, dexHave, dexNeed,
                    questsHave, questsNeed, moneyHave, moneyNeed,
                    "§cTe faltan registros en la Pokédex Gen 1-2 (§e" + dexHave + "§7/§e" + dexNeed + "§c).");
        }
        if (questsHave < questsNeed) {
            return new UpgradeCheck(false, current, next, badgesHave, badgesNeed, dexHave, dexNeed,
                    questsHave, questsNeed, moneyHave, moneyNeed,
                    "§cTe faltan misiones completadas (§e" + questsHave + "§7/§e" + questsNeed + "§c).");
        }
        if (moneyHave < moneyNeed) {
            return new UpgradeCheck(false, current, next, badgesHave, badgesNeed, dexHave, dexNeed,
                    questsHave, questsNeed, moneyHave, moneyNeed,
                    "§cSaldo insuficiente para el trámite (§e" + moneyHave + "§7/§e" + moneyNeed + " PokéDólares§c).");
        }

        return new UpgradeCheck(true, current, next, badgesHave, badgesNeed, dexHave, dexNeed,
                questsHave, questsNeed, moneyHave, moneyNeed, null);
    }

    public boolean upgrade(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        long playerId;
        try {
            playerId = LunaEternal.players().resolve(uuid, player.getName().getString());
        } catch (Exception e) {
            player.sendMessage(Text.literal("§cError resolviendo datos del jugador."), false);
            return false;
        }

        UpgradeCheck check = checkUpgrade(player, playerId);
        if (!check.canUpgrade()) {
            player.sendMessage(Text.literal(check.failReason()), false);
            return false;
        }

        TrainerLicense next = check.nextLicense();
        if (next == null) return false;

        try {
            if (next.upgradeFee > 0) {
                String idempotency = "license_upgrade_" + uuid + "_" + next.name();
                LunaEternal.economy().debit(playerId, Currency.POKEDOLLAR, next.upgradeFee,
                        "licencia_ascenso", idempotency);
            }
            save(playerId, next);
            CACHE.put(uuid, next);

            player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 1.0f, 1.0f);

            player.sendMessage(Text.literal(
                    "\n§a§l¡FELICITACIONES! §r§fHas obtenido la Licencia §r" + next.formatted + "§f.\n"
                    + "§7Se han descontado §e" + next.upgradeFee + " PokéDólares §7por los trámites oficiales de la Liga.\n"), false);

            var server = player.getServer();
            if (server != null) {
                server.getPlayerManager().broadcast(Text.literal(
                        "§e★ §fEl entrenador §b" + player.getName().getString()
                        + " §fha ascendido a Licencia " + next.formatted + "§f."), false);
            }
            return true;
        } catch (Exception e) {
            LunaEternal.LOG.error("Error al ascender licencia para {}", player.getName().getString(), e);
            player.sendMessage(Text.literal("§cOcurrió un error al procesar el ascenso de licencia."), false);
            return false;
        }
    }
}
