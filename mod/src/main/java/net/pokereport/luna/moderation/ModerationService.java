package net.pokereport.luna.moderation;

import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.pokereport.luna.LunaEternal;
import net.pokereport.luna.world.StaffRoles;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio centralizado de moderación con persistencia en MariaDB (staff_punishment).
 * Gestiona advertencias, expulsiones, silenciamientos y suspensiones temporales o permanentes.
 */
public final class ModerationService {

    public enum Type {
        WARN, KICK, MUTE, TEMPMUTE, BAN, TEMPBAN
    }

    public record Punishment(
            long id,
            UUID targetUuid,
            String targetName,
            UUID staffUuid,
            String staffName,
            Type type,
            String reason,
            long createdAt,
            Long expiresAt,
            boolean active
    ) {
        public boolean isExpired() {
            return expiresAt != null && System.currentTimeMillis() >= expiresAt;
        }
    }

    private static final Map<UUID, Punishment> ACTIVE_MUTES = new ConcurrentHashMap<>();
    private static final Map<UUID, Punishment> ACTIVE_BANS = new ConcurrentHashMap<>();
    private static volatile boolean initialized = false;

    private ModerationService() {}

    public static synchronized void registrar() {
        if (initialized) return;
        initialized = true;

        cargarCastigosActivos();

        // Bloqueo de chat para jugadores silenciados
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            Punishment mute = getActiveMute(sender.getUuid());
            if (mute != null) {
                if (mute.isExpired()) {
                    ACTIVE_MUTES.remove(sender.getUuid());
                    desactivarCastigoDb(mute.id());
                    return true;
                }
                String tiempo = mute.expiresAt() == null ? "permanente" : "expira en " + formatearRestante(mute.expiresAt());
                sender.sendMessage(Text.literal("§c[Moderación] No puedes hablar. Estás silenciado (" + tiempo + "). Motivo: §f" + mute.reason()), false);
                return false;
            }
            return true;
        });

        // Verificación de baneos al conectarse
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            if (player != null) {
                Punishment ban = getActiveBan(player.getUuid());
                if (ban != null) {
                    if (ban.isExpired()) {
                        ACTIVE_BANS.remove(player.getUuid());
                        desactivarCastigoDb(ban.id());
                    } else {
                        String expira = ban.expiresAt() == null ? "§4Permanente" : "§e" + formatearRestante(ban.expiresAt());
                        player.networkHandler.disconnect(Text.literal("§cHas sido sancionado de PokeReport Network.\n\n"
                                + "§7Motivo: §f" + ban.reason() + "\n"
                                + "§7Duración: " + expira + "\n"
                                + "§7Staff: §e" + ban.staffName()));
                    }
                }
            }
        });
    }

    public static void cargarCastigosActivos() {
        LunaEternal.submit(() -> {
            try (Connection c = LunaEternal.database().connection();
                 Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT * FROM staff_punishment WHERE active = TRUE AND type IN ('MUTE', 'TEMPMUTE', 'BAN', 'TEMPBAN')")) {
                while (rs.next()) {
                    Punishment p = mapear(rs);
                    if (!p.isExpired()) {
                        if (p.type() == Type.MUTE || p.type() == Type.TEMPMUTE) {
                            ACTIVE_MUTES.put(p.targetUuid(), p);
                        } else if (p.type() == Type.BAN || p.type() == Type.TEMPBAN) {
                            ACTIVE_BANS.put(p.targetUuid(), p);
                        }
                    } else {
                        desactivarCastigoDb(p.id());
                    }
                }
                LunaEternal.LOG.info("[MODERATION] Cargados {} mutes activos y {} bans activos.", ACTIVE_MUTES.size(), ACTIVE_BANS.size());
            } catch (Exception e) {
                LunaEternal.LOG.error("[MODERATION] Error al cargar castigos activos desde BD", e);
            }
        });
    }

    public static Punishment getActiveMute(UUID uuid) {
        Punishment p = ACTIVE_MUTES.get(uuid);
        if (p != null && p.isExpired()) {
            ACTIVE_MUTES.remove(uuid);
            desactivarCastigoDb(p.id());
            return null;
        }
        return p;
    }

    public static Punishment getActiveBan(UUID uuid) {
        Punishment p = ACTIVE_BANS.get(uuid);
        if (p != null && p.isExpired()) {
            ACTIVE_BANS.remove(uuid);
            desactivarCastigoDb(p.id());
            return null;
        }
        return p;
    }

    public static boolean isMuted(UUID uuid) {
        return getActiveMute(uuid) != null;
    }

    public static boolean isBanned(UUID uuid) {
        return getActiveBan(uuid) != null;
    }

    public static Punishment aplicarCastigo(
            UUID targetUuid,
            String targetName,
            UUID staffUuid,
            String staffName,
            Type type,
            String reason,
            Long durationMs
    ) {
        long now = System.currentTimeMillis();
        Long expiresAt = durationMs != null && durationMs > 0 ? now + durationMs : null;

        long id = insertarCastigoDb(targetUuid, targetName, staffUuid, staffName, type, reason, now, expiresAt);
        Punishment p = new Punishment(id, targetUuid, targetName, staffUuid, staffName, type, reason, now, expiresAt, true);

        if (type == Type.MUTE || type == Type.TEMPMUTE) {
            ACTIVE_MUTES.put(targetUuid, p);
        } else if (type == Type.BAN || type == Type.TEMPBAN) {
            ACTIVE_BANS.put(targetUuid, p);
        }

        return p;
    }

    public static boolean indultar(UUID targetUuid, Type tipoGrupo, UUID staffUuid, String staffName) {
        boolean indultado = false;
        if (tipoGrupo == Type.MUTE) {
            Punishment p = ACTIVE_MUTES.remove(targetUuid);
            if (p != null) indultado = true;
        } else if (tipoGrupo == Type.BAN) {
            Punishment p = ACTIVE_BANS.remove(targetUuid);
            if (p != null) indultado = true;
        }

        LunaEternal.submit(() -> {
            String sql = "UPDATE staff_punishment SET active = FALSE WHERE target_uuid = ? AND active = TRUE AND type IN ("
                    + (tipoGrupo == Type.MUTE ? "'MUTE', 'TEMPMUTE'" : "'BAN', 'TEMPBAN'") + ")";
            try (Connection c = LunaEternal.database().connection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, targetUuid.toString());
                ps.executeUpdate();
            } catch (Exception e) {
                LunaEternal.LOG.error("Error al indultar castigo para {}", targetUuid, e);
            }
        });

        return indultado;
    }

    public static List<Punishment> obtenerHistorial(UUID targetUuid) {
        List<Punishment> lista = new ArrayList<>();
        try (Connection c = LunaEternal.database().connection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM staff_punishment WHERE target_uuid = ? ORDER BY id DESC")) {
            ps.setString(1, targetUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        } catch (Exception e) {
            LunaEternal.LOG.error("Error al consultar historial de {}", targetUuid, e);
        }
        return lista;
    }

    public static long parsearDuracion(String str) {
        if (str == null || str.isBlank()) return 0;
        str = str.toLowerCase(Locale.ROOT).trim();
        long multi = 1000L;
        if (str.endsWith("s")) {
            str = str.substring(0, str.length() - 1);
            multi = 1000L;
        } else if (str.endsWith("m")) {
            str = str.substring(0, str.length() - 1);
            multi = 60 * 1000L;
        } else if (str.endsWith("h")) {
            str = str.substring(0, str.length() - 1);
            multi = 3600 * 1000L;
        } else if (str.endsWith("d")) {
            str = str.substring(0, str.length() - 1);
            multi = 86400 * 1000L;
        } else if (str.endsWith("w")) {
            str = str.substring(0, str.length() - 1);
            multi = 7 * 86400 * 1000L;
        }
        try {
            return Long.parseLong(str) * multi;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static String formatearRestante(long expiresAt) {
        long delta = Math.max(0, expiresAt - System.currentTimeMillis());
        long segundos = (delta / 1000) % 60;
        long minutos = (delta / (1000 * 60)) % 60;
        long horas = (delta / (1000 * 60 * 60)) % 24;
        long dias = delta / (1000 * 60 * 60 * 24);

        if (dias > 0) return dias + "d " + horas + "h";
        if (horas > 0) return horas + "h " + minutos + "m";
        if (minutos > 0) return minutos + "m " + segundos + "s";
        return segundos + "s";
    }

    private static long insertarCastigoDb(UUID targetUuid, String targetName, UUID staffUuid, String staffName,
                                          Type type, String reason, long createdAt, Long expiresAt) {
        String sql = "INSERT INTO staff_punishment (target_uuid, target_name, staff_uuid, staff_name, type, reason, created_at, expires_at, active) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, TRUE)";
        try (Connection c = LunaEternal.database().connection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, targetUuid.toString());
            ps.setString(2, targetName);
            ps.setString(3, staffUuid.toString());
            ps.setString(4, staffName);
            ps.setString(5, type.name());
            ps.setString(6, reason);
            ps.setLong(7, createdAt);
            if (expiresAt != null) ps.setLong(8, expiresAt); else ps.setNull(8, java.sql.Types.BIGINT);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
        } catch (Exception e) {
            LunaEternal.LOG.error("Error al registrar sanción en BD para {}", targetName, e);
        }
        return 0;
    }

    private static void desactivarCastigoDb(long id) {
        LunaEternal.submit(() -> {
            try (Connection c = LunaEternal.database().connection();
                 PreparedStatement ps = c.prepareStatement("UPDATE staff_punishment SET active = FALSE WHERE id = ?")) {
                ps.setLong(1, id);
                ps.executeUpdate();
            } catch (Exception e) {
                LunaEternal.LOG.error("Error al desactivar castigo id={}", id, e);
            }
        });
    }

    private static Punishment mapear(ResultSet rs) throws Exception {
        Long exp = rs.getObject("expires_at") != null ? rs.getLong("expires_at") : null;
        return new Punishment(
                rs.getLong("id"),
                UUID.fromString(rs.getString("target_uuid")),
                rs.getString("target_name"),
                UUID.fromString(rs.getString("staff_uuid")),
                rs.getString("staff_name"),
                Type.valueOf(rs.getString("type")),
                rs.getString("reason"),
                rs.getLong("created_at"),
                exp,
                rs.getBoolean("active")
        );
    }
}
