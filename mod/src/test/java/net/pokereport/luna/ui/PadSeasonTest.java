package net.pokereport.luna.ui;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PadSeasonTest {
    @Test void novemberRestoresNormalThemeWithoutReplacingFiles() {
        assertTrue(PadSeason.halloween(LocalDate.of(2026,10,1)));
        assertTrue(PadSeason.halloween(LocalDate.of(2026,10,31)));
        assertFalse(PadSeason.halloween(LocalDate.of(2026,11,1)));
        assertFalse(PadSeason.halloween(LocalDate.of(2026,9,30)));
    }
}
