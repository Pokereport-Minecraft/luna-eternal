package net.pokereport.luna.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.pokereport.luna.LunaEternal;
import net.pokereport.luna.moderation.ModerationService;
import net.pokereport.luna.spectate.StaffSpectateService;
import net.pokereport.luna.world.StaffRoles;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class ModerationCommands {

    private ModerationCommands() {}

    public static void registrar(CommandDispatcher<ServerCommandSource> dispatcher) {
        // /warn <player> <reason>
        dispatcher.register(
                literal("warn")
                        .requires(src -> puedeModerar(src, false))
                        .then(argument("jugador", EntityArgumentType.player())
                                .then(argument("motivo", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "jugador");
                                            String motivo = StringArgumentType.getString(ctx, "motivo");
                                            ejecutarWarn(ctx.getSource(), target, motivo);
                                            return 1;
                                        })))
        );

        // /kick <player> <reason>
        dispatcher.register(
                literal("kick")
                        .requires(src -> puedeModerar(src, false))
                        .then(argument("jugador", EntityArgumentType.player())
                                .then(argument("motivo", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "jugador");
                                            String motivo = StringArgumentType.getString(ctx, "motivo");
                                            ejecutarKick(ctx.getSource(), target, motivo);
                                            return 1;
                                        })))
        );

        // /mute <player> <reason> (perm)
        dispatcher.register(
                literal("mute")
                        .requires(src -> puedeModerar(src, false))
                        .then(argument("jugador", EntityArgumentType.player())
                                .then(argument("motivo", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "jugador");
                                            String motivo = StringArgumentType.getString(ctx, "motivo");
                                            ejecutarMute(ctx.getSource(), target, null, motivo);
                                            return 1;
                                        })))
        );

        // /tempmute <player> <duracion> <reason>
        dispatcher.register(
                literal("tempmute")
                        .requires(src -> puedeModerar(src, false))
                        .then(argument("jugador", EntityArgumentType.player())
                                .then(argument("duracion", StringArgumentType.word())
                                        .then(argument("motivo", StringArgumentType.greedyString())
                                                .executes(ctx -> {
                                                    ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "jugador");
                                                    String durStr = StringArgumentType.getString(ctx, "duracion");
                                                    String motivo = StringArgumentType.getString(ctx, "motivo");
                                                    long durMs = ModerationService.parsearDuracion(durStr);
                                                    if (durMs <= 0) {
                                                        ctx.getSource().sendFeedback(() -> Text.literal("§cDuración inválida. Ejemplos: 15m, 2h, 1d, 7d"), false);
                                                        return 0;
                                                    }
                                                    ejecutarMute(ctx.getSource(), target, durMs, motivo);
                                                    return 1;
                                                }))))
        );

        // /unmute <player>
        dispatcher.register(
                literal("unmute")
                        .requires(src -> puedeModerar(src, false))
                        .then(argument("jugador", StringArgumentType.word())
                                .executes(ctx -> {
                                    String targetName = StringArgumentType.getString(ctx, "jugador");
                                    ejecutarUnmute(ctx.getSource(), targetName);
                                    return 1;
                                }))
        );

        // /ban <player> <reason>
        dispatcher.register(
                literal("ban")
                        .requires(src -> puedeModerar(src, true))
                        .then(argument("jugador", StringArgumentType.word())
                                .then(argument("motivo", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            String targetName = StringArgumentType.getString(ctx, "jugador");
                                            String motivo = StringArgumentType.getString(ctx, "motivo");
                                            ejecutarBan(ctx.getSource(), targetName, null, motivo);
                                            return 1;
                                        })))
        );

        // /tempban <player> <duracion> <reason>
        dispatcher.register(
                literal("tempban")
                        .requires(src -> puedeModerar(src, true))
                        .then(argument("jugador", StringArgumentType.word())
                                .then(argument("duracion", StringArgumentType.word())
                                        .then(argument("motivo", StringArgumentType.greedyString())
                                                .executes(ctx -> {
                                                    String targetName = StringArgumentType.getString(ctx, "jugador");
                                                    String durStr = StringArgumentType.getString(ctx, "duracion");
                                                    String motivo = StringArgumentType.getString(ctx, "motivo");
                                                    long durMs = ModerationService.parsearDuracion(durStr);
                                                    if (durMs <= 0) {
                                                        ctx.getSource().sendFeedback(() -> Text.literal("§cDuración inválida. Ejemplos: 1h, 1d, 7d, 30d"), false);
                                                        return 0;
                                                    }
                                                    ejecutarBan(ctx.getSource(), targetName, durMs, motivo);
                                                    return 1;
                                                }))))
        );

        // /unban <player>
        dispatcher.register(
                literal("unban")
                        .requires(src -> puedeModerar(src, true))
                        .then(argument("jugador", StringArgumentType.word())
                                .executes(ctx -> {
                                    String targetName = StringArgumentType.getString(ctx, "jugador");
                                    ejecutarUnban(ctx.getSource(), targetName);
                                    return 1;
                                }))
        );

        // /history <player>
        dispatcher.register(
                literal("history")
                        .requires(src -> puedeModerar(src, false))
                        .then(argument("jugador", StringArgumentType.word())
                                .executes(ctx -> {
                                    String targetName = StringArgumentType.getString(ctx, "jugador");
                                    mostrarHistorial(ctx.getSource(), targetName);
                                    return 1;
                                }))
        );
    }

    private static boolean puedeModerar(ServerCommandSource src, boolean requiereAdmin) {
        if (src.hasPermissionLevel(requiereAdmin ? 3 : 2)) return true;
        if (src.getEntity() instanceof ServerPlayerEntity player) {
            var rank = StaffRoles.display(player.getUuid());
            if (rank == null) return false;
            if (requiereAdmin) {
                return rank == net.pokereport.luna.ui.Tablist.Rank.ADMIN
                        || rank == net.pokereport.luna.ui.Tablist.Rank.HEAD_ADMIN
                        || rank == net.pokereport.luna.ui.Tablist.Rank.OWNER;
            }
            return rank.esStaff();
        }
        return true; // consola
    }

    private static void ejecutarWarn(ServerCommandSource src, ServerPlayerEntity target, String motivo) {
        UUID staffUuid = src.getEntity() != null ? src.getEntity().getUuid() : UUID.nameUUIDFromBytes("CONSOLE".getBytes());
        String staffName = src.getName();

        ModerationService.aplicarCastigo(target.getUuid(), target.getGameProfile().getName(),
                staffUuid, staffName, ModerationService.Type.WARN, motivo, null);

        target.sendMessage(Text.literal("§e========================================"), false);
        target.sendMessage(Text.literal("§c§l¡ADVERTENCIA DE MODERACIÓN!"), false);
        target.sendMessage(Text.literal("§7Motivo: §f" + motivo), false);
        target.sendMessage(Text.literal("§7Staff: §e" + staffName), false);
        target.sendMessage(Text.literal("§e========================================"), false);

        src.sendFeedback(() -> Text.literal("§a[Moderación] Advertencia aplicada a §e" + target.getGameProfile().getName() + "§a: " + motivo), true);
    }

    private static void ejecutarKick(ServerCommandSource src, ServerPlayerEntity target, String motivo) {
        UUID staffUuid = src.getEntity() != null ? src.getEntity().getUuid() : UUID.nameUUIDFromBytes("CONSOLE".getBytes());
        String staffName = src.getName();

        ModerationService.aplicarCastigo(target.getUuid(), target.getGameProfile().getName(),
                staffUuid, staffName, ModerationService.Type.KICK, motivo, null);

        target.networkHandler.disconnect(Text.literal("§cHas sido expulsado del servidor.\n\n§7Motivo: §f" + motivo + "\n§7Staff: §e" + staffName));
        src.sendFeedback(() -> Text.literal("§a[Moderación] §e" + target.getGameProfile().getName() + " §afue expulsado por: " + motivo), true);
    }

    private static void ejecutarMute(ServerCommandSource src, ServerPlayerEntity target, Long durMs, String motivo) {
        UUID staffUuid = src.getEntity() != null ? src.getEntity().getUuid() : UUID.nameUUIDFromBytes("CONSOLE".getBytes());
        String staffName = src.getName();
        var tipo = durMs == null ? ModerationService.Type.MUTE : ModerationService.Type.TEMPMUTE;

        ModerationService.aplicarCastigo(target.getUuid(), target.getGameProfile().getName(),
                staffUuid, staffName, tipo, motivo, durMs);

        String durStr = durMs == null ? "permanentemente" : "por " + ModerationService.formatearRestante(System.currentTimeMillis() + durMs);
        target.sendMessage(Text.literal("§cHas sido silenciado " + durStr + ".\n§7Motivo: §f" + motivo), false);
        src.sendFeedback(() -> Text.literal("§a[Moderación] §e" + target.getGameProfile().getName() + " §asilenciado " + durStr + "."), true);
    }

    private static void ejecutarUnmute(ServerCommandSource src, String targetName) {
        MinecraftServer server = src.getServer();
        ServerPlayerEntity online = server.getPlayerManager().getPlayer(targetName);
        UUID targetUuid = online != null ? online.getUuid() : LunaEternal.players().uuidOf(targetName);

        if (targetUuid == null) {
            src.sendFeedback(() -> Text.literal("§cNo se encontró al jugador: " + targetName), false);
            return;
        }

        boolean ok = ModerationService.indultar(targetUuid, ModerationService.Type.MUTE,
                src.getEntity() != null ? src.getEntity().getUuid() : UUID.nameUUIDFromBytes("CONSOLE".getBytes()), src.getName());

        if (online != null) {
            online.sendMessage(Text.literal("§aTu sanción de silencio ha sido revocada."), false);
        }
        src.sendFeedback(() -> Text.literal("§a[Moderación] Silencio revocado para §e" + targetName), true);
    }

    private static void ejecutarBan(ServerCommandSource src, String targetName, Long durMs, String motivo) {
        MinecraftServer server = src.getServer();
        ServerPlayerEntity online = server.getPlayerManager().getPlayer(targetName);
        UUID targetUuid = online != null ? online.getUuid() : LunaEternal.players().uuidOf(targetName);

        if (targetUuid == null) {
            src.sendFeedback(() -> Text.literal("§cNo se encontró al jugador: " + targetName), false);
            return;
        }

        UUID staffUuid = src.getEntity() != null ? src.getEntity().getUuid() : UUID.nameUUIDFromBytes("CONSOLE".getBytes());
        String staffName = src.getName();
        var tipo = durMs == null ? ModerationService.Type.BAN : ModerationService.Type.TEMPBAN;

        ModerationService.aplicarCastigo(targetUuid, targetName, staffUuid, staffName, tipo, motivo, durMs);

        if (online != null) {
            String expira = durMs == null ? "§4Permanente" : "§e" + ModerationService.formatearRestante(System.currentTimeMillis() + durMs);
            online.networkHandler.disconnect(Text.literal("§cHas sido sancionado de PokeReport Network.\n\n"
                    + "§7Motivo: §f" + motivo + "\n"
                    + "§7Duración: " + expira + "\n"
                    + "§7Staff: §e" + staffName));
        }

        String durStr = durMs == null ? "permanentemente" : "por " + ModerationService.formatearRestante(System.currentTimeMillis() + durMs);
        src.sendFeedback(() -> Text.literal("§a[Moderación] §e" + targetName + " §abaneado " + durStr + " por: " + motivo), true);
    }

    private static void ejecutarUnban(ServerCommandSource src, String targetName) {
        UUID targetUuid = LunaEternal.players().uuidOf(targetName);
        if (targetUuid == null) {
            src.sendFeedback(() -> Text.literal("§cNo se encontró al jugador: " + targetName), false);
            return;
        }

        ModerationService.indultar(targetUuid, ModerationService.Type.BAN,
                src.getEntity() != null ? src.getEntity().getUuid() : UUID.nameUUIDFromBytes("CONSOLE".getBytes()), src.getName());

        src.sendFeedback(() -> Text.literal("§a[Moderación] Suspensión revocada para §e" + targetName), true);
    }

    private static void mostrarHistorial(ServerCommandSource src, String targetName) {
        UUID targetUuid = LunaEternal.players().uuidOf(targetName);
        if (targetUuid == null) {
            src.sendFeedback(() -> Text.literal("§cNo se encontró al jugador: " + targetName), false);
            return;
        }

        LunaEternal.submit(() -> {
            var historial = ModerationService.obtenerHistorial(targetUuid);
            src.sendFeedback(() -> Text.literal("§6=== Historial de Sanciones: §e" + targetName + " §6(" + historial.size() + ") ==="), false);
            if (historial.isEmpty()) {
                src.sendFeedback(() -> Text.literal("§7El jugador no tiene sanciones registradas."), false);
                return;
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            for (var p : historial) {
                String fecha = sdf.format(new Date(p.createdAt()));
                String estado = p.active() ? (p.isExpired() ? "§8(Expirado)" : "§a(Activo)") : "§7(Indultado)";
                src.sendFeedback(() -> Text.literal("§e#" + p.id() + " §6[" + p.type() + "] §f" + fecha
                        + " " + estado + "\n  §7Motivo: §f" + p.reason()
                        + " §7| Staff: §e" + p.staffName()), false);
            }
        });
    }
}
