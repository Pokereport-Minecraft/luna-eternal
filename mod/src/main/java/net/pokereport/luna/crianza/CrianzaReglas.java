package net.pokereport.luna.crianza;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Reglas puras de herencia biológica para la crianza (Luna Eternal).
 *
 * <p>Aisla las decisiones de variantes regionales, habilidades ocultas y movimientos huevo
 * para permitir validación determinista y desacoplada de la API de Cobblemon.
 */
public final class CrianzaReglas {

    private CrianzaReglas() {}

    public static final Set<String> FORMAS_REGIONALES = Set.of(
            "alolan", "galarian", "hisuian", "paldean"
    );

    public static final List<String> STATS_CANONICOS = List.of(
            "hp", "attack", "defence", "special_attack", "special_defence", "speed"
    );

    /**
     * Mapeo de Objetos Recios (Brazales de poder) a su estadística correspondiente:
     * - power_weight (Pesa Recia) -> HP (PS)
     * - power_bracer (Brazal Recio) -> Attack (Ataque)
     * - power_belt (Cinto Recio) -> Defence (Defensa)
     * - power_lens (Lente Recia) -> Special Attack (Ataque Especial)
     * - power_band (Banda Recia) -> Special Defence (Defensa Especial)
     * - power_anklet (Franja Recia) -> Speed (Velocidad)
     */
    public static final java.util.Map<String, String> OBJETOS_RECIOS = java.util.Map.of(
            "power_weight", "hp",
            "power_bracer", "attack",
            "power_belt", "defence",
            "power_lens", "special_attack",
            "power_band", "special_defence",
            "power_anklet", "speed"
    );

    public static String normalizarItemId(String itemId) {
        if (itemId == null || itemId.isBlank()) return "";
        String clean = itemId.toLowerCase(Locale.ROOT).trim();
        int colon = clean.indexOf(':');
        if (colon >= 0) clean = clean.substring(colon + 1);
        return clean;
    }

    public static String obtenerStatObjetoRecio(String itemId) {
        String clean = normalizarItemId(itemId);
        return OBJETOS_RECIOS.get(clean);
    }

    public static boolean esDestinyKnot(String itemId) {
        return normalizarItemId(itemId).equals("destiny_knot");
    }

    public static boolean esEverstone(String itemId) {
        return normalizarItemId(itemId).equals("everstone");
    }

    public static <T> T determinarNaturalezaHeredada(
            T natMadre, String itemMadre,
            T natPadre, String itemPadre,
            double rng) {
        boolean everMadre = esEverstone(itemMadre);
        boolean everPadre = esEverstone(itemPadre);

        if (everMadre && !everPadre) return natMadre;
        if (everPadre && !everMadre) return natPadre;
        if (everMadre && everPadre) return rng < 0.5 ? natMadre : natPadre;
        return null;
    }

