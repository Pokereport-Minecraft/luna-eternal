package net.pokereport.luna.crianza;

import net.pokereport.luna.progression.ProgressionBalanceConfig;
import net.pokereport.luna.ui.Tablist;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BreedingSlotTest {

    @Test
    @DisplayName("F2P base slots: 1 slot inicial, ranura 1 requiere rango Élite")
    void testF2PBaseSlots() {
        // En progresión 2.0 (ProgressionBalanceConfig enabled por defecto)
        // Ranura 0: F2P (escalón 0)
        assertEquals(0, CrianzaService.escalonQuePide(0), "Ranura 0 debe ser gratuita (escalón 0)");
        assertTrue(CrianzaService.estaDesbloqueada(0, 0, false), "Ranura 0 debe estar desbloqueada para jugador F2P");

        // Ranura 1: requiere rango Élite
        assertEquals(Tablist.Rank.ELITE.escalon, CrianzaService.escalonQuePide(1),
                "Ranura 1 debe requerir escalón de rango Élite");
        assertFalse(CrianzaService.estaDesbloqueada(1, 0, false),
                "Ranura 1 NO debe estar desbloqueada para F2P sin rango");

        // Con rango Élite:
        assertTrue(CrianzaService.estaDesbloqueada(1, Tablist.Rank.ELITE.escalon, false),
                "Ranura 1 debe estar desbloqueada para rango Élite");
    }

    @Test
    @DisplayName("Preservación de compras de ranuras con monedas")
    void testPreservacionRanurasCompradas() {
        // Ranuras 2, 3, 4 son comprables con monedas (escalonQuePide = -1)
        assertTrue(CrianzaService.esComprable(2));
        assertTrue(CrianzaService.esComprable(3));
        assertTrue(CrianzaService.esComprable(4));

        // Un jugador F2P (escalon 0) que compró la ranura 2 con monedas no la pierde
        assertTrue(CrianzaService.estaDesbloqueada(2, 0, true),
                "La ranura comprada con monedas DEBE permanecer desbloqueada para F2P");
        assertFalse(CrianzaService.estaDesbloqueada(2, 0, false),
                "La ranura 2 sin comprar no debe estar disponible");
    }

    @Test
    @DisplayName("Ranuras avanzadas por rango Maestro y Leyenda")
    void testRanurasMaestroYLeyenda() {
        assertEquals(Tablist.Rank.MAESTRO.escalon, CrianzaService.escalonQuePide(5));
        assertEquals(Tablist.Rank.LEYENDA.escalon, CrianzaService.escalonQuePide(6));

        assertFalse(CrianzaService.estaDesbloqueada(5, Tablist.Rank.ELITE.escalon, false));
        assertTrue(CrianzaService.estaDesbloqueada(5, Tablist.Rank.MAESTRO.escalon, false));

        assertFalse(CrianzaService.estaDesbloqueada(6, Tablist.Rank.MAESTRO.escalon, false));
        assertTrue(CrianzaService.estaDesbloqueada(6, Tablist.Rank.LEYENDA.escalon, false));
    }
}
