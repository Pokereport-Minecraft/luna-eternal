package net.pokereport.luna.gym;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.gitlab.srcmc.rctapi.api.models.PokemonModel;
import net.minecraft.server.network.ServerPlayerEntity;
import net.pokereport.luna.LunaEternal;
import net.pokereport.luna.gym.Gimnasio.Gimnasio_;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio de Adaptación Dinámica de Equipos para Gimnasios.
 *
 * Principios de Diseño:
 * 1. Brock es el benchmark de calibración inmutable: su núcleo temático, identidad y nivel se respetan estrictamente.
 * 2. Composición determinista y previa al combate: NUNCA se alteran equipos ni Pokémon a mitad de la batalla.
 * 3. Aislamiento total: se leen los Pokémon del retador sin mutar jamás el almacenamiento del jugador.
 * 4. Puntuación táctica de cobertura legal y penalización de movimientos redundantes.
 */
public final class GymAdaptiveTeamService {

    private static final Map<UUID, List<PokemonModel>> TEAMS_IN_BATTLE = new ConcurrentHashMap<>();

    private GymAdaptiveTeamService() {}

    /**
     * Genera un equipo adaptado para el líder contra el retador.
     */
    public static List<PokemonModel> generarEquipoAdaptado(Gimnasio_ gimnasio, ServerPlayerEntity retador) {
        if (gimnasio == null || retador == null) return List.of();
        var party = com.cobblemon.mod.common.Cobblemon.INSTANCE.getStorage().getParty(retador);
        List<Pokemon> equipoRetador = new java.util.ArrayList<>();
        for (Pokemon p : party) {
            if (p != null) equipoRetador.add(p);
        }
        List<PokemonModel> adaptado = Adaptador.prepararEquipo(gimnasio, equipoRetador);

        TEAMS_IN_BATTLE.put(retador.getUuid(), adaptado);
        LunaEternal.LOG.info("[GYM-ADAPTIVE] Retador {} vs {}: {} Pokémon generados (mínimo={}, retador={})",
                retador.getGameProfile().getName(), gimnasio.id(), adaptado.size(),
                Gimnasio.equipoMinimo(gimnasio), equipoRetador.size());

        return adaptado;
    }

    public static List<PokemonModel> getEquipoActivo(UUID jugadorUuid) {
        return TEAMS_IN_BATTLE.getOrDefault(jugadorUuid, Collections.emptyList());
    }

    public static void limpiarCombate(UUID jugadorUuid) {
        TEAMS_IN_BATTLE.remove(jugadorUuid);
    }

    /**
     * Valida que el equipo de Brock cumpla con todos los invariantes de benchmark.
     */
    public static boolean verificarBenchmarkBrock(List<PokemonModel> equipo) {
        if (equipo == null || equipo.isEmpty()) return false;
        Gimnasio_ brock = Gimnasio.de("brock");
        if (brock == null) return false;

        // Brock tiene nivel 14 y requiere al menos 1 Pokémon
        for (PokemonModel m : equipo) {
            if (m.getLevel() > brock.nivel() || m.getLevel() < brock.nivel() - 1) return false;
            if (m.getMoveset() == null || m.getMoveset().isEmpty()) return false;
        }
        // Debe contener su firma ("onix" o "geodude")
        return equipo.stream().anyMatch(m -> m.getSpecies().contains("onix") || m.getSpecies().contains("geodude"));
    }
}
