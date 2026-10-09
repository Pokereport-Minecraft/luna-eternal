package net.pokereport.luna.progression;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class AltAccountTelemetryTest {

    @BeforeEach
    void setUp() {
        AltAccountTelemetry.reset();
        ProgressionBalanceConfig.Data data = new ProgressionBalanceConfig.Data();
        ProgressionBalanceConfig.set(data);
    }

    @Test
    @DisplayName("Cálculo de RiskScore base para jugador normal")
    void testNormalPlayerRiskScore() {
        UUID normalPlayer = UUID.randomUUID();
        AltAccountTelemetry.recordIpConnection(normalPlayer, "192.168.1.50");

        int score = AltAccountTelemetry.calculateRiskScore(normalPlayer, "192.168.1.50");
        assertEquals(0, score, "Un jugador normal en IP única debe tener RiskScore 0");
    }

    @Test
    @DisplayName("Incremento de riesgo por múltiples cuentas en la misma IP")
    void testMultipleAccountsOnSameIp() {
        String sharedIp = "200.50.10.20";
        UUID mainPlayer = UUID.randomUUID();
        UUID alt1 = UUID.randomUUID();
        UUID alt2 = UUID.randomUUID();

        AltAccountTelemetry.recordIpConnection(mainPlayer, sharedIp);
        AltAccountTelemetry.recordIpConnection(alt1, sharedIp);
        AltAccountTelemetry.recordIpConnection(alt2, sharedIp);

        // Con 3 cuentas en la misma IP (> 2), debe sumar 25 puntos
        int score = AltAccountTelemetry.calculateRiskScore(mainPlayer, sharedIp);
        assertEquals(25, score);

        // Añadir 2 más (> 4 cuentas)
        AltAccountTelemetry.recordIpConnection(UUID.randomUUID(), sharedIp);
        AltAccountTelemetry.recordIpConnection(UUID.randomUUID(), sharedIp);

        int highRiskScore = AltAccountTelemetry.calculateRiskScore(mainPlayer, sharedIp);
        assertEquals(50, highRiskScore);
    }

    @Test
    @DisplayName("Detección de embudo de dinero y ráfaga de kits hacia cuenta principal")
    void testMoneyFunnelAndKitBurst() {
        String sharedIp = "181.45.60.10";
        UUID mainPlayer = UUID.randomUUID();
        UUID alt1 = UUID.randomUUID();
        UUID alt2 = UUID.randomUUID();

        AltAccountTelemetry.recordIpConnection(mainPlayer, sharedIp);
        AltAccountTelemetry.recordIpConnection(alt1, sharedIp);
        AltAccountTelemetry.recordIpConnection(alt2, sharedIp); // +25 por IP

        // Transferencias anómalas hacia el main (> 100,000 -> +35)
        AltAccountTelemetry.recordMoneyTransfer(alt1, mainPlayer, 60_000L);
        AltAccountTelemetry.recordMoneyTransfer(alt2, mainPlayer, 50_000L);

        // Ráfaga de kits en el main (> 5 kits -> +10)
        for (int i = 0; i < 6; i++) {
            AltAccountTelemetry.recordKitRedemption(mainPlayer);
        }

        // Total esperado: 25 (IP) + 35 (Dinero) + 10 (Kits) = 70
        int score = AltAccountTelemetry.calculateRiskScore(mainPlayer, sharedIp);
        assertEquals(70, score);
        assertTrue(score >= ProgressionBalanceConfig.get().alts.riskScoreAlertThreshold,
                "Debe alcanzar el umbral de alerta para revisión del staff");
    }

    @Test
    @DisplayName("El RiskScore está acotado estrictamente a 100")
    void testRiskScoreClamping() {
        String sharedIp = "10.0.0.1";
        UUID target = UUID.randomUUID();
        for (int i = 0; i < 10; i++) {
            AltAccountTelemetry.recordIpConnection(UUID.randomUUID(), sharedIp);
        }
        AltAccountTelemetry.recordIpConnection(target, sharedIp); // +50

        AltAccountTelemetry.recordMoneyTransfer(UUID.randomUUID(), target, 500_000L); // +35
        for (int i = 0; i < 15; i++) {
            AltAccountTelemetry.recordKitRedemption(target); // +25
        }

        // 50 + 35 + 25 = 110 -> clampeado a 100
        int score = AltAccountTelemetry.calculateRiskScore(target, sharedIp);
        assertEquals(100, score);
    }
}
