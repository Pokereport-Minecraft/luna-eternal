package net.pokereport.luna.world;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.pokereport.luna.ui.Tablist;

/** Server-side destination policy, independent of client mods and game mode. */
public final class RestrictedDimensions {
    private RestrictedDimensions() {}
    public static boolean denied(String dimension, boolean operator, Tablist.Rank role) {
        boolean restricted = "minecraft:the_nether".equals(dimension)
                || "minecraft:the_end".equals(dimension);
        return restricted && !(operator && role != null && role.esStaff());
    }
    public static boolean denied(ServerPlayerEntity player, ServerWorld destination) {
        var server = player.getServer();
        return denied(destination.getRegistryKey().getValue().toString(),
                server != null && server.getPlayerManager().isOperator(player.getGameProfile()),
                server == null ? null : Tablist.rankOf(server, player));
    }
    public static void notifyDenied(ServerPlayerEntity player) {
        player.sendMessage(Text.literal("§cEl Nether y el End no están habilitados para jugadores."), true);
    }
    /** Also handles persisted login/respawn positions and other mods restoring old worlds. */
    public static void enforce(MinecraftServer server) {
        for (var player : server.getPlayerManager().getPlayerList()) {
            if (denied(player, player.getServerWorld())) {
                TravelService.asegurarEntradaLobby(player);
                notifyDenied(player);
            }
        }
    }
}
