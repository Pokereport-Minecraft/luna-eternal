package net.pokereport.luna.crianza;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para A09: Herencia de variantes regionales, habilidades ocultas y movimientos huevo.
 */
public class CrianzaVariantesTest {

    @Test
    @DisplayName("Madre con Everstone y aspecto regional transmite su forma al bebe")
    void testDeterminarAspectoRegionalMadreConEverstone() {
        Set<String> aspectosMadre = Set.of("alolan");
        Set<String> aspectosPadre = Set.of();

        String aspecto = CrianzaReglas.determinarAspectoRegional(
                aspectosMadre, true, false,
                aspectosPadre, false, false,
                0.1
        );

        assertEquals("alolan", aspecto, "La cria debe heredar la forma Alola de la madre con Everstone");
    }

    @Test
    @DisplayName("Madre regional sin Everstone no transmite forma (revierte a forma canonica)")
    void testDeterminarAspectoRegionalSinEverstoneRevierteAComun() {
        Set<String> aspectosMadre = Set.of("alolan");
        Set<String> aspectosPadre = Set.of();

        String aspecto = CrianzaReglas.determinarAspectoRegional(
                aspectosMadre, false, false,
                aspectosPadre, false, false,
                0.1
        );

        assertNull(aspecto, "Sin Everstone, la cria debe revertir a la forma base estandar");
    }

    @Test
    @DisplayName("Padre regional con Everstone y Ditto transmite su forma regional")
    void testDeterminarAspectoRegionalPadreConDittoYEverstone() {
        Set<String> aspectosMadre = Set.of();
        Set<String> aspectosPadre = Set.of("galarian");

        String aspecto = CrianzaReglas.determinarAspectoRegional(
                aspectosMadre, false, true, // Madre es Ditto
                aspectosPadre, true, false,  // Padre con Everstone
                0.1
        );

        assertEquals("galarian", aspecto, "Con Ditto, el padre con Everstone debe transmitir su forma Galar");
    }

    @Test
    @DisplayName("Ditto con Everstone no transmite aspectos como si fuera progenitor de especie")
    void testDeterminarAspectoRegionalDittoNoTransmiteAspecto() {
        Set<String> aspectosMadre = Set.of("alolan");
        Set<String> aspectosPadre = Set.of();

        String aspecto = CrianzaReglas.determinarAspectoRegional(
                aspectosMadre, true, true,  // Madre es Ditto con aspecto hipotetico
                aspectosPadre, false, false, // Padre sin Everstone
                0.1
        );

        assertNull(aspecto, "Ditto no debe transmitir su aspecto regional a la cria");
    }

    @Test
    @DisplayName("Ambos progenitores con Everstone y formas distintas resuelven 50/50")
    void testDeterminarAspectoRegionalAmbosConEverstone() {
        Set<String> aspectosMadre = Set.of("alolan");
        Set<String> aspectosPadre = Set.of("galarian");

        String aspectoMadre = CrianzaReglas.determinarAspectoRegional(
                aspectosMadre, true, false,
                aspectosPadre, true, false,
                0.2 // RNG < 0.5 -> Madre
        );
        assertEquals("alolan", aspectoMadre);

        String aspectoPadre = CrianzaReglas.determinarAspectoRegional(
                aspectosMadre, true, false,
                aspectosPadre, true, false,
                0.8 // RNG >= 0.5 -> Padre
        );
        assertEquals("galarian", aspectoPadre);
    }

    @Test
    @DisplayName("Aspectos no regionales (cosmeticos, sombrios, etc.) no son transmitidos por Everstone")
    void testAspectosNoRegionalesNoHeredan() {
        Set<String> aspectosMadre = Set.of("cosmetico_sombrero", "gigante", "custom_skin");

        String aspecto = CrianzaReglas.determinarAspectoRegional(
                aspectosMadre, true, false,
                Set.of(), false, false,
                0.5
        );

        assertNull(aspecto, "Solo aspectos regionales (alolan, galarian, hisuian, paldean) deben ser heredables");
    }

