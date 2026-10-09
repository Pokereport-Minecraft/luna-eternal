package net.pokereport.luna.cards;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CardsNpcInteractionTest {

    @Test
    @DisplayName("CardsNpc constantes y etiquetas de identificación")
    void testCardsNpcConstantes() {
        assertEquals("luna_npc_cartas", CardsNpc.TAG_CARDS_NPC);
        assertEquals("Coleccionista de Cartas", CardsNpc.NOMBRE);
        assertEquals(29.690, CardsNpc.POSICION.x, 0.001);
        assertEquals(72.0, CardsNpc.POSICION.y, 0.001);
        assertEquals(69.037, CardsNpc.POSICION.z, 0.001);
        assertEquals(180.0f, CardsNpc.YAW, 0.01f);
    }
}
