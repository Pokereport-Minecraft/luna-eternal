package net.pokereport.luna.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.pokereport.luna.gym.GymChallengePolicy;
import net.pokereport.luna.gym.GymChallengeService;
import net.pokereport.luna.gym.Ranuras;

import static net.minecraft.server.command.CommandManager.literal;

/**
 * Comandos de gimnasio para jugadores: abandono seguro y consulta de estado.
 */
public final class GymCommands {

    private GymCommands() {}

    public static void registrar(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("gym")
                .then(literal("leave").executes(ctx -> abandonar(ctx.getSource())))
                .then(literal("salir").executes(ctx -> abandonar(ctx.getSource())))
        );

        dispatcher.register(literal("gimnasio")
                .then(literal("leave").executes(ctx -> abandonar(ctx.getSource())))
                .then(literal("salir").executes(ctx -> abandonar(ctx.getSource())))
        );
    }

    private static int abandonar(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;

        if (Ranuras.asignacionDe(player.getUuid()) == null && !GymChallengePolicy.isInGymDimension(player.getWorld())) {
            source.sendError(net.minecraft.text.Text.literal("§cNo te encuentras en ningún reto de gimnasio activo."));
            return 0;
        }

        GymChallengeService.abandonarReto(player);
        return 1;
    }
}
