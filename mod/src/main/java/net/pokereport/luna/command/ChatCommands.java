package net.pokereport.luna.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.pokereport.luna.chat.ChatChannel;
import net.pokereport.luna.chat.ChatChannelService;
import net.pokereport.luna.chat.ChatDimensionPolicy;
import net.pokereport.luna.chat.ServerAnnouncementService;
import net.pokereport.luna.ui.Tablist;

import java.util.UUID;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class ChatCommands {

    private ChatCommands() {}

    public static void registrar(CommandDispatcher<ServerCommandSource> dispatcher) {
        // /canal y /channel
        dispatcher.register(literal("canal")
                .executes(ctx -> mostrarCanalActual(ctx.getSource()))
                .then(argument("nombre", StringArgumentType.word())
                        .executes(ctx -> cambiarCanal(ctx.getSource(), StringArgumentType.getString(ctx, "nombre")))));

        dispatcher.register(literal("channel")
                .executes(ctx -> mostrarCanalActual(ctx.getSource()))
                .then(argument("nombre", StringArgumentType.word())
                        .executes(ctx -> cambiarCanal(ctx.getSource(), StringArgumentType.getString(ctx, "nombre")))));

        // Atajos de canal
        registrarAtajo(dispatcher, "g", ChatChannel.GLOBAL);
        registrarAtajo(dispatcher, "global", ChatChannel.GLOBAL);

        registrarAtajo(dispatcher, "l", ChatChannel.LOCAL);
        registrarAtajo(dispatcher, "local", ChatChannel.LOCAL);

        registrarAtajo(dispatcher, "tr", ChatChannel.COMERCIO);
        registrarAtajo(dispatcher, "trade", ChatChannel.COMERCIO);
        registrarAtajo(dispatcher, "comercio", ChatChannel.COMERCIO);

        registrarAtajo(dispatcher, "c", ChatChannel.CLAN);
        // /clan se registra para mensajería si no hay subcomando existente
        dispatcher.register(literal("c")
                .executes(ctx -> cambiarCanal(ctx.getSource(), "clan"))
                .then(argument("mensaje", StringArgumentType.greedyString())
                        .executes(ctx -> enviarMensajeCanal(ctx.getSource(), ChatChannel.CLAN, StringArgumentType.getString(ctx, "mensaje")))));

        registrarAtajo(dispatcher, "sc", ChatChannel.STAFF);
        registrarAtajo(dispatcher, "staff", ChatChannel.STAFF);

        // /msg, /tell, /w
        dispatcher.register(literal("msg")
                .then(argument("objetivo", EntityArgumentType.player())
                        .then(argument("mensaje", StringArgumentType.greedyString())
                                .executes(ctx -> enviarPrivado(ctx.getSource(), EntityArgumentType.getPlayer(ctx, "objetivo"), StringArgumentType.getString(ctx, "mensaje"))))));

        dispatcher.register(literal("tell")
                .then(argument("objetivo", EntityArgumentType.player())
                        .then(argument("mensaje", StringArgumentType.greedyString())
                                .executes(ctx -> enviarPrivado(ctx.getSource(), EntityArgumentType.getPlayer(ctx, "objetivo"), StringArgumentType.getString(ctx, "mensaje"))))));

        dispatcher.register(literal("w")
                .then(argument("objetivo", EntityArgumentType.player())
                        .then(argument("mensaje", StringArgumentType.greedyString())
                                .executes(ctx -> enviarPrivado(ctx.getSource(), EntityArgumentType.getPlayer(ctx, "objetivo"), StringArgumentType.getString(ctx, "mensaje"))))));

        // /r (reply)
        dispatcher.register(literal("r")
                .then(argument("mensaje", StringArgumentType.greedyString())
                        .executes(ctx -> responderPrivado(ctx.getSource(), StringArgumentType.getString(ctx, "mensaje")))));

        dispatcher.register(literal("reply")
                .then(argument("mensaje", StringArgumentType.greedyString())
                        .executes(ctx -> responderPrivado(ctx.getSource(), StringArgumentType.getString(ctx, "mensaje")))));

        // Anuncios administrativos
        dispatcher.register(literal("anuncios")
                .requires(source -> source.hasPermissionLevel(2))
                .then(literal("reload")
                        .executes(ctx -> {
                            ServerAnnouncementService.reset();
                            ctx.getSource().sendFeedback(() -> Text.literal("§aAnuncios oficiales reiniciados y recargados."), false);
                            return 1;
                        }))
                .then(literal("interval")
                        .then(argument("minutos", IntegerArgumentType.integer(1, 60))
                                .executes(ctx -> {
                                    int min = IntegerArgumentType.getInteger(ctx, "minutos");
                                    ServerAnnouncementService.setIntervalMinutes(min);
                                    ctx.getSource().sendFeedback(() -> Text.literal("§aIntervalo de anuncios configurado a " + min + " minutos."), false);
                                    return 1;
                                })))
                .then(literal("test")
                        .executes(ctx -> {
                            var a = ServerAnnouncementService.getNextAnnouncement();
                            if (a != null) {
                                ServerAnnouncementService.broadcastAnnouncement(ctx.getSource().getServer(), a);
                                ctx.getSource().sendFeedback(() -> Text.literal("§aAnuncio de prueba enviado: " + a.id()), false);
                            }
                            return 1;
                        })));
    }

    private static void registrarAtajo(CommandDispatcher<ServerCommandSource> dispatcher, String alias, ChatChannel canal) {
        dispatcher.register(literal(alias)
                .executes(ctx -> cambiarCanal(ctx.getSource(), canal.getId()))
                .then(argument("mensaje", StringArgumentType.greedyString())
                        .executes(ctx -> enviarMensajeCanal(ctx.getSource(), canal, StringArgumentType.getString(ctx, "mensaje")))));
    }

    private static int mostrarCanalActual(ServerCommandSource source) {
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            source.sendFeedback(() -> Text.literal("§cComando exclusivo para jugadores."), false);
            return 0;
        }
        ChatChannel actual = ChatChannelService.getActiveChannel(player);
        player.sendMessage(Text.literal("§6§lPOKEREPORT §8» §7Tu canal actual es: " + actual.getTag()), false);
        player.sendMessage(Text.literal("§7Canales disponibles: §a/g §7(Global), §e/l §7(Local), §6/tr §7(Comercio), §b/c §7(Clan), §c/sc §7(Staff)"), false);
        return 1;
    }

    private static int cambiarCanal(ServerCommandSource source, String nombre) {
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            source.sendFeedback(() -> Text.literal("§cComando exclusivo para jugadores."), false);
            return 0;
        }

        ChatChannel nuevo = ChatChannel.fromString(nombre);

        if (nuevo == ChatChannel.STAFF) {
            Tablist.Rank rank = Tablist.rankOf(source.getServer(), player);
            if (rank == null || !rank.esStaff()) {
                player.sendMessage(Text.literal("§cNo tienes permisos para acceder al canal de Staff."), false);
                return 0;
            }
        }

        if (nuevo == ChatChannel.CLAN) {
            Tablist.ClanInfo clan = Tablist.clanDe(player);
            if (clan == null || clan.etiqueta().isEmpty()) {
                player.sendMessage(Text.literal("§cNo perteneces a ningún clan."), false);
                return 0;
            }
        }

        ChatChannelService.setActiveChannel(player, nuevo);
        player.sendMessage(Text.literal("§6§lPOKEREPORT §8» §7Canal activo cambiado a: " + nuevo.getTag()), false);
        return 1;
    }

    private static int enviarMensajeCanal(ServerCommandSource source, ChatChannel canal, String mensaje) {
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            source.sendFeedback(() -> Text.literal("§cComando exclusivo para jugadores."), false);
            return 0;
        }
        boolean ok = ChatChannelService.broadcastMessage(player, canal, mensaje);
        return ok ? 1 : 0;
    }

    private static int enviarPrivado(ServerCommandSource source, ServerPlayerEntity target, String mensaje) {
        if (!(source.getEntity() instanceof ServerPlayerEntity sender)) {
            source.sendFeedback(() -> Text.literal("§cComando exclusivo para jugadores."), false);
            return 0;
        }
        boolean ok = ChatChannelService.sendPrivateMessage(sender, target, mensaje);
        return ok ? 1 : 0;
    }

    private static int responderPrivado(ServerCommandSource source, String mensaje) {
        if (!(source.getEntity() instanceof ServerPlayerEntity sender)) {
            source.sendFeedback(() -> Text.literal("§cComando exclusivo para jugadores."), false);
            return 0;
        }
        UUID ultimo = ChatChannelService.getLastPrivateSender(sender.getUuid());
        if (ultimo == null) {
            sender.sendMessage(Text.literal("§6§lPOKEREPORT §8» §cNo tienes a nadie a quien responder."), false);
            return 0;
        }
        ServerPlayerEntity target = source.getServer().getPlayerManager().getPlayer(ultimo);
        if (target == null) {
            sender.sendMessage(Text.literal("§6§lPOKEREPORT §8» §cEl jugador ya no está conectado."), false);
            return 0;
        }
        boolean ok = ChatChannelService.sendPrivateMessage(sender, target, mensaje);
        return ok ? 1 : 0;
    }
}
