package net.pokereport.luna.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.pokereport.luna.spectate.SpectateFollowConfig;
import net.pokereport.luna.spectate.SpectateFollowManager;
import net.pokereport.luna.world.StaffRoles;
import net.pokereport.luna.world.WorldProtectionManager;

import java.util.Set;

/**
 * Comando /spectatefollow para el control de la cámara de streaming 24/7.
 */
public final class SpectateFollowCommand {

    private SpectateFollowCommand() {}

    public static void registrar() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> {
            dispatcher.register(
                CommandManager.literal("spectatefollow")
                    .requires(SpectateFollowCommand::isAuthorized)
                    // /spectatefollow (sin argumentos -> muestra estado o ayuda)
                    .executes(context -> {
                        ServerCommandSource source = context.getSource();
                        if (source.getEntity() instanceof ServerPlayerEntity player) {
                            player.sendMessage(SpectateFollowManager.getStatusText(player), false);
                        } else {
                            source.sendFeedback(() -> Text.literal("§cEste comando debe ser ejecutado por un jugador como cámara."), false);
                        }
                        return 1;
                    })
                    // /spectatefollow auto
                    .then(CommandManager.literal("auto")
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            if (source.getEntity() instanceof ServerPlayerEntity player) {
                                SpectateFollowManager.startAuto(player);
                            } else {
                                source.sendFeedback(() -> Text.literal("§cSolo un jugador puede activar el modo automático."), false);
                            }
                            return 1;
                        }))
                    // /spectatefollow next
                    .then(CommandManager.literal("next")
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            if (source.getEntity() instanceof ServerPlayerEntity player) {
                                SpectateFollowManager.nextTarget(player);
                            }
                            return 1;
                        }))
                    // /spectatefollow previous
                    .then(CommandManager.literal("previous")
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            if (source.getEntity() instanceof ServerPlayerEntity player) {
                                SpectateFollowManager.previousTarget(player);
                            }
                            return 1;
                        }))
                    // /spectatefollow off
                    .then(CommandManager.literal("off")
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            if (source.getEntity() instanceof ServerPlayerEntity player) {
                                SpectateFollowManager.stopSpectating(player);
                            }
                            return 1;
                        }))
                    // /spectatefollow status
                    .then(CommandManager.literal("status")
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            if (source.getEntity() instanceof ServerPlayerEntity player) {
                                player.sendMessage(SpectateFollowManager.getStatusText(player), false);
                            } else {
                                source.sendFeedback(() -> Text.literal("§7Sesiones activas: " + SpectateFollowManager.getSessions().size()), false);
                            }
                            return 1;
                        }))
                    // /spectatefollow rotate <min|off>
                    .then(CommandManager.literal("rotate")
                        .then(CommandManager.literal("off")
                            .executes(context -> {
                                ServerCommandSource source = context.getSource();
                                if (source.getEntity() instanceof ServerPlayerEntity player) {
                                    SpectateFollowManager.setRotation(player, 0);
                                }
                                return 1;
                            }))
                        .then(CommandManager.argument("minutos", IntegerArgumentType.integer(1, 1440))
                            .executes(context -> {
                                ServerCommandSource source = context.getSource();
                                int minutos = IntegerArgumentType.getInteger(context, "minutos");
                                if (source.getEntity() instanceof ServerPlayerEntity player) {
                                    SpectateFollowManager.setRotation(player, minutos);
                                }
                                return 1;
                            })))
                    // /spectatefollow exclude <add|remove|list>
                    .then(CommandManager.literal("exclude")
                        .then(CommandManager.literal("list")
                            .executes(context -> {
                                ServerCommandSource source = context.getSource();
                                Set<String> excluded = SpectateFollowManager.getConfig().getExcluded();
                                if (excluded.isEmpty()) {
                                    source.sendFeedback(() -> Text.literal("§7No hay jugadores en la lista de exclusión."), false);
                                } else {
                                    source.sendFeedback(() -> Text.literal("§6Jugadores excluidos (" + excluded.size() + "): §f" + String.join(", ", excluded)), false);
                                }
                                return 1;
                            }))
                        .then(CommandManager.literal("add")
                            .then(CommandManager.argument("jugador", StringArgumentType.word())
                                .executes(context -> {
                                    ServerCommandSource source = context.getSource();
                                    String target = StringArgumentType.getString(context, "jugador");
                                    SpectateFollowConfig cfg = SpectateFollowManager.getConfig();
                                    boolean added = cfg.addExcluded(target);
                                    if (added) {
                                        SpectateFollowManager.onExclusionAdded(target);
                                        source.sendFeedback(() -> Text.literal("§aJugador §e" + target + " §aañadido a la lista de exclusión."), false);
                                    } else {
                                        source.sendFeedback(() -> Text.literal("§e" + target + " ya estaba en la lista de exclusión."), false);
                                    }
                                    return 1;
                                })))
                        .then(CommandManager.literal("remove")
                            .then(CommandManager.argument("jugador", StringArgumentType.word())
                                .executes(context -> {
                                    ServerCommandSource source = context.getSource();
                                    String target = StringArgumentType.getString(context, "jugador");
                                    SpectateFollowConfig cfg = SpectateFollowManager.getConfig();
                                    boolean removed = cfg.removeExcluded(target);
                                    if (removed) {
                                        source.sendFeedback(() -> Text.literal("§aJugador §e" + target + " §aeliminado de la lista de exclusión."), false);
                                    } else {
                                        source.sendFeedback(() -> Text.literal("§c" + target + " no estaba en la lista de exclusión."), false);
                                    }
                                    return 1;
                                }))))
                    // /spectatefollow <jugador> (manual target)
                    .then(CommandManager.argument("jugador", EntityArgumentType.player())
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "jugador");
                            if (source.getEntity() instanceof ServerPlayerEntity player) {
                                SpectateFollowManager.startManual(player, target);
                            } else {
                                source.sendFeedback(() -> Text.literal("§cSolo un jugador en el servidor puede seguir a otro como cámara."), false);
                            }
                            return 1;
                        }))
            );
        });
    }

    private static boolean isAuthorized(ServerCommandSource source) {
        if (source.hasPermissionLevel(2)) return true;
        if (source.getEntity() instanceof ServerPlayerEntity player) {
            if (StaffRoles.display(player.getUuid()) != null) return true;
            return WorldProtectionManager.hasAdministrativeEditAccess(player);
        }
        return true; // consola
    }
}
