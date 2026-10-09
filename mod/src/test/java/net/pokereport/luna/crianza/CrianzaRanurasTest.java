package net.pokereport.luna.crianza;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CrianzaRanurasTest {
    @Test void lasDosPrimerasSonGratuitas() {
        for (int i = 0; i < 2; i++) assertTrue(CrianzaRanuras.disponible(i, 0, false, 0));
    }
    @Test void lasTresExpansionesExigenCompraInclusoConLeyenda() {
        for (int i = 2; i <= 4; i++) {
            assertFalse(CrianzaRanuras.disponible(i, 5, false, -1));
            assertTrue(CrianzaRanuras.disponible(i, 0, true, -1));
        }
    }
    @Test void comprasAntiguasNoSustituyenMaestroNiLeyenda() {
        assertFalse(CrianzaRanuras.disponible(5, 3, true, 4));
        assertTrue(CrianzaRanuras.disponible(5, 4, false, 4));
        assertTrue(CrianzaRanuras.disponible(5, 5, false, 4));
        assertFalse(CrianzaRanuras.disponible(6, 4, true, 5));
        assertTrue(CrianzaRanuras.disponible(6, 5, false, 5));
    }
    @Test void indicesInvalidosYStaffSinRangoNoDesbloqueanBeneficios() {
        assertFalse(CrianzaRanuras.disponible(-1, 5, true, 0));
        assertFalse(CrianzaRanuras.disponible(7, 5, true, 0));
        assertFalse(CrianzaRanuras.disponible(5, -1, true, 4));
        assertFalse(CrianzaRanuras.disponible(6, -1, true, 5));
    }
}
