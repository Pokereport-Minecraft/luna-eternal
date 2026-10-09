package net.pokereport.luna.world;

import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.border.WorldBorderListener;
import net.pokereport.luna.LunaEternal;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestión de bordes de mundo (WorldBorder) por dimensión.
 *
 * <p>En Vanilla Minecraft, {@code MinecraftServer} conecta todos los bordes de dimensiones
 * hijas al Overworld mediante {@code WorldBorderSyncer}. Esto causa que cualquier cambio
 * al borde del Overworld sobreescriba las demás dimensiones con su mismo tamaño.
 *
 * <p>Este servicio desvincula los escuchadores automáticos para que cada dimensión mantenga
 * su propio radio exacto de acuerdo a la pregeneración realizada (Chunky):
 * <ul>
 *   <li><b>Hogar (Overworld):</b> Radio 5.000 bloques (Diámetro 10.000).</li>
 *   <li><b>Mundos Salvajes (1 a 6):</b> Radio 3.000 bloques (Diámetro 6.000).</li>
 * </ul>
 */
public final class BordeMundoService {

    public static final double RADIO_HOGAR = 5000.0;
    public static final double RADIO_SALVAJE = 3000.0;

    private BordeMundoService() {}

    /**
     * Aplica los límites exactos de pregeneración y desvincula la propagación automática.
     */
    public static void aplicar(MinecraftServer server) {
        if (server == null) return;

        ServerWorld overworld = server.getOverworld();
        if (overworld == null) return;

        WorldBorder overworldBorder = overworld.getWorldBorder();

        // ⚠ Desvincular todas las dimensiones del syncer de Overworld ANTES de modificar el tamaño
        // de Overworld, para que no propague el radio de 5.000 a gimnasios o dimensiones personalizadas.
        for (ServerWorld sw : server.getWorlds()) {
            if (sw != overworld) {
                desincronizar(overworldBorder, sw.getWorldBorder());
            }
        }

        // 1. Configurar Overworld (Hogar): Radio 5.000, Diámetro 10.000
        overworldBorder.setCenter(0.0, 0.0);
        overworldBorder.setSize(RADIO_HOGAR * 2.0);
        overworldBorder.setDamagePerBlock(0.2);
        overworldBorder.setSafeZone(5.0);
        overworldBorder.setWarningBlocks(20);

        // 2. Configurar cada uno de los 6 mundos salvajes: Radio 3.000, Diámetro 6.000
        int salvajesConfigurados = 0;
        for (RegistryKey<World> clave : LunaDimensions.SALVAJES) {
            ServerWorld salvaje = server.getWorld(clave);
            if (salvaje == null) continue;

            WorldBorder salvajeBorder = salvaje.getWorldBorder();
            salvajeBorder.setCenter(0.0, 0.0);
            salvajeBorder.setSize(RADIO_SALVAJE * 2.0);
            salvajeBorder.setDamagePerBlock(0.2);
            salvajeBorder.setSafeZone(5.0);
            salvajeBorder.setWarningBlocks(20);
            salvajesConfigurados++;
        }

        // 3. Dimensiones de gimnasios, islas naranja, ciudadela, torre y lobby:
        // Deben tener el tamaño máximo vanilla (59999968.0) para que no limiten
        // gimnasios que están en X=5120..23000 (Sabrina, Blaine, Giovanni, Johto).
        RegistryKey<World>[] customDims = new RegistryKey[]{
            LunaDimensions.GIMNASIOS,
            LunaDimensions.ISLAS_NARANJA,
            LunaDimensions.GIMNASIO_MORTI,
            LunaDimensions.CIUDADELA,
            LunaDimensions.TORRE,
            LunaDimensions.LOBBY
        };
        for (RegistryKey<World> dimKey : customDims) {
            ServerWorld dimWorld = server.getWorld(dimKey);
            if (dimWorld == null) continue;
            WorldBorder b = dimWorld.getWorldBorder();
            b.setCenter(0.0, 0.0);
            b.setSize(59999968.0);
            b.setDamagePerBlock(0.0);
        }

        LunaEternal.LOG.info("BordeMundo: Hogar fijado en radio {} (tamaño {}). "
                        + "{} mundos salvajes fijados en radio {} (tamaño {}). Dimensiones de gimnasios y ciudadela liberadas.",
                (long) RADIO_HOGAR, (long) (RADIO_HOGAR * 2.0),
                salvajesConfigurados, (long) RADIO_SALVAJE, (long) (RADIO_SALVAJE * 2.0));
    }

    /**
     * Remueve el {@code WorldBorderSyncer} del Overworld que apunta al borde hijo.
     */
    public static void desincronizar(WorldBorder overworldBorder, WorldBorder hijoBorder) {
        try {
            Field listenersField = null;
            for (Field f : WorldBorder.class.getDeclaredFields()) {
                if (List.class.isAssignableFrom(f.getType())) {
                    listenersField = f;
                    break;
                }
            }
            if (listenersField == null) return;
            listenersField.setAccessible(true);

            @SuppressWarnings("unchecked")
            List<WorldBorderListener> listeners = (List<WorldBorderListener>) listenersField.get(overworldBorder);
            if (listeners == null) return;

            Field borderField = null;
            List<WorldBorderListener> toRemove = new ArrayList<>();

            for (WorldBorderListener l : listeners) {
                if (l instanceof WorldBorderListener.WorldBorderSyncer syncer) {
                    if (borderField == null) {
                        for (Field f : syncer.getClass().getDeclaredFields()) {
                            if (f.getType() == WorldBorder.class) {
                                borderField = f;
                                borderField.setAccessible(true);
                                break;
                            }
                        }
                    }
                    if (borderField != null && borderField.get(syncer) == hijoBorder) {
                        toRemove.add(syncer);
                    }
                }
            }

            for (WorldBorderListener l : toRemove) {
                overworldBorder.removeListener(l);
            }
        } catch (Exception e) {
            LunaEternal.LOG.warn("No se pudo desincronizar WorldBorderSyncer", e);
        }
    }

    public static int comandoEstado(ServerCommandSource source) {
        MinecraftServer server = source.getServer();
        source.sendMessage(Text.literal("§6=== ESTADO DE BORDES DE MUNDO (WORLDBORDER) ==="));

        ServerWorld overworld = server.getOverworld();
        if (overworld != null) {
            WorldBorder b = overworld.getWorldBorder();
            source.sendMessage(Text.literal(String.format(
                    "§eHogar (Overworld): §fCentro (%.1f, %.1f) · Radio: §a%.0f §f· Diámetro: §b%.0f",
                    b.getCenterX(), b.getCenterZ(), b.getSize() / 2.0, b.getSize())));
        }

        int index = 1;
        for (RegistryKey<World> clave : LunaDimensions.SALVAJES) {
            ServerWorld sw = server.getWorld(clave);
            if (sw != null) {
                WorldBorder b = sw.getWorldBorder();
                source.sendMessage(Text.literal(String.format(
                        "§2Salvaje %d (%s): §fCentro (%.1f, %.1f) · Radio: §a%.0f §f· Diámetro: §b%.0f",
                        index, clave.getValue().getPath(),
                        b.getCenterX(), b.getCenterZ(), b.getSize() / 2.0, b.getSize())));
            }
            index++;
        }
        return 1;
    }

    public static int comandoAplicar(ServerCommandSource source) {
        aplicar(source.getServer());
        source.sendMessage(Text.literal("§aBordes de mundo aplicados y desvinculados exitosamente."));
        return comandoEstado(source);
    }
}
