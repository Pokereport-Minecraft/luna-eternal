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
    @DisplayName("Admin and Owner receive join notices in Lobby")
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
    }
}
