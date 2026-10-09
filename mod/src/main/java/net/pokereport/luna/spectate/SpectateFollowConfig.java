package net.pokereport.luna.spectate;

import net.pokereport.luna.LunaEternal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Configuración persistente para el sistema de cámara de espectador (/spectatefollow).
 * Archivo: config/lunaeternal/spectatefollow.properties
 */
public final class SpectateFollowConfig {

    public static final Path FILE = Path.of("config", "lunaeternal", "spectatefollow.properties");

    private volatile boolean enabled = true;
    private volatile boolean includeStaff = false;
    private volatile int rotationMinutes = 5;
    private volatile int joinDelaySeconds = 4;
    private volatile boolean autoSwitchOnDisconnect = true;
    private volatile boolean returnToPreferredTarget = false;
    private final Set<String> excluded = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public SpectateFollowConfig() {
        load();
    }

    public synchronized void load() {
        if (!Files.exists(FILE)) {
            save();
            return;
        }

        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(FILE)) {
            props.load(in);
            this.enabled = Boolean.parseBoolean(props.getProperty("enabled", "true"));
            this.includeStaff = Boolean.parseBoolean(props.getProperty("includeStaff", "false"));
            this.rotationMinutes = Math.max(0, Integer.parseInt(props.getProperty("rotationMinutes", "5")));
            this.joinDelaySeconds = Math.max(1, Integer.parseInt(props.getProperty("joinDelaySeconds", "4")));
            this.autoSwitchOnDisconnect = Boolean.parseBoolean(props.getProperty("autoSwitchOnDisconnect", "true"));
            this.returnToPreferredTarget = Boolean.parseBoolean(props.getProperty("returnToPreferredTarget", "false"));

            this.excluded.clear();
            String rawExcluded = props.getProperty("excluded", "");
            if (!rawExcluded.isBlank()) {
                for (String part : rawExcluded.split(",")) {
                    String trimmed = part.trim().toLowerCase(Locale.ROOT);
                    if (!trimmed.isEmpty()) {
                        this.excluded.add(trimmed);
                    }
                }
            }
        } catch (Exception e) {
            LunaEternal.LOG.error("[SPECTATE-FOLLOW] Error al cargar configuración desde {}", FILE, e);
        }
    }

    public synchronized void save() {
        try {
            if (FILE.getParent() != null) {
                Files.createDirectories(FILE.getParent());
            }

            Properties props = new Properties();
            props.setProperty("enabled", String.valueOf(this.enabled));
            props.setProperty("includeStaff", String.valueOf(this.includeStaff));
            props.setProperty("rotationMinutes", String.valueOf(this.rotationMinutes));
            props.setProperty("joinDelaySeconds", String.valueOf(this.joinDelaySeconds));
            props.setProperty("autoSwitchOnDisconnect", String.valueOf(this.autoSwitchOnDisconnect));
            props.setProperty("returnToPreferredTarget", String.valueOf(this.returnToPreferredTarget));
            props.setProperty("excluded", String.join(",", this.excluded.stream().sorted().toList()));

            try (OutputStream out = Files.newOutputStream(FILE)) {
                props.store(out, "PokeReport Luna Eternal - Spectate Follow Stream Configuration");
            }
        } catch (IOException e) {
            LunaEternal.LOG.error("[SPECTATE-FOLLOW] Error al guardar configuración en {}", FILE, e);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        save();
    }

    public boolean isIncludeStaff() {
        return includeStaff;
    }

    public void setIncludeStaff(boolean includeStaff) {
        this.includeStaff = includeStaff;
        save();
    }

    public int getRotationMinutes() {
        return rotationMinutes;
    }

    public void setRotationMinutes(int rotationMinutes) {
        this.rotationMinutes = Math.max(0, rotationMinutes);
        save();
    }

    public int getJoinDelaySeconds() {
        return joinDelaySeconds;
    }

    public void setJoinDelaySeconds(int joinDelaySeconds) {
        this.joinDelaySeconds = Math.max(1, joinDelaySeconds);
        save();
    }

    public boolean isAutoSwitchOnDisconnect() {
        return autoSwitchOnDisconnect;
    }

    public void setAutoSwitchOnDisconnect(boolean autoSwitchOnDisconnect) {
        this.autoSwitchOnDisconnect = autoSwitchOnDisconnect;
        save();
    }

    public boolean isReturnToPreferredTarget() {
        return returnToPreferredTarget;
    }

    public void setReturnToPreferredTarget(boolean returnToPreferredTarget) {
        this.returnToPreferredTarget = returnToPreferredTarget;
        save();
    }

    public boolean isExcluded(UUID uuid, String name) {
        if (uuid != null && excluded.contains(uuid.toString().toLowerCase(Locale.ROOT))) {
            return true;
        }
        if (name != null && excluded.contains(name.toLowerCase(Locale.ROOT))) {
            return true;
        }
        return false;
    }

    public boolean addExcluded(String target) {
        if (target == null || target.isBlank()) return false;
        boolean added = excluded.add(target.trim().toLowerCase(Locale.ROOT));
        if (added) {
            save();
        }
        return added;
    }

    public boolean removeExcluded(String target) {
        if (target == null || target.isBlank()) return false;
        boolean removed = excluded.remove(target.trim().toLowerCase(Locale.ROOT));
        if (removed) {
            save();
        }
        return removed;
    }

    public Set<String> getExcluded() {
        return Collections.unmodifiableSet(new TreeSet<>(excluded));
    }
}
