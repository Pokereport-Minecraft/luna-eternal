package net.pokereport.luna.santuario;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.pokereport.luna.LunaEternal;
import net.pokereport.luna.net.Red;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.*;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Pipeline Asíncrono de Procesamiento, Transmisión y Descarga Segura de Imágenes para el Santuario.
 *
 * Soluciona las causas raíz de desconexión, saturación y vulnerabilidades:
 * 1. Protección Anti-SSRF: Bloquea loopback, redes privadas RFC1918, link-local (169.254.169.254),
 *    esquemas peligrosos (file:, ftp:, jar:) y valida cada salto en redirecciones HTTP.
 * 2. Descompresión segura (anti zip-bomb / pixel-bomb): valida cabeceras y dimensiones máximas antes de decodificar.
 * 3. Procesamiento fuera del hilo principal (Worker Thread Pool dedicado).
 * 4. Transmisión con control de flujo (Paced Packet Streaming): no satura los buffers de Netty enviando
 *    paquetes de 16KB en ráfagas dosificadas protegiendo la conexión de los clientes.
 * 5. Caché SHA-256 e idempotencia de subidas.
 */
public final class SanctuaryImagePipeline {

    public static final int MAX_FILE_BYTES = 2_500_000;
    public static final int MAX_INPUT_DIMENSION = 4096;
    public static final long MAX_UNCOMPRESSED_PIXELS = 16_777_216L; // 16 megapixels max
    public static final int TARGET_MAX_DIMENSION = 512;
    public static final int CHUNK_SIZE = 16 * 1024;
    public static final int CHUNKS_PER_BURST = 3;
    public static final long BURST_INTERVAL_MS = 25L;

    private static final ScheduledExecutorService ASYNC_POOL = Executors.newScheduledThreadPool(4, r -> {
        Thread t = new Thread(r, "Sanctuary-Image-Worker");
        t.setDaemon(true);
        return t;
    });

    private static final Map<String, byte[]> PROCESSED_CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, ScheduledFuture<?>> ACTIVE_TRANSMISSIONS = new ConcurrentHashMap<>();

    private SanctuaryImagePipeline() {}

    public record ValidationResult(boolean valid, String error, int width, int height) {
        public static ValidationResult ok(int w, int h) { return new ValidationResult(true, null, w, h); }
        public static ValidationResult fail(String err) { return new ValidationResult(false, err, 0, 0); }
    }

    public record ProcessResult(boolean success, String error, byte[] data, String sha256, String sha1) {}

    /**
     * Valida si una dirección IP pertenece a rangos seguros (Internet pública) o si es interna/SSRF.
     */
    public static boolean isSafeIp(InetAddress addr) {
        if (addr == null) return false;
        if (addr.isAnyLocalAddress() || addr.isLoopbackAddress() || addr.isLinkLocalAddress() || addr.isSiteLocalAddress()) {
            return false;
        }
        if (addr instanceof Inet4Address ip4) {
            byte[] b = ip4.getAddress();
            int b0 = b[0] & 0xFF;
            int b1 = b[1] & 0xFF;
            // 0.0.0.0/8
            if (b0 == 0) return false;
            // 10.0.0.0/8
            if (b0 == 10) return false;
            // 127.0.0.0/8
            if (b0 == 127) return false;
            // 169.254.0.0/16 (incluye metadata cloud 169.254.169.254)
            if (b0 == 169 && b1 == 254) return false;
            // 172.16.0.0/12
            if (b0 == 172 && (b1 >= 16 && b1 <= 31)) return false;
            // 192.168.0.0/16
            if (b0 == 192 && b1 == 168) return false;
            // 100.64.0.0/10 (CGNAT)
            if (b0 == 100 && (b1 >= 64 && b1 <= 127)) return false;
            // 198.18.0.0/15
            if (b0 == 198 && (b1 >= 18 && b1 <= 19)) return false;
            // 224.0.0.0/4 y 240.0.0.0/4
            if (b0 >= 224) return false;
        } else if (addr instanceof Inet6Address ip6) {
            byte[] b = ip6.getAddress();
            // IPv4 mapped ::ffff:0:0/96
            if (b[0] == 0 && b[1] == 0 && b[2] == 0 && b[3] == 0 && b[4] == 0 && b[5] == 0 &&
                    b[6] == 0 && b[7] == 0 && b[8] == 0 && b[9] == 0 && (b[10] & 0xFF) == 0xFF && (b[11] & 0xFF) == 0xFF) {
                try {
                    byte[] ip4Bytes = Arrays.copyOfRange(b, 12, 16);
                    return isSafeIp(Inet4Address.getByAddress(ip4Bytes));
                } catch (Exception e) {
                    return false;
                }
            }
            // Unique local fc00::/7
            int firstByte = b[0] & 0xFF;
            if ((firstByte & 0xFE) == 0xFC) return false;
            // Link local fe80::/10
            if (firstByte == 0xFE && (b[1] & 0xC0) == 0x80) return false;
        }
        return true;
    }

