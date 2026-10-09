package net.pokereport.luna.event;

import net.pokereport.luna.progression.ProgressionBalanceConfig;
import net.pokereport.luna.progression.TrainerLicense;
import net.pokereport.luna.progression.TrainerLicenseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class EventShopTest {

    private UUID playerUuid;
    private EventShopService shopService;

    @BeforeEach
    void setUp() {
        playerUuid = UUID.randomUUID();
        shopService = new EventShopService();

        ProgressionBalanceConfig.Data data = new ProgressionBalanceConfig.Data();
        data.events.active = true;
        data.events.activeEventId = "halloween_2026";
        ProgressionBalanceConfig.set(data);

        TrainerLicenseService.setLicenseCache(playerUuid, TrainerLicense.NOVATO);
    }

    @Test
    @DisplayName("Catálogo estacional inicializado con artículos válidos")
    void testSeasonalCatalogInitialization() {
        assertFalse(shopService.getCatalog().isEmpty());
        assertTrue(shopService.getItem("dusk_ball_pack").isPresent());
        assertTrue(shopService.getItem("rare_candy_seasonal").isPresent());
        assertTrue(shopService.getItem("dusk_stone_seasonal").isPresent());
    }

    @Test
    @DisplayName("Requisito de Licencia de Entrenador mínima")
    void testTrainerLicenseRequirement() {
        // rare_candy_seasonal requiere Licencia BRONCE. Novato debe ser rechazado.
        var resNovato = shopService.purchase(playerUuid, 1001L, "rare_candy_seasonal", 1);
        assertFalse(resNovato.success(), "Jugador con licencia Novato no puede comprar artículo que requiere Bronce");
        assertTrue(resNovato.message().contains("Requiere Licencia"));

        // Ascender a BRONCE
        TrainerLicenseService.setLicenseCache(playerUuid, TrainerLicense.BRONCE);
        var resBronce = shopService.purchase(playerUuid, 1001L, "rare_candy_seasonal", 1);
        assertTrue(resBronce.success(), "Jugador con licencia Bronce debe poder comprar artículo que requiere Bronce");
    }

    @Test
    @DisplayName("Límite estricto de existencias por jugador")
    void testPerPlayerStockLimit() {
        // dusk_stone_seasonal tiene límite de 2 unidades y requiere PLATA
        TrainerLicenseService.setLicenseCache(playerUuid, TrainerLicense.PLATA);

        // Compra 2 unidades
        var res1 = shopService.purchase(playerUuid, 1001L, "dusk_stone_seasonal", 2);
        assertTrue(res1.success());
        assertEquals(2, shopService.getPurchasedCount(playerUuid, "dusk_stone_seasonal"));

        // Intento de comprar 1 unidad adicional debe ser denegado
        var res2 = shopService.purchase(playerUuid, 1001L, "dusk_stone_seasonal", 1);
        assertFalse(res2.success(), "Debe denegar la compra si se supera el límite de existencias del jugador");
        assertTrue(res2.message().contains("Supera el límite de compra"));
    }

    @Test
    @DisplayName("Tienda cerrada si el evento no está activo")
    void testShopClosedWhenEventInactive() {
        ProgressionBalanceConfig.get().events.active = false;

        var res = shopService.purchase(playerUuid, 1001L, "dusk_ball_pack", 1);
        assertFalse(res.success(), "No se puede comprar en la tienda de eventos si el evento está inactivo");
    }
}
