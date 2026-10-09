package net.pokereport.luna.progression;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Telemetría de la economía de Miel y producción de caramelos/mochis.
 */
public final class HoneyTelemetry {

    private HoneyTelemetry() {}

    private static final AtomicLong honeyBottlesGenerated = new AtomicLong(0);
    private static final AtomicLong honeycombsGenerated = new AtomicLong(0);

    private static final AtomicLong honeyBottlesConsumed = new AtomicLong(0);
    private static final AtomicLong honeycombsConsumed = new AtomicLong(0);

    private static final AtomicLong expCandiesCooked = new AtomicLong(0);
    private static final AtomicLong evCandiesCooked = new AtomicLong(0);
    private static final AtomicLong mochisCooked = new AtomicLong(0);

    public static void recordHarvest(boolean isBottle, int amount) {
        if (isBottle) {
            honeyBottlesGenerated.addAndGet(amount);
        } else {
            honeycombsGenerated.addAndGet(amount);
        }
    }

    public static void recordConsumption(boolean isBottle, int amount) {
        if (isBottle) {
            honeyBottlesConsumed.addAndGet(amount);
        } else {
            honeycombsConsumed.addAndGet(amount);
        }
    }

    public static void recordCooking(String recipeType, int amount) {
        if ("EXP_CANDY".equalsIgnoreCase(recipeType)) {
            expCandiesCooked.addAndGet(amount);
        } else if ("EV_CANDY".equalsIgnoreCase(recipeType)) {
            evCandiesCooked.addAndGet(amount);
        } else if ("MOCHI".equalsIgnoreCase(recipeType)) {
            mochisCooked.addAndGet(amount);
        }
    }

    public static long getHoneyBottlesGenerated() { return honeyBottlesGenerated.get(); }
    public static long getHoneycombsGenerated() { return honeycombsGenerated.get(); }
    public static long getHoneyBottlesConsumed() { return honeyBottlesConsumed.get(); }
    public static long getHoneycombsConsumed() { return honeycombsConsumed.get(); }
    public static long getExpCandiesCooked() { return expCandiesCooked.get(); }
    public static long getEvCandiesCooked() { return evCandiesCooked.get(); }
    public static long getMochisCooked() { return mochisCooked.get(); }

    public static void reset() {
        honeyBottlesGenerated.set(0);
        honeycombsGenerated.set(0);
        honeyBottlesConsumed.set(0);
        honeycombsConsumed.set(0);
        expCandiesCooked.set(0);
        evCandiesCooked.set(0);
        mochisCooked.set(0);
    }
}
