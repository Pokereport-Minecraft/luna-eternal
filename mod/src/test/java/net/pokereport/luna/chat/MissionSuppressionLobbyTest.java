package net.pokereport.luna.chat;

import net.minecraft.text.Text;
import net.pokereport.luna.ui.StaffPlayerNotices;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica que los anuncios automáticos de misiones y quests sean suprimidos del chat global.
 */
public class MissionSuppressionLobbyTest {

    @Test
    @DisplayName("Mission and quest completion announcements are suppressed from global chat")
    void testMissionsSuppressed() {
        Text missionMsg1 = Text.literal("¡El jugador ha completado la misión de captura!");
        Text missionMsg2 = Text.literal("Quest completed: Defeat Brock");
        Text translatableQuest = Text.translatable("quest.completed.notification", Text.literal("Player"));

        assertTrue(StaffPlayerNotices.isMissionOrQuest(missionMsg1));
        assertTrue(StaffPlayerNotices.isMissionOrQuest(missionMsg2));
        assertTrue(StaffPlayerNotices.isMissionOrQuest(translatableQuest));

        // Un mensaje de chat normal no debe ser clasificado como misión
        Text normalChat = Text.literal("Hola a todos en el chat");
        assertFalse(StaffPlayerNotices.isMissionOrQuest(normalChat));

        assertDoesNotThrow(() -> StaffPlayerNotices.dispatch(null, missionMsg1, false));
    }
}
