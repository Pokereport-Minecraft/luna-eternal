package net.pokereport.luna.pase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BattlePassRewardTest {

    @Test
    @DisplayName("Nivel 1 otorga Charizard Shiny nivel 15")
    void testNivel1CharizardShiny() {
        Recompensa r1 = PaseCatalogo.de(1);
        assertNotNull(r1, "El nivel 1 debe existir");
        assertEquals(Recompensa.Tipo.POKEMON, r1.tipo(), "El nivel 1 debe ser un Pokémon");
        assertEquals("charizard", r1.id(), "Debe ser Charizard");
        assertEquals(15, r1.nivel(), "Debe ser nivel 15");
        assertTrue(r1.shiny(), "El Charizard del nivel 1 DEBE ser Shiny");
    }

    @Test
    @DisplayName("Nivel 100 otorga Charizard Shiny 50 con casco mecha y entrega Master Ball adicional")
    void testNivel100CharizardShinyYMasterBall() {
        Recompensa r100 = PaseCatalogo.de(100);
        assertNotNull(r100, "El nivel 100 debe existir");
        assertEquals(Recompensa.Tipo.POKEMON, r100.tipo(), "El nivel 100 debe ser un Pokémon");
        assertEquals("charizard", r100.id(), "Debe ser Charizard");
        assertEquals(50, r100.nivel(), "Debe ser nivel 50");
        assertTrue(r100.shiny(), "El Charizard del nivel 100 DEBE ser Shiny");
        assertEquals(Recompensa.ASPECTO_MECHA, r100.aspecto(), "El nivel 100 debe llevar el aspecto mecha");

        List<Recompensa> entregables = PaseCatalogo.entregables(100);
        assertNotNull(entregables, "Entregables del nivel 100 no deben ser nulos");
        assertTrue(entregables.size() >= 2, "El nivel 100 debe entregar al menos 2 recompensas (Charizard + Master Ball)");
        assertTrue(entregables.stream().anyMatch(r -> r.tipo() == Recompensa.Tipo.POKEMON && r.shiny() && r.nivel() == 50),
                "Debe entregar el Charizard Shiny nivel 50");
        assertTrue(entregables.stream().anyMatch(r -> "cobblemon:master_ball".equals(r.id()) && r.cantidad() >= 1),
                "Debe entregar 1 Master Ball adicional");
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
