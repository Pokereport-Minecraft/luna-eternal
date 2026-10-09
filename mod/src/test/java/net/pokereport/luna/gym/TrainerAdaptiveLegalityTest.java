package net.pokereport.luna.gym;

import com.gitlab.srcmc.rctapi.api.models.Gimmicks;
import com.gitlab.srcmc.rctapi.api.models.PokemonModel;
import com.gitlab.srcmc.rctapi.api.util.Text;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class TrainerAdaptiveLegalityTest {

    @Test
    @DisplayName("Equipo legal para Brock (Benchmark Nivel 15) es aprobado")
    void testEquipoLegalBrock() {
        var brock = Gimnasio.de("brock");
        assertNotNull(brock);
        assertEquals(15, brock.nivel());

        var ivs = new PokemonModel.StatsModel(31, 31, 31, 31, 31, 31);
        var evs = new PokemonModel.StatsModel(252, 0, 252, 0, 4, 0);

        PokemonModel geodude = new PokemonModel(
                "geodude", Text.empty(), "MALE", 14, "adamant", "rock_head",
                Set.of("rockthrow", "mudslap", "tackle", "defensecurl"),
                ivs, evs, false, List.of("sitrus_berry"), Set.of(), new Gimmicks());

        PokemonModel onix = new PokemonModel(
                "onix", Text.empty(), "MALE", 15, "hardy", "sturdy",
                Set.of("rocktomb", "bind", "screech", "rocksmash"),
                ivs, evs, false, List.of("sitrus_berry"), Set.of(), new Gimmicks());

        var equipo = List.of(geodude, onix);
        var res = GymTeamValidator.validar(equipo, brock);
        assertTrue(res.valid(), "El equipo adaptativo canónico de Brock debe ser válido");
    }

    @Test
    @DisplayName("Rechazo de Pokémon por encima del nivel máximo del gimnasio")
    void testRechazoSobreNivel() {
        var brock = Gimnasio.de("brock");
        assertNotNull(brock);

        var ivs = new PokemonModel.StatsModel(31, 31, 31, 31, 31, 31);
        var evs = new PokemonModel.StatsModel(252, 0, 252, 0, 4, 0);

        // Onix nivel 16 cuando el límite de Brock es 15
        PokemonModel onixIlegal = new PokemonModel(
                "onix", Text.empty(), "MALE", 16, "hardy", "sturdy",
                Set.of("rocktomb", "bind", "screech", "rocksmash"),
                ivs, evs, false, List.of("sitrus_berry"), Set.of(), new Gimmicks());

        var res = GymTeamValidator.validar(List.of(onixIlegal), brock);
        assertFalse(res.valid());
        assertTrue(res.reason().contains("Nivel ilegal"));
    }
}
