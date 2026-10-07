package net.pokereport.luna.world;

import java.nio.file.*;
import java.util.*;
import net.pokereport.luna.ui.Tablist.Rank;

/** Operational display roles; never replaces the commercial rank in the database. */
public final class StaffRoles {
    public static final Path FILE = Path.of("config", "lunaeternal", "staff-roles.properties");
    private static volatile Map<UUID, String> roles = Map.of();
    private StaffRoles() {}
    public static Map<UUID, String> read() throws java.io.IOException {
        var parsed = new HashMap<UUID, String>();
        if (Files.exists(FILE)) {
            var props = new Properties();
            try (var input = Files.newInputStream(FILE)) { props.load(input); }
            for (String key : props.stringPropertyNames()) {
                String value = props.getProperty(key);
                if (!Set.of("moderator", "builder", "developer", "developer-edit", "admin", "head-admin", "owner").contains(value))
                    throw new IllegalArgumentException("Rol visual inválido");
                parsed.put(UUID.fromString(key), value);
            }
        }
        return Map.copyOf(parsed);
    }
    public static void load() {
        roles = Map.of();
        try { roles = read(); }
        catch (Exception e) { net.pokereport.luna.LunaEternal.LOG.error("No se pudieron cargar las etiquetas de staff", e); }
    }
    public static Map<UUID, String> snapshot() { return roles; }
    public static void replace(Map<UUID, String> updated) { roles = Map.copyOf(updated); }
    public static Rank display(UUID id) {
        return switch (roles.getOrDefault(id, "")) {
            case "owner" -> Rank.OWNER;
            case "head-admin" -> Rank.HEAD_ADMIN;
            case "admin" -> Rank.ADMIN;
            case "builder" -> Rank.BUILDER;
            case "developer", "developer-edit" -> Rank.DEV;
            case "moderator" -> Rank.MODERADOR;
            default -> null;
        };
    }
}
