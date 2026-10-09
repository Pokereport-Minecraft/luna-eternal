package net.pokereport.luna.economy;

import net.pokereport.luna.gts.GtsService;
import net.pokereport.luna.progression.ProgressionBalanceConfig;
import net.pokereport.luna.progression.TrainerLicense;
import net.pokereport.luna.ui.Tablist;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EconomySinkTest {

    @BeforeEach
    void setUp() {
        ProgressionBalanceConfig.Data data = new ProgressionBalanceConfig.Data();
        ProgressionBalanceConfig.set(data);
    }

    @Test
    void testTrainerLicenseUpgradeSinks() {
        // Licencias como sumidero institucional
        assertEquals(0L, TrainerLicense.NOVATO.upgradeFee);
        assertEquals(5_000L, TrainerLicense.BRONCE.upgradeFee);
        assertEquals(20_000L, TrainerLicense.PLATA.upgradeFee);
        assertEquals(50_000L, TrainerLicense.ORO.upgradeFee);
        assertEquals(150_000L, TrainerLicense.MAESTRO.upgradeFee);
        assertEquals(500_000L, TrainerLicense.CAMPEON.upgradeFee);

        long totalSinkToMax = 0;
        for (TrainerLicense lic : TrainerLicense.values()) {
            totalSinkToMax += lic.upgradeFee;
        }
        assertEquals(725_000L, totalSinkToMax, "El sumidero completo de licencias debe ser 725,000 PokéDólares");
    }

    @Test
    void testGtsTaxAndFeeSinks() {
        long price = 10_000L;
        // Listing fee (2% = 200)
        long listingFee = GtsService.listingFee(price);
        assertEquals(200L, listingFee);

        // F2P Sales Tax (primer tramo 10,000 a 5% = 500)
        long f2pTax = GtsService.taxFor(price, Tablist.Rank.ENTRENADOR);
        assertEquals(500L, f2pTax);

        // Elite Sales Tax (500 - 50 = 450)
        long eliteTax = GtsService.taxFor(price, Tablist.Rank.ELITE);
        assertEquals(450L, eliteTax);

        // Campeon Sales Tax (500 - 100 = 400)
        long campTax = GtsService.taxFor(price, Tablist.Rank.CAMPEON);
        assertEquals(400L, campTax);

        // Maestro Sales Tax (500 - 150 = 350)
        long maesTax = GtsService.taxFor(price, Tablist.Rank.MAESTRO);
        assertEquals(350L, maesTax);

        // Leyenda Sales Tax (500 - 200 = 300)
        long leyTax = GtsService.taxFor(price, Tablist.Rank.LEYENDA);
        assertEquals(300L, leyTax);

        // Total sink for a 10,000 GTS transaction (Fee + Tax)
        long totalSinkF2P = listingFee + f2pTax;
        assertEquals(700L, totalSinkF2P, "Una venta F2P de 10,000 drena 700 PokéDólares (200 fee + 500 tax)");
    }

    @Test
    void testEventTicketSinkConfig() {
        var cfg = ProgressionBalanceConfig.get().events;
        assertEquals(25_000L, cfg.ticketCostPokedollars, "El billete debe costar 25,000 PokéDólares como sumidero");
        assertEquals(500L, cfg.ticketCostLunaCoins, "El billete alternativo debe costar 500 LunaCoins");
    }
}
