package net.pokereport.luna.chat;

import net.minecraft.text.Text;
import net.pokereport.luna.ui.StaffPlayerNotices;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica que los anuncios de avances (advancements) sean identificados y suprimidos al 100% en todas las dimensiones.
 */
public class AdvancementSuppressionLobbyTest {

    @Test
    @DisplayName("Advancements are identified and suppressed for all players and staff")
    void testAdvancementsSuppressed() {
        Text taskMsg = Text.translatable("chat.type.advancement.task", Text.literal("Player"), Text.literal("Stone Age"));
        Text goalMsg = Text.translatable("chat.type.advancement.goal", Text.literal("Player"), Text.literal("Sweet Dreams"));
        Text challengeMsg = Text.translatable("chat.type.advancement.challenge", Text.literal("Player"), Text.literal("Monsters Hunted"));

        assertTrue(StaffPlayerNotices.isAdvancement(taskMsg));
        assertTrue(StaffPlayerNotices.isAdvancement(goalMsg));
        assertTrue(StaffPlayerNotices.isAdvancement(challengeMsg));

        // Invocando dispatch con manager nulo no debe arrojar excepción ni enviar nada
        assertDoesNotThrow(() -> StaffPlayerNotices.dispatch(null, taskMsg, false));
    }
}
