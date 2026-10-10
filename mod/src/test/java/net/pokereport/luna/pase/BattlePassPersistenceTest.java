package net.pokereport.luna.pase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BattlePassPersistenceTest {

    @Test
    @DisplayName("Invariante de paquetes de recompensas para nivel 100")
    void testPaqueteNivel100() {
        Recompensa r100 = PaseCatalogo.de(100);
        assertTrue(PaseCatalogo.esPaquete(r100), "El premio del nivel 100 debe reconocerse como paquete");

        List<Recompensa> entregables = PaseCatalogo.entregables(100);
        assertEquals(3, entregables.size(), "Nivel 100 entrega los tres componentes del kit");
        assertTrue(entregables.contains(r100), "Debe contener las Master Balls del catálogo");
    }

    @Test
    @DisplayName("Nivel intermedio no es paquete y entrega únicamente su recompensa")
    void testNivelIntermedioNoEsPaquete() {
        Recompensa r5 = PaseCatalogo.de(5);
        assertFalse(PaseCatalogo.esPaquete(r5), "El nivel 5 no debe ser paquete");

        List<Recompensa> entregables = PaseCatalogo.entregables(5);
        assertEquals(1, entregables.size(), "Nivel 5 entrega únicamente 1 elemento");
        assertEquals(r5, entregables.get(0), "El entregable debe ser idéntico al catálogo");
    }
}
