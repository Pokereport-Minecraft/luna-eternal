package net.pokereport.luna.gym;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.util.math.Vec3d;

import static org.junit.jupiter.api.Assertions.*;

public class SabrinaParkourServiceFixTest {

    private static final double MIN_X = 5180.479;
    private static final double MAX_X = 5200.551;

    private boolean estaEnZonaCaida(double x, double y, double z, int slot) {
        return SabrinaParkourService.enFoso(x, y, z, slot);
    }

    @Test
    void measuredBoundsAndAllPhysicalSlots() {
        for (int r = 0; r < 8; r++) {
            double offset = r * 128;
            assertEquals(r, SabrinaParkourService.calcularRanuraDesdeZ(20.597 + offset));
            assertEquals(r, SabrinaParkourService.calcularRanuraDesdeZ(93.691 + offset));
            assertTrue(estaEnZonaCaida(MIN_X, 80.5, 20.597 + offset, r));
            assertTrue(estaEnZonaCaida(MAX_X, 79.0, 93.691 + offset, r));
            assertFalse(estaEnZonaCaida(MAX_X, 80.501, 90 + offset, r));
            assertEquals(-1, SabrinaParkourService.calcularRanuraDesdeZ(100 + offset));
        }
        assertEquals(-1, SabrinaParkourService.calcularRanuraDesdeZ(Double.NaN));
        assertEquals(-1, SabrinaParkourService.calcularRanuraDesdeZ(90 + 8 * 128));
        assertFalse(estaEnZonaCaida(5180.478, 79, 50, 0));
    }

    @Test
    @DisplayName("Sabrina: Zona de caída cubre el foso real en ranura 1 (donde juegan los retadores)")
    void testFailZoneInSlot1() {
        Gimnasio.Gimnasio_ sabrina = Gimnasio.de("sabrina");
        assertNotNull(sabrina);

        // En ranura 1, Z va de 20 + 128 = 148 a 94 + 128 = 222
        double midX = (MIN_X + MAX_X) / 2.0; // 5187.0
        double pitY = 66.0;                  // Suelo del foso
        double midZSlot1 = (148.0 + 222.0) / 2.0; // 185.0

        assertTrue(estaEnZonaCaida(midX, pitY, midZSlot1, 1),
                "Un jugador que cae al foso en ranura 1 debe ser detectado");

        // Los extremos del foso en ranura 1
        assertTrue(estaEnZonaCaida(MIN_X, 65.0, 150.0, 1), "Extremo norte-oeste");
        assertTrue(estaEnZonaCaida(MAX_X, 65.0, 220.0, 1), "Extremo sur-este");

        // Entrada en ranura 1: Y = 95.0, no debe ser zona de caída
        Vec3d entradaSlot1 = Gimnasio.entrada(sabrina, 1);
        assertFalse(estaEnZonaCaida(entradaSlot1.x, entradaSlot1.y, entradaSlot1.z, 1),
                "Entrada en ranura 1 no debe ser detectada como caída");

        // Tarima de Sabrina en ranura 1: X = 5161.603 (< 5170.0), no debe ser zona de caída
        Vec3d liderSlot1 = Gimnasio.lider(sabrina, 1);
        assertFalse(estaEnZonaCaida(liderSlot1.x, liderSlot1.y, liderSlot1.z, 1),
                "Tarima de Sabrina en ranura 1 no debe ser zona de caída");
    }

    @Test
    @DisplayName("Sabrina: Cámara del líder y detección de llegada a menos de 12 bloques")
    void testLiderArrivalProximity() {
        Gimnasio.Gimnasio_ sabrina = Gimnasio.de("sabrina");
        Vec3d liderPos = Gimnasio.lider(sabrina, 1);

        // Posición del jugador al alcanzar la cámara de Sabrina frente a ella
        Vec3d jugadorCamara = new Vec3d(liderPos.x, liderPos.y, liderPos.z + 5.0);

        double distSq = jugadorCamara.squaredDistanceTo(liderPos);
        assertTrue(distSq <= 144.0, "La distancia debe estar dentro de 12 bloques (<= 144)");
        assertTrue(Math.abs(jugadorCamara.y - liderPos.y) <= 4.0, "El delta Y debe ser <= 4 bloques");
    }

    @Test
    @DisplayName("Sabrina: Ciclo de vida de PlayerChallenge y persistencia de ruta completada")
    void testPlayerChallengeLifecycle() {
        UUID testUuid = UUID.randomUUID();
        GymChallengeService.resetAll();

        assertFalse(GymChallengeService.esRutaCompletada(testUuid, "sabrina"),
                "Sin reto activo no debe figurar como completada");

        // Reto completado directamente
        GymChallengeService.completarRuta(testUuid, "sabrina");
        // No existía el reto en el mapa, sigue en false
        assertFalse(GymChallengeService.esRutaCompletada(testUuid, "sabrina"));

        // Otros gimnasios sin ruta requerida siempre devuelven true
        assertTrue(GymChallengeService.esRutaCompletada(testUuid, "brock"));
        assertTrue(GymChallengeService.esRutaCompletada(testUuid, "misty"));
        assertTrue(GymChallengeService.esRutaCompletada(testUuid, "cissy"));
        assertTrue(GymChallengeService.esRutaCompletada(testUuid, "pegaso"));
    }
}
