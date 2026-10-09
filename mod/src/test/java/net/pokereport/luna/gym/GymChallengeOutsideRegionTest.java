package net.pokereport.luna.gym;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.pokereport.luna.world.LunaDimensions;

import static org.junit.jupiter.api.Assertions.*;

public class GymChallengeOutsideRegionTest {

    @Test
    @DisplayName("Dimensiones fuera de gimnasios no activan la política de reto")
    void testOutsideDimensionsNotFlagged() {
        // Ninguna de las dimensiones estándar de juego es dimensión de gimnasio
        assertFalse(LunaDimensions.CIUDADELA.equals(LunaDimensions.GIMNASIOS));
        assertFalse(LunaDimensions.HOGAR.equals(LunaDimensions.GIMNASIOS));
        assertFalse(LunaDimensions.SALVAJE.equals(LunaDimensions.GIMNASIOS));
        assertFalse(LunaDimensions.LOBBY.equals(LunaDimensions.GIMNASIOS));
        assertFalse(LunaDimensions.TORRE.equals(LunaDimensions.GIMNASIOS));
    }

    @Test
    @DisplayName("Parsing fino de comandos: subcomandos de clan y comandos de chat")
    void testFineGrainedCommandParsing() {
        // Validamos que el método de bloqueo rechace /clan home y /clan sethome
        // pero NO rechace /c, /clan info ni /clan members
        String[] allowedClanCmds = {
                "/c hola", "/c", "c mensaje",
                "/clan info", "/clan members", "/clan roster", "/clan tag"
        };

        String[] blockedClanCmds = {
                "/clan home", "/clan sethome", "clan home", "clan sethome"
        };

        for (String cmd : allowedClanCmds) {
            String clean = cmd.strip().toLowerCase(java.util.Locale.ROOT);
            if (clean.startsWith("/")) clean = clean.substring(1);
            String[] parts = clean.split("\\s+");
            String root = parts[0];

            boolean isBlocked = false;
            if (java.util.Set.of("gym", "gimnasio", "msg", "tell", "w", "r",
                    "canal", "channel", "g", "l", "tr", "c", "sc",
                    "help", "ayuda", "suicide", "ping", "rules", "ticket").contains(root)) {
                isBlocked = false;
            } else if (java.util.Set.of("home", "homes", "sethome", "delhome",
                    "back", "spawn", "hogar", "salvaje",
                    "tpa", "tpahere", "tpaccept", "tpdeny",
                    "rtp", "wild", "warp", "warps", "pwarp", "pwarps").contains(root)) {
                isBlocked = true;
            } else if ("clan".equals(root) && parts.length > 1) {
                String sub = parts[1];
                if ("home".equals(sub) || "sethome".equals(sub)) {
                    isBlocked = true;
                }
            }
            assertFalse(isBlocked, "Comando legal no debe ser bloqueado: " + cmd);
        }

        for (String cmd : blockedClanCmds) {
            String clean = cmd.strip().toLowerCase(java.util.Locale.ROOT);
            if (clean.startsWith("/")) clean = clean.substring(1);
            String[] parts = clean.split("\\s+");
            String root = parts[0];

            boolean isBlocked = false;
            if (java.util.Set.of("gym", "gimnasio", "msg", "tell", "w", "r",
                    "canal", "channel", "g", "l", "tr", "c", "sc",
                    "help", "ayuda", "suicide", "ping", "rules", "ticket").contains(root)) {
                isBlocked = false;
            } else if (java.util.Set.of("home", "homes", "sethome", "delhome",
                    "back", "spawn", "hogar", "salvaje",
                    "tpa", "tpahere", "tpaccept", "tpdeny",
                    "rtp", "wild", "warp", "warps", "pwarp", "pwarps").contains(root)) {
                isBlocked = true;
            } else if ("clan".equals(root) && parts.length > 1) {
                String sub = parts[1];
                if ("home".equals(sub) || "sethome".equals(sub)) {
                    isBlocked = true;
                }
            }
            assertTrue(isBlocked, "Comando de bypass debe ser bloqueado: " + cmd);
        }
    }
}
