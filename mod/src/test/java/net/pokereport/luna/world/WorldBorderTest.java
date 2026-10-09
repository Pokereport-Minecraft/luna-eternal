package net.pokereport.luna.world;

import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.border.WorldBorderListener;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WorldBorderTest {

    @Test
    @DisplayName("Desincronización de WorldBorderSyncer previene sobreescritura de dimensiones")
    void testDesyncWorldBorderSyncer() throws Exception {
        WorldBorder overworldBorder = new WorldBorder();
        WorldBorder salvajeBorder = new WorldBorder();

        // Estado inicial vanilla: Overworld a 60M, salvaje conectado via syncer
        overworldBorder.setSize(59999968.0);
        salvajeBorder.setSize(59999968.0);

        WorldBorderListener.WorldBorderSyncer syncer = new WorldBorderListener.WorldBorderSyncer(salvajeBorder);
        overworldBorder.addListener(syncer);

        // Si overworld cambia, syncer actualiza salvaje
        overworldBorder.setSize(10000.0);
        assertEquals(10000.0, salvajeBorder.getSize(), "Syncer debió propagar cambio a salvaje");

        // Desincronización: remover syncer
        overworldBorder.removeListener(syncer);

        // Ahora salvaje se fija en 6000 (radio 3000)
        salvajeBorder.setSize(6000.0);
        assertEquals(6000.0, salvajeBorder.getSize());

        // Si overworld cambia otra vez, salvaje NO cambia
        overworldBorder.setSize(12000.0);
        assertEquals(6000.0, salvajeBorder.getSize(), "Salvaje debe mantener su propio tamaño tras desincronizar");
        assertEquals(12000.0, overworldBorder.getSize());
    }

    @Test
    @DisplayName("Identificar y remover syncers usando reflexión sobre campos")
    void testReflectionRemovalOfSyncer() throws Exception {
        WorldBorder overworldBorder = new WorldBorder();
        WorldBorder salvajeBorder = new WorldBorder();

        WorldBorderListener.WorldBorderSyncer syncer = new WorldBorderListener.WorldBorderSyncer(salvajeBorder);
        overworldBorder.addListener(syncer);

        // Obtener listeners mediante getListeners() o campo privado listeners
        Field listenersField = WorldBorder.class.getDeclaredField("listeners");
        listenersField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<WorldBorderListener> listeners = (List<WorldBorderListener>) listenersField.get(overworldBorder);

        assertNotNull(listeners);
        assertTrue(listeners.contains(syncer));

        // Encontrar el campo 'border' en WorldBorderSyncer
        Field syncerBorderField = null;
        for (Field f : WorldBorderListener.WorldBorderSyncer.class.getDeclaredFields()) {
            if (f.getType() == WorldBorder.class) {
                syncerBorderField = f;
                break;
            }
        }
        assertNotNull(syncerBorderField, "Debe existir un campo WorldBorder en WorldBorderSyncer");
        syncerBorderField.setAccessible(true);

        List<WorldBorderListener> toRemove = new ArrayList<>();
        for (WorldBorderListener l : listeners) {
            if (l instanceof WorldBorderListener.WorldBorderSyncer s) {
                if (syncerBorderField.get(s) == salvajeBorder) {
                    toRemove.add(s);
                }
            }
        }

        assertEquals(1, toRemove.size());
        for (WorldBorderListener l : toRemove) {
            overworldBorder.removeListener(l);
        }

        assertFalse(listeners.contains(syncer));
    }

    @Test
    @DisplayName("BordeMundoService.desincronizar remueve correctamente el syncer")
    void testBordeMundoServiceDesincronizarMethod() {
        WorldBorder overworldBorder = new WorldBorder();
        WorldBorder salvajeBorder = new WorldBorder();

        WorldBorderListener.WorldBorderSyncer syncer = new WorldBorderListener.WorldBorderSyncer(salvajeBorder);
        overworldBorder.addListener(syncer);

        // Desincronizar usando BordeMundoService
        BordeMundoService.desincronizar(overworldBorder, salvajeBorder);

        overworldBorder.setSize(10000.0);
        // salvajeBorder no debe haber cambiado con overworld
        assertNotEquals(10000.0, salvajeBorder.getSize());

        salvajeBorder.setSize(6000.0);
        assertEquals(6000.0, salvajeBorder.getSize());
        assertEquals(10000.0, overworldBorder.getSize());
    }

    @Test
    @DisplayName("Gimnasios con X > 5000 (Sabrina, Blaine, Giovanni, Johto) están dentro del borde liberado")
    void testGymCoordinatesInsideLiberatedBorder() {
        WorldBorder border = new WorldBorder();
        border.setCenter(0.0, 0.0);
        border.setSize(59999968.0);

        // Koga: 4096 (entraba antes)
        assertTrue(border.contains(new net.minecraft.util.math.BlockPos(4096, 64, 0)));
        // Sabrina: 5120 (quedaba fuera con radio 5000)
        assertTrue(border.contains(new net.minecraft.util.math.BlockPos(5120, 64, 0)));
        // Blaine: 6144
        assertTrue(border.contains(new net.minecraft.util.math.BlockPos(6144, 64, 0)));
        // Giovanni: 7168
        assertTrue(border.contains(new net.minecraft.util.math.BlockPos(7168, 64, 0)));
        // Pegaso: 14336
        assertTrue(border.contains(new net.minecraft.util.math.BlockPos(14336, 64, 0)));
        // Lance: 22528
        assertTrue(border.contains(new net.minecraft.util.math.BlockPos(22528, 64, 0)));
    }
}
