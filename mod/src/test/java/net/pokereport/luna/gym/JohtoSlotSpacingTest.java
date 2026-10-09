package net.pokereport.luna.gym;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import static org.junit.jupiter.api.Assertions.*;

public class JohtoSlotSpacingTest {

    @Test
    @DisplayName("Johto: Todos los 9 gimnasios de Johto usan paso de ranura 512")
    void testJohtoGymsUse512Spacing() {
        var johtoGyms = Gimnasio.deRegion(Gimnasio.Region.JOHTO);
        assertEquals(9, johtoGyms.size(), "Debe haber 9 gimnasios en la región de Johto");

        for (Gimnasio.Gimnasio_ g : johtoGyms) {
            int paso = Gimnasio.pasoRanura(g);
            assertEquals(512, paso, "Gimnasio " + g.id() + " debe tener pasoRanura = 512");
        }
    }

    @Test
    @DisplayName("Johto: Las ranuras 0..7 no solapan y avanzan exactamente 512 bloques en Z")
    void testJohtoSlotZAdvancement() {
        var johtoGyms = Gimnasio.deRegion(Gimnasio.Region.JOHTO);
        for (Gimnasio.Gimnasio_ g : johtoGyms) {
            BlockPos o0 = Gimnasio.origen(g, 0);
            Vec3d ent0 = Gimnasio.entrada(g, 0);
            Vec3d lid0 = Gimnasio.lider(g, 0);

            for (int r = 1; r < Gimnasio.RANURAS; r++) {
                BlockPos or = Gimnasio.origen(g, r);
                Vec3d entr = Gimnasio.entrada(g, r);
                Vec3d lidr = Gimnasio.lider(g, r);

                assertEquals(o0.getX(), or.getX(), "Origen X debe ser idéntico");
                assertEquals(o0.getY(), or.getY(), "Origen Y debe ser idéntico");
                assertEquals(o0.getZ() + r * 512, or.getZ(), "Origen Z debe avanzar r * 512");

                assertEquals(ent0.x, entr.x, 0.001, "Entrada X debe ser idéntica");
                assertEquals(ent0.y, entr.y, 0.001, "Entrada Y debe ser idéntica");
                assertEquals(ent0.z + r * 512, entr.z, 0.001, "Entrada Z debe avanzar r * 512");

                assertEquals(lid0.x, lidr.x, 0.001, "Líder X debe ser idéntico");
                assertEquals(lid0.y, lidr.y, 0.001, "Líder Y debe ser idéntico");
                assertEquals(lid0.z + r * 512, lidr.z, 0.001, "Líder Z debe avanzar r * 512");
            }
        }
    }

    @Test
    @DisplayName("Kanto: Gimnasios de Kanto preservan paso de ranura 128 estándar")
    void testKantoGymsUse128Spacing() {
        var kantoGyms = Gimnasio.deRegion(Gimnasio.Region.KANTO);
        assertEquals(9, kantoGyms.size(), "Debe haber 9 gimnasios/campeón en Kanto");

        for (Gimnasio.Gimnasio_ g : kantoGyms) {
            int paso = Gimnasio.pasoRanura(g);
            assertEquals(128, paso, "Gimnasio " + g.id() + " de Kanto debe mantener pasoRanura = 128");
        }
    }
}
