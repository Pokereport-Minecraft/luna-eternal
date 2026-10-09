package net.pokereport.luna.chat;

import net.pokereport.luna.ui.StaffPlayerNotices;
import net.pokereport.luna.ui.Tablist;
import net.pokereport.luna.world.LunaDimensions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica que administradores y owners sí reciban el aviso administrativo de conexión incluso estando en Lobby.
 */
public class AdminJoinNotificationInLobbyTest {

    @Test
    @DisplayName("Admin, Head Admin and Owner receive join notices in Lobby")
    void testAdminAndOwnerReceiveJoinNoticeInLobby() {
        assertTrue(StaffPlayerNotices.canReceiveJoinLeave(true, Tablist.Rank.ADMIN),
                "ADMIN debe recibir el aviso administrativo de conexión");
        assertTrue(StaffPlayerNotices.canReceiveJoinLeave(true, Tablist.Rank.OWNER),
                "OWNER debe recibir el aviso administrativo de conexión");
        assertTrue(StaffPlayerNotices.canReceiveJoinLeave(true, Tablist.Rank.HEAD_ADMIN),
                "HEAD_ADMIN debe recibir el aviso administrativo de conexión");

        // Verificamos por categoría de política
        assertTrue(ChatDimensionPolicy.canReceive(LunaDimensions.LOBBY, Tablist.Rank.ADMIN, MessageCategory.JOIN_LEAVE_ADMIN),
                "Categoría JOIN_LEAVE_ADMIN permitida para ADMIN en Lobby");
        assertTrue(ChatDimensionPolicy.canReceive(LunaDimensions.LOBBY, Tablist.Rank.OWNER, MessageCategory.JOIN_LEAVE_ADMIN),
                "Categoría JOIN_LEAVE_ADMIN permitida para OWNER en Lobby");
        assertTrue(ChatDimensionPolicy.canReceive(LunaDimensions.LOBBY, Tablist.Rank.HEAD_ADMIN, MessageCategory.JOIN_LEAVE_ADMIN),
                "Categoría JOIN_LEAVE_ADMIN permitida para HEAD_ADMIN en Lobby");
    }

    @Test
    @DisplayName("Non-admin ranks (Moderator, Helper, Donators, Players) NEVER receive join notices")
    void testNonAdminRanksNeverReceiveJoinNotices() {
        Tablist.Rank[] forbidden = {
                Tablist.Rank.MODERADOR,
                Tablist.Rank.DEV,
                Tablist.Rank.BUILDER,
                Tablist.Rank.LEYENDA,
                Tablist.Rank.MAESTRO,
                Tablist.Rank.CAMPEON,
                Tablist.Rank.ELITE,
                Tablist.Rank.ENTRENADOR
        };

        for (Tablist.Rank r : forbidden) {
            assertFalse(StaffPlayerNotices.canReceiveJoinLeave(true, r),
                    r.name() + " NUNCA debe recibir avisos de conexión/desconexión");
            assertFalse(StaffPlayerNotices.canReceiveJoinLeave(false, r),
                    r.name() + " NUNCA debe recibir avisos de conexión/desconexión");
            assertFalse(ChatDimensionPolicy.canReceive(LunaDimensions.LOBBY, r, MessageCategory.JOIN_LEAVE_ADMIN),
                    r.name() + " debe ser bloqueado por política de categoría JOIN_LEAVE_ADMIN en Lobby");
            assertFalse(ChatDimensionPolicy.canReceive(LunaDimensions.CIUDADELA, r, MessageCategory.JOIN_LEAVE_ADMIN),
                    r.name() + " debe ser bloqueado por política de categoría JOIN_LEAVE_ADMIN en Ciudadela");
        }

        assertFalse(StaffPlayerNotices.canReceiveJoinLeave(true, null));
        assertFalse(StaffPlayerNotices.canReceiveJoinLeave(false, null));
    }
}
