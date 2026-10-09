package net.pokereport.luna.chat;

import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.pokereport.luna.LunaEternal;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio centralizado de anuncios oficiales periódicos en PokeReport.
 */
public final class ServerAnnouncementService {

    public static final Set<String> DOMAIN_WHITELIST = Set.of(
            "pokereport.online",
            "discord.gg"
    );

    public record Announcement(
            String id,
            boolean enabled,
            String title,
            String body,
            String url,
            String hover,
            List<String> allowedDimensions
    ) {}

    private static final List<Announcement> DEFAULT_ANNOUNCEMENTS = List.of(
            new Announcement(
                    "wiki",
                    true,
                    "§6§lPOKEREPORT §8» §eGUÍAS Y WIKI",
                    "§7¿Tienes dudas sobre crianza, gimnasios o comandos? Consulta nuestra Wiki oficial.",
                    "https://pokereport.online/wiki/",
                    "§7Click para abrir la Wiki oficial",
                    List.of("lunaeternal:lobby", "lunaeternal:ciudadela")
            ),
            new Announcement(
                    "discord",
                    true,
                    "§6§lPOKEREPORT §8» §9COMUNIDAD DISCORD",
                    "§7Únete a nuestra comunidad para noticias, eventos, sorteos y soporte de staff.",
                    "https://discord.gg/JsUTQWRH8H",
                    "§7Click para unirte a nuestro Discord",
                    List.of("lunaeternal:lobby", "lunaeternal:ciudadela")
            ),
            new Announcement(
                    "tienda",
                    true,
                    "§6§lPOKEREPORT §8» §aTIENDA OFICIAL",
                    "§7Apoya al servidor obteniendo rangos, cosméticos exclusivos y LunaCoins.",
                    "https://pokereport.online/tienda/",
                    "§7Click para visitar la Tienda oficial",
                    List.of("lunaeternal:lobby", "lunaeternal:ciudadela")
            ),
            new Announcement(
                    "soporte",
                    true,
                    "§6§lPOKEREPORT §8» §cSOPORTE Y AYUDA",
                    "§7¿Encontraste un problema o necesitas ayuda? Abre un ticket de soporte en Discord.",
                    "https://discord.gg/JsUTQWRH8H",
                    "§7Click para abrir ticket de soporte",
                    List.of("lunaeternal:lobby", "lunaeternal:ciudadela")
            ),
            new Announcement(
                    "guias",
                    true,
                    "§6§lPOKEREPORT §8» §bGUÍA DE INICIO",
                    "§7¡Bienvenido a PokeReport! Habla con el guardián para comenzar tu aventura en la Ciudadela.",
                    "https://pokereport.online/wiki/",
                    "§7Click para ver la guía de inicio",
                    List.of("lunaeternal:lobby", "lunaeternal:ciudadela")
            )
    );

    private static final List<Announcement> registry = new ArrayList<>(DEFAULT_ANNOUNCEMENTS);
    private static int currentIndex = 0;
    private static boolean enabled = true;
    private static int intervalMinutes = 6;
    private static long ticksSinceLast = 0;
    public static final long TICKS_PER_MINUTE = 1200L;
    public static final long INITIAL_JOIN_DELAY_MS = 45_000L;

    private static final Map<UUID, Long> PLAYER_JOIN_TIMESTAMPS = new ConcurrentHashMap<>();

    private ServerAnnouncementService() {}

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static int getIntervalMinutes() {
        return intervalMinutes;
    }

    public static void setIntervalMinutes(int minutes) {
        intervalMinutes = Math.max(1, minutes);
    }

    public static List<Announcement> getAnnouncements() {
        return Collections.unmodifiableList(registry);
    }

    public static void setAnnouncements(List<Announcement> list) {
        registry.clear();
        if (list != null) {
            registry.addAll(list);
        }
        currentIndex = 0;
    }