    /**
     * Valida una URL para prevenir ataques SSRF.
     */
    public static ValidationResult validateUrl(String urlString) {
        if (urlString == null || urlString.isBlank()) {
            return ValidationResult.fail("empty_url");
        }
        URI uri;
        try {
            uri = new URI(urlString);
        } catch (URISyntaxException e) {
            return ValidationResult.fail("malformed_url: " + e.getMessage());
        }

        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            return ValidationResult.fail("forbidden_scheme: only http/https allowed");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            return ValidationResult.fail("missing_host");
        }

        String lowerHost = host.toLowerCase(java.util.Locale.ROOT);
        if (lowerHost.equals("localhost") || lowerHost.endsWith(".localhost") || lowerHost.endsWith(".local") || lowerHost.endsWith(".internal")) {
            return ValidationResult.fail("ssrf_blocked_host: " + host);
        }

        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            if (addresses == null || addresses.length == 0) {
                return ValidationResult.fail("unresolvable_host");
            }
            for (InetAddress addr : addresses) {
                if (!isSafeIp(addr)) {
                    return ValidationResult.fail("ssrf_blocked_ip: " + addr.getHostAddress());
                }
            }
        } catch (UnknownHostException e) {
            return ValidationResult.fail("unknown_host: " + host);
        }

        return ValidationResult.ok(0, 0);
    }

    /**
     * Descarga y procesa de forma asíncrona una imagen desde una URL externa,
     * aplicando validación SSRF estricta en la URL inicial y en cada salto de redirección.
     */
    public static CompletableFuture<ProcessResult> fetchAndProcessAsync(String urlString) {
        return CompletableFuture.supplyAsync(() -> {
            String currentUrl = urlString;
            int redirects = 0;
            final int maxRedirects = 5;

            while (redirects < maxRedirects) {
                ValidationResult vr = validateUrl(currentUrl);
                if (!vr.valid()) {
                    return new ProcessResult(false, vr.error(), null, null, null);
                }

                try {
                    URL url = new URI(currentUrl).toURL();
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setInstanceFollowRedirects(false); // Validación manual de cada redirección
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    conn.setRequestProperty("User-Agent", "PokeReport-Sanctuary-ImagePipeline/1.0");

                    int code = conn.getResponseCode();
                    if (code == HttpURLConnection.HTTP_MOVED_PERM || code == HttpURLConnection.HTTP_MOVED_TEMP
                            || code == HttpURLConnection.HTTP_SEE_OTHER || code == 307 || code == 308) {
                        String loc = conn.getHeaderField("Location");
                        if (loc == null || loc.isBlank()) {
                            return new ProcessResult(false, "redirect_missing_location", null, null, null);
                        }
                        URI target = new URI(currentUrl).resolve(loc);
                        currentUrl = target.toString();
                        redirects++;
                        conn.disconnect();
                        continue;
                    }

                    if (code != HttpURLConnection.HTTP_OK) {
                        conn.disconnect();
                        return new ProcessResult(false, "http_status_" + code, null, null, null);
                    }

                    int contentLength = conn.getContentLength();
                    if (contentLength > MAX_FILE_BYTES) {
                        conn.disconnect();
                        return new ProcessResult(false, "file_too_large", null, null, null);
                    }

                    byte[] data;
                    try (InputStream in = conn.getInputStream();
                         ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                        byte[] buffer = new byte[8192];
                        int totalRead = 0;
                        int read;
                        while ((read = in.read(buffer)) != -1) {
                            totalRead += read;
                            if (totalRead > MAX_FILE_BYTES) {
                                conn.disconnect();
                                return new ProcessResult(false, "file_too_large", null, null, null);
                            }
                            baos.write(buffer, 0, read);
                        }
                        data = baos.toByteArray();
                    } finally {
                        conn.disconnect();
                    }

                    return processAsync(data).join();
                } catch (Exception e) {
                    return new ProcessResult(false, "download_exception: " + e.getMessage(), null, null, null);
                }
            }
            return new ProcessResult(false, "too_many_redirects", null, null, null);
        }, ASYNC_POOL);
    }

    /**
     * Valida de forma segura las dimensiones del archivo examinando solo los metadatos de cabecera.
     * Evita ataques de agotamiento de memoria por imágenes sobredimensionadas.
     */
    public static ValidationResult validateImageHeader(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return ValidationResult.fail("empty_data");
        }
        if (bytes.length > MAX_FILE_BYTES) {
            return ValidationResult.fail("file_too_large");
        }

        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (in == null) return ValidationResult.fail("unreadable_stream");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                return ValidationResult.fail("unsupported_format");
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(in, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);

                if (width <= 0 || height <= 0) {
                    return ValidationResult.fail("invalid_dimensions");
                }
                if (width > MAX_INPUT_DIMENSION || height > MAX_INPUT_DIMENSION) {
                    return ValidationResult.fail("dimensions_exceed_limit");
                }
                long totalPixels = (long) width * height;
                if (totalPixels > MAX_UNCOMPRESSED_PIXELS) {
                    return ValidationResult.fail("pixel_bomb_detected");
                }
                return ValidationResult.ok(width, height);
            } finally {
                reader.dispose();
            }
        } catch (Exception e) {
            return ValidationResult.fail("header_read_exception: " + e.getMessage());
        }
    }

    /**
     * Procesa asíncronamente una imagen: valida, decodifica, redimensiona y calcula hashes.
     */
    public static CompletableFuture<ProcessResult> processAsync(byte[] bytes) {
        return CompletableFuture.supplyAsync(() -> {
            ValidationResult vr = validateImageHeader(bytes);
            if (!vr.valid()) {
                return new ProcessResult(false, vr.error(), null, null, null);
            }

            try {
                BufferedImage original = ImageIO.read(new ByteArrayInputStream(bytes));
                if (original == null) {
                    return new ProcessResult(false, "decode_failed", null, null, null);
                }

                BufferedImage scaled;
                if (original.getWidth() > TARGET_MAX_DIMENSION || original.getHeight() > TARGET_MAX_DIMENSION) {
                    double k = Math.min((double) TARGET_MAX_DIMENSION / original.getWidth(),
                            (double) TARGET_MAX_DIMENSION / original.getHeight());
                    int nw = Math.max(1, (int) Math.round(original.getWidth() * k));
                    int nh = Math.max(1, (int) Math.round(original.getHeight() * k));

                    scaled = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g = scaled.createGraphics();
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                    g.drawImage(original, 0, 0, nw, nh, null);
                    g.dispose();
                } else {
                    scaled = original;
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                if (!ImageIO.write(scaled, "PNG", baos)) {
                    return new ProcessResult(false, "png_encode_failed", null, null, null);
                }

                byte[] processedBytes = baos.toByteArray();
                String sha256 = hash(processedBytes, "SHA-256");
                String sha1 = hash(processedBytes, "SHA-1");

                PROCESSED_CACHE.put(sha1, processedBytes);
                PROCESSED_CACHE.put(sha256, processedBytes);

                return new ProcessResult(true, null, processedBytes, sha256, sha1);
            } catch (Exception e) {
                LunaEternal.LOG.error("[SANCTUARY-PIPELINE] Error procesando imagen", e);
                return new ProcessResult(false, "exception: " + e.getMessage(), null, null, null);
            }
        }, ASYNC_POOL);
    }

    /**
     * Envía la imagen al jugador de manera controlada y dosificada (paced streaming).
     * Evita desbordar la cola de paquetes de Netty y protege la conexión contra desconexiones por timeout.
     */
    public static void streamPhotoToPlayer(ServerPlayerEntity player, String sha1, byte[] bytes) {
        if (player == null || player.isRemoved() || bytes == null || bytes.length == 0) return;

        cancelActiveTransmission(player.getUuid());

        final int totalBytes = bytes.length;
        final int totalChunks = (totalBytes + CHUNK_SIZE - 1) / CHUNK_SIZE;
        final int[] nextChunkIndex = {0};

        ScheduledFuture<?> task = ASYNC_POOL.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (player.isRemoved()) {
                    cancelActiveTransmission(player.getUuid());
                    return;
                }

                MinecraftServer server = player.getServer();
                if (server == null) {
                    cancelActiveTransmission(player.getUuid());
                    return;
                }

                server.execute(() -> {
                    if (player.isRemoved()) return;

                    for (int i = 0; i < CHUNKS_PER_BURST && nextChunkIndex[0] < totalChunks; i++) {
                        int idx = nextChunkIndex[0]++;
                        int from = idx * CHUNK_SIZE;
                        int to = Math.min(totalBytes, from + CHUNK_SIZE);
                        byte[] tramo = Arrays.copyOfRange(bytes, from, to);

                        ServerPlayNetworking.send(player, new Red.FotoTramo(sha1, totalBytes, idx, tramo));
                    }

                    if (nextChunkIndex[0] >= totalChunks) {
                        cancelActiveTransmission(player.getUuid());
                    }
                });
            }
        }, 0, BURST_INTERVAL_MS, TimeUnit.MILLISECONDS);

        ACTIVE_TRANSMISSIONS.put(player.getUuid(), task);
    }

    public static void cancelActiveTransmission(UUID playerUuid) {
        ScheduledFuture<?> future = ACTIVE_TRANSMISSIONS.remove(playerUuid);
        if (future != null && !future.isDone()) {
            future.cancel(false);
        }
    }

    public static byte[] getCached(String hash) {
        return PROCESSED_CACHE.get(hash);
    }

    public static void putCache(String hash, byte[] data) {
        PROCESSED_CACHE.put(hash, data);
    }

    private static String hash(byte[] data, String algorithm) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] digest = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Digest algorithm not available: " + algorithm, e);
        }
    }
}
