package net.pokereport.luna.progression;

/**
 * Licencias de Entrenador de PokeReport Network.
 * Progresión institucional ligada a méritos (Medallas, Dex, Misiones, Pokédólares).
 */
public enum TrainerLicense {
    NOVATO("Novato", "§7Novato", 0, 0.0, 0, 0L),
    BRONCE("Bronce", "§6Bronce", 2, 0.15, 5, 5_000L),
    PLATA("Plata", "§fPlata", 4, 0.30, 15, 20_000L),
    ORO("Oro", "§eOro", 6, 0.50, 30, 50_000L),
    MAESTRO("Maestro", "§dMaestro", 8, 0.70, 50, 150_000L),
    CAMPEON("Campeón", "§b§lCampeón", 12, 0.85, 75, 500_000L);

    public final String id;
    public final String formatted;
    public final int requiredBadges;
    public final double requiredDexPct;
    public final int requiredQuests;
    public final long upgradeFee;

    TrainerLicense(String id, String formatted, int requiredBadges, double requiredDexPct,
                   int requiredQuests, long upgradeFee) {
        this.id = id;
        this.formatted = formatted;
        this.requiredBadges = requiredBadges;
        this.requiredDexPct = requiredDexPct;
        this.requiredQuests = requiredQuests;
        this.upgradeFee = upgradeFee;
    }

    public TrainerLicense next() {
        int nextOrdinal = ordinal() + 1;
        if (nextOrdinal < values().length) {
            return values()[nextOrdinal];
        }
        return null;
    }

    public static TrainerLicense de(String name) {
        if (name == null || name.isBlank()) return NOVATO;
        for (TrainerLicense lic : values()) {
            if (lic.name().equalsIgnoreCase(name.trim()) || lic.id.equalsIgnoreCase(name.trim())) {
                return lic;
            }
        }
        return NOVATO;
    }
}
