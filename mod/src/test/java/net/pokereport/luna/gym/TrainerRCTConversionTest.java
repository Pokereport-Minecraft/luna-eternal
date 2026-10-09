package net.pokereport.luna.gym;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TrainerRCTConversionTest {

    @Test
    @DisplayName("Repertorio de Brock contiene fichas válidas y niveles no sobrepasan 15")
    void testRepertorioBrock() {
        var brock = Gimnasio.de("brock");
        assertNotNull(brock);

        var fichas = Repertorio.de("brock");
        assertFalse(fichas.isEmpty(), "Brock debe tener fichas de repertorio registradas");
        assertTrue(fichas.stream().anyMatch(Repertorio.Ficha::firma), "Brock debe tener al menos un as de firma");

        for (var f : fichas) {
            assertNotNull(f.especie());
            assertFalse(f.ataques().isEmpty());
        }
    }
}
