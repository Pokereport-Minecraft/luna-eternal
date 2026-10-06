package net.pokereport.luna.mixin;

import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.pokereport.luna.ui.Tablist;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(PlayerManager.class)
public abstract class ChatFormatMixin {

    @ModifyVariable(
            method = "broadcast(Lnet/minecraft/network/message/SignedMessage;Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/network/message/MessageType$Parameters;)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private MessageType.Parameters luna$formatChatPrefix(MessageType.Parameters params, SignedMessage message, ServerPlayerEntity sender) {
        if (sender != null) {
            MutableText prefix = Text.empty();
            Tablist.Rank rank = Tablist.rankOf(sender.server, sender);
            
            // Format requested: "[ELITE] PlayerName: Message"
            // For unranked (ENTRENADOR): "PlayerName: Message"
            if (rank != Tablist.Rank.ENTRENADOR) {
                prefix.append(Text.literal("[" + rank.titulo + "] ").formatted(rank.color));
            }
            
            Tablist.ClanInfo clan = Tablist.clanDe(sender);
            if (clan != null && !clan.etiqueta().isEmpty()) {
                prefix.append(Text.literal("[" + clan.etiqueta() + "] ").formatted(net.minecraft.util.Formatting.GRAY));
            }
            
            // params is a record in 1.21.1: (RegistryEntry<MessageType> type, Text name, Text targetName)
            Text newName = prefix.append(sender.getName().copy()); // we use getName() to avoid double team prefixes from getDisplayName()
            return new MessageType.Parameters(params.type(), newName, params.targetName());
        }
        return params;
    }
}
