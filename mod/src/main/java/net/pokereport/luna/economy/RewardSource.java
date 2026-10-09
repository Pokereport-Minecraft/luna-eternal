package net.pokereport.luna.economy;

/**
 * Origen formal de cualquier recompensa, ítem o emisión económica en PokeReport Network.
 * Permite trazabilidad contable completa (Section 25).
 */
public enum RewardSource {
    WORLD_LOOT,
    POKEMON_DROP,
    MINING,
    FARMING,
    FISHING,
    NPC_SHOP,
    QUEST,
    GYM,
    RAID,
    EXPEDITION,
    GTS,
    KIT,
    EVENT,
    PREMIUM,
    CRAFTING,
    BREEDING,
    TRIVIA,
    ADMIN,
    OTHER;

    public static RewardSource fromReason(String reason) {
        if (reason == null) return OTHER;
        String r = reason.toLowerCase();
        if (r.contains("quest")) return QUEST;
        if (r.contains("shop")) return NPC_SHOP;
        if (r.contains("hunt")) return POKEMON_DROP;
        if (r.contains("gts")) return GTS;
        if (r.contains("oficio") || r.contains("minero")) return MINING;
        if (r.contains("crianza") || r.contains("breed")) return BREEDING;
        if (r.contains("gym") || r.contains("medalla")) return GYM;
        if (r.contains("event")) return EVENT;
        if (r.contains("kit")) return KIT;
        if (r.contains("admin")) return ADMIN;
        if (r.contains("quien_es_ese_pokemon")) return TRIVIA;
        if (r.contains("tebex") || r.contains("lunacoin")) return PREMIUM;
        return OTHER;
    }
}
