package net.pokereport.luna.progression;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.pokereport.luna.LunaEternal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Configuración centralizada de balance de progresión, fauna, crianza, experiencia y economía.
 *
 * <p>Permite ajustar o revertir ("kill-switch") cualquier multiplicador en caliente
 * sin recompilar ni reiniciar el servidor de producción.
 */
public final class ProgressionBalanceConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static volatile Data data = new Data();
    private static Path configPath = null;

    private ProgressionBalanceConfig() {}

    public static final class Data {
        public boolean enabled = true;

        public SpawnBalance spawn = new SpawnBalance();
        public ShinyBalance shiny = new ShinyBalance();
        public HoneyBalance honey = new HoneyBalance();
        public ExperienceBalance experience = new ExperienceBalance();
        public BreedingBalance breeding = new BreedingBalance();
        public GtsBalance gts = new GtsBalance();
        public EconomyBalance economy = new EconomyBalance();
        public EventConfig events = new EventConfig();
        public AltAccountConfig alts = new AltAccountConfig();
    }

    public static final class SpawnBalance {
        public double commonMultiplier = 1.0;
        public double uncommonMultiplier = 0.90;
        public double rareMultiplier = 0.65;
        public double starterMultiplier = 0.35;
        public double pseudoLegendaryMultiplier = 0.20;
        public double legendaryMultiplier = 0.05;
        public boolean nerfOceanDratini = true;
    }

    public static final class ShinyBalance {
        public boolean enabled = true;
        public double wildMultiplier = 1.0;
        public double breedingMultiplier = 1.0;
        public double eventMultiplier = 1.0;
        public int fossilMachineRate = 4096;
    }

    public static final class HoneyBalance {
        public boolean enabled = true;
        public double candyCookMultiplier = 0.50;
        public int slatherShinyChance = 4000;
        public long baseHoneyValue = 250L;
    }

    public static final class ExperienceBalance {
        public double wildMultiplier = 0.60;
        public double trainerMultiplier = 1.00;
        public double gymMultiplier = 1.10;
        public boolean pvpExperienceEnabled = true;
        public boolean pvpDiminishingReturns = true;
        public int blisseyBoostingThreshold = 3;
    }

    public static final class BreedingBalance {
        public int f2pBaseSlots = 1;
        public boolean preserveCoinPurchases = true;
        public int maxSlots = 7;
    }

    public static final class GtsBalance {
        public int f2pMaxListings = 3;
        public int eliteMaxListings = 5;
        public int campeonMaxListings = 8;
        public int maestroMaxListings = 12;
        public int leyendaMaxListings = 20;

        public double listingFeePercent = 2.0;
        public double baseSalesTaxPercent = 8.0;
        public double eliteTaxDiscount = 0.5;
        public double campeonTaxDiscount = 1.0;
        public double maestroTaxDiscount = 1.5;
        public double leyendaTaxDiscount = 2.0;
    }

    public static final class EconomyBalance {
        public boolean diminishingReturnsEnabled = true;
        public long dailyBracket1 = 20_000L;
        public double rateBracket1 = 1.00;
        public long dailyBracket2 = 50_000L;
        public double rateBracket2 = 0.80;
        public long dailyBracket3 = 100_000L;
        public double rateBracket3 = 0.50;
        public double rateBracketExcess = 0.25;

        public double getMarginalMultiplier(long dailyEarnings) {
            if (!diminishingReturnsEnabled) return 1.0;
            if (dailyEarnings < dailyBracket1) return rateBracket1;
            if (dailyEarnings < dailyBracket2) return rateBracket2;
            if (dailyEarnings < dailyBracket3) return rateBracket3;
            return rateBracketExcess;
        }
    }

    public static final class EventConfig {
        public String activeEventId = "ninguno";
        public boolean active = false;
        public long ticketCostLunaCoins = 500L;
        public long ticketCostPokedollars = 25_000L;
        public int maxTicketsPerPlayer = 1;
    }

    public static final class AltAccountConfig {
        public boolean enabled = true;
        public int maxDailyTransfers = 5;
        public long highRiskTransferThreshold = 50_000L;
        public int riskScoreAlertThreshold = 70;
    }

    public static Data get() {
        return data;
    }

    public static void set(Data newData) {
        if (newData != null) {
            data = newData;
        }
    }

    public static boolean isEnabled() {
        return data != null && data.enabled;
    }

    public static void load() {
        Path file = null;
        try {
            file = FabricLoader.getInstance().getConfigDir()
                    .resolve("lunaeternal").resolve("progression_balance.json");
        } catch (Throwable ignored) {
            file = Path.of("config", "lunaeternal", "progression_balance.json");
        }
        load(file);
    }

    public static void load(Path file) {
        configPath = file;
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            if (!Files.exists(file)) {
                Files.writeString(file, GSON.toJson(new Data()) + System.lineSeparator(),
                        StandardCharsets.UTF_8);
            }
            try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                Data loaded = GSON.fromJson(reader, Data.class);
                if (loaded != null) {
                    data = loaded;
                    try {
                        LunaEternal.LOG.info("ProgressionBalanceConfig cargada exitosamente (enabled={})", data.enabled);
                    } catch (Throwable ignored) {}
                }
            }
        } catch (IOException e) {
            try {
                LunaEternal.LOG.error("Error al cargar config/lunaeternal/progression_balance.json, usando valores por defecto", e);
            } catch (Throwable ignored) {}
            data = new Data();
        }
    }

    public static void reload() {
        if (configPath != null) {
            load(configPath);
        } else {
            load();
        }
    }
}
