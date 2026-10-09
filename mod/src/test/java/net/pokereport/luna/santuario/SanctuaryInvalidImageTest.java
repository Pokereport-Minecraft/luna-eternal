package net.pokereport.luna.santuario;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SanctuaryInvalidImageTest {

    @Test
    @DisplayName("Validación rechaza bytes nulos o vacíos")
    void testBytesVaciosRechazados() {
        var resNull = SanctuaryImagePipeline.validateImageHeader(null);
        assertFalse(resNull.valid());
        assertEquals("empty_data", resNull.error());

        var resEmpty = SanctuaryImagePipeline.validateImageHeader(new byte[0]);
        assertFalse(resEmpty.valid());
        assertEquals("empty_data", resEmpty.error());
    }

    @Test
    @DisplayName("Validación rechaza archivos mayores a 2.5MB")
    void testArchivoSobredimensionadoRechazado() {
        byte[] grande = new byte[SanctuaryImagePipeline.MAX_FILE_BYTES + 10];
        var res = SanctuaryImagePipeline.validateImageHeader(grande);
        assertFalse(res.valid());
        assertEquals("file_too_large", res.error());
    }

    @Test
    @DisplayName("Validación rechaza datos corruptos o no reconocibles como imagen")
    void testDatosCorruptosRechazados() {
        byte[] basura = "ESTO_NO_ES_UNA_IMAGEN_SINO_TEXTO_ALEATORIO".getBytes();
        var res = SanctuaryImagePipeline.validateImageHeader(basura);
        assertFalse(res.valid());
    }
}
