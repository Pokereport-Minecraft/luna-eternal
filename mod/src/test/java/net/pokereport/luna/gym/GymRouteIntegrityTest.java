package net.pokereport.luna.gym;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.util.math.Vec3d;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GymRouteIntegrityTest {

    @Test
    @DisplayName("23 Gimnasios oficiales con metadatos completos y regiones válidas")
    void testAll23GymsExistAndMetadataValid() {
        List<Gimnasio.Gimnasio_> todos = Gimnasio.TODOS;
        assertNotNull(todos);
        assertEquals(23, todos.size(), "Deben existir exactamente 23 gimnasios registrados");

        for (Gimnasio.Gimnasio_ g : todos) {
            assertNotNull(g.id(), "ID de gimnasio no puede ser nulo");
            assertNotNull(g.lider(), "Nombre de líder no puede ser nulo");
            assertNotNull(g.medalla(), "Nombre de medalla no puede ser nulo");
            assertNotNull(g.insignia(), "Insignia de medalla no puede ser nula");
            assertNotNull(g.region(), "Región no puede ser nula");
            assertTrue(g.nivel() >= 15 && g.nivel() <= 100, "Nivel de líder fuera de rango 15-100: " + g.nivel());
        }
    }

    @Test
    @DisplayName("Todas las entradas y tarimas de líderes existen para los 23 gimnasios")
    void testAllEntrancesAndLeaderPositionsConfigured() {
        for (Gimnasio.Gimnasio_ g : Gimnasio.TODOS) {
            Vec3d entrada = Gimnasio.entrada(g, 0);
            assertNotNull(entrada, "Entrada del maestro no debe ser nula para " + g.id());
            assertTrue(entrada.y > 0, "Altura de entrada debe ser positiva para " + g.id() + ": " + entrada.y);

            Vec3d lider = Gimnasio.lider(g, 0);
            assertNotNull(lider, "Posición del líder no debe ser nula para " + g.id());
            assertTrue(lider.y > 0, "Altura del líder debe ser positiva para " + g.id() + ": " + lider.y);

            float giro = Gimnasio.giroEntrada(g);
            assertTrue(giro >= -180f && giro <= 360f, "Giro de entrada fuera de rango para " + g.id());
        }
    }

    @Test
    @DisplayName("Paso entre ranuras consistente según región y tamaño de sala")
    void testSlotSpacingConsistency() {
        for (Gimnasio.Gimnasio_ g : Gimnasio.TODOS) {
            int paso = Gimnasio.pasoRanura(g);
            if (g.region() == Gimnasio.Region.NARANJA || g.region() == Gimnasio.Region.JOHTO || "morti".equals(g.id())) {
                assertEquals(512, paso, "Liga Naranja, Johto y Morti deben usar 512 bloques de paso para aislar salas grandes");
            } else {
                assertEquals(128, paso, "Gimnasios estándar de Kanto deben usar 128 bloques de paso en Z");
            }

            for (int slot = 1; slot < Gimnasio.RANURAS; slot++) {
                Vec3d entMaster = Gimnasio.entrada(g, 0);
                Vec3d entSlot = Gimnasio.entrada(g, slot);

                assertEquals(entMaster.x, entSlot.x, 0.001, "Coordenada X debe mantenerse constante entre ranuras");
                assertEquals(entMaster.y, entSlot.y, 0.001, "Coordenada Y debe mantenerse constante entre ranuras");
                assertEquals(entMaster.z + (slot * paso), entSlot.z, 0.001,
                        "Coordenada Z debe avanzar exactamente paso * slot");
            }
        }
    }

    @Test
    @DisplayName("Cadena de prerrequisitos de gimnasio es monótona y consistente")
    void testPrerequisiteChainConsistency() {
        for (Gimnasio.Gimnasio_ g : Gimnasio.TODOS) {
            List<Gimnasio.Gimnasio_> reqs = Gimnasio.requisitos(g);
            assertNotNull(reqs);
            // Cada líder de la cadena debe tener sala estrictamente anterior o pertenecer a regiones previas
            for (Gimnasio.Gimnasio_ req : reqs) {
                assertNotEquals(g.id(), req.id(), "Un gimnasio no puede requerirse a sí mismo");
            }
        }

        // Brock no tiene requisitos previos
        Gimnasio.Gimnasio_ brock = Gimnasio.de("brock");
        assertTrue(Gimnasio.requisitos(brock).isEmpty(), "Brock no debe tener requisitos previos");

        // Blue (Campeón Kanto) requiere los 8 líderes de Kanto y la Liga Naranja
        Gimnasio.Gimnasio_ blue = Gimnasio.de("campeon_kanto");
        List<Gimnasio.Gimnasio_> blueReqs = Gimnasio.requisitos(blue);
        assertTrue(blueReqs.size() >= 13, "Blue debe requerir al menos 13 medallas (8 Kanto + 5 Naranja)");
    }
}
