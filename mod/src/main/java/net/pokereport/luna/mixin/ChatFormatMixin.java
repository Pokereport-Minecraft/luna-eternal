package net.pokereport.luna.mixin;

import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.pokereport.luna.chat.ChatChannel;
import net.pokereport.luna.chat.ChatChannelService;
import net.pokereport.luna.chat.ChatDimensionPolicy;
import net.pokereport.luna.ui.Tablist;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.function.Predicate;

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
            ChatChannel channel = ChatChannelService.getActiveChannel(sender);
            prefix.append(channel.getFormattedTag());

            Tablist.Rank rank = Tablist.rankOf(sender.server, sender);
            prefix.append(Tablist.prefixOf(rank));

            Tablist.ClanInfo clan = Tablist.clanDe(sender);
            if (clan != null && !clan.etiqueta().isEmpty()) {
                prefix.append(Tablist.tagText(clan)).append(Text.literal(" "));
            }

            // params is a record in 1.21.1: (RegistryEntry<MessageType> type, Text name, Text targetName)
            Text newName = prefix.append(sender.getName().copy());
            return new MessageType.Parameters(params.type(), newName, params.targetName());
        }
        return params;
    }

    @ModifyVariable(
            method = "broadcast(Lnet/minecraft/network/message/SignedMessage;Ljava/util/function/Predicate;Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/network/message/MessageType$Parameters;)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private Predicate<ServerPlayerEntity> luna$filterChatRecipients(Predicate<ServerPlayerEntity> shouldSend,
                                                                   SignedMessage message,
                                                                   Predicate<ServerPlayerEntity> original,
                                                                   ServerPlayerEntity sender,
                                                                   MessageType.Parameters params) {
        return recipient -> {
            if (shouldSend != null && !shouldSend.test(recipient)) {
                return false;
            }
            if (sender == null) {
                return true;
            }
            ChatChannel channel = ChatChannelService.getActiveChannel(sender);
            return ChatDimensionPolicy.canReceiveChat(recipient, sender, channel);
        };
    }
}
