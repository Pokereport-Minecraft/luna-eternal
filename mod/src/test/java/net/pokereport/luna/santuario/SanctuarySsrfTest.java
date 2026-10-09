package net.pokereport.luna.santuario;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.*;

public class SanctuarySsrfTest {

    @Test
    @DisplayName("Bloqueo de esquemas peligrosos no HTTP/HTTPS")
    void testEsquemasPeligrosos() {
        var res1 = SanctuaryImagePipeline.validateUrl("file:///etc/passwd");
        assertFalse(res1.valid());
        assertTrue(res1.error().contains("forbidden_scheme"));

        var res2 = SanctuaryImagePipeline.validateUrl("ftp://ftp.example.com/image.png");
        assertFalse(res2.valid());
        assertTrue(res2.error().contains("forbidden_scheme"));

        var res3 = SanctuaryImagePipeline.validateUrl("jar:file:/app.jar!/icon.png");
        assertFalse(res3.valid());
        assertTrue(res3.error().contains("forbidden_scheme"));
    }

    @Test
    @DisplayName("Bloqueo de localhost y nombres de host locales")
    void testLocalhost() {
        var res1 = SanctuaryImagePipeline.validateUrl("http://localhost/image.png");
        assertFalse(res1.valid());
        assertTrue(res1.error().contains("ssrf_blocked_host"));

        var res2 = SanctuaryImagePipeline.validateUrl("http://app.localhost:8080/image.png");
        assertFalse(res2.valid());
        assertTrue(res2.error().contains("ssrf_blocked_host"));

        var res3 = SanctuaryImagePipeline.validateUrl("http://server.local/photo.jpg");
        assertFalse(res3.valid());
        assertTrue(res3.error().contains("ssrf_blocked_host"));
    }

    @Test
    @DisplayName("Bloqueo de direcciones IP privadas y loopback (RFC 1918)")
    void testIpsPrivadas() throws Exception {
        // Loopback 127.0.0.1
        assertFalse(SanctuaryImagePipeline.isSafeIp(InetAddress.getByName("127.0.0.1")));
        assertFalse(SanctuaryImagePipeline.isSafeIp(InetAddress.getByName("127.0.1.1")));

        // 10.0.0.0/8
        assertFalse(SanctuaryImagePipeline.isSafeIp(InetAddress.getByName("10.0.0.1")));
        assertFalse(SanctuaryImagePipeline.isSafeIp(InetAddress.getByName("10.254.1.1")));

        // 172.16.0.0/12
        assertFalse(SanctuaryImagePipeline.isSafeIp(InetAddress.getByName("172.16.0.1")));
        assertFalse(SanctuaryImagePipeline.isSafeIp(InetAddress.getByName("172.31.255.254")));

        // 192.168.0.0/16
        assertFalse(SanctuaryImagePipeline.isSafeIp(InetAddress.getByName("192.168.1.1")));
        assertFalse(SanctuaryImagePipeline.isSafeIp(InetAddress.getByName("192.168.100.50")));

        // Metadata cloud 169.254.169.254
        assertFalse(SanctuaryImagePipeline.isSafeIp(InetAddress.getByName("169.254.169.254")));

        // IPv6 Loopback ::1
        assertFalse(SanctuaryImagePipeline.isSafeIp(InetAddress.getByName("::1")));
    }

    @Test
    @DisplayName("Permitir IPs públicas legítimas")
    void testIpsPublicasLegitimas() throws Exception {
        // 8.8.8.8 (Google DNS)
        assertTrue(SanctuaryImagePipeline.isSafeIp(InetAddress.getByName("8.8.8.8")));
        // 1.1.1.1 (Cloudflare DNS)
        assertTrue(SanctuaryImagePipeline.isSafeIp(InetAddress.getByName("1.1.1.1")));
    }
}
