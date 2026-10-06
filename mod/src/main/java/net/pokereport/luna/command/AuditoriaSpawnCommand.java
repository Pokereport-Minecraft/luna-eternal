package net.pokereport.luna.command;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;

public class AuditoriaSpawnCommand {

    public static void registrar(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("spawn-audit")
            .requires(source -> source.hasPermissionLevel(2))
            .executes(AuditoriaSpawnCommand::ejecutar));
    }

    private static int ejecutar(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        
        src.sendFeedback(() -> Text.literal("§a--- AUDITORÍA DE SPAWN ---"), false);
        
        for (ServerWorld world : src.getServer().getWorlds()) {
            int players = world.getPlayers().size();
            if (players == 0) continue; // Skip empty worlds for cleaner output
            
            int loadedChunks = world.getChunkManager().getLoadedChunkCount();
            int wildPokemon = 0;
            
            for (Entity entity : world.iterateEntities()) {
                if (entity instanceof PokemonEntity pe) {
                    var p = pe.getPokemon();
                    if (p != null && p.getOwnerUUID() == null && p.getOwnerNPC() == null && !pe.isBattling()) {
                        wildPokemon++;
                    }
                }
            }
            
            float perPlayer = players > 0 ? (float) wildPokemon / players : 0;
            float perChunk = loadedChunks > 0 ? (float) wildPokemon / loadedChunks : 0;
            
            src.sendFeedback(() -> Text.literal(
                String.format("§bWORLD: §f%s\n" +
                              "§7players=§e%d\n" +
                              "§7loadedChunks=§e%d\n" +
                              "§7wildPokemon=§e%d\n" +
                              "§7pokemonPerPlayer=§e%.2f\n" +
                              "§7pokemonPerLoadedChunk=§e%.2f", 
                              world.getRegistryKey().getValue().toString(),
                              players, loadedChunks, wildPokemon, perPlayer, perChunk)), false);
        }
        return 1;
    }
}
