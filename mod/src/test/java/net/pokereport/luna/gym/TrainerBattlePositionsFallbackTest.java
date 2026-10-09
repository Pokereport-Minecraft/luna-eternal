package net.pokereport.luna.gym;

import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TrainerBattlePositionsFallbackTest {

    @Test
    @DisplayName("Cálculo de separación lateral para combates dobles")
    void testSeparacionLateralDobles() {
        Vec3d base = new Vec3d(100.0, 64.0, 200.0);
        Vec3d opuesto = new Vec3d(100.0, 64.0, 210.0);

        double dx = opuesto.getX() - base.getX(); // 0.0
        double dz = opuesto.getZ() - base.getZ(); // 10.0
        double largo = Math.hypot(dx, dz); // 10.0

        double lateralSlot0 = -1.5;
        double lateralSlot1 = 1.5;

        Vec3d posSlot0 = base.add((dz / largo) * lateralSlot0, 0.0, (-dx / largo) * lateralSlot0);
        Vec3d posSlot1 = base.add((dz / largo) * lateralSlot1, 0.0, (-dx / largo) * lateralSlot1);

        assertEquals(98.5, posSlot0.getX(), 0.01);
        assertEquals(200.0, posSlot0.getZ(), 0.01);

        assertEquals(101.5, posSlot1.getX(), 0.01);
        assertEquals(200.0, posSlot1.getZ(), 0.01);

        // La separación entre ambos slots debe ser exactamente 3 bloques
        assertEquals(3.0, posSlot1.distanceTo(posSlot0), 0.01);
    }
}
