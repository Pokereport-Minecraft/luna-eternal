package net.pokereport.luna.santuario;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SanctuaryImageStressTest {

    @Test
    @DisplayName("Invariantes de streaming y control de flujo en la transmisión")
    void testConstantesStreaming() {
        assertEquals(16 * 1024, SanctuaryImagePipeline.CHUNK_SIZE, "Trozo debe ser exactamente 16KB");
        assertEquals(3, SanctuaryImagePipeline.CHUNKS_PER_BURST, "Ráfaga máxima de 3 trozos por burst");
        assertTrue(SanctuaryImagePipeline.BURST_INTERVAL_MS >= 20, "Intervalo de al menos 20ms para evitar desbordar Netty");
        assertEquals(2_500_000, SanctuaryImagePipeline.MAX_FILE_BYTES, "Máximo archivo de 2.5MB");
        assertEquals(512, SanctuaryImagePipeline.TARGET_MAX_DIMENSION, "Lado máximo normalizado de 512px");
    }
}
