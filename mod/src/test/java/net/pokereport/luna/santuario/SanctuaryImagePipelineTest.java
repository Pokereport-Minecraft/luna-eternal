package net.pokereport.luna.santuario;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

public class SanctuaryImagePipelineTest {

    @Test
    @DisplayName("Procesamiento y reescalado asíncrono de imagen válida")
    void testProcesamientoImagenValida() throws Exception {
        // Crear imagen sintética de 600x600 (mayor a TARGET_MAX_DIMENSION de 512)
        BufferedImage img = new BufferedImage(600, 600, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", baos);
        byte[] bytes = baos.toByteArray();

        CompletableFuture<SanctuaryImagePipeline.ProcessResult> future = SanctuaryImagePipeline.processAsync(bytes);
        SanctuaryImagePipeline.ProcessResult res = future.get();

        assertTrue(res.success(), "El procesamiento debe ser exitoso");
        assertNotNull(res.data(), "Los datos procesados no deben ser nulos");
        assertNotNull(res.sha256(), "Debe generar hash SHA-256");
        assertEquals(64, res.sha256().length(), "SHA-256 debe tener 64 caracteres");
        assertNotNull(res.sha1(), "Debe generar hash SHA-1");
        assertEquals(40, res.sha1().length(), "SHA-1 debe tener 40 caracteres");

        // Verificar que la imagen resultante no sobrepasa 512x512
        BufferedImage resultado = ImageIO.read(new java.io.ByteArrayInputStream(res.data()));
        assertNotNull(resultado);
        assertTrue(resultado.getWidth() <= 512);
        assertTrue(resultado.getHeight() <= 512);
    }
}
