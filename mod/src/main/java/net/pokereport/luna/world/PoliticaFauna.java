package net.pokereport.luna.world;

import java.util.Set;

/** Solo fauna vanilla y entrenadores ambientales; nunca entidades genéricas del mundo. */
public final class PoliticaFauna {
    public static final float DENSIDAD_HOGAR = 0.04f;
    public static final float DENSIDAD_SALVAJE = 0.75f;
    public static final float MULTIPLICADOR_ULTRARRARO_SALVAJE = 1.10f;
    private static final Set<String> RAREZAS_PROHIBIDAS_HOGAR = Set.of(
            "legendary", "mythical", "ultra_beast", "paradox", "restricted");
    
    private static java.util.List<String> dittoPool = null;

    private static final java.util.List<String> DITTO_POOL_FALLBACK = java.util.List.of(
            "bulbasaur", "charmander", "squirtle", "caterpie", "weedle", "pidgey", "rattata", "spearow",
            "ekans", "sandshrew", "nidoran-f", "nidoran-m", "vulpix", "zubat", "oddish", "paras",
            "venonat", "diglett", "meowth", "psyduck", "mankey", "growlithe", "poliwag", "abra",
            "machop", "bellsprout", "tentacool", "geodude", "ponyta", "slowpoke", "magnemite",
            "doduo", "seel", "grimer", "shellder", "gastly", "onix", "drowzee", "krabby",
            "voltorb", "exeggcute", "cubone", "lickitung", "koffing", "rhyhorn", "tangela", "kangaskhan",
            "horsea", "goldeen", "staryu", "scyther", "pinsir", "tauros", "magikarp", "lapras",
            "eevee", "porygon", "omanyte", "kabuto", "aerodactyl", "snorlax", "dratini",
            "chikorita", "cyndaquil", "totodile", "sentret", "hoothoot", "ledyba", "spinarak", "chinchou",
            "pichu", "cleffa", "igglybuff", "togepi", "natu", "mareep", "marill", "sudowoodo",
            "hoppip", "aipom", "sunkern", "yanma", "wooper", "murkrow", "misdreavus", "wobbuffet",
            "girafarig", "pineco", "dunsparce", "gligar", "snubbull", "qwilfish", "shuckle", "heracross",
            "sneasel", "teddiursa", "slugma", "swinub", "corsola", "remoraid", "delibird", "mantine",
            "skarmory", "houndour", "phanpy", "stantler", "smeargle", "tyrogue", "smoochum", "elekid",
            "magby", "miltank", "larvitar"
    );

    public static synchronized String getDittoPoolRandom() {
        if (dittoPool == null || dittoPool.isEmpty()) {
            dittoPool = new java.util.ArrayList<>();
            try {
                for (com.cobblemon.mod.common.pokemon.Species spec : com.cobblemon.mod.common.api.pokemon.PokemonSpecies.INSTANCE.getImplemented()) {
                    try {
                        if (spec == null) continue;
                        if (generacionPermitida(spec.getNationalPokedexNumber(), null, spec.getLabels())) {
                            boolean isLegendary = spec.getLabels() != null && (
                                    spec.getLabels().contains("legendary") ||
                                    spec.getLabels().contains("mythical") ||
                                    spec.getLabels().contains("restricted")
                            );
                            if (!isLegendary && spec.getPreEvolution() == null && !spec.getName().equalsIgnoreCase("ditto")) {
                                var eggGroups = spec.getEggGroups();
                                boolean isUndiscovered = eggGroups != null && eggGroups.contains(com.cobblemon.mod.common.api.pokemon.egg.EggGroup.UNDISCOVERED);
                                boolean isBaby = spec.getLabels() != null && spec.getLabels().contains("baby");
                                if (!isUndiscovered || isBaby) {
                                    dittoPool.add(spec.getName());
                                }
                            }
                        }
                    } catch (Throwable ignored) {}
                }
            } catch (Throwable t) {
                System.err.println("PoliticaFauna: Error al construir dittoPool dinámico: " + t.getMessage());
            }
        }
        if (dittoPool == null || dittoPool.isEmpty()) {
            dittoPool = new java.util.ArrayList<>(DITTO_POOL_FALLBACK);
        }
        return dittoPool.get(new java.util.Random().nextInt(dittoPool.size()));
    }

    private PoliticaFauna() {}

    public static final int MAX_GENERACION = 2;

    public static int getGeneracion(int dex, String form, Set<String> etiquetas) {
        if (etiquetas != null) {
            for (String etiqueta : etiquetas) {
                String e = etiqueta.toLowerCase();
                if (e.matches("^gen[1-9]$")) {
                    return Integer.parseInt(e.substring(3));
                }
            }
            // Fallbacks if genN label is missing but regional label exists
            for (String etiqueta : etiquetas) {
                String e = etiqueta.toLowerCase();
                if (e.contains("alola")) return 7;
                if (e.contains("galar")) return 8;
                if (e.contains("hisui")) return 8;
                if (e.contains("paldea")) return 9;
            }
        }
        if (form != null) {
            String f = form.toLowerCase();
            if (f.contains("alola")) return 7;
            if (f.contains("galar")) return 8;
            if (f.contains("hisui")) return 8;
            if (f.contains("paldea")) return 9;
        }
        if (dex <= 151) return 1;
        if (dex <= 251) return 2;
        if (dex <= 386) return 3;
        if (dex <= 493) return 4;
        if (dex <= 649) return 5;
        if (dex <= 721) return 6;
        if (dex <= 809) return 7;
        if (dex <= 905) return 8;
        return 9;
    }
    public static boolean generacionPermitida(int dex, String form, Set<String> etiquetas) {
        return getGeneracion(dex, form, etiquetas) <= MAX_GENERACION;
    }

