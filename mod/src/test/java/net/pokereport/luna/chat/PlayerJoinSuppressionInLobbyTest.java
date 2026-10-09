package net.pokereport.luna.chat;

import net.pokereport.luna.ui.StaffPlayerNotices;
import net.pokereport.luna.ui.Tablist;
import net.pokereport.luna.world.LunaDimensions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica que los jugadores regulares y rangos de pago jamás reciban mensajes de conexión/desconexión en el chat.
 */
public class PlayerJoinSuppressionInLobbyTest {

    @Test
    @DisplayName("Non-admin players never receive join/leave notices in Lobby or elsewhere")
    void testNonAdminNeverReceiveJoinLeave() {
        Tablist.Rank[] rangosNormales = {
                Tablist.Rank.ENTRENADOR,
                Tablist.Rank.ELITE,
                Tablist.Rank.CAMPEON,
                Tablist.Rank.MAESTRO,
                Tablist.Rank.LEYENDA,
                Tablist.Rank.MODERADOR // MOD no recibe por defecto
        };

        for (Tablist.Rank rank : rangosNormales) {
            assertFalse(StaffPlayerNotices.canReceiveJoinLeave(false, rank),
                    "El rango " + rank.name() + " NO debe recibir avisos de join/leave");
            assertFalse(ChatDimensionPolicy.canReceive(LunaDimensions.LOBBY, rank, MessageCategory.JOIN_LEAVE_ADMIN),
                    "Categoría JOIN_LEAVE_ADMIN denegada para " + rank.name());
        }
    }
}
