package net.pokereport.luna.spectate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class StaffSpectateTest {

    @Test
    @DisplayName("Invariantes del estado de sesión de espectador staff")
    void testSessionStateInvariants() {
        UUID staffUuid = UUID.randomUUID();
        UUID targetUuid = UUID.randomUUID();

        StaffSpectateService.SessionState state = new StaffSpectateService.SessionState(
                staffUuid,
                "AdminUser",
                net.minecraft.world.GameMode.SURVIVAL,
                net.minecraft.world.World.OVERWORLD,
                new net.minecraft.util.math.Vec3d(100, 70, -200),
                90.0f,
                0.0f,
                targetUuid,
                "SuspectPlayer",
                System.currentTimeMillis()
        );

        assertEquals(staffUuid, state.staffUuid());
        assertEquals("AdminUser", state.staffName());
        assertEquals(net.minecraft.world.GameMode.SURVIVAL, state.originalGameMode());
        assertEquals(net.minecraft.world.World.OVERWORLD, state.originalWorld());
        assertEquals(100, state.originalPos().x);
        assertEquals(targetUuid, state.targetUuid());
        assertEquals("SuspectPlayer", state.targetName());
    }

    @Test
    @DisplayName("Consulta de espectador no activo devuelve false")
    void testEspectadorNoActivo() {
        UUID random = UUID.randomUUID();
        assertFalse(StaffSpectateService.isSpectating(random));
        assertNull(StaffSpectateService.getSession(random));
    }
}
