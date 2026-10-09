package net.pokereport.luna.chat;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.TextColor;
import net.pokereport.luna.ui.Tablist;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de regresión visual y seguridad del formato de Clanes:
 * - Degradado real carácter por carácter (2 colores HEX).
 * - Soporte para 1 solo color HEX uniforme.
 * - Soporte para HEX en mayúsculas y minúsculas (#ffaa00 y #FFAA00).
 * - Fallback seguro ante HEX inválido (mantiene RGB de color legacy).
 * - Sanitización estricta: inyección de códigos de formato '§' tratada como texto literal.
 * - Comportamiento con etiquetas de 1 carácter, vacías o largas.
 */
public class ClanFormatVisualTest {

    @Test
    @DisplayName("Single color Clan HEX applies uniform color without crash")
    void testSingleColorClanHex() {
        // Tag con el mismo color de inicio y fin
        Tablist.ClanInfo clan = new Tablist.ClanInfo("TEST", 'b', "#FF5555", "#FF5555", true, false);
        MutableText text = Tablist.tagText(clan);

        assertNotNull(text);
        String plain = text.getString();
        assertEquals("[TEST]", plain);

        // Cada hermano debe tener color #FF5555
        for (var sibling : text.getSiblings()) {
            Style s = sibling.getStyle();
            assertNotNull(s.getColor());
            assertEquals(0xFF5555, s.getColor().getRgb());
            assertTrue(s.isBold());
            assertFalse(s.isItalic());
        }
    }

    @Test
    @DisplayName("Single color with null or invalid colorFin defaults to colorInicio")
    void testSingleColorNullFinFallback() {
        Tablist.ClanInfo clan = new Tablist.ClanInfo("SOLO", 'a', "#55FF55", null, false, true);
        MutableText text = Tablist.tagText(clan);

        assertNotNull(text);
        assertEquals("[SOLO]", text.getString());

        for (var sibling : text.getSiblings()) {
            Style s = sibling.getStyle();
            assertNotNull(s.getColor());
            assertEquals(0x55FF55, s.getColor().getRgb());
            assertTrue(s.isItalic());
        }
    }

    @Test
    @DisplayName("Two color Clan gradient interpolates RGB correctly")
    void testTwoColorGradient() {
        // Degradado de negro (#000000) a blanco (#FFFFFF)
        Tablist.ClanInfo clan = new Tablist.ClanInfo("ABC", 'f', "#000000", "#FFFFFF", false, false);
        MutableText text = Tablist.tagText(clan);

        assertEquals("[ABC]", text.getString());
        // Hermano 0: '[' con color inicio 0x000000
        // Hermano 1: 'A' (t=0.0) -> 0x000000
        // Hermano 2: 'B' (t=0.5) -> ~0x7F7F7F or 0x808080
        // Hermano 3: 'C' (t=1.0) -> 0xFFFFFF
        // Hermano 4: ']' con color fin 0xFFFFFF
        var siblings = text.getSiblings();
        assertEquals(5, siblings.size());

        TextColor colorFirst = siblings.get(1).getStyle().getColor();
        assertNotNull(colorFirst);
        assertEquals(0x000000, colorFirst.getRgb());

        TextColor colorMid = siblings.get(2).getStyle().getColor();
        assertNotNull(colorMid);
        int midRgb = colorMid.getRgb();
        int midR = (midRgb >> 16) & 0xFF;
        assertTrue(midR >= 127 && midR <= 128, "Canal R intermedio debe rondar 127-128");

        TextColor colorLast = siblings.get(3).getStyle().getColor();
        assertNotNull(colorLast);
        assertEquals(0xFFFFFF, colorLast.getRgb());
    }

    @Test
    @DisplayName("Supports both lowercase and uppercase HEX formats")
    void testUppercaseAndLowercaseHex() {
        Tablist.ClanInfo clanLower = new Tablist.ClanInfo("LOW", 'b', "#55ffff", "#ff5555", false, false);
        MutableText textLower = Tablist.tagText(clanLower);
        assertEquals(0x55FFFF, textLower.getSiblings().get(0).getStyle().getColor().getRgb());

        Tablist.ClanInfo clanUpper = new Tablist.ClanInfo("UPP", 'b', "#FFAA00", "#AA00AA", false, false);
        MutableText textUpper = Tablist.tagText(clanUpper);
        assertEquals(0xFFAA00, textUpper.getSiblings().get(0).getStyle().getColor().getRgb());
    }

    @Test
    @DisplayName("Invalid HEX safely falls back to legacy color RGB without exception")
    void testInvalidHexFallback() {
        // color '6' corresponde a legacy 0xFFAA00
        Tablist.ClanInfo clanInvalid = new Tablist.ClanInfo("FALL", '6', "INVALID_HEX", "ALSO_INVALID", false, false);
        MutableText text = Tablist.tagText(clanInvalid);

        assertEquals("[FALL]", text.getString());
        for (var sibling : text.getSiblings()) {
            TextColor c = sibling.getStyle().getColor();
            assertNotNull(c);
            assertEquals(0xFFAA00, c.getRgb(), "Debe usar fallback legacyRgb('6') = 0xFFAA00");
        }
    }

    @Test
    @DisplayName("Format code injection (§c, §k) is treated as plain literal characters")
    void testFormatCodeInjectionProtection() {
        // Intento de inyectar códigos de formato para romper el chat
        String maliciousTag = "§kHACK§r";
        Tablist.ClanInfo clan = new Tablist.ClanInfo(maliciousTag, 'b', "#00FF00", "#00FF00", false, false);
        MutableText text = Tablist.tagText(clan);

        // El texto literal debe contener los caracteres exactos sin interpretar formato
        assertEquals("[" + maliciousTag + "]", text.getString());
        // El color del componente sigue siendo #00FF00
        for (var sibling : text.getSiblings()) {
            assertEquals(0x00FF00, sibling.getStyle().getColor().getRgb());
        }
    }

    @Test
    @DisplayName("Single character Clan tag renders properly without division by zero")
    void testSingleCharacterClanTag() {
        Tablist.ClanInfo clan = new Tablist.ClanInfo("X", 'b', "#123456", "#654321", false, false);
        MutableText text = Tablist.tagText(clan);

        assertEquals("[X]", text.getString());
        var siblings = text.getSiblings();
        assertEquals(3, siblings.size()); // '[', 'X', ']'
        assertEquals(0x123456, siblings.get(1).getStyle().getColor().getRgb());
    }
}
