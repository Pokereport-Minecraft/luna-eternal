package net.pokereport.luna.world;

/** Adventure left by staff assignment/editmode must not follow staff into survival worlds. */
public final class StaffSurvivalPolicy {
    private StaffSurvivalPolicy() {}

    public static boolean restore(boolean staff, boolean adventure, String dimension) {
        return staff && adventure && ("minecraft:overworld".equals(dimension)
                || dimension != null && dimension.matches("lunaeternal:salvaje[2-6]?"));
    }
}
