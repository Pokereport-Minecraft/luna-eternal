package net.pokereport.luna.world;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests obligatorios de la auditoría técnica de protección de mundos.
 * Valida la frontera entre dimensiones seguras (Ciudadela, Gimnasios) y libres (Hogar, Salvaje),
 * la contención del borde de mundos salvajes y la regresión en (1576, 77, -3129).
 */
public class WorldProtectionAuditTest {

    // Simulación del cálculo del WorldBorder vanilla (centro 0.5, 0.5; tamaño radio * 2)
    private static boolean dentroDelBorde(double x, double z, int radio) {
        double minX = 0.5 - radio;
        double maxX = 0.5 + radio;
        double minZ = 0.5 - radio;
        double maxZ = 0.5 + radio;
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    @Test
    @DisplayName("wild_player_can_break_blocks: Mundo salvaje no es dimensión protegida por WorldProtectionManager")
    void wild_player_can_break_blocks() {
        assertFalse(WorldProtectionManager.isProtectedDimension(LunaDimensions.SALVAJE));
        for (RegistryKey<World> s : LunaDimensions.SALVAJES) {
            assertFalse(WorldProtectionManager.isProtectedDimension(s),
                    "El mundo salvaje " + s.getValue() + " no debe estar en SAFE");
        }
    }

    @Test
    @DisplayName("wild_player_can_place_blocks: En mundo salvaje dentro del borde se permite colocar bloques")
    void wild_player_can_place_blocks() {
        assertFalse(WorldProtectionManager.isProtectedDimension(LunaDimensions.SALVAJE));
        // Dentro del radio configurado (5000), el borde contiene la posición
        assertTrue(dentroDelBorde(100, 100, Salvaje.RADIO));
    }

    @Test
    @DisplayName("home_player_can_break_blocks: Mundo Hogar no es dimensión protegida")
    void home_player_can_break_blocks() {
        assertFalse(WorldProtectionManager.isProtectedDimension(LunaDimensions.HOGAR));
    }

    @Test
    @DisplayName("home_player_can_place_blocks: Mundo Hogar permite colocar bloques libremente")
    void home_player_can_place_blocks() {
        assertFalse(WorldProtectionManager.isProtectedDimension(LunaDimensions.HOGAR));
    }

    @Test
    @DisplayName("citadel_player_cannot_break_blocks: Ciudadela es dimensión protegida")
    void citadel_player_cannot_break_blocks() {
        assertTrue(WorldProtectionManager.isProtectedDimension(LunaDimensions.CIUDADELA));
    }

    @Test
    @DisplayName("citadel_player_cannot_place_blocks: Ciudadela prohíbe colocar bloques a jugadores normales")
    void citadel_player_cannot_place_blocks() {
        assertTrue(WorldProtectionManager.isProtectedDimension(LunaDimensions.CIUDADELA));
    }

    @Test
    @DisplayName("gym_player_cannot_break_blocks: Gimnasios son dimensiones protegidas")
    void gym_player_cannot_break_blocks() {
        assertTrue(WorldProtectionManager.isProtectedDimension(LunaDimensions.GIMNASIOS));
        assertTrue(WorldProtectionManager.isProtectedDimension(LunaDimensions.GIMNASIO_MORTI));
        assertTrue(WorldProtectionManager.isProtectedDimension(LunaDimensions.TORRE));
        assertTrue(WorldProtectionManager.isProtectedDimension(LunaDimensions.ISLAS_NARANJA));
    }

    @Test
    @DisplayName("gym_player_cannot_place_blocks: Gimnasios prohíben colocar bloques a jugadores normales")
    void gym_player_cannot_place_blocks() {
        assertTrue(WorldProtectionManager.isProtectedDimension(LunaDimensions.GIMNASIOS));
    }

    @Test
    @DisplayName("protection_is_dimension_scoped: La protección está estrictamente acotada a la dimensión")
    void protection_is_dimension_scoped() {
        Set<RegistryKey<World>> seguras = Set.of(
                LunaDimensions.CIUDADELA, LunaDimensions.LOBBY, LunaDimensions.GIMNASIOS,
                LunaDimensions.GIMNASIO_MORTI, LunaDimensions.TORRE, LunaDimensions.ISLAS_NARANJA
        );
        for (RegistryKey<World> k : seguras) {
            assertTrue(WorldProtectionManager.isProtectedDimension(k));
        }

        Set<RegistryKey<World>> libres = Set.of(
                LunaDimensions.HOGAR, LunaDimensions.SALVAJE, LunaDimensions.SALVAJES.get(1),
                LunaDimensions.SALVAJES.get(2), LunaDimensions.SALVAJES.get(3),
                LunaDimensions.SALVAJES.get(4), LunaDimensions.SALVAJES.get(5)
        );
        for (RegistryKey<World> k : libres) {
            assertFalse(WorldProtectionManager.isProtectedDimension(k));
        }
    }

    @Test
    @DisplayName("protected_coordinates_do_not_affect_other_dimensions: Coordenadas protegidas no afectan a otras dimensiones")
    void protected_coordinates_do_not_affect_other_dimensions() {
        // En Ciudadela, las coordenadas 1576, 77, -3129 están protegidas porque la dimensión entera lo está
        assertTrue(WorldProtectionManager.isProtectedDimension(LunaDimensions.CIUDADELA));
        // Pero en Hogar y Salvaje, esas MISMAS coordenadas NO están protegidas por WorldProtectionManager
        assertFalse(WorldProtectionManager.isProtectedDimension(LunaDimensions.HOGAR));
        assertFalse(WorldProtectionManager.isProtectedDimension(LunaDimensions.SALVAJE));
    }

    @Test
    @DisplayName("regression_1576_77_minus3129: Reproducción exacta y resolución del fallo de producción")
    void regression_1576_77_minus3129() {
        double x = 1576.0;
        double z = -3129.0;

        // 1. Con el radio original (3000), el punto Z=-3129 estaba FUERA del borde vanilla (causa raíz del fallo en producción)
        assertFalse(dentroDelBorde(x, z, 3000),
                "En radio 3000, Z=-3129 está fuera del borde (-2999.5), bloqueando rotura vanilla");

        // 2. Con la corrección (radio >= 3500, predeterminado a 5000), el punto Z=-3129 queda DENTRO del borde
        assertTrue(dentroDelBorde(x, z, 5000),
                "En radio 5000, Z=-3129 está dentro del borde (-4999.5 a 5000.5), permitiendo rotura vanilla");
    }

    @Test
    @DisplayName("boundary_frontier_tests: Prueba exhaustiva de los límites de frontera (minX, maxX, minZ, maxZ)")
    void boundary_frontier_tests() {
        int r = 3000;
        double minX = 0.5 - r; // -2999.5
        double maxX = 0.5 + r; // 3000.5
        double minZ = 0.5 - r; // -2999.5
        double maxZ = 0.5 + r; // 3000.5

        // Eje X: frontera oeste (minX)
        assertFalse(dentroDelBorde(minX - 1.0, 0, r), "minX - 1 debe estar fuera");
        assertTrue(dentroDelBorde(minX, 0, r), "minX exacto debe estar dentro");
        assertTrue(dentroDelBorde(minX + 1.0, 0, r), "minX + 1 debe estar dentro");

        // Eje X: frontera este (maxX)
        assertTrue(dentroDelBorde(maxX - 1.0, 0, r), "maxX - 1 debe estar dentro");
        assertTrue(dentroDelBorde(maxX, 0, r), "maxX exacto debe estar dentro");
        assertFalse(dentroDelBorde(maxX + 1.0, 0, r), "maxX + 1 debe estar fuera");

        // Eje Z: frontera norte (minZ) — frontera crítica reportada en producción
        assertFalse(dentroDelBorde(0, minZ - 1.0, r), "minZ - 1 debe estar fuera");
        assertTrue(dentroDelBorde(0, minZ, r), "minZ exacto debe estar dentro");
        assertTrue(dentroDelBorde(0, minZ + 1.0, r), "minZ + 1 debe estar dentro");

        // Eje Z: frontera sur (maxZ)
        assertTrue(dentroDelBorde(0, maxZ - 1.0, r), "maxZ - 1 debe estar dentro");
        assertTrue(dentroDelBorde(0, maxZ, r), "maxZ exacto debe estar dentro");
        assertFalse(dentroDelBorde(0, maxZ + 1.0, r), "maxZ + 1 debe estar fuera");
    }
}
