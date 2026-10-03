package net.pokereport.luna.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import java.nio.file.*;
import java.util.*;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.OperatorEntry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;
import net.pokereport.luna.LunaEternal;
import net.pokereport.luna.world.WorldEditPermissions;
import net.pokereport.luna.world.WorldProtectionManager;

/** Live staff grants, persisted to the same files used by the offline tool. */
public final class StaffRoleCommand {
    private static final Map<String, Integer> LEVELS = Map.of("player", 0, "moderator", 3,
            "builder", 4, "developer", 4, "developer-edit", 4, "admin", 4, "head-admin", 4, "owner", 4);
    private StaffRoleCommand() {}

    public static boolean authorized(ServerCommandSource source) {
        return source.getEntity() == null && source.hasPermissionLevel(4)
                || source.getEntity() instanceof ServerPlayerEntity p && p.hasPermissionLevel(4)
                && WorldEditPermissions.allowed(p.getUuid(), WorldEditPermissions.COMMANDS);
    }

    public static com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> node() {
        return CommandManager.literal("rol").requires(StaffRoleCommand::authorized)
                .then(CommandManager.literal("lista").executes(ctx -> {
                    ctx.getSource().sendFeedback(() -> Text.literal("Roles: " + String.join(", ", new TreeSet<>(LEVELS.keySet()))
                            + ". Usar /pradmin rol asignar <jugador conectado> <rol>; player retira acceso."), false);
                    return 1;
                }))
                .then(CommandManager.literal("asignar")
                        .then(CommandManager.argument("jugador", EntityArgumentType.player())
                                .then(CommandManager.argument("rol", StringArgumentType.word())
                                        .suggests((ctx, builder) -> net.minecraft.command.CommandSource.suggestMatching(LEVELS.keySet(), builder))
                                        .executes(ctx -> assign(ctx.getSource(), EntityArgumentType.getPlayer(ctx, "jugador"),
                                                StringArgumentType.getString(ctx, "rol"))))));
    }

