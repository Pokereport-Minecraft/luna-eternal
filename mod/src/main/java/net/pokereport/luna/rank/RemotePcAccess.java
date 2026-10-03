package net.pokereport.luna.rank;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.pc.link.PCLink;
import com.cobblemon.mod.common.api.storage.pc.link.PCLinkManager;
import com.cobblemon.mod.common.net.messages.client.storage.pc.OpenPCPacket;
import com.cobblemon.mod.common.util.PlayerExtensionsKt;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.pokereport.luna.puerta.Puerta;
import net.pokereport.luna.ui.Tablist.Rank;

/** Server policy shared by /pc and LumyMon's remote-PC key/network request. */
public final class RemotePcAccess {
    private RemotePcAccess() {}

    public static boolean allows(Rank rank, boolean operator) {
        return operator || (rank != null && (rank.equipo || rank.escalon >= Rank.CAMPEON.escalon));
    }

    public static boolean check(ServerPlayerEntity player) {
        if (Puerta.bloqueado(player)) {
            player.sendMessage(Text.literal("§cCompleta el acceso al servidor antes de usar el PC remoto."), false);
            return false;
        }
        if (!allows(RankService.enCache(player.getUuid()), player.hasPermissionLevel(2))) {
            player.sendMessage(Text.literal("§cEl PC remoto requiere Campeón, Maestro o Leyenda. Puedes usar los PCs físicos del Centro Pokémon."), false);
            return false;
        }
        return true;
    }

    public static int open(ServerPlayerEntity player) {
        if (!check(player)) return 0;
        if (PlayerExtensionsKt.isInBattle(player)) {
            player.sendMessage(Text.translatable("cobblemon.pc.inbattle"), false);
            return 0;
        }
        var store = Cobblemon.INSTANCE.getStorage().getPC(player);
        PCLinkManager.INSTANCE.addLink(new PCLink(store, player.getUuid()));
        new OpenPCPacket(store, 0).sendToPlayer(player);
        return 1;
    }
}