    @Test
    @DisplayName("Madre con Habilidad Oculta puede transmitirla")
    void testHerenciaHabilidadOcultaMadre() {
        boolean puede = CrianzaReglas.puedeTransmitirHabilidadOculta(true, false, false, false);
        assertTrue(puede, "La madre con Habilidad Oculta siempre puede transmitirla");
    }

    @Test
    @DisplayName("Padre con Habilidad Oculta NO puede transmitirla sin Ditto")
    void testHerenciaHabilidadOcultaPadreSinDittoNoTransmite() {
        boolean puede = CrianzaReglas.puedeTransmitirHabilidadOculta(false, true, false, false);
        assertFalse(puede, "El padre con Habilidad Oculta en pareja normal no transmite su HO (regla canonica)");
    }

    @Test
    @DisplayName("Padre con Habilidad Oculta si puede transmitirla si cria con Ditto")
    void testHerenciaHabilidadOcultaPadreConDittoTransmite() {
        boolean puede = CrianzaReglas.puedeTransmitirHabilidadOculta(false, true, true, false);
        assertTrue(puede, "El macho o sin genero transmite su HO cuando cria con Ditto");
    }

    @Test
    @DisplayName("Ningun progenitor con HO no genera posibilidad de transmitir HO")
    void testHerenciaHabilidadOcultaNingunoNoTransmite() {
        boolean puede = CrianzaReglas.puedeTransmitirHabilidadOculta(false, false, false, false);
        assertFalse(puede, "Sin progenitores con HO no debe haber posibilidad de heredarla");
    }

    @Test
    @DisplayName("Filtrado de movimientos huevo de ambos padres sin duplicados y solo movimientos legales")
    void testFiltrarMovimientosHuevo() {
        List<String> movsPadre = List.of("Hydro Pump", "Tackle", "Aqua Jet");
        List<String> movsMadre = List.of("Growl", "Aqua Jet", "Belly Drum");
        Set<String> eggMovesValidos = Set.of("aqua jet", "belly drum", "dragon dance");

        List<String> heredados = CrianzaReglas.filtrarMovimientosHuevo(movsMadre, movsPadre, eggMovesValidos);

        assertEquals(2, heredados.size(), "Debe heredar exactamente 2 movimientos huevo validos");
        assertTrue(heredados.contains("Aqua Jet"), "Debe heredar Aqua Jet");
        assertTrue(heredados.contains("Belly Drum"), "Debe heredar Belly Drum");
        assertFalse(heredados.contains("Hydro Pump"), "Hydro Pump no es egg move");
        assertFalse(heredados.contains("Tackle"), "Tackle no es egg move");
        assertFalse(heredados.contains("Growl"), "Growl no es egg move");
    }

    @Test
    @DisplayName("Madre con Brazal Recio (power_bracer) garantiza herencia de Ataque de la madre")
    void testPowerBracerMadreGarantizaAtaque() {
        var rng = new java.util.Random(12345L);
        var herencia = CrianzaReglas.resolverHerenciaIVs("power_bracer", null, rng);

        assertEquals(3, herencia.size(), "Debe heredar 3 estadísticas sin Lazo Destino");
        assertTrue(herencia.containsKey("attack"), "Ataque debe estar en las estadísticas heredadas");
        assertTrue(herencia.get("attack"), "Ataque debe provenir 100% de la madre con Brazal Recio");
    }

    @Test
    @DisplayName("Padre con Franja Recia (power_anklet) garantiza herencia de Velocidad del padre")
    void testPowerAnkletPadreGarantizaVelocidad() {
        var rng = new java.util.Random(54321L);
        var herencia = CrianzaReglas.resolverHerenciaIVs(null, "cobblemon:power_anklet", rng);

        assertEquals(3, herencia.size(), "Debe heredar 3 estadísticas sin Lazo Destino");
        assertTrue(herencia.containsKey("speed"), "Velocidad debe estar en las estadísticas heredadas");
        assertFalse(herencia.get("speed"), "Velocidad debe provenir 100% del padre con Franja Recia");
    }

