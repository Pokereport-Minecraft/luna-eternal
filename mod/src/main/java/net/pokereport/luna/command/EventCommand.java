package net.pokereport.luna.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.pokereport.luna.LunaEternal;
import net.pokereport.luna.pase.PaseService;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Comandos unificados de Eventos y Pase de Batalla de Temporada.
 * /event, /event pass, /event shop
 */
public final class EventCommand {

    private EventCommand() {}

    public static void registrar(CommandDispatcher<ServerCommandSource> dispatcher) {
        var root = CommandManager.literal("event")
                .executes(EventCommand::mostrarEvento)
                .then(CommandManager.literal("info")
                        .executes(EventCommand::mostrarEvento))
                .then(CommandManager.literal("pass")
                        .executes(EventCommand::mostrarPase))
                .then(CommandManager.literal("pase")
                        .executes(EventCommand::mostrarPase))
                .then(CommandManager.literal("shop")
                        .executes(EventCommand::mostrarTienda))
                .then(CommandManager.literal("tienda")
                        .executes(EventCommand::mostrarTienda));

        dispatcher.register(root);

        // Alias en español
        dispatcher.register(CommandManager.literal("evento")
                .executes(EventCommand::mostrarEvento)
                .then(CommandManager.literal("info")
                        .executes(EventCommand::mostrarEvento))
                .then(CommandManager.literal("pass")
                        .executes(EventCommand::mostrarPase))
                .then(CommandManager.literal("pase")
                        .executes(EventCommand::mostrarPase))
                .then(CommandManager.literal("shop")
                        .executes(EventCommand::mostrarTienda))
                .then(CommandManager.literal("tienda")
                        .executes(EventCommand::mostrarTienda)));
    }

    private static int mostrarEvento(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        if (!(src.getEntity() instanceof ServerPlayerEntity player)) {
            src.sendFeedback(() -> Text.literal("§cEste comando solo puede ejecutarse como jugador."), false);
            return 0;
        }

        var pase = LunaEternal.pase();
        String temporadaNombre = "Temporada Activa";
        int diasRestantes = 0;

        if (pase != null) {
            try {
                var temp = pase.temporada();
                if (temp != null) {
                    temporadaNombre = "Temporada " + temp.numero();
                    diasRestantes = temp.diasRestantes();
                }
            } catch (Exception ignored) {}
        }

        player.sendMessage(Text.literal("§8§m--------------------------------------------------"), false);
        player.sendMessage(Text.literal("       §d§l★ EVENTO DE TEMPORADA · POKEREPORT ★"), false);
        player.sendMessage(Text.literal("§8§m--------------------------------------------------"), false);
        player.sendMessage(Text.literal("§7Temporada actual: §e" + temporadaNombre), false);
        if (diasRestantes > 0) {
            player.sendMessage(Text.literal("§7Tiempo restante: §a" + diasRestantes + " días"), false);
        } else {
            player.sendMessage(Text.literal("§7Estado: §6Etapa final de temporada"), false);
        }
        player.sendMessage(Text.literal(""), false);
        player.sendMessage(Text.literal("§fComandos disponibles:"), false);

        MutableText botonPase = Text.literal("  §e• /event pass §7- Consulta tu nivel y premios del Pase de Batalla")
                .styled(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/event pass"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("§7Haz clic para ver el Pase"))));
        player.sendMessage(botonPase, false);

        MutableText botonTienda = Text.literal("  §e• /event shop §7- Explora artículos y recompensas del evento")
                .styled(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/event shop"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("§7Haz clic para ver la Tienda"))));
        player.sendMessage(botonTienda, false);

        player.sendMessage(Text.literal("§8§m--------------------------------------------------"), false);
        return 1;
    }

    private static int mostrarPase(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        if (!(src.getEntity() instanceof ServerPlayerEntity player)) {
            src.sendFeedback(() -> Text.literal("§cEste comando solo puede ejecutarse como jugador."), false);
            return 0;
        }

        var pase = LunaEternal.pase();
        if (pase == null) {
            player.sendMessage(Text.literal("§cEl sistema de Pase no está disponible en este momento."), false);
            return 0;
        }

        LunaEternal.submit(() -> {
            try {
                long playerId = LunaEternal.players().resolve(player.getUuid(), player.getGameProfile().getName());
                var estado = pase.estado(playerId);
                var temp = estado.temporada();
                boolean premium = estado.premium();

                player.getServer().execute(() -> {
                    if (player.isRemoved()) return;
                    player.sendMessage(Text.literal("§8§m--------------------------------------------------"), false);
                    player.sendMessage(Text.literal("          §6§l★ PASE DE BATALLA · TEMPORADA " + temp.numero() + " ★"), false);
                    player.sendMessage(Text.literal("§8§m--------------------------------------------------"), false);
                    player.sendMessage(Text.literal("§7Nivel actual: §e" + estado.nivel() + "§8/§e50"), false);
                    player.sendMessage(Text.literal("§7Puntos de XP: §b" + estado.xp() + " XP"), false);
                    player.sendMessage(Text.literal("§7XP acumulada hoy: §e" + estado.xpHoy() + "§8/§e" + estado.topeHoy() + " XP"), false);
                    player.sendMessage(Text.literal("§7Vía Premium (Luna): " + (premium ? "§a¡DESBLOQUEADA! ✔" : "§cNo desbloqueada (15.000 LunaCoins)")), false);
                    player.sendMessage(Text.literal(""), false);
                    player.sendMessage(Text.literal("§7Gana XP completando misiones, capturas de Pokédex y combates en la Torre."), false);
                    player.sendMessage(Text.literal("§8§m--------------------------------------------------"), false);
                });
            } catch (Exception e) {
                player.sendMessage(Text.literal("§cError consultando el progreso del Pase."), false);
            }
        });

        return 1;
    }

    private static int mostrarTienda(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        if (!(src.getEntity() instanceof ServerPlayerEntity player)) {
            src.sendFeedback(() -> Text.literal("§cEste comando solo puede ejecutarse como jugador."), false);
            return 0;
        }

        player.sendMessage(Text.literal("§8§m--------------------------------------------------"), false);
        player.sendMessage(Text.literal("         §6§l★ TIENDA Y RECOMPENSAS DE TEMPORADA ★"), false);
        player.sendMessage(Text.literal("§8§m--------------------------------------------------"), false);
        player.sendMessage(Text.literal("§7Visita a los mercaderes y centros oficiales en la Ciudadela:"), false);
        player.sendMessage(Text.literal("  §e• Buhonero Misterioso §7- Intercambio de objetos especiales."), false);
        player.sendMessage(Text.literal("  §e• Tienda de Ciudadela §7- Bayas competitivas, MTs y equipamiento."), false);
        player.sendMessage(Text.literal("  §e• Vendedor de Crianza §7- Lazos Destino, Piedras Eternas y Brazales."), false);
        player.sendMessage(Text.literal(""), false);
        player.sendMessage(Text.literal("§aTip: Usa §e/viajes §apara desplazarte de forma inmediata."), false);
        player.sendMessage(Text.literal("§8§m--------------------------------------------------"), false);
        return 1;
    }
}