    public static boolean bloquear(String namespace, boolean mob, boolean personalizado,
                                   boolean entrenadorAmbiental, boolean persistente) {
        if (!mob || personalizado) return false;
        if (namespace.equals("minecraft")) return true;
        return entrenadorAmbiental && !persistente;
    }
    public static float densidad(String dimension) {
        if (dimension.equals("minecraft:overworld")) return DENSIDAD_HOGAR;
        if (dimension.matches("lunaeternal:salvaje[2-6]?")) return DENSIDAD_SALVAJE;
        // La Ciudadela no comparte el recorte del Hogar ni el multiplicador de
        // las zonas salvajes: conserva la densidad base de su dimensión.
        if (dimension.equals("lunaeternal:ciudadela")) return 1f;
        // No imponemos una política de spawn a dimensiones ajenas al sistema.
        return 1f;
    }

    public static boolean esSalvaje(String dimension) {
        return dimension != null && dimension.matches("lunaeternal:salvaje[2-6]?");
    }

    /** Política de aparicion natural: solo Gen 1-2; Hogar sin shiny ni rarezas mayores. */
    public static boolean permitirPokemon(String dimension, int dex, boolean shiny,
                                           Set<String> etiquetas) {
        if (!generacionPermitida(dex, null, etiquetas)) return false;
        if (!"minecraft:overworld".equals(dimension)) return true;
        if (shiny) return false;
        if (etiquetas == null) return true;
        for (String etiqueta : etiquetas) {
            if (RAREZAS_PROHIBIDAS_HOGAR.contains(etiqueta.toLowerCase())) return false;
        }
        return true;
    }

    public static boolean esInicial(int dex) {
        return (dex >= 1 && dex <= 9) || (dex >= 152 && dex <= 160);
    }

    public static boolean esPseudolegendario(int dex) {
        return (dex >= 147 && dex <= 149) || (dex >= 246 && dex <= 248);
    }

    public static boolean esLegendarioOMitico(int dex, Set<String> etiquetas) {
        if (dex == 144 || dex == 145 || dex == 146 || dex == 150 || dex == 151
                || (dex >= 243 && dex <= 245) || dex == 249 || dex == 250 || dex == 251) {
            return true;
        }
        if (etiquetas != null) {
            for (String tag : etiquetas) {
                String t = tag.toLowerCase();
                if (t.contains("legendary") || t.contains("mythical")) return true;
            }
        }
        return false;
    }

    public static boolean esRaro(int dex, Set<String> etiquetas) {
        if (dex == 113 || dex == 131 || dex == 132 || dex == 133 || dex == 137
                || dex == 142 || dex == 143 || dex == 242) {
            return true;
        }
        if (etiquetas != null) {
            for (String tag : etiquetas) {
                String t = tag.toLowerCase();
                if (t.contains("rare") || t.contains("ultra-rare")) return true;
            }
        }
        return false;
    }

    public static double obtenerMultiplicadorSpawn(int dex, String especie, Set<String> etiquetas) {
        if (!net.pokereport.luna.progression.ProgressionBalanceConfig.isEnabled()) {
            return 1.0;
        }
        var cfg = net.pokereport.luna.progression.ProgressionBalanceConfig.get().spawn;

        if (esLegendarioOMitico(dex, etiquetas)) {
            return cfg.legendaryMultiplier;
        }
        if (esPseudolegendario(dex)) {
            return cfg.pseudoLegendaryMultiplier;
        }
        if (esInicial(dex)) {
            return cfg.starterMultiplier;
        }
        if (esRaro(dex, etiquetas)) {
            return cfg.rareMultiplier;
        }
        return cfg.commonMultiplier;
    }

    public static boolean evaluarProbabilidadSpawn(com.cobblemon.mod.common.pokemon.Pokemon pokemon,
                                                   double x, double y, double z) {
        if (pokemon == null || !net.pokereport.luna.progression.ProgressionBalanceConfig.isEnabled()) {
            return true;
        }
        int dex = pokemon.getSpecies().getNationalPokedexNumber();
        String especie = pokemon.getSpecies().getName().toLowerCase();
        Set<String> etiquetas = pokemon.getSpecies().getLabels();

        double mult = obtenerMultiplicadorSpawn(dex, especie, etiquetas);
        if (mult >= 1.0) {
            return true;
        }

        double roll = java.util.concurrent.ThreadLocalRandom.current().nextDouble();
        boolean spawns = roll < mult;

        if (spawns && esLegendarioOMitico(dex, etiquetas)) {
            net.pokereport.luna.LunaEternal.LOG.info(
                    "[LEGENDARY_SPAWN] Especie: {} (Dex: {}), Coords: [x={:.1f}, y={:.1f}, z={:.1f}]",
                    especie, dex, x, y, z);
        }

        return spawns;
    }
}