    @Test
    @DisplayName("Madre con Brazal Recio y Padre con Lazo Destino garantiza Ataque de madre y 5 stats en total")
    void testPowerBracerConLazoDestinoGarantizaAtaqueY5Stats() {
        var rng = new java.util.Random(99999L);
        var herencia = CrianzaReglas.resolverHerenciaIVs("power_bracer", "destiny_knot", rng);

        assertEquals(5, herencia.size(), "Con Lazo Destino debe heredar exactamente 5 estadísticas");
        assertTrue(herencia.containsKey("attack"), "Ataque debe estar garantizado por el Brazal Recio");
        assertTrue(herencia.get("attack"), "Ataque debe provenir de la madre");
    }

    @Test
    @DisplayName("Ambos padres con objetos recios distintos garantizan ambas estadísticas de sus respectivos padres")
    void testAmbosObjetosReciosDistintosGarantizanAmbos() {
        var rng = new java.util.Random(42L);
        // Madre lleva Pesa Recia (hp), Padre lleva Franja Recia (speed)
        var herencia = CrianzaReglas.resolverHerenciaIVs("power_weight", "power_anklet", rng);

        assertEquals(3, herencia.size(), "Debe heredar 3 estadísticas en total");
        assertTrue(herencia.containsKey("hp"), "HP debe estar garantizado");
        assertTrue(herencia.get("hp"), "HP debe provenir de la madre con Pesa Recia");
        assertTrue(herencia.containsKey("speed"), "Velocidad debe estar garantizada");
        assertFalse(herencia.get("speed"), "Velocidad debe provenir del padre con Franja Recia");
    }

    @Test
    @DisplayName("Ambos padres con el mismo objeto recio garantizan la estadística")
    void testAmbosMismoObjetoRecioGarantizaStat() {
        var rng = new java.util.Random(777L);
        var herencia = CrianzaReglas.resolverHerenciaIVs("power_lens", "power_lens", rng);

        assertEquals(3, herencia.size(), "Debe heredar 3 estadísticas en total");
        assertTrue(herencia.containsKey("special_attack"), "Ataque Especial debe estar garantizado");
    }

    @Test
    @DisplayName("Sin objetos equipados hereda exactamente 3 estadísticas aleatorias")
    void testSinObjetosHereda3Stats() {
        var rng = new java.util.Random(888L);
        var herencia = CrianzaReglas.resolverHerenciaIVs(null, null, rng);

        assertEquals(3, herencia.size(), "Sin objetos debe heredar exactamente 3 estadísticas");
    }

    @Test
    @DisplayName("Solo Lazo Destino hereda exactamente 5 estadísticas aleatorias")
    void testSoloLazoDestinoHereda5Stats() {
        var rng = new java.util.Random(111L);
        var herencia = CrianzaReglas.resolverHerenciaIVs("cobblemon:destiny_knot", null, rng);

        assertEquals(5, herencia.size(), "Con Lazo Destino debe heredar exactamente 5 estadísticas");
    }

    @Test
    @DisplayName("Piedra Eterna (everstone) hereda naturaleza correctamente")
    void testEverstoneHeredaNaturaleza() {
        String nat = CrianzaReglas.determinarNaturalezaHeredada("adamant", "everstone", "modest", null, 0.5);
        assertEquals("adamant", nat, "Debe heredar naturaleza Firme de la madre con Everstone");

        String natPadre = CrianzaReglas.determinarNaturalezaHeredada("adamant", null, "modest", "cobblemon:everstone", 0.5);
        assertEquals("modest", natPadre, "Debe heredar naturaleza Modesta del padre con Everstone");

        String sinEver = CrianzaReglas.determinarNaturalezaHeredada("adamant", "power_bracer", "modest", "destiny_knot", 0.5);
        assertNull(sinEver, "Sin Everstone no debe forzar naturaleza (retorna null para dejar la aleatoria)");
    }
}
