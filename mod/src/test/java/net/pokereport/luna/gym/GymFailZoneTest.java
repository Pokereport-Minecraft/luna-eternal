package net.pokereport.luna.gym;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.util.math.Vec3d;

import static org.junit.jupiter.api.Assertions.*;

public class GymFailZoneTest {

    private static final double MIN_X = 5182.564;
    private static final double MAX_X = 5200.490;
    private static final double REL_MIN_Z = 153.509;
    private static final double REL_MAX_Z = 221.492;
    private static final double MAX_Y = 77.0;

    private boolean estaEnZonaCaida(double px, double py, double pz, int slot) {
        double minZ = REL_MIN_Z + slot * Gimnasio.PASO_RANURA;
        double maxZ = REL_MAX_Z + slot * Gimnasio.PASO_RANURA;
        return px >= MIN_X && px <= MAX_X && pz >= minZ && pz <= maxZ && py <= MAX_Y;
    }

    @Test
    @DisplayName("Sabrina: Delimitación de zona de caída precisa en todas las ranuras")
    void testSabrinaFailZoneCoordinatesAcrossSlots() {
        for (int slot = 0; slot < Gimnasio.RANURAS; slot++) {
            // Punto central dentro del foso de caída
            double midX = (MIN_X + MAX_X) / 2.0;
            double midY = 60.0; // En el foso, bajo Y=77
            double midZ = (REL_MIN_Z + REL_MAX_Z) / 2.0 + slot * Gimnasio.PASO_RANURA;

            assertTrue(estaEnZonaCaida(midX, midY, midZ, slot),
                    "El centro del foso en ranura " + slot + " debe ser detectado como zona de caída");

            // Punto justo por encima del umbral de caída (Y = 78.0)
            assertFalse(estaEnZonaCaida(midX, 78.0, midZ, slot),
                    "Punto por encima de Y=77 no debe ser detectado como caída");

            // Plataforma del líder (X = 5161.603, Y = 71.0, Z = 52.524 + slot*128)
            Vec3d lider = Gimnasio.lider(Gimnasio.de("sabrina"), slot);
            assertFalse(estaEnZonaCaida(lider.x, lider.y, lider.z, slot),
                    "La tarima de combate de Sabrina en ranura " + slot + " NUNCA debe estar en la zona de caída");

            // Punto de entrada al gimnasio (X = 5189.947, Y = 95.0, Z = 90.78 + slot*128)
            Vec3d entrada = Gimnasio.entrada(Gimnasio.de("sabrina"), slot);
            assertFalse(estaEnZonaCaida(entrada.x, entrada.y, entrada.z, slot),
                    "El punto de entrada al gimnasio en ranura " + slot + " no debe estar en zona de caída");
        }
    }

    @Test
    @DisplayName("Sabrina: Aislamiento estricto de zonas de caída entre ranuras contiguas")
    void testSabrinaFailZoneSlotIsolation() {
        // Un jugador en el foso de la ranura 1 NO debe activar la detección de la ranura 2
        double x = (MIN_X + MAX_X) / 2.0;
        double y = 65.0;
        double zSlot1 = (REL_MIN_Z + REL_MAX_Z) / 2.0 + 1 * Gimnasio.PASO_RANURA;

        assertTrue(estaEnZonaCaida(x, y, zSlot1, 1), "Debe detectar caída en ranura 1");
        assertFalse(estaEnZonaCaida(x, y, zSlot1, 2), "No debe detectar caída en ranura 2 para posición de ranura 1");
        assertFalse(estaEnZonaCaida(x, y, zSlot1, 0), "No debe detectar caída en ranura 0 para posición de ranura 1");
    }

    @Test
    @DisplayName("Sabrina: Destino de reinicio coincide exactamente con Gimnasio.entrada")
    void testSabrinaResetTargetMatchesEntrance() {
        Gimnasio.Gimnasio_ sabrina = Gimnasio.de("sabrina");
        assertNotNull(sabrina);

        for (int slot = 0; slot < Gimnasio.RANURAS; slot++) {
            Vec3d entrada = Gimnasio.entrada(sabrina, slot);
            assertEquals(5120.0 + 69.947, entrada.x, 0.001);
            assertEquals(64.0 + 31.0, entrada.y, 0.001);
            assertEquals(90.780 + slot * 128, entrada.z, 0.001);
            assertEquals(180.0f, Gimnasio.giroEntrada(sabrina), 0.001f);
        }
    }
}
