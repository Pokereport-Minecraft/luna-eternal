package net.pokereport.luna.gym;

import com.gitlab.srcmc.rctapi.api.models.PokemonModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class GymFairnessTest {

    @Test
    @DisplayName("Invariantes de equidad para Brock Benchmark")
    void testBrockInvariantes() {
        PokemonModel onix = new PokemonModel(
                "cobblemon:onix", "MALE", 15, "hardy", "sturdy",
                Set.of("rockthrow", "bind", "tackle", "screech"),
                new PokemonModel.StatsModel(), new PokemonModel.StatsModel(), false, "", Set.of()
        );

        PokemonModel geodude = new PokemonModel(
                "cobblemon:geodude", "MALE", 14, "hardy", "rockhead",
                Set.of("rockthrow", "defensecurl", "tackle", "sandattack"),
                new PokemonModel.StatsModel(), new PokemonModel.StatsModel(), false, "", Set.of()
        );

        List<PokemonModel> equipoBrock = List.of(onix, geodude);

        assertTrue(GymAdaptiveTeamService.verificarBenchmarkBrock(equipoBrock),
                "El equipo canónico de Brock debe superar la verificación del benchmark");
    }

    @Test
    @DisplayName("Equipo inválido falla la verificación de Brock Benchmark")
    void testBrockEquipoInvalidoFalla() {
        PokemonModel charizard = new PokemonModel(
                "cobblemon:charizard", "MALE", 50, "adamant", "blaze",
                Set.of("flamethrower"),
                new PokemonModel.StatsModel(), new PokemonModel.StatsModel(), false, "", Set.of()
        );

        assertFalse(GymAdaptiveTeamService.verificarBenchmarkBrock(List.of(charizard)),
                "Un Charizard nivel 50 debe fallar la verificación de Brock benchmark");
        assertFalse(GymAdaptiveTeamService.verificarBenchmarkBrock(List.of()),
                "Un equipo vacío debe fallar");
    }
}
