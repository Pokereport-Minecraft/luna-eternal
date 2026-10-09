package net.pokereport.luna.progression;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class HotReloadTest {

    @Test
    @DisplayName("Carga y recarga en caliente de configuración desde archivo JSON")
    void testHotReloadFromPath(@TempDir Path tempDir) throws IOException {
        Path configFile = tempDir.resolve("progression_balance.json");

        // 1. Cargar archivo inexistente genera archivo por defecto
        assertFalse(Files.exists(configFile));
        ProgressionBalanceConfig.load(configFile);
        assertTrue(Files.exists(configFile), "Debe haber generado el archivo JSON por defecto");
        assertTrue(ProgressionBalanceConfig.isEnabled());

        // 2. Modificar el archivo con nuevos parámetros y recargar
        String modifiedJson = """
        {
          "enabled": true,
          "spawn": {
            "starterMultiplier": 0.15,
            "pseudoLegendaryMultiplier": 0.10
          },
          "shiny": {
            "wildMultiplier": 0.50,
            "fossilMachineRate": 8192
          },
          "honey": {
            "candyCookMultiplier": 0.25
          },
          "events": {
            "activeEventId": "evento_reloaded",
            "active": true,
            "ticketCostPokedollars": 50000
          }
        }
        """;
        Files.writeString(configFile, modifiedJson);

        ProgressionBalanceConfig.reload();

        // 3. Verificar que los valores en memoria se actualizaron en caliente
        var cfg = ProgressionBalanceConfig.get();
        assertEquals(0.15, cfg.spawn.starterMultiplier, 0.001);
        assertEquals(0.10, cfg.spawn.pseudoLegendaryMultiplier, 0.001);
        assertEquals(0.50, cfg.shiny.wildMultiplier, 0.001);
        assertEquals(8192, cfg.shiny.fossilMachineRate);
        assertEquals(0.25, cfg.honey.candyCookMultiplier, 0.001);
        assertEquals("evento_reloaded", cfg.events.activeEventId);
        assertTrue(cfg.events.active);
        assertEquals(50_000L, cfg.events.ticketCostPokedollars);

        // 4. Kill-switch: desactivación global en caliente
        String disabledJson = """
        {
          "enabled": false
        }
        """;
        Files.writeString(configFile, disabledJson);
        ProgressionBalanceConfig.reload();
        assertFalse(ProgressionBalanceConfig.isEnabled(), "Kill-switch debe haber desactivado el sistema");
    }
}
