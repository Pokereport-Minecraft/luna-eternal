package net.pokereport.luna.santuario;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

public class SanctuaryRedirectSsrfTest {

    @Test
    @DisplayName("Bloqueo de resolución de redirección a IP privada / loopback")
    void testRedireccionPrivadaBloqueada() throws Exception {
        // Simular intento de redirección hacia loopback o metadata
        URI base = new URI("https://ejemplo-externo.com/api/imagen");
        URI destinoPeligroso = base.resolve("http://127.0.0.1/admin/secret.png");
        
        var validacionDestino = SanctuaryImagePipeline.validateUrl(destinoPeligroso.toString());
        assertFalse(validacionDestino.valid(), "El destino de redirección a 127.0.0.1 debe ser rechazado");
        assertTrue(validacionDestino.error().contains("ssrf_blocked_ip"));

        URI destinoMetadata = base.resolve("http://169.254.169.254/latest/meta-data/");
        var validacionMeta = SanctuaryImagePipeline.validateUrl(destinoMetadata.toString());
        assertFalse(validacionMeta.valid(), "El destino de redirección a cloud metadata debe ser rechazado");
        assertTrue(validacionMeta.error().contains("ssrf_blocked_ip"));
    }

    @Test
    @DisplayName("fetchAndProcessAsync rechaza URLs maliciosas sin iniciar conexiones inseguras")
    void testFetchUrlInseguraRechazada() throws Exception {
        CompletableFuture<SanctuaryImagePipeline.ProcessResult> future =
                SanctuaryImagePipeline.fetchAndProcessAsync("http://127.0.0.1:8080/exploit.png");
        SanctuaryImagePipeline.ProcessResult res = future.get();
        assertFalse(res.success(), "La descarga no debe ser exitosa para IPs locales");
        assertNotNull(res.error());
        assertTrue(res.error().contains("ssrf_blocked_ip") || res.error().contains("blocked"));
    }
}
