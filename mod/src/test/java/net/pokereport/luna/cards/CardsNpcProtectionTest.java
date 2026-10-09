package net.pokereport.luna.cards;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CardsNpcProtectionTest {

    @Test
    @DisplayName("CardsNpc dimensiones y protección de ciudadela")
    void testCardsNpcUbicacionCiudadela() {
        assertNotNull(CardsNpc.POSICION);
        assertTrue(CardsNpc.POSICION.y >= 70, "Y debe estar al nivel del suelo de Ciudadela");
        assertEquals(180.0f, CardsNpc.YAW, "Debe mirar hacia el norte (180 grados)");
    }
}
