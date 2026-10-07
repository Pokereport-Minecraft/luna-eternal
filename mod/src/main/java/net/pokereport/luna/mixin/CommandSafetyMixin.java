package net.pokereport.luna.mixin;

import com.mojang.brigadier.ParseResults;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.pokereport.luna.world.CommandSafetyPolicy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CommandManager.class)
public abstract class CommandSafetyMixin {
    @Inject(method = "execute", at = @At("HEAD"), cancellable = true)
    private void luna$authorizeCommands(ParseResults<ServerCommandSource> parsed, String command, CallbackInfo ci) {
        var context = parsed.getContext();
        if (context.getSource().getEntity() instanceof ServerPlayerEntity player && !context.getNodes().isEmpty()) {
            int start = context.getNodes().getFirst().getRange().getStart();
            if (!CommandSafetyPolicy.allowed(player, command.substring(start))) {
                context.getSource().sendError(Text.literal("Acción administrativa no autorizada."));
                ci.cancel();
            }
        }
    }
}
