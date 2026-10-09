package net.pokereport.luna.shop;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class ShopPriceTest {

    @Test
    @DisplayName("Invariante anti-arbitraje: ningún ítem puede venderse por más de lo que cuesta comprarlo")
    void testAntiArbitrageInvariantAcrossAllEntries() {
        try (InputStream in = getClass().getResourceAsStream("/data/lunaeternal/shop_catalog.json")) {
            assertNotNull(in, "shop_catalog.json no encontrado");
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray categories = root.getAsJsonArray("categories");

            int totalChecked = 0;
            for (JsonElement catElem : categories) {
                JsonObject cat = catElem.getAsJsonObject();
                JsonArray entries = cat.getAsJsonArray("entries");
                for (JsonElement entryElem : entries) {
                    JsonObject entry = entryElem.getAsJsonObject();
                    long buy = entry.get("buy").getAsLong();
                    long sell = entry.get("sell").getAsLong();

                    assertTrue(buy > 0, "Precio de compra debe ser estrictamente positivo");
                    assertTrue(sell >= 0, "Precio de venta no puede ser negativo");
                    assertTrue(sell <= buy, "VULNERABILIDAD DE ARBITRAJE: sell (" + sell + ") > buy (" + buy + ") en " + entry.get("item").getAsString());
                    totalChecked++;
                }
            }
            assertTrue(totalChecked > 100, "Debe haberse auditado un número sustancial de entradas (>100)");
        } catch (Exception e) {
            fail("Error leyendo catálogo de la tienda: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Validar comportamiento de precios de referencia esenciales")
    void testEssentialItemPriceBoundaries() {
        try (InputStream in = getClass().getResourceAsStream("/data/lunaeternal/shop_catalog.json")) {
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray categories = root.getAsJsonArray("categories");

            boolean foundPokeBall = false;
            for (JsonElement catElem : categories) {
                JsonObject cat = catElem.getAsJsonObject();
                for (JsonElement entryElem : cat.getAsJsonArray("entries")) {
                    JsonObject entry = entryElem.getAsJsonObject();
                    if ("cobblemon:poke_ball".equals(entry.get("item").getAsString())) {
                        foundPokeBall = true;
                        long buy = entry.get("buy").getAsLong();
                        long sell = entry.get("sell").getAsLong();
                        assertEquals(400L, buy, "Poké Ball buy debe ser 400");
                        assertEquals(40L, sell, "Poké Ball sell debe ser 40 (ratio 10%)");
                    }
                }
            }
            assertTrue(foundPokeBall, "cobblemon:poke_ball debe existir en la tienda");
        } catch (Exception e) {
            fail("Error verificando precios de referencia: " + e.getMessage());
        }
    }
}
