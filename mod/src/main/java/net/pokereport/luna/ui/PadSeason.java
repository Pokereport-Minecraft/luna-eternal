package net.pokereport.luna.ui;

import java.time.LocalDate;
import java.time.ZoneId;

/** Calendar rule shared by tests and the client; October ends at Bogotá midnight. */
public final class PadSeason {
    private PadSeason() {}
    public static boolean halloween(LocalDate date) {
        return date.getMonthValue() == 10;
    }
    public static boolean halloween() {
        return halloween(LocalDate.now(ZoneId.of("America/Bogota")));
    }
}
