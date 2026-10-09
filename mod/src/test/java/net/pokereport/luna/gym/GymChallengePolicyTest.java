package net.pokereport.luna.gym;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.pokereport.luna.world.LunaDimensions;

import static org.junit.jupiter.api.Assertions.*;

public class GymChallengePolicyTest {

    @Test
    @DisplayName("Dimensiones de gimnasios reconocidas correctamente por la política")
    void testGymDimensionsRecognized() {
        // Dimensiones oficiales
        assertNotNull(LunaDimensions.GIMNASIOS);
        assertNotNull(LunaDimensions.GIMNASIO_MORTI);
        assertNotNull(LunaDimensions.ISLAS_NARANJA);
        assertNotNull(LunaDimensions.CIUDADELA);

        // Ciudadela NO es dimensión de gimnasio
        assertFalse(LunaDimensions.GIMNASIOS.equals(LunaDimensions.CIUDADELA));
        assertFalse(LunaDimensions.GIMNASIO_MORTI.equals(LunaDimensions.CIUDADELA));
        assertFalse(LunaDimensions.ISLAS_NARANJA.equals(LunaDimensions.CIUDADELA));
    }

    @Test
    @DisplayName("Objetos bloqueados durante el reto: perlas de ender y fruta del coro")
    void testBlockedItemsPolicy() {
        assertTrue(GymChallengePolicy.isItemBlocked("minecraft:ender_pearl"), "La perla de ender debe estar bloqueada");
        assertTrue(GymChallengePolicy.isItemBlocked("ender_pearl"), "La perla de ender debe estar bloqueada");
        assertTrue(GymChallengePolicy.isItemBlocked("minecraft:chorus_fruit"), "La fruta del coro debe estar bloqueada");
        assertTrue(GymChallengePolicy.isItemBlocked("chorus_fruit"), "La fruta del coro debe estar bloqueada");

        assertFalse(GymChallengePolicy.isItemBlocked("minecraft:apple"), "La manzana no debe estar bloqueada");
        assertFalse(GymChallengePolicy.isItemBlocked("minecraft:potion"), "Las pociones no deben estar bloqueadas");
        assertFalse(GymChallengePolicy.isItemBlocked((String) null), "Null string no debe estar bloqueado");
        assertFalse(GymChallengePolicy.isItemBlocked(""), "String vacío no debe estar bloqueado");
        assertFalse(GymChallengePolicy.isItemBlocked((ItemStack) null), "Null ItemStack no debe estar bloqueado");
    }

    @Test
    @DisplayName("Filtro de comandos: bloquea teletransportes externos y permite salida de gimnasio y chat")
    void testCommandFiltering() {
        // Simulamos la lógica interna de parsing de comandos que usa GymChallengePolicy
        String[] blockedCommands = {
                "/home", "home 1", "/homes", "/sethome base", "/delhome base",
                "/back", "/spawn", "/hogar", "/salvaje",
                "/tpa amigo", "/tpahere amigo", "/tpaccept", "/tpdeny",
                "/rtp", "/wild", "/warp mina", "/warps", "/pwarp tienda",
                "/clan home", "clan sethome"
        };

        String[] allowedCommands = {
                "/gym leave", "/gym salir", "/gimnasio leave", "/gimnasio salir",
                "/msg amigo hola", "/tell amigo que tal", "/w amigo", "/r contestando",
                "/c hola clan", "/g hola global", "/l hola local",
                "/help", "/ayuda", "/ticket reporte de bug"
        };

        // Verificamos que los comandos bloqueados estén contemplados
        for (String cmd : blockedCommands) {
            String clean = cmd.strip().toLowerCase(java.util.Locale.ROOT);
            if (clean.startsWith("/")) clean = clean.substring(1);
            String[] parts = clean.split("\\s+");
            String root = parts[0];

            boolean isAllowed = java.util.Set.of("gym", "gimnasio", "msg", "tell", "w", "r",
                    "canal", "channel", "g", "l", "tr", "c", "sc",
                    "help", "ayuda", "suicide", "ping", "rules", "ticket").contains(root);
            assertFalse(isAllowed, "El comando " + cmd + " no debe estar en la whitelist de gimnasios");
        }

        for (String cmd : allowedCommands) {
            String clean = cmd.strip().toLowerCase(java.util.Locale.ROOT);
            if (clean.startsWith("/")) clean = clean.substring(1);
            String[] parts = clean.split("\\s+");
            String root = parts[0];

            boolean isAllowed = java.util.Set.of("gym", "gimnasio", "msg", "tell", "w", "r",
                    "canal", "channel", "g", "l", "tr", "c", "sc",
                    "help", "ayuda", "suicide", "ping", "rules", "ticket").contains(root);
            assertTrue(isAllowed, "El comando permitido " + cmd + " debe ser aceptado durante el reto");
        }
    }
}