    public static int assign(ServerCommandSource source, ServerPlayerEntity target, String role) {
        if (!authorized(source)) return 0;
        role = role.toLowerCase(Locale.ROOT);
        if (!LEVELS.containsKey(role)) { source.sendError(Text.literal("Rol desconocido. Usa /pradmin rol lista.")); return 0; }
        var id = target.getUuid();
        if (net.pokereport.luna.puerta.Puerta.bloqueado(target)) {
            source.sendError(Text.literal("El jugador debe autenticarse y salir del lobby antes de recibir staff."));
            return 0;
        }
        var ops = source.getServer().getPlayerManager().getOpList();
        var profile = target.getGameProfile();
        var previousOp = ops.get(profile);
        Path file = Path.of("config", "lunaeternal", "world-edit.properties");
        byte[] original = null;
        boolean present = Files.exists(file);
        boolean changedOp = false;
        var previousGrants = WorldEditPermissions.snapshot();
        var previousRoles = net.pokereport.luna.world.StaffRoles.snapshot();
        Path roleFile = net.pokereport.luna.world.StaffRoles.FILE;
        boolean rolePresent = Files.exists(roleFile);
        byte[] originalRoles = null;
        boolean changedFiles = false;
        try {
            if (rolePresent) originalRoles = Files.readAllBytes(roleFile);
            var roles = new HashMap<>(net.pokereport.luna.world.StaffRoles.read());
            roles.remove(id);
            if (!role.equals("player")) roles.put(id, role);
            // Read disk rather than silently overwriting changes from other tools.
            Properties properties = new Properties();
            if (present) { original = Files.readAllBytes(file); try (var input = new java.io.ByteArrayInputStream(original)) { properties.load(input); } }
            Map<UUID, Set<String>> updated = new HashMap<>();
            for (String key : properties.stringPropertyNames()) {
                var nodes = Set.of(properties.getProperty(key).strip().split("\\s*,\\s*"));
                if (!Set.of(WorldEditPermissions.EDIT, WorldEditPermissions.MODE, WorldEditPermissions.COMMANDS).containsAll(nodes))
                    throw new IllegalArgumentException("Archivo de permisos inválido");
                updated.put(UUID.fromString(key), nodes);
            }
            Set<String> nodes = switch (role) {
                case "player", "moderator" -> Set.of();
                case "builder" -> Set.of(WorldEditPermissions.EDIT, WorldEditPermissions.MODE);
                case "developer" -> Set.of(WorldEditPermissions.COMMANDS);
                default -> Set.of(WorldEditPermissions.EDIT, WorldEditPermissions.MODE, WorldEditPermissions.COMMANDS);
            };
            updated.remove(id);
            if (!nodes.isEmpty()) updated.put(id, nodes);
            Path backup = Path.of(".staff-permission-backups", "live-" + UUID.randomUUID());
            Files.createDirectories(backup);
            if (original != null) Files.write(backup.resolve("world-edit.properties"), original);
            Files.writeString(backup.resolve("grants-originally-present.txt"), Boolean.toString(present));
            if (originalRoles != null) Files.write(backup.resolve("staff-roles.properties"), originalRoles);
            Files.writeString(backup.resolve("roles-originally-present.txt"), Boolean.toString(rolePresent));
            Files.copy(ops.getFile().toPath(), backup.resolve("ops.json"));
            if (Files.getFileStore(backup).supportsFileAttributeView("posix")) {
                Files.setPosixFilePermissions(backup, java.nio.file.attribute.PosixFilePermissions.fromString("rwx------"));
                try (var entries = Files.list(backup)) {
                    for (Path entry : entries.toList()) Files.setPosixFilePermissions(entry,
                            java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
                }
            }
            StringBuilder content = new StringBuilder("# Explicit staff UUID grants\n");
            updated.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> content.append(entry.getKey())
                    .append('=').append(String.join(",", new TreeSet<>(entry.getValue()))).append('\n'));
            Files.createDirectories(file.getParent());
            changedFiles = true;
            Path temporary = Files.createTempFile(file.getParent(), "staff-", ".tmp");
            try {
                Files.writeString(temporary, content);
                if (present && Files.getFileStore(file).supportsFileAttributeView("posix"))
                    Files.setPosixFilePermissions(temporary, Files.getPosixFilePermissions(file));
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } finally { Files.deleteIfExists(temporary); }
            StringBuilder roleContent = new StringBuilder("# Operational staff display roles; commercial ranks remain in DB\n");
            roles.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> roleContent.append(entry.getKey())
                    .append('=').append(entry.getValue()).append('\n'));
            Path roleTemporary = Files.createTempFile(roleFile.getParent(), "staff-role-", ".tmp");
            try {
                Files.writeString(roleTemporary, roleContent);
                Files.move(roleTemporary, roleFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } finally { Files.deleteIfExists(roleTemporary); }
            changedOp = true;
            if (LEVELS.get(role) == 0) ops.remove(profile);
            else ops.add(new OperatorEntry(profile, LEVELS.get(role), false));
            ops.save();
            WorldEditPermissions.replace(updated);
            net.pokereport.luna.world.StaffRoles.replace(roles);
            WorldProtectionManager.setEditing(target, false);
            if (WorldProtectionManager.isProtectedDimension(target.getWorld())) target.changeGameMode(GameMode.ADVENTURE);
            source.getServer().getPlayerManager().sendCommandTree(target);
            var clan = net.pokereport.luna.ui.Tablist.clanDe(target);
            if (clan == null) net.pokereport.luna.ui.Tablist.aplicarEtiqueta(source.getServer(), target, "", 'b');
            else net.pokereport.luna.ui.Tablist.aplicarEtiqueta(source.getServer(), target, clan.etiqueta(), clan.color(),
                    clan.colorInicio(), clan.colorFin(), clan.negrita(), clan.cursiva());
            String assignedRole = role;
            LunaEternal.LOG.info("[STAFF-ROLE] actor={} target={} uuid={} role={} backup={}", source.getName(), profile.getName(), id, role, backup);
            source.sendFeedback(() -> Text.literal("Rol " + assignedRole + " asignado a " + profile.getName()
                    + ". Edición desactivada; quien tenga permiso puede usar /pradmin editmode on."), false);
            target.sendMessage(Text.literal("Tu rol administrativo ahora es: " + role), false);
            return 1;
        } catch (Exception e) {
            WorldEditPermissions.replace(previousGrants);
            net.pokereport.luna.world.StaffRoles.replace(previousRoles);
            try {
                if (changedFiles) {
                    if (original != null) Files.write(file, original);
                    else if (!present) Files.deleteIfExists(file);
                    if (originalRoles != null) Files.write(roleFile, originalRoles);
                    else if (!rolePresent) Files.deleteIfExists(roleFile);
                }
                if (changedOp) { if (previousOp == null) ops.remove(profile); else ops.add(previousOp); ops.save(); }
            } catch (Exception rollback) { e.addSuppressed(rollback); }
            LunaEternal.LOG.error("No se pudo guardar el rol de {}", id, e);
            source.sendError(Text.literal("No se aplicó el rol. Revisar logs/backup antes de reintentar."));
            return 0;
        }
    }
}
