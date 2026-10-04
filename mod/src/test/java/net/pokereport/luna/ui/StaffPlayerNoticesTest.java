package net.pokereport.luna.ui;
import net.minecraft.text.Text;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class StaffPlayerNoticesTest {
    @Test void capturesAutomaticPlayerEventsIncludingDecoratedMessages() {
        for (String key : new String[]{"multiplayer.player.joined", "multiplayer.player.joined.renamed",
                "multiplayer.player.left", "death.attack.fall", "death.attack.player",
                "chat.type.advancement.task", "chat.type.advancement.goal", "chat.type.advancement.challenge"})
            assertTrue(StaffPlayerNotices.isPlayerEvent(Text.translatable(key)), key);
        assertTrue(StaffPlayerNotices.isPlayerEvent(Text.empty().append(Text.translatable("death.attack.fall"))));
    }
    @Test void retainsChatAndPersonalFeedbackEvenWhenTheyQuoteAnEvent() {
        assertFalse(StaffPlayerNotices.isPlayerEvent(Text.literal("Player died or joined")));
        assertFalse(StaffPlayerNotices.isPlayerEvent(Text.translatable("chat.type.text", Text.literal("Player"), Text.literal("Hello"))));
        assertFalse(StaffPlayerNotices.isPlayerEvent(Text.translatable("commands.kill.success.single", Text.translatable("death.attack.fall"))));
        assertFalse(StaffPlayerNotices.isPlayerEvent(Text.translatable("commands.gamemode.success.self")));
    }
    @Test void paidRanksAndUnprivilegedDisplayRolesCannotSeeStaffEvents() {
        for (var rank : Tablist.Rank.values()) {
            assertFalse(StaffPlayerNotices.canReceive(false, rank), rank.name());
            assertEquals(rank.esStaff(), StaffPlayerNotices.canReceive(true, rank), rank.name());
        }
        assertFalse(StaffPlayerNotices.canReceive(true, null));
    }
}
