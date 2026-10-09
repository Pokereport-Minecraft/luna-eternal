package net.pokereport.luna.gts;

import net.pokereport.luna.progression.ProgressionBalanceConfig;
import net.pokereport.luna.ui.Tablist;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GtsTaxTest {

    @BeforeEach
    void setUp() {
        ProgressionBalanceConfig.Data data = new ProgressionBalanceConfig.Data();
        ProgressionBalanceConfig.set(data);
    }

    @Test
    @DisplayName("Cálculo de comisión de publicación (Listing Fee) fija al 2%")
    void testListingFeeCalculation() {
        assertEquals(0L, GtsService.listingFee(0L));
        assertEquals(2L, GtsService.listingFee(100L));
        assertEquals(20L, GtsService.listingFee(1_000L));
        assertEquals(200L, GtsService.listingFee(10_000L));
        assertEquals(2_000L, GtsService.listingFee(100_000L));
    }

    @Test
    @DisplayName("Cálculo de impuesto de venta por rango (Sales Tax)")
    void testSalesTaxByRank() {
        long price = 100_000L;

        // F2P / Entrenador: 10,000 * 5% + 90,000 * 8% = 7,700
        assertEquals(7_700L, GtsService.taxFor(price, null));
        assertEquals(7_700L, GtsService.taxFor(price, Tablist.Rank.ENTRENADOR));

        // Élite: 7,700 - 500 (0.5%) = 7,200
        assertEquals(7_200L, GtsService.taxFor(price, Tablist.Rank.ELITE));

        // Campeón: 7,700 - 1,000 (1.0%) = 6,700
        assertEquals(6_700L, GtsService.taxFor(price, Tablist.Rank.CAMPEON));

        // Maestro: 7,700 - 1,500 (1.5%) = 6,200
        assertEquals(6_200L, GtsService.taxFor(price, Tablist.Rank.MAESTRO));

        // Leyenda: 7,700 - 2,000 (2.0%) = 5,700
        assertEquals(5_700L, GtsService.taxFor(price, Tablist.Rank.LEYENDA));
    }

    @Test
    @DisplayName("Net payout del vendedor e inyección neta retenida por sumidero")
    void testNetSellerPayoutAndSinkTotal() {
        long price = 50_000L;
        long fee = GtsService.listingFee(price); // 2% = 1,000
        long f2pTax = GtsService.taxFor(price, Tablist.Rank.ENTRENADOR); // 10k * 5% + 40k * 8% = 3,700

        long netReceived = price - f2pTax;
        assertEquals(46_300L, netReceived);

        long totalSink = fee + f2pTax;
        assertEquals(4_700L, totalSink, "El sumidero total (publicación + venta progresiva)");
    }

    @Test
    @DisplayName("Comisiones e impuestos con configuración personalizada")
    void testCustomGtsConfig() {
        var cfg = ProgressionBalanceConfig.get().gts;
        cfg.listingFeePercent = 1.0;
        cfg.baseSalesTaxPercent = 5.0;

        assertEquals(100L, GtsService.listingFee(10_000L));
        assertEquals(500L, GtsService.taxFor(10_000L, Tablist.Rank.ENTRENADOR));
    }
}
