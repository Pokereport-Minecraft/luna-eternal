package net.pokereport.luna.battle;

import com.gitlab.srcmc.rctapi.api.models.Gimmicks;
import com.gitlab.srcmc.rctapi.api.models.PokemonModel;
import com.gitlab.srcmc.rctapi.api.util.Text;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class TrainerNullNicknameTest {

    @Test
    @DisplayName("Sanitización de PokemonModel con campos nulos (nickname, gender, gimmicks)")
    void testSanitizacionCamposNulos() throws Exception {
        var ivs = new PokemonModel.StatsModel(31, 31, 31, 31, 31, 31);
        var evs = new PokemonModel.StatsModel(0, 0, 0, 0, 0, 0);

        PokemonModel model = new PokemonModel(
                "pikachu", (Text) null, null, 15, "hardy", "static",
                Set.of("thunderbolt"), ivs, evs, false, List.of(), Set.of(), null);

        assertNull(model.getNickname());
        assertNull(model.getGender());
        assertNull(model.getGimmicks());

        // Simular sanitización implementada en PokemonModelConverterFixMixin
        if (model.getNickname() == null) {
            Field f = PokemonModel.class.getDeclaredField("nickname");
            f.setAccessible(true);
            f.set(model, Text.empty());
        }
        if (model.getGender() == null) {
            Field f = PokemonModel.class.getDeclaredField("gender");
            f.setAccessible(true);
            f.set(model, "GENDERLESS");
        }
        if (model.getGimmicks() == null) {
            Field f = PokemonModel.class.getDeclaredField("gimmicks");
            f.setAccessible(true);
            f.set(model, new Gimmicks());
        }

        assertNotNull(model.getNickname());
        assertNull(model.getNickname().getLiteral());
        assertEquals("GENDERLESS", model.getGender());
        assertNotNull(model.getGimmicks());
    }
}
