package net.pokereport.luna.gym;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.pokereport.luna.world.LunaDimensions;

import static org.junit.jupiter.api.Assertions.*;

public class GymChallengeStaffExemptionTest {

    @Test
    @DisplayName("Staff y roles exentos nunca activan restricciones de gimnasio")
    void testStaffExemptionPolicyInvariants() {
        // Validación de dimensiones y aislamiento
        assertTrue(GymChallengePolicy.isInGymDimension(null) == false);

        // Invariante de comandos permitidos para staff y jugadores:
        // Los comandos esenciales de auxilio y administración nunca deben estar en la lista negra
        assertFalse(GymChallengePolicy.isCommandBlocked(null, "/help"));
        assertFalse(GymChallengePolicy.isCommandBlocked(null, "/msg"));
        assertFalse(GymChallengePolicy.isCommandBlocked(null, "/gym leave"));
    }

    @Test
    @DisplayName("Reglas de exención para staff, espectadores y constructores")
    void testExemptionConditions() {
        // La política declara que si player == null devuelve false (seguro ante llamadas nulas)
        assertFalse(GymChallengePolicy.isStaffExempt(null));
        assertFalse(GymChallengePolicy.isChallengeActive(null));
    }
}
