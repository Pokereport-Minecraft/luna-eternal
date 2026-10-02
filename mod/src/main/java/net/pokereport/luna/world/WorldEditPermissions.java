package net.pokereport.luna.world;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import net.pokereport.luna.LunaEternal;

/** Granular grants supplement the existing operator levels; no second role store. */
public final class WorldEditPermissions {
    public static final String EDIT = "pokereport.world.edit";
    public static final String MODE = "pokereport.admin.editmode";
    public static final String COMMANDS = "pokereport.admin.commands";
    private static volatile Map<UUID, Set<String>> grants = Map.of();
    private WorldEditPermissions() {}

    public static void load() {
        Path file = Path.of("config", "lunaeternal", "world-edit.properties");
        grants = Map.of();
        try {
            Files.createDirectories(file.getParent());
            if (!Files.exists(file)) {
                Files.writeString(file, "# UUID=pokereport.world.edit,pokereport.admin.editmode\n"
                        + "# Only explicitly authorized level-4 administrators may edit. No wildcard grants.\n");
            }
            Properties properties = new Properties();
            try (var input = Files.newInputStream(file)) { properties.load(input); }
            Map<UUID, Set<String>> parsed = new HashMap<>();
            for (String id : properties.stringPropertyNames()) {
                Set<String> nodes = Set.of(properties.getProperty(id).strip().split("\\s*,\\s*"));
                if (!Set.of(EDIT, MODE, COMMANDS).containsAll(nodes)) throw new IllegalArgumentException("Unknown permission node");
                parsed.put(UUID.fromString(id), nodes);
            }
            grants = Map.copyOf(parsed);
        } catch (Exception e) {
            LunaEternal.LOG.error("Permisos de edición inválidos: edición denegada para todos", e);
        }
    }

    public static boolean allowed(UUID id, String node) {
        return grants.getOrDefault(id, Set.of()).contains(node);
    }
}
