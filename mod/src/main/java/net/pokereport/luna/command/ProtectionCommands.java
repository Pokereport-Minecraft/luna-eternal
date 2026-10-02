package net.pokereport.luna.command;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;
import net.pokereport.luna.LunaEternal;
import net.pokereport.luna.world.WorldProtectionManager;

/** Uses existing staff ranks; an operator flag never grants world edit access. */
public final class ProtectionCommands {
    private ProtectionCommands() {}

    public static void registrar() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) ->
                dispatcher.register(CommandManager.literal("pradmin")
                        .requires(source -> source.getEntity() == null || (source.getEntity() instanceof net.minecraft.server.network.ServerPlayerEntity player
                                && WorldProtectionManager.hasAdministrativeEditAccess(player)))
                        .then(CommandManager.literal("editmode")
                                .then(CommandManager.literal("on").executes(context -> mode(context.getSource(), true)))
                                .then(CommandManager.literal("off").executes(context -> mode(context.getSource(), false))))
                        .then(CommandManager.literal("status").executes(context -> {
                            var player = context.getSource().getEntity() instanceof net.minecraft.server.network.ServerPlayerEntity sp ? sp : null;
                            context.getSource().sendFeedback(() -> Text.literal("World Protection: ENABLED; dimensión segura: "
                                    + (player == null ? "consola" : WorldProtectionManager.isProtectedDimension(player.getWorld()))
                                    + "; edición: " + (player != null && WorldProtectionManager.editing(player))), false);
                            return 1;
                        }))
                        .then(CommandManager.literal("selftest")
                                .requires(source -> source.getEntity() == null)
                                .executes(context -> net.pokereport.luna.test.ProtectionSelfTest.run(context.getSource())))
                        .then(CommandManager.literal("commands")
                                .requires(source -> source.getEntity() == null)
                                .executes(context -> {
                                    var lines = new java.util.ArrayList<String>();
                                    var source = context.getSource();
                                    for (var node : dispatcher.getRoot().getChildren()) {
                                        for (String usage : dispatcher.getAllUsage(node, source, false)) {
                                            lines.add("/" + node.getName() + (usage.isBlank() ? "" : " " + usage));
                                        }
                                    }
                                    java.util.Collections.sort(lines);
                                    try {
                                        java.nio.file.Files.createDirectories(java.nio.file.Path.of("logs"));
                                        java.nio.file.Files.write(java.nio.file.Path.of("logs", "pokereport-commands.txt"), lines);
                                        source.sendFeedback(() -> Text.literal("Comandos exportados: " + lines.size()), false);
                                        return lines.size();
                                    } catch (java.io.IOException e) {
                                        LunaEternal.LOG.error("No se pudo exportar el inventario de comandos", e);
                                        return 0;
                                    }
                                }))));
    }

    private static int mode(net.minecraft.server.command.ServerCommandSource source, boolean on)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player = source.getPlayerOrThrow();
        if (!WorldProtectionManager.hasAdministrativeEditAccess(player)) return 0;
        WorldProtectionManager.setEditing(player, on);
        player.changeGameMode(on ? GameMode.CREATIVE
                : WorldProtectionManager.isProtectedDimension(player.getWorld()) ? GameMode.ADVENTURE : GameMode.SURVIVAL);
        LunaEternal.LOG.info("[WORLD-EDIT] {} uuid={} dimension={} position={} mode={}",
                player.getGameProfile().getName(), player.getUuid(), player.getWorld().getRegistryKey().getValue(),
                player.getBlockPos(), on ? "ON" : "OFF");
        source.sendFeedback(() -> Text.literal(on ? "Edición autorizada activada." : "Edición desactivada."), false);
        return 1;
    }
}
