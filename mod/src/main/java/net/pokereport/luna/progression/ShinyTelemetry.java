package net.pokereport.luna.progression;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Telemetría centralizada de eventos Shiny para auditar ratios de generación salvaje,
 * crianza, fósiles y eventos especiales.
 */
public final class ShinyTelemetry {

    private ShinyTelemetry() {}

    private static final AtomicLong wildSpawnsChecked = new AtomicLong(0);
    private static final AtomicLong wildShiniesGenerated = new AtomicLong(0);
    private static final AtomicLong wildShiniesCaught = new AtomicLong(0);

    private static final AtomicLong eggsHatched = new AtomicLong(0);
    private static final AtomicLong breedingShiniesGenerated = new AtomicLong(0);

    private static final AtomicLong fossilsRevived = new AtomicLong(0);
    private static final AtomicLong fossilShiniesGenerated = new AtomicLong(0);

    public static void recordWildSpawn(boolean isShiny) {
        wildSpawnsChecked.incrementAndGet();
        if (isShiny) {
            wildShiniesGenerated.incrementAndGet();
        }
    }

    public static void recordWildCapture(boolean isShiny) {
        if (isShiny) {
            wildShiniesCaught.incrementAndGet();
        }
    }

    public static void recordEggHatch(boolean isShiny) {
        eggsHatched.incrementAndGet();
        if (isShiny) {
            breedingShiniesGenerated.incrementAndGet();
        }
    }

    public static void recordFossilRevival(boolean isShiny) {
        fossilsRevived.incrementAndGet();
        if (isShiny) {
            fossilShiniesGenerated.incrementAndGet();
        }
    }

    public static long getWildSpawnsChecked() { return wildSpawnsChecked.get(); }
    public static long getWildShiniesGenerated() { return wildShiniesGenerated.get(); }
    public static long getWildShiniesCaught() { return wildShiniesCaught.get(); }
    public static long getEggsHatched() { return eggsHatched.get(); }
    public static long getBreedingShiniesGenerated() { return breedingShiniesGenerated.get(); }
    public static long getFossilsRevived() { return fossilsRevived.get(); }
    public static long getFossilShiniesGenerated() { return fossilShiniesGenerated.get(); }

    public static double getEffectiveWildRate() {
        long total = wildSpawnsChecked.get();
        return total == 0 ? 0.0 : (double) wildShiniesGenerated.get() / total;
    }

    public static void reset() {
        wildSpawnsChecked.set(0);
        wildShiniesGenerated.set(0);
        wildShiniesCaught.set(0);
        eggsHatched.set(0);
        breedingShiniesGenerated.set(0);
        fossilsRevived.set(0);
        fossilShiniesGenerated.set(0);
    }
}
