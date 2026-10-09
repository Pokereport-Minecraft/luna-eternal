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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Pipeline Asíncrono de Procesamiento y Transmisión de Imágenes para el Santuario.
 *
 * Soluciona las causas raíz de desconexión y kick de jugadores:
 * 1. Descompresión segura (anti zip-bomb / pixel-bomb): valida cabeceras y dimensiones máximas antes de decodificar.
 * 2. Procesamiento fuera del hilo principal (Worker Thread Pool dedicado).
 * 3. Transmisión con control de flujo (Paced Packet Streaming): no satura los buffers de Netty enviando
 *    150 paquetes de 16KB en un solo tick, sino que dosifica los tramos protegiendo la conexión.
 * 4. Caché SHA-256 e idempotencia de subidas.
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

        // Cancelar cualquier transmisión previa activa para el mismo jugador
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

                // Enviar una ráfaga acotada (CHUNKS_PER_BURST) en el hilo del servidor
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
