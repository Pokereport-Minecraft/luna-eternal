package net.pokereport.luna.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.pokereport.luna.spectate.StaffSpectateService;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class StaffSpectateCommand {

    private StaffSpectateCommand() {}

    public static void registrar(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                literal("staffspectate")
                        .requires(StaffSpectateCommand::isAuthorized)
                        .then(literal("off")
                                .executes(ctx -> {
                                    if (ctx.getSource().getEntity() instanceof ServerPlayerEntity player) {
                                        StaffSpectateService.stopSpectate(player);
                                    } else {
                                        ctx.getSource().sendFeedback(() -> Text.literal("§cSolo jugadores pueden ejecutar este comando."), false);
                                    }
                                    return 1;
                                }))
                        .then(argument("jugador", EntityArgumentType.player())
                                .executes(ctx -> {
                                    if (ctx.getSource().getEntity() instanceof ServerPlayerEntity player) {
                                        ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "jugador");
                                        StaffSpectateService.startSpectate(player, target, "Auditoría en vivo");
                                    }
                                    return 1;
                                })
                                .then(argument("motivo", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            if (ctx.getSource().getEntity() instanceof ServerPlayerEntity player) {
                                                ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "jugador");
                                                String motivo = StringArgumentType.getString(ctx, "motivo");
                                                StaffSpectateService.startSpectate(player, target, motivo);
                                            }
                                            return 1;
                                        })))
        );

        dispatcher.register(
                literal("staffmode")
                        .requires(StaffSpectateCommand::isAuthorized)
                        .executes(ctx -> {
                            if (ctx.getSource().getEntity() instanceof ServerPlayerEntity player) {
                                StaffSpectateService.toggleStaffMode(player);
                            } else {
                                ctx.getSource().sendFeedback(() -> Text.literal("§cSolo jugadores pueden ejecutar este comando."), false);
                            }
                            return 1;
                        })
        );
    }

    private static boolean isAuthorized(ServerCommandSource source) {
        if (source.hasPermissionLevel(2)) return true;
        if (source.getEntity() instanceof ServerPlayerEntity player) {
            return StaffSpectateService.isAuthorized(player);
        }
        return false;
    }
}
