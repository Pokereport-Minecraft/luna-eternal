package net.pokereport.luna.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.pokereport.luna.world.LunaDimensions;
import net.pokereport.luna.world.TravelService;

public class SpawnCommand {
    public static void registrar(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("spawn")
            .executes(ctx -> ejecutarViaje(ctx, LunaDimensions.CIUDADELA, "la Ciudadela")));
        dispatcher.register(CommandManager.literal("ciudadela")
            .executes(ctx -> ejecutarViaje(ctx, LunaDimensions.CIUDADELA, "la Ciudadela")));
        dispatcher.register(CommandManager.literal("hogar")
            .executes(ctx -> ejecutarViaje(ctx, LunaDimensions.HOGAR, "el Mundo Hogar")));
        dispatcher.register(CommandManager.literal("salvaje")
            .executes(ctx -> ejecutarViaje(ctx, LunaDimensions.SALVAJE, "el Mundo Salvaje")));
    }

    private static int ejecutarViaje(CommandContext<ServerCommandSource> ctx, net.minecraft.registry.RegistryKey<net.minecraft.world.World> dimension, String nombre) {
        ServerPlayerEntity p = ctx.getSource().getPlayer();
        if (p == null) return 0;
        
        net.pokereport.luna.world.Espera.pedir(p, "viajar a " + nombre, () -> {
            try {
                if (com.cobblemon.mod.common.Cobblemon.INSTANCE.getBattleRegistry().getBattleByParticipatingPlayer(p) != null) {
                    p.sendMessage(Text.literal("§cEl viaje se canceló porque estás en combate Pokémon."), false);
                    return;
                }
            } catch (Throwable ignored) {}
            TravelService.travel(p, dimension, nombre);
        });
        return 1;
    }
}
