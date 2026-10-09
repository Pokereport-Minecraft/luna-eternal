package net.pokereport.luna.gym;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.util.math.Vec3d;

import static org.junit.jupiter.api.Assertions.*;

public class GymSafeTeleportTest {

    @Test
    @DisplayName("Coordenadas de teletransporte seguras: sin NaN, sin Infinito, alturas realistas")
    void testTeleportCoordinatesFinitudeAndSafeHeights() {
        for (Gimnasio.Gimnasio_ g : Gimnasio.TODOS) {
            for (int slot = 0; slot < Gimnasio.RANURAS; slot++) {
                Vec3d entrada = Gimnasio.entrada(g, slot);
                assertNotNull(entrada, "Entrada no puede ser nula para " + g.id() + " ranura " + slot);
                assertFalse(Double.isNaN(entrada.x) || Double.isInfinite(entrada.x), "X no puede ser NaN o Inf");
                assertFalse(Double.isNaN(entrada.y) || Double.isInfinite(entrada.y), "Y no puede ser NaN o Inf");
                assertFalse(Double.isNaN(entrada.z) || Double.isInfinite(entrada.z), "Z no puede ser NaN o Inf");

                assertTrue(entrada.y >= 50.0 && entrada.y <= 130.0,
                        "Altura de entrada para " + g.id() + " fuera de límites razonables de construcción: " + entrada.y);

                Vec3d lider = Gimnasio.lider(g, slot);
                assertNotNull(lider, "Líder no puede ser nulo para " + g.id() + " ranura " + slot);
                assertFalse(Double.isNaN(lider.x) || Double.isInfinite(lider.x), "Líder X no puede ser NaN o Inf");
                assertFalse(Double.isNaN(lider.y) || Double.isInfinite(lider.y), "Líder Y no puede ser NaN o Inf");
                assertFalse(Double.isNaN(lider.z) || Double.isInfinite(lider.z), "Líder Z no puede ser NaN o Inf");

                assertTrue(lider.y >= 50.0 && lider.y <= 130.0,
                        "Altura de líder para " + g.id() + " fuera de límites razonables: " + lider.y);
            }
        }
    }

    @Test
    @DisplayName("Orientaciones de cámara (yaw) válidas para entradas de gimnasio")
    void testCameraYawValidOrientations() {
        for (Gimnasio.Gimnasio_ g : Gimnasio.TODOS) {
            float yaw = Gimnasio.giroEntrada(g);
            assertFalse(Float.isNaN(yaw) || Float.isInfinite(yaw), "Yaw no puede ser NaN o Inf");
            assertTrue(yaw >= -180.0f && yaw <= 360.0f, "Yaw fuera de rango normal de rotación");
        }
    }
}
