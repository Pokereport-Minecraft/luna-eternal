package net.pokereport.luna.spectate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class StaffSpectateReconnectTest {

    @Test
    @DisplayName("Detener espectador con jugador nulo es seguro y devuelve false")
    void testStopSpectateJugadorNulo() {
        assertFalse(StaffSpectateService.stopSpectate(null));
        assertFalse(StaffSpectateService.stopSpectate(null, false));
    }
}
