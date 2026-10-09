package net.pokereport.luna.progression;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ExperienceBalanceTest {

    private UUID player1;
    private UUID player2;

    @BeforeEach
    void setUp() {
        ExperienceBalanceService.limpiarHistorial();
        player1 = UUID.randomUUID();
        player2 = UUID.randomUUID();
    }

    @Test
    @DisplayName("PvP Diminishing Returns: 1ª 100%, 2ª 50%, 3ª 20%, 4ª+ 0%")
    void testPvPDiminishingReturns() {
        long now = System.currentTimeMillis();

        // 1ª victoria contra el mismo jugador
        double f1 = ExperienceBalanceService.calcularFactorPvP(player1, player2, now);
        assertEquals(1.00, f1, 0.001, "La 1ª victoria en 24h debe otorgar 100% de XP");

        // 2ª victoria
        double f2 = ExperienceBalanceService.calcularFactorPvP(player1, player2, now + 1000);
        assertEquals(0.50, f2, 0.001, "La 2ª victoria en 24h debe otorgar 50% de XP");

        // 3ª victoria
        double f3 = ExperienceBalanceService.calcularFactorPvP(player1, player2, now + 2000);
        assertEquals(0.20, f3, 0.001, "La 3ª victoria en 24h debe otorgar 20% de XP");

        // 4ª victoria (explotación / farmeo)
        double f4 = ExperienceBalanceService.calcularFactorPvP(player1, player2, now + 3000);
        assertEquals(0.00, f4, 0.001, "La 4ª victoria o más en 24h debe otorgar 0% de XP");

        // 5ª victoria
        double f5 = ExperienceBalanceService.calcularFactorPvP(player1, player2, now + 4000);
        assertEquals(0.00, f5, 0.001, "Cualquier combate subsiguiente en 24h debe otorgar 0% de XP");
    }

    @Test
    @DisplayName("Blissey/Chansey boosting threshold bloquea tras 3 faints en 5 minutos")
    void testBlisseyBoostingThreshold() {
        long now = System.currentTimeMillis();

        // 1st Blissey faint
        boolean b1 = ExperienceBalanceService.esBlisseyBoosting(player1, player2, now);
        assertFalse(b1, "1er Blissey derrotado no debe activar la alerta de boosting");

        // 2nd Blissey faint
        boolean b2 = ExperienceBalanceService.esBlisseyBoosting(player1, player2, now + 5000);
        assertFalse(b2, "2do Blissey derrotado no debe activar la alerta de boosting");

        // 3rd Blissey faint en ventana corta
        boolean b3 = ExperienceBalanceService.esBlisseyBoosting(player1, player2, now + 10000);
        assertTrue(b3, "3er Blissey derrotado en 5 min DEBE activar el bloqueo de boosting");
    }

    @Test
    @DisplayName("Ajuste de experiencia salvaje (0.60x) y gimnasios (1.10x)")
    void testAjusteExperienciaCalculos() {
        // Salvaje: 100 XP -> 60 XP
        int expSalvaje = ExperienceBalanceService.ajustarExperiencia(100, 0.60);
        assertEquals(60, expSalvaje, "100 base con multiplicador salvaje 0.60 debe resultar en 60 XP");

        // Gimnasio: 1000 XP -> 1100 XP
        int expGym = ExperienceBalanceService.ajustarExperiencia(1000, 1.10);
        assertEquals(1100, expGym, "1000 base con multiplicador gym 1.10 debe resultar en 1100 XP");

        // Factor 0.0 -> 0 XP
        int expCero = ExperienceBalanceService.ajustarExperiencia(500, 0.0);
        assertEquals(0, expCero, "Factor 0.0 debe devolver 0 XP");
    }
}
