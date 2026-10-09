package net.pokereport.luna.progression;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class HoneyEconomyTest {

    @BeforeEach
    void setUp() {
        HoneyTelemetry.reset();
        ProgressionBalanceConfig.Data data = new ProgressionBalanceConfig.Data();
        ProgressionBalanceConfig.set(data);
    }

    @Test
    void testConfigDefaults() {
        var honeyCfg = ProgressionBalanceConfig.get().honey;
        assertTrue(honeyCfg.enabled);
        assertEquals(0.50, honeyCfg.candyCookMultiplier, 0.001);
        assertEquals(4000, honeyCfg.slatherShinyChance);
        assertEquals(250L, honeyCfg.baseHoneyValue);
    }

    @Test
    void testHoneyHarvestAndConsumptionTelemetry() {
        HoneyTelemetry.recordHarvest(true, 10);  // 10 bottles
        HoneyTelemetry.recordHarvest(false, 25); // 25 honeycombs

        assertEquals(10, HoneyTelemetry.getHoneyBottlesGenerated());
        assertEquals(25, HoneyTelemetry.getHoneycombsGenerated());

        HoneyTelemetry.recordConsumption(true, 4);
        HoneyTelemetry.recordConsumption(false, 15);

        assertEquals(4, HoneyTelemetry.getHoneyBottlesConsumed());
        assertEquals(15, HoneyTelemetry.getHoneycombsConsumed());

        HoneyTelemetry.recordCooking("EXP_CANDY", 5);
        HoneyTelemetry.recordCooking("EV_CANDY", 8);
        HoneyTelemetry.recordCooking("MOCHI", 2);

        assertEquals(5, HoneyTelemetry.getExpCandiesCooked());
        assertEquals(8, HoneyTelemetry.getEvCandiesCooked());
        assertEquals(2, HoneyTelemetry.getMochisCooked());

        HoneyTelemetry.reset();
        assertEquals(0, HoneyTelemetry.getHoneyBottlesGenerated());
        assertEquals(0, HoneyTelemetry.getExpCandiesCooked());
    }

    @Test
    void testCandyCookMultiplierMath() {
        var honeyCfg = ProgressionBalanceConfig.get().honey;
        int rawOutput = 10;
        int adjustedOutput = Math.max(1, (int) Math.round(rawOutput * honeyCfg.candyCookMultiplier));
        assertEquals(5, adjustedOutput, "Un multiplicador de 0.50 debe reducir 10 caramelos a 5");
    }
}
