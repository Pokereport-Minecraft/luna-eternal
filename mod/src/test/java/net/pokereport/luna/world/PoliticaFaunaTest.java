package net.pokereport.luna.world;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class PoliticaFaunaTest {
    @Test void soloMobsVanillaNoPersonalizados() {
        assertTrue(PoliticaFauna.bloquear("minecraft",true,false,false,false));
        assertTrue(PoliticaFauna.bloquear("minecraft",true,false,false,true));
        assertFalse(PoliticaFauna.bloquear("minecraft",false,false,false,false));
        assertFalse(PoliticaFauna.bloquear("minecraft",true,true,false,false));
        assertFalse(PoliticaFauna.bloquear("cobblemon",true,false,false,false));
        assertFalse(PoliticaFauna.bloquear("mi_mod",true,false,false,false));
    }
    @Test void conservaNpcDeServicios() {
        assertTrue(PoliticaFauna.bloquear("rctmod",true,false,true,false));
        assertFalse(PoliticaFauna.bloquear("rctmod",true,false,true,true));
        assertFalse(PoliticaFauna.bloquear("rctmod",true,true,true,false));
    }
    @Test void densidadDiferenteSinAfectarOtrasDimensiones() {
        assertEquals(0.04f,PoliticaFauna.densidad("minecraft:overworld"));
        for(String id:new String[]{"salvaje","salvaje2","salvaje3","salvaje4","salvaje5","salvaje6"})
            assertEquals(0.75f,PoliticaFauna.densidad("lunaeternal:"+id));
        assertEquals(1f,PoliticaFauna.densidad("lunaeternal:ciudadela"));
        assertEquals(1f,PoliticaFauna.densidad("otro:salvaje"));

        assertTrue(PoliticaFauna.permitirPokemon("minecraft:overworld", 1, false, java.util.Set.of()));
        assertTrue(PoliticaFauna.permitirPokemon("minecraft:overworld", 251, false, java.util.Set.of()));
        assertFalse(PoliticaFauna.permitirPokemon("minecraft:overworld", 252, false, java.util.Set.of()));
        assertFalse(PoliticaFauna.permitirPokemon("lunaeternal:salvaje", 252, false, java.util.Set.of()));
        assertFalse(PoliticaFauna.permitirPokemon("minecraft:overworld", 25, true, java.util.Set.of()));
        assertFalse(PoliticaFauna.permitirPokemon("minecraft:overworld", 150, false,
                java.util.Set.of("legendary")));
        assertFalse(PoliticaFauna.permitirPokemon("minecraft:overworld", 151, false,
                java.util.Set.of("mythical")));
        assertTrue(PoliticaFauna.permitirPokemon("lunaeternal:salvaje", 150, false,
                java.util.Set.of("legendary")));
        assertTrue(PoliticaFauna.permitirPokemon("lunaeternal:salvaje6", 25, true,
                java.util.Set.of()));
        assertEquals(1.10f, PoliticaFauna.MULTIPLICADOR_ULTRARRARO_SALVAJE);
    }
    
    @Test
    public void testGen12Permitida() {
        // Pikachu (25) -> Raichu (26)
        assertTrue(PoliticaFauna.generacionPermitida(26, null, null));
        // Golbat (42) -> Crobat (169)
        assertTrue(PoliticaFauna.generacionPermitida(169, null, null));
        // Onix (95) -> Steelix (208)
        assertTrue(PoliticaFauna.generacionPermitida(208, null, null));
        // Scyther (123) -> Scizor (212)
        assertTrue(PoliticaFauna.generacionPermitida(212, null, null));
        // Eevee (133) -> Espeon (196) / Umbreon (197)
        assertTrue(PoliticaFauna.generacionPermitida(196, null, null));
        assertTrue(PoliticaFauna.generacionPermitida(197, null, null));
    }

    @Test
    public void testGen3PlusBloqueada() {
        // Magneton -> Magnezone (462)
        assertFalse(PoliticaFauna.generacionPermitida(462, null, null));
        // Rhydon -> Rhyperior (464)
        assertFalse(PoliticaFauna.generacionPermitida(464, null, null));
        // Electabuzz -> Electivire (466)
        assertFalse(PoliticaFauna.generacionPermitida(466, null, null));
        // Magmar -> Magmortar (467)
        assertFalse(PoliticaFauna.generacionPermitida(467, null, null));
        // Togetic -> Togekiss (468)
        assertFalse(PoliticaFauna.generacionPermitida(468, null, null));
        // Porygon2 -> Porygon-Z (474)
        assertFalse(PoliticaFauna.generacionPermitida(474, null, null));
        // Eevee -> Leafeon (470), Glaceon (471), Sylveon (700)
        assertFalse(PoliticaFauna.generacionPermitida(470, null, null));
        assertFalse(PoliticaFauna.generacionPermitida(471, null, null));
        assertFalse(PoliticaFauna.generacionPermitida(700, null, null));
    }

    @Test
    public void testFormasRegionalesBloqueadas() {
        // Pikachu -> Alolan Raichu (26, form="alola")
        assertFalse(PoliticaFauna.generacionPermitida(26, "alola", null));
        // Alolan Raichu with label "gen7"
        assertFalse(PoliticaFauna.generacionPermitida(26, null, Set.of("gen7", "alola_form")));
        // Exeggcute -> Alolan Exeggutor (103, "alola")
        assertFalse(PoliticaFauna.generacionPermitida(103, "alola", null));
        assertFalse(PoliticaFauna.generacionPermitida(103, null, Set.of("gen7", "alola")));
        
        // Koffing -> Galarian Weezing
        assertFalse(PoliticaFauna.generacionPermitida(110, "galar", null));
        assertFalse(PoliticaFauna.generacionPermitida(110, null, Set.of("gen8", "galar_form")));
    }
}
