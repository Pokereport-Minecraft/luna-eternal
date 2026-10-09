package net.pokereport.luna.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.pokereport.luna.LunaEternal;
import net.pokereport.luna.progression.TrainerLicense;
import net.pokereport.luna.progression.TrainerLicenseService;

import java.util.UUID;

/**
 * Comandos de Licencias de Entrenador (Progresión 2.0).
 * /licencia, /licencia ascender, /licencia set <jugador> <licencia>
 */
public final class LicenciaCommand {

    private LicenciaCommand() {}

    public static void registrar(CommandDispatcher<ServerCommandSource> dispatcher) {
        var root = CommandManager.literal("licencia")
                .executes(LicenciaCommand::mostrarLicencia)
                .then(CommandManager.literal("info")
                        .executes(LicenciaCommand::mostrarLicencia))
                .then(CommandManager.literal("ascender")
                        .executes(LicenciaCommand::ascenderLicencia))
                .then(CommandManager.literal("upgrade")
                        .executes(LicenciaCommand::ascenderLicencia))
                .then(CommandManager.literal("set")
                        .requires(src -> src.hasPermissionLevel(2))
                        .then(CommandManager.argument("jugador", EntityArgumentType.player())
                                .then(CommandManager.argument("tipo", StringArgumentType.word())
                                        .executes(LicenciaCommand::setLicencia))));

        dispatcher.register(root);

        // Alias en inglés
        dispatcher.register(CommandManager.literal("license")
                .executes(LicenciaCommand::mostrarLicencia)
                .then(CommandManager.literal("info")
                        .executes(LicenciaCommand::mostrarLicencia))
                .then(CommandManager.literal("upgrade")
                        .executes(LicenciaCommand::ascenderLicencia)));
    }

    private static int mostrarLicencia(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        if (!(src.getEntity() instanceof ServerPlayerEntity player)) {
            src.sendFeedback(() -> Text.literal("§cEste comando solo puede ejecutarse como jugador."), false);
            return 0;
        }

        UUID uuid = player.getUuid();
        TrainerLicense current = TrainerLicenseService.getLicense(uuid);
        var service = LunaEternal.licenses();

        long playerId;
        try {
            playerId = LunaEternal.players().resolve(uuid, player.getGameProfile().getName());
        } catch (Exception e) {
            player.sendMessage(Text.literal("§cNo se pudo consultar el perfil de entrenador."), false);
            return 0;
        }

        var check = service != null ? service.checkUpgrade(player, playerId) : null;

        player.sendMessage(Text.literal("§8§m--------------------------------------------------"), false);
        player.sendMessage(Text.literal("       §6§l★ LIGA POKEREPORT · LICENCIA DE ENTRENADOR ★"), false);
        player.sendMessage(Text.literal("§8§m--------------------------------------------------"), false);
        player.sendMessage(Text.literal("§7Licencia actual: §r" + current.formatted + " §8[Nivel " + (current.ordinal() + 1) + "]"), false);

        if (current.next() == null) {
            player.sendMessage(Text.literal("§a¡Has alcanzado la máxima distinción oficial de la Liga!"), false);
            player.sendMessage(Text.literal("§eBeneficios: Acceso irrestricto a todas las zonas, crianza y torneos."), false);
        } else {
            TrainerLicense next = current.next();
            player.sendMessage(Text.literal("§7Siguiente ascenso: §r" + next.formatted), false);
            player.sendMessage(Text.literal("§fRequisitos necesarios:"), false);

            if (check != null) {
                String badgeMark = check.badgesHave() >= check.badgesNeed() ? "§a✔" : "§c✘";
                String dexMark = check.dexHave() >= check.dexNeed() ? "§a✔" : "§c✘";
                String questMark = check.questsHave() >= check.questsNeed() ? "§a✔" : "§c✘";
                String moneyMark = check.moneyHave() >= check.moneyNeed() ? "§a✔" : "§c✘";

                player.sendMessage(Text.literal("  " + badgeMark + " §7Medallas de Gimnasio: §e" + check.badgesHave() + "§8/§e" + check.badgesNeed()), false);
                player.sendMessage(Text.literal("  " + dexMark + " §7Pokédex Gen 1-2: §e" + check.dexHave() + "§8/§e" + check.dexNeed()
                        + " §8(" + String.format("%.1f", next.requiredDexPct * 100) + "%)"), false);
                player.sendMessage(Text.literal("  " + questMark + " §7Misiones Completadas: §e" + check.questsHave() + "§8/§e" + check.questsNeed()), false);
                player.sendMessage(Text.literal("  " + moneyMark + " §7Tasa Oficial: §e" + check.moneyNeed() + " PokéDólares §8(Saldo: §e" + check.moneyHave() + "§8)"), false);

                player.sendMessage(Text.literal(""), false);
                if (check.canUpgrade()) {
                    MutableText boton = Text.literal("  §a§l[¡SOLICITAR ASCENSO AQUÍ!]§r §7(Clic)")
                            .styled(style -> style
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/licencia ascender"))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("§aHaz clic para ascender a " + next.formatted))));
                    player.sendMessage(boton, false);
                } else {
                    player.sendMessage(Text.literal("  §c" + check.failReason()), false);
                }
            }
        }
        player.sendMessage(Text.literal("§8§m--------------------------------------------------"), false);
        return 1;
    }

    private static int ascenderLicencia(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        if (!(src.getEntity() instanceof ServerPlayerEntity player)) {
            src.sendFeedback(() -> Text.literal("§cEste comando solo puede ejecutarse como jugador."), false);
            return 0;
        }

        var service = LunaEternal.licenses();
        if (service == null) {
            player.sendMessage(Text.literal("§cEl servicio de licencias no está disponible temporalmente."), false);
            return 0;
        }

        service.upgrade(player);
        return 1;
    }

    private static int setLicencia(CommandContext<ServerCommandSource> ctx) {
        try {
            ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "jugador");
            String tipoStr = StringArgumentType.getString(ctx, "tipo");
            TrainerLicense lic = TrainerLicense.de(tipoStr);

            var service = LunaEternal.licenses();
            if (service == null) {
                ctx.getSource().sendFeedback(() -> Text.literal("§cServicio no disponible."), false);
                return 0;
            }

            long playerId = LunaEternal.players().resolve(target.getUuid(), target.getGameProfile().getName());
            service.save(playerId, lic);
            TrainerLicenseService.setLicenseCache(target.getUuid(), lic);

            ctx.getSource().sendFeedback(() -> Text.literal("§aLicencia de " + target.getName().getString() + " establecida en " + lic.formatted + "§a."), true);
            target.sendMessage(Text.literal("§eUn administrador ha actualizado tu Licencia a §r" + lic.formatted + "§e."), false);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFeedback(() -> Text.literal("§cError: " + e.getMessage()), false);
            return 0;
        }
    }
}
