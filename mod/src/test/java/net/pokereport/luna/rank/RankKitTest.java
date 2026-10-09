package net.pokereport.luna.rank;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class RankKitTest {

    private UUID playerUuid;
    private SupporterShopService supporterShop;

    @BeforeEach
    void setUp() {
        playerUuid = UUID.randomUUID();
        supporterShop = new SupporterShopService();
    }

    @Test
    @DisplayName("Propiedades de RankToken: ligado permanente, no comerciable, no convertible")
    void testRankTokenInvariants() {
        RankToken token = new RankToken(playerUuid, 10L);

        assertEquals(playerUuid, token.getOwnerUuid());
        assertEquals(10L, token.getBalance());
        assertTrue(token.isBound(), "El token de rango debe ser BOUND");
        assertFalse(token.canTrade(), "El token de rango NO se puede comerciar");
        assertFalse(token.canSell(), "El token de rango NO se puede vender");
        assertFalse(token.canConvertToCurrency(), "El token de rango NO se puede convertir a dinero");

        assertTrue(token.debit(4L));
        assertEquals(6L, token.getBalance());
        assertFalse(token.debit(10L), "No puede gastar más de lo que tiene");
        assertEquals(6L, token.getBalance());

        token.credit(5L);
        assertEquals(11L, token.getBalance());
    }

    @Test
    @DisplayName("Supporter Shop ofrece exclusivamente artículos cosméticos sin ventaja competitiva")
    void testSupporterShopPurelyCosmetic() {
        assertFalse(supporterShop.getCatalog().isEmpty());

        for (SupporterShopService.CosmeticItem item : supporterShop.getCatalog()) {
            assertNotNull(item.type());
            assertTrue(
                    item.type() == SupporterShopService.CosmeticType.AURA ||
                    item.type() == SupporterShopService.CosmeticType.TITULO ||
                    item.type() == SupporterShopService.CosmeticType.POKEPAD_STYLE ||
                    item.type() == SupporterShopService.CosmeticType.CHAT_COLOR,
                    "Todos los artículos deben ser cosméticos (CERO pay-to-win)"
            );
            assertTrue(item.tokenCost() > 0, "El coste en tokens debe ser > 0");
        }
    }

    @Test
    @DisplayName("Flujo de compra de cosmético con RankTokens y prevención de duplicados")
    void testSupporterShopPurchaseFlow() {
        RankToken token = new RankToken(playerUuid, 10L);

        // Desbloquear aura_luna_aurora (coste: 5 tokens)
        var res1 = supporterShop.purchase(token, "aura_luna_aurora");
        assertTrue(res1.success());
        assertEquals(5L, token.getBalance(), "Debe haberse descontado 5 tokens");
        assertTrue(supporterShop.hasUnlocked(playerUuid, "aura_luna_aurora"));

        // Intentar comprar de nuevo el mismo cosmético
        var resDup = supporterShop.purchase(token, "aura_luna_aurora");
        assertFalse(resDup.success(), "No se puede comprar dos veces el mismo cosmético");
        assertEquals(5L, token.getBalance(), "No debe descontar tokens por compra duplicada");

        // Intentar comprar otro cosmético que cuesta más de lo que queda
        // Descontamos tokens para dejar balance insuficiente
        token.debit(4L); // queda 1 token
        var resPobre = supporterShop.purchase(token, "pokepad_neon_cyan"); // cuesta 4
        assertFalse(resPobre.success(), "Debe fallar si los tokens son insuficientes");
        assertEquals(1L, token.getBalance());
    }
}
