package net.pokereport.luna.santuario;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class SanctuaryImageTimeoutTest {

    @Test
    @DisplayName("Cancelación de transmisión activa es idempotente y no lanza excepciones")
    void testCancelacionTransmision() {
        UUID jugador = UUID.randomUUID();
        assertDoesNotThrow(() -> SanctuaryImagePipeline.cancelActiveTransmission(jugador));
        assertDoesNotThrow(() -> SanctuaryImagePipeline.cancelActiveTransmission(jugador));
    }
}