    /**
     * Resuelve qué estadísticas de IV se heredan y de qué progenitor (true = madre, false = padre).
     *
     * @param itemMadre Objeto equipado en la madre (ej. "power_bracer", "destiny_knot", etc.)
     * @param itemPadre Objeto equipado en el padre (ej. "power_anklet", "everstone", etc.)
     * @param rng       Generador pseudoaleatorio determinista para tests o Math.random en producción
     * @return Mapa de stat canonico -> Boolean (true si hereda de la madre, false del padre)
     */
    public static java.util.Map<String, Boolean> resolverHerenciaIVs(
            String itemMadre, String itemPadre, java.util.Random rng) {
        String powerStatMadre = obtenerStatObjetoRecio(itemMadre);
        String powerStatPadre = obtenerStatObjetoRecio(itemPadre);
        boolean destinyKnot = esDestinyKnot(itemMadre) || esDestinyKnot(itemPadre);
        int totalHeredar = destinyKnot ? 5 : 3;

        java.util.Map<String, Boolean> herencia = new java.util.LinkedHashMap<>();

        // 1. Garantías de Objetos Recios (Brazal, Pesa, Cinto, Lente, Banda, Franja)
        if (powerStatMadre != null && powerStatPadre != null) {
            if (powerStatMadre.equalsIgnoreCase(powerStatPadre)) {
                // Mismo objeto recio en ambos: estadística garantizada, 50% donante madre / 50% padre
                boolean deMadre = rng.nextDouble() < 0.5;
                herencia.put(powerStatMadre, deMadre);
            } else {
                // Objetos recios diferentes: ambos transmiten garantizado su stat respectivo
                herencia.put(powerStatMadre, true);
                herencia.put(powerStatPadre, false);
            }
        } else if (powerStatMadre != null) {
            herencia.put(powerStatMadre, true);
        } else if (powerStatPadre != null) {
            herencia.put(powerStatPadre, false);
        }

        // 2. Completar las restantes estadísticas a heredar (hasta 3 normales o 5 con Lazo Destino)
        List<String> disponibles = new ArrayList<>(STATS_CANONICOS);
        disponibles.removeAll(herencia.keySet());
        java.util.Collections.shuffle(disponibles, rng);

        int faltantes = Math.max(0, totalHeredar - herencia.size());
        for (int i = 0; i < faltantes && i < disponibles.size(); i++) {
            String stat = disponibles.get(i);
            boolean deMadre = rng.nextDouble() < 0.5;
            herencia.put(stat, deMadre);
        }

        return herencia;
    }

    public static String determinarAspectoRegional(
            Set<String> aspectosMadre, boolean everMadre, boolean madreEsDitto,
            Set<String> aspectosPadre, boolean everPadre, boolean padreEsDitto,
            double rng) {
        boolean madreElegible = everMadre && !madreEsDitto && aspectosMadre != null;
        boolean padreElegible = everPadre && !padreEsDitto && aspectosPadre != null;

        String aspectoMadre = null;
        if (madreElegible) {
            for (String asp : aspectosMadre) {
                if (FORMAS_REGIONALES.contains(asp.toLowerCase(Locale.ROOT))) {
                    aspectoMadre = asp.toLowerCase(Locale.ROOT);
                    break;
                }
            }
        }

        String aspectoPadre = null;
        if (padreElegible) {
            for (String asp : aspectosPadre) {
                if (FORMAS_REGIONALES.contains(asp.toLowerCase(Locale.ROOT))) {
                    aspectoPadre = asp.toLowerCase(Locale.ROOT);
                    break;
                }
            }
        }

        if (aspectoMadre != null && aspectoPadre != null) {
            return rng < 0.5 ? aspectoMadre : aspectoPadre;
        }
        if (aspectoMadre != null) return aspectoMadre;
        if (aspectoPadre != null) return aspectoPadre;
        return null;
    }

    public static boolean puedeTransmitirHabilidadOculta(
            boolean madreHO, boolean padreHO, boolean madreEsDitto, boolean padreEsDitto) {
        if (!madreEsDitto && madreHO) return true;
        if (madreEsDitto && padreHO) return true;
        if (padreEsDitto && madreHO) return true;
        return false;
    }

    public static List<String> filtrarMovimientosHuevo(
            List<String> movimientosMadre, List<String> movimientosPadre, Set<String> eggMovesValidos) {
        List<String> resultado = new ArrayList<>();
        Set<String> vistos = new HashSet<>();
        if (eggMovesValidos == null || eggMovesValidos.isEmpty()) return resultado;

        List<List<String>> listas = new ArrayList<>();
        if (movimientosPadre != null) listas.add(movimientosPadre);
        if (movimientosMadre != null) listas.add(movimientosMadre);

        for (var lista : listas) {
            for (String m : lista) {
                if (m == null || m.isBlank()) continue;
                String lower = m.toLowerCase(Locale.ROOT);
                if (vistos.contains(lower)) continue;
                for (String egg : eggMovesValidos) {
                    if (egg != null && egg.equalsIgnoreCase(m)) {
                        resultado.add(m);
                        vistos.add(lower);
                        break;
                    }
                }
            }
        }
        return resultado;
    }
}
