package net.pokereport.luna.world;

import java.util.Set;

/** Solo fauna vanilla y entrenadores ambientales; nunca entidades genéricas del mundo. */
public final class PoliticaFauna {
    public static final float DENSIDAD_HOGAR = 0.04f;
    public static final float DENSIDAD_SALVAJE = 3.0f;
    public static final float MULTIPLICADOR_ULTRARRARO_SALVAJE = 1.10f;
    private static final Set<String> RAREZAS_PROHIBIDAS_HOGAR = Set.of(
            "legendary", "mythical", "ultra_beast", "paradox", "restricted");
    
    private static java.util.List<String> dittoPool = null;
    
    public static synchronized String getDittoPoolRandom() {
        if (dittoPool == null) {
            dittoPool = new java.util.ArrayList<>();
            for (com.cobblemon.mod.common.pokemon.Species spec : com.cobblemon.mod.common.api.pokemon.PokemonSpecies.INSTANCE.getImplemented()) {
                if (generacionPermitida(spec.getNationalPokedexNumber(), null, spec.getLabels())) {
                    boolean isLegendary = spec.getLabels() != null && (spec.getLabels().contains("legendary") || spec.getLabels().contains("mythical") || spec.getLabels().contains("restricted"));
                    if (!isLegendary && spec.getPreEvolution() == null && !spec.getForms().get(0).getEggGroups().contains(com.cobblemon.mod.common.api.pokemon.egg.EggGroup.UNDISCOVERED)) {
                        dittoPool.add(spec.getName());
                    }
                }
            }
        }
        if (dittoPool.isEmpty()) return "ditto";
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
}