    public static void reset() {
        registry.clear();
        registry.addAll(DEFAULT_ANNOUNCEMENTS);
        currentIndex = 0;
        ticksSinceLast = 0;
        enabled = true;
        intervalMinutes = 6;
        PLAYER_JOIN_TIMESTAMPS.clear();
    }

    public static boolean isUrlWhitelisted(String urlStr) {
        if (urlStr == null || urlStr.isBlank()) return false;
        try {
            URI uri = URI.create(urlStr.trim());
            String host = uri.getHost();
            if (host == null) return false;
            host = host.toLowerCase(Locale.ROOT);
            for (String allowed : DOMAIN_WHITELIST) {
                if (host.equals(allowed) || host.endsWith("." + allowed)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public static void onPlayerJoin(ServerPlayerEntity player) {
        if (player != null) {
            PLAYER_JOIN_TIMESTAMPS.put(player.getUuid(), System.currentTimeMillis());
        }
    }

    public static void onPlayerDisconnect(UUID uuid) {
        if (uuid != null) {
            PLAYER_JOIN_TIMESTAMPS.remove(uuid);
        }
    }

    public static boolean hasCompletedJoinDelay(UUID uuid) {
        if (uuid == null) return true;
        Long joinTime = PLAYER_JOIN_TIMESTAMPS.get(uuid);
        if (joinTime == null) return true;
        return (System.currentTimeMillis() - joinTime) >= INITIAL_JOIN_DELAY_MS;
    }

    public static Announcement getNextAnnouncement() {
        List<Announcement> active = registry.stream().filter(Announcement::enabled).toList();
        if (active.isEmpty()) return null;

        if (currentIndex >= active.size()) {
            currentIndex = 0;
        }
        Announcement chosen = active.get(currentIndex);
        currentIndex = (currentIndex + 1) % active.size();
        return chosen;
    }

    public static Text formatAnnouncement(Announcement a) {
        if (a == null) return Text.empty();
        MutableText comp = Text.empty();
        comp.append(Text.literal("§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n"));
        comp.append(Text.literal(a.title() + "\n"));
        comp.append(Text.literal(a.body() + "\n"));
        if (a.url() != null && !a.url().isBlank()) {
            MutableText link = Text.literal("➜ §b§n[Abrir Enlace]§r §8(" + a.url() + ")");
            if (isUrlWhitelisted(a.url())) {
                link.styled(style -> style
                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, a.url()))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Text.literal(a.hover() != null ? a.hover() : "Click para abrir enlace oficial")))
                );
            }
            comp.append(link).append(Text.literal("\n"));
        }
        comp.append(Text.literal("§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        return comp;
    }

    public static boolean canDeliverTo(RegistryKey<net.minecraft.world.World> world, UUID playerUuid, Announcement a) {
        if (a == null || !a.enabled()) return false;
        if (playerUuid != null && !hasCompletedJoinDelay(playerUuid)) return false;
        if (world == null) return false;
        String worldId = world.getValue().toString();
        return a.allowedDimensions().contains(worldId);
    }

    public static boolean canDeliverTo(ServerPlayerEntity player, Announcement a) {
        if (player == null || player.getWorld() == null) return false;
        return canDeliverTo(player.getWorld().getRegistryKey(), player.getUuid(), a);
    }

    public static void tick(MinecraftServer server) {
        if (!enabled || server == null) return;
        ticksSinceLast++;
        long threshold = intervalMinutes * TICKS_PER_MINUTE;
        if (ticksSinceLast >= threshold) {
            ticksSinceLast = 0;
            Announcement announcement = getNextAnnouncement();
            if (announcement != null) {
                broadcastAnnouncement(server, announcement);
            }
        }
    }

    public static void broadcastAnnouncement(MinecraftServer server, Announcement announcement) {
        if (server == null || announcement == null) return;
        Text formatted = formatAnnouncement(announcement);
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (canDeliverTo(player, announcement)) {
                player.sendMessage(formatted, false);
            }
        }
    }
}
