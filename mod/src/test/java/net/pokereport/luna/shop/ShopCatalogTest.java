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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias de integridad económica y catálogo de la tienda.
 */
class ShopCatalogTest {

    @Test
    @DisplayName("Catálogo JSON es sintácticamente válido y respeta reglas anti-arbitraje")
    void testShopCatalogIntegrity() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/data/lunaeternal/shop_catalog.json")) {
            assertNotNull(in, "El archivo shop_catalog.json debe existir en el classpath");

            JsonObject root = JsonParser.parseReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();

            assertTrue(root.has("categories"), "El JSON debe contener 'categories'");
            JsonArray categories = root.getAsJsonArray("categories");
            assertTrue(categories.size() > 0, "Debe haber al menos una categoría");

            for (JsonElement catElem : categories) {
                JsonObject cat = catElem.getAsJsonObject();
                String catId = cat.get("id").getAsString();
                assertFalse(catId.isBlank(), "El ID de categoría no debe estar vacío");

                JsonArray entries = cat.getAsJsonArray("entries");
                Set<String> seenKeys = new HashSet<>();

                for (JsonElement entryElem : entries) {
                    JsonObject entry = entryElem.getAsJsonObject();
                    String item = entry.get("item").getAsString();
                    assertFalse(item.isBlank(), "El item no debe estar vacío en categoría: " + catId);

                    String give = entry.has("give") ? entry.get("give").getAsString() : "";
                    String clave = give.isEmpty() ? item : item + "#" + give;

                    assertTrue(seenKeys.add(clave),
                            "Entrada duplicada en categoría " + catId + ": " + clave);

                    long buy = entry.get("buy").getAsLong();
                    long sell = entry.get("sell").getAsLong();

                    assertTrue(buy > 0, "Precio de compra debe ser > 0 para " + catId + "/" + clave);
                    assertTrue(sell >= 0, "Precio de venta debe ser >= 0 para " + catId + "/" + clave);
                    assertTrue(sell < buy, "Anti-arbitraje violado (sell >= buy) para " + catId + "/" + clave);

                    String currency = entry.has("currency") ? entry.get("currency").getAsString()
                            : (cat.has("currency") ? cat.get("currency").getAsString() : "POKEDOLLAR");

                    if ("LUNACOIN".equalsIgnoreCase(currency)) {
                        assertEquals(0, sell, "Items en LUNACOIN no deben tener recompra: " + catId + "/" + clave);
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("Categoría Variedades incluye drops de mobs y utilidades esenciales con ratio 10%")
    void testVariedadesMobDropsAndPrices() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/data/lunaeternal/shop_catalog.json")) {
            assertNotNull(in);
            JsonObject root = JsonParser.parseReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();

            JsonObject variedades = null;
            for (JsonElement catElem : root.getAsJsonArray("categories")) {
                JsonObject cat = catElem.getAsJsonObject();
                if ("variedades".equals(cat.get("id").getAsString())) {
                    variedades = cat;
                    break;
                }
            }

            assertNotNull(variedades, "Categoría 'variedades' debe existir");

            List<String> requiredMobItems = List.of(
                    "minecraft:string",
                    "minecraft:spider_eye",
                    "minecraft:rotten_flesh",
                    "minecraft:bone",
                    "minecraft:gunpowder",
                    "minecraft:leather",
                    "minecraft:feather",
                    "minecraft:flint",
                    "minecraft:slime_ball",
                    "minecraft:ender_pearl",
                    "minecraft:blaze_rod",
                    "minecraft:ghast_tear",
                    "minecraft:phantom_membrane"
            );

            JsonArray entries = variedades.getAsJsonArray("entries");
            Set<String> presentItems = new HashSet<>();

            for (JsonElement e : entries) {
                JsonObject entry = e.getAsJsonObject();
                String item = entry.get("item").getAsString();
                presentItems.add(item);

                long buy = entry.get("buy").getAsLong();
                long sell = entry.get("sell").getAsLong();

                // Ratio económico 10%
                assertEquals(buy / 10, sell, "El precio de venta debe ser 10% de compra para: " + item);
            }

            for (String requiredItem : requiredMobItems) {
                assertTrue(presentItems.contains(requiredItem),
                        "La categoría variedades debe contener el ítem: " + requiredItem);
            }
        }
    }
}
