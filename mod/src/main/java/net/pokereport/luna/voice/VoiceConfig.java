package net.pokereport.luna.voice;

import java.nio.file.*;
import java.io.*;
import java.util.Properties;

public record VoiceConfig(boolean enabled, boolean requireRelationship, double inviteDistance,
                          int expirationSeconds, int rejectCooldownSeconds, int maxPending,
                          boolean nearbyHud) {
    public static VoiceConfig defaults() { return new VoiceConfig(true,true,12,30,60,3,true); }
    public VoiceConfig {
        if (!Double.isFinite(inviteDistance) || inviteDistance < 1 || inviteDistance > 64
                || expirationSeconds < 5 || expirationSeconds > 120
                || rejectCooldownSeconds < 1 || rejectCooldownSeconds > 3600
                || maxPending < 1 || maxPending > 10) throw new IllegalArgumentException("PokeVoice configuration outside bounds");
    }
    public static VoiceConfig load(Path path) throws IOException {
        Properties p = new Properties();
        if (Files.exists(path)) try (var in=Files.newInputStream(path)) { p.load(in); }
        else {
            Files.createDirectories(path.getParent());
            p.setProperty("enabled","true"); p.setProperty("require_relationship_for_proximity","true");
            p.setProperty("invite_distance","12"); p.setProperty("invite_expiration_seconds","30");
            p.setProperty("reject_cooldown_seconds","60"); p.setProperty("max_pending_invitations","3");
            p.setProperty("show_nearby_hud","true");
            try (var out=Files.newOutputStream(path,StandardOpenOption.CREATE_NEW)) { p.store(out,"PokeVoice - server authoritative"); }
        }
        return new VoiceConfig(bool(p,"enabled",true),bool(p,"require_relationship_for_proximity",true),
            Double.parseDouble(p.getProperty("invite_distance","12")),
            Integer.parseInt(p.getProperty("invite_expiration_seconds","30")),
            Integer.parseInt(p.getProperty("reject_cooldown_seconds","60")),
            Integer.parseInt(p.getProperty("max_pending_invitations","3")),bool(p,"show_nearby_hud",true));
    }
    private static boolean bool(Properties p,String key,boolean fallback) {
        String s=p.getProperty(key,Boolean.toString(fallback));
        if (!s.equals("true") && !s.equals("false")) throw new IllegalArgumentException(key);
        return Boolean.parseBoolean(s);
    }
}
