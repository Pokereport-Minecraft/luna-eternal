package net.pokereport.luna.progression;

import net.pokereport.luna.world.PoliticaFauna;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class SpawnBalanceTest {

    @Test
    @DisplayName("Identificación de categorías de rareza de Pokémon Gen 1-2")
    void testIdentificacionCategorias() {
        // Iniciales: Charmander (4), Cyndaquil (155)
        assertTrue(PoliticaFauna.esInicial(4), "Charmander debe identificarse como Inicial");
        assertTrue(PoliticaFauna.esInicial(155), "Cyndaquil debe identificarse como Inicial");
        assertFalse(PoliticaFauna.esInicial(16), "Pidgey no es inicial");

        // Pseudolegendarios: Dratini (147), Dragonite (149), Larvitar (246), Tyranitar (248)
        assertTrue(PoliticaFauna.esPseudolegendario(147), "Dratini debe identificarse como Pseudolegendario");
        assertTrue(PoliticaFauna.esPseudolegendario(149), "Dragonite debe identificarse como Pseudolegendario");
        assertTrue(PoliticaFauna.esPseudolegendario(246), "Larvitar debe identificarse como Pseudolegendario");
        assertTrue(PoliticaFauna.esPseudolegendario(248), "Tyranitar debe identificarse como Pseudolegendario");
        assertFalse(PoliticaFauna.esPseudolegendario(25), "Pikachu no es pseudolegendario");

        // Legendarios / Míticos: Mewtwo (150), Mew (151), Lugia (249), Celebi (251)
        assertTrue(PoliticaFauna.esLegendarioOMitico(150, Set.of("legendary")), "Mewtwo debe identificarse como Legendario");
        assertTrue(PoliticaFauna.esLegendarioOMitico(151, Set.of("mythical")), "Mew debe identificarse como Mítico");
        assertTrue(PoliticaFauna.esLegendarioOMitico(249, Set.of()), "Lugia debe identificarse como Legendario");
        assertTrue(PoliticaFauna.esLegendarioOMitico(251, Set.of()), "Celebi debe identificarse como Mítico");
        assertFalse(PoliticaFauna.esLegendarioOMitico(130, Set.of()), "Gyarados no es legendario");

        // Raros: Lapras (131), Snorlax (143), Chansey (113), Blissey (242)
        assertTrue(PoliticaFauna.esRaro(131, Set.of()), "Lapras es Raro");
        assertTrue(PoliticaFauna.esRaro(143, Set.of()), "Snorlax es Raro");
        assertTrue(PoliticaFauna.esRaro(113, Set.of()), "Chansey es Raro");
        assertTrue(PoliticaFauna.esRaro(242, Set.of()), "Blissey es Raro");
    }

    @Test
    @DisplayName("Multiplicadores de spawn según nivel de rareza")
    void testMultiplicadoresSpawn() {
        var cfg = ProgressionBalanceConfig.get().spawn;

        // Común: Rattata (19)
        double multComun = PoliticaFauna.obtenerMultiplicadorSpawn(19, "rattata", Set.of());
        assertEquals(cfg.commonMultiplier, multComun, 0.001, "Común debe tener multiplicador 1.0");

        // Raro: Snorlax (143)
        double multRaro = PoliticaFauna.obtenerMultiplicadorSpawn(143, "snorlax", Set.of());
        assertEquals(cfg.rareMultiplier, multRaro, 0.001, "Raro debe tener multiplicador 0.65");

        // Inicial: Charmander (4)
        double multInicial = PoliticaFauna.obtenerMultiplicadorSpawn(4, "charmander", Set.of());
        assertEquals(cfg.starterMultiplier, multInicial, 0.001, "Inicial debe tener multiplicador 0.35");

        // Pseudo: Dratini (147)
        double multPseudo = PoliticaFauna.obtenerMultiplicadorSpawn(147, "dratini", Set.of());
        assertEquals(cfg.pseudoLegendaryMultiplier, multPseudo, 0.001, "Pseudolegendario debe tener multiplicador 0.20");

        // Legendario: Mewtwo (150)
        double multLegendario = PoliticaFauna.obtenerMultiplicadorSpawn(150, "mewtwo", Set.of("legendary"));
        assertEquals(cfg.legendaryMultiplier, multLegendario, 0.001, "Legendario debe tener multiplicador 0.05");
    }
}
