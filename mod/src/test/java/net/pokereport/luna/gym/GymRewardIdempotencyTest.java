package net.pokereport.luna.gym;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class GymRewardIdempotencyTest {

    @Test
    @DisplayName("Idempotencia de máscara de medallas: victorias repetidas producen la misma máscara")
    void testBadgeMaskBitwiseIdempotency() {
        int mascaraInicial = 0;

        // Derrotar a Brock (bit 0)
        Gimnasio.Gimnasio_ brock = Gimnasio.de("brock");
        assertNotNull(brock);
        int bitBrock = 1 << brock.sala();

        int mascara1 = mascaraInicial | bitBrock;
        assertEquals(1, mascara1);

        // Volver a derrotar a Brock (mismo bit)
        int mascara2 = mascara1 | bitBrock;
        assertEquals(1, mascara2, "La máscara tras derrotar al líder por segunda vez debe ser idéntica");

        // Derrotar a Misty (bit 1)
        Gimnasio.Gimnasio_ misty = Gimnasio.de("misty");
        assertNotNull(misty);
        int bitMisty = 1 << misty.sala();

        int mascara3 = mascara2 | bitMisty;
        assertEquals(3, mascara3);

        // Volver a aplicar Brock y Misty repetidas veces
        int mascara4 = mascara3 | bitBrock | bitMisty;
        assertEquals(3, mascara4, "La aplicación múltiple de medallas debe ser estrictamente idempotente");
    }

    @Test
    @DisplayName("Tope de nivel (Level Cap) idempotente ante victorias repetidas")
    void testLevelCapIdempotency() {
        UUID player = UUID.randomUUID();

        // Inicial sin medallas
        MedallaService.ponerEnCache(player, 0);
        int cap0 = GymLevelCapService.nivelMaximo(player);
        assertEquals(15, cap0);

        // Victoria en Brock
        int bitBrock = 1 << Gimnasio.de("brock").sala();
        MedallaService.ponerEnCache(player, bitBrock);
        int capBrock1 = GymLevelCapService.nivelMaximo(player);
        assertEquals(19, capBrock1);

        // Repetición de victoria en Brock (misma medalla)
        MedallaService.ponerEnCache(player, bitBrock);
        int capBrock2 = GymLevelCapService.nivelMaximo(player);
        assertEquals(19, capBrock2, "El nivel máximo debe permanecer exactamente igual tras una victoria repetida");

        // Limpieza de caché de prueba
        MedallaService.olvidar(player);
    }
}
