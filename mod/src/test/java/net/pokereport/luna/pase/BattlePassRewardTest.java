package net.pokereport.luna.pase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BattlePassRewardTest {

    @Test
    @DisplayName("Nivel 1 otorga Charizard Shiny mecha nivel 50")
    void testNivel1CharizardShiny() {
        Recompensa r1 = PaseCatalogo.de(1);
        assertNotNull(r1, "El nivel 1 debe existir");
        assertEquals(Recompensa.Tipo.POKEMON, r1.tipo(), "El nivel 1 debe ser un Pokémon");
        assertEquals("charizard", r1.id(), "Debe ser Charizard");
        assertEquals(50, r1.nivel(), "El mecha conserva su nivel original");
        assertEquals(Recompensa.ASPECTO_MECHA, r1.aspecto());
        assertTrue(r1.shiny(), "El Charizard del nivel 1 DEBE ser Shiny");
    }

    @Test
    @DisplayName("Nivel 100 entrega kit de maestría sin Pokémon")
    void testNivel100KitMaestria() {
        Recompensa r100 = PaseCatalogo.de(100);
        assertNotNull(r100, "El nivel 100 debe existir");
        assertEquals(Recompensa.Tipo.OBJETO, r100.tipo());
        assertEquals("cobblemon:master_ball", r100.id());
        assertEquals(3, r100.cantidad());

        List<Recompensa> entregables = PaseCatalogo.entregables(100);
        assertNotNull(entregables, "Entregables del nivel 100 no deben ser nulos");
        assertEquals(3, entregables.size());
        assertTrue(entregables.stream().allMatch(r -> r.tipo() == Recompensa.Tipo.OBJETO));
        assertTrue(entregables.stream().anyMatch(r -> "cobblemon:ability_patch".equals(r.id()) && r.cantidad() == 3));
        assertTrue(entregables.stream().anyMatch(r -> "cobblemon:rare_candy".equals(r.id()) && r.cantidad() == 16));
        assertEquals(1, PaseCatalogo.cuantosPokemon());
    }

    @Test
    @DisplayName("Todos los 100 niveles tienen recompensas válidas")
    void testTodosNivelesValidos() {
        for (int i = 1; i <= PaseNivel.MAX; i++) {
            Recompensa r = PaseCatalogo.de(i);
            assertNotNull(r, "Nivel " + i + " no debe ser nulo");
            assertNotNull(r.tipo(), "Tipo de recompensa en nivel " + i + " no debe ser nulo");
            assertNotNull(r.rareza(), "Rareza en nivel " + i + " no debe ser nula");
            assertFalse(r.id().isBlank(), "ID de recompensa en nivel " + i + " no debe estar vacío");
        }
    }
}
