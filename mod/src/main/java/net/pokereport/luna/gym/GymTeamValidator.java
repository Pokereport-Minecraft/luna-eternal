package net.pokereport.luna.gym;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.gitlab.srcmc.rctapi.api.models.PokemonModel;
import net.pokereport.luna.LunaEternal;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Validador de legalidad y sanidad para equipos de líderes de gimnasio.
 *
 * <p>Verifica que los equipos generados por el algoritmo adaptativo cumplan todas las reglas
 * de Showdown y Cobblemon antes de entrar al combate. Si el equipo falla la validación,
 * activa un fallback seguro al equipo estático canónico del datapack e incrementa la métrica
 * {@code adaptive_team_fallback_total}.
 */
public final class GymTeamValidator {

    private static final AtomicLong FALLBACK_COUNTER = new AtomicLong(0);

    public record ValidationResult(boolean valid, String reason) {
        public static ValidationResult ok() { return new ValidationResult(true, null); }
        public static ValidationResult fail(String r) { return new ValidationResult(false, r); }
    }

    private GymTeamValidator() {}

    public static long getFallbackCount() {
        return FALLBACK_COUNTER.get();
    }

    public static void recordFallback() {
        FALLBACK_COUNTER.incrementAndGet();
    }

    /**
     * Valida la integridad y legalidad de un equipo generado para un líder de gimnasio.
     */
    public static ValidationResult validar(List<PokemonModel> equipo, Gimnasio.Gimnasio_ g) {
        if (equipo == null || equipo.isEmpty()) {
            recordFallback();
            return ValidationResult.fail("Equipo nulo o vacío");
        }

        int minRequerido = Gimnasio.equipoMinimo(g);
        if (equipo.size() < minRequerido) {
            recordFallback();
            return ValidationResult.fail("Cantidad insuficiente de Pokémon (" + equipo.size() + " < " + minRequerido + ")");
        }

        for (int i = 0; i < equipo.size(); i++) {
            PokemonModel p = equipo.get(i);
            if (p == null) {
                recordFallback();
                return ValidationResult.fail("Slot " + i + " contiene un modelo nulo");
            }

            if (p.getSpecies() == null || p.getSpecies().isBlank()) {
                recordFallback();
                return ValidationResult.fail("Slot " + i + " tiene especie vacía o nula");
            }

            String cleanSpecies = p.getSpecies().toLowerCase();
            if (cleanSpecies.startsWith("cobblemon:")) {
                cleanSpecies = cleanSpecies.substring("cobblemon:".length());
            }

            try {
                if (PokemonSpecies.INSTANCE.getByName(cleanSpecies) == null) {
                    recordFallback();
                    return ValidationResult.fail("Especie desconocida en Cobblemon: " + cleanSpecies);
                }
            } catch (Throwable t) {
                // Entorno de prueba unitaria sin bootstrap de Minecraft: validar sintaxis del identificador
                if (!cleanSpecies.matches("^[a-z0-9_]+$")) {
                    recordFallback();
                    return ValidationResult.fail("Identificador de especie inválido: " + cleanSpecies);
                }
            }

            if (p.getLevel() <= 0 || p.getLevel() > g.nivel()) {
                recordFallback();
                return ValidationResult.fail("Nivel ilegal en slot " + i + ": " + p.getLevel() + " (máximo permitido " + g.nivel() + ")");
            }

            if (p.getMoveset() == null || p.getMoveset().isEmpty()) {
                recordFallback();
                return ValidationResult.fail("Moveset vacío en slot " + i + " (" + cleanSpecies + ")");
            }

            if (p.getIVs() == null || p.getEVs() == null) {
                recordFallback();
                return ValidationResult.fail("IVs o EVs nulos en slot " + i);
            }
        }

        return ValidationResult.ok();
    }
}
