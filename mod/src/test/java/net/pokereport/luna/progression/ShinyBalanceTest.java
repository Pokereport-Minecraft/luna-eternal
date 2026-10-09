package net.pokereport.luna.progression;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ShinyBalanceTest {

    @BeforeEach
    void setUp() {
        ShinyTelemetry.reset();
        ProgressionBalanceConfig.Data data = new ProgressionBalanceConfig.Data();
        ProgressionBalanceConfig.set(data);
    }

    @Test
    void testConfigDefaultsAndFossilNerf() {
        var shinyCfg = ProgressionBalanceConfig.get().shiny;
        assertTrue(shinyCfg.enabled, "Shiny balance debe estar habilitado por defecto");
        assertEquals(1.0, shinyCfg.wildMultiplier, 0.001);
        assertEquals(1.0, shinyCfg.breedingMultiplier, 0.001);
        assertEquals(1.0, shinyCfg.eventMultiplier, 0.001);
        // Fósiles nerfeados de 100 (1%) a 4096 (1/4096)
        assertEquals(4096, shinyCfg.fossilMachineRate, "La tasa de máquina de fósiles debe ser 4096");
    }

    @Test
    void testShinyTelemetryRecording() {
        ShinyTelemetry.recordWildSpawn(false);
        ShinyTelemetry.recordWildSpawn(false);
        ShinyTelemetry.recordWildSpawn(true);
        ShinyTelemetry.recordWildCapture(true);

        assertEquals(3, ShinyTelemetry.getWildSpawnsChecked());
        assertEquals(1, ShinyTelemetry.getWildShiniesGenerated());
        assertEquals(1, ShinyTelemetry.getWildShiniesCaught());
        assertEquals(1.0 / 3.0, ShinyTelemetry.getEffectiveWildRate(), 0.001);

        ShinyTelemetry.recordEggHatch(false);
        ShinyTelemetry.recordEggHatch(true);
        assertEquals(2, ShinyTelemetry.getEggsHatched());
        assertEquals(1, ShinyTelemetry.getBreedingShiniesGenerated());

        ShinyTelemetry.recordFossilRevival(false);
        ShinyTelemetry.recordFossilRevival(true);
        assertEquals(2, ShinyTelemetry.getFossilsRevived());
        assertEquals(1, ShinyTelemetry.getFossilShiniesGenerated());

        ShinyTelemetry.reset();
        assertEquals(0, ShinyTelemetry.getWildSpawnsChecked());
        assertEquals(0, ShinyTelemetry.getWildShiniesGenerated());
        assertEquals(0.0, ShinyTelemetry.getEffectiveWildRate());
    }

    @Test
    void testCustomMultipliers() {
        var cfg = ProgressionBalanceConfig.get();
        cfg.shiny.wildMultiplier = 0.5;
        cfg.shiny.breedingMultiplier = 2.0;
        cfg.shiny.eventMultiplier = 1.5;

        assertEquals(0.5, ProgressionBalanceConfig.get().shiny.wildMultiplier, 0.001);
        assertEquals(2.0, ProgressionBalanceConfig.get().shiny.breedingMultiplier, 0.001);
        assertEquals(1.5, ProgressionBalanceConfig.get().shiny.eventMultiplier, 0.001);
    }
}
