package net.pokereport.luna.economy;

import net.pokereport.luna.progression.ProgressionBalanceConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EconomyRewardTest {

    @BeforeEach
    void setUp() {
        ProgressionBalanceConfig.Data data = new ProgressionBalanceConfig.Data();
        ProgressionBalanceConfig.set(data);
    }

    @Test
    void testRewardSourceFromReason() {
        assertEquals(RewardSource.QUEST, RewardSource.fromReason("quest_reward_daily"));
        assertEquals(RewardSource.NPC_SHOP, RewardSource.fromReason("shop_sell_items"));
        assertEquals(RewardSource.POKEMON_DROP, RewardSource.fromReason("hunt_reward_pidgey"));
        assertEquals(RewardSource.GTS, RewardSource.fromReason("gts_sale_item"));
        assertEquals(RewardSource.MINING, RewardSource.fromReason("oficio_minero_exp"));
        assertEquals(RewardSource.BREEDING, RewardSource.fromReason("crianza_slot_egg"));
        assertEquals(RewardSource.GYM, RewardSource.fromReason("gym_leader_win"));
        assertEquals(RewardSource.EVENT, RewardSource.fromReason("event_participation"));
        assertEquals(RewardSource.KIT, RewardSource.fromReason("kit_daily_claim"));
        assertEquals(RewardSource.ADMIN, RewardSource.fromReason("admin_compensation"));
        assertEquals(RewardSource.TRIVIA, RewardSource.fromReason("quien_es_ese_pokemon_guess"));
        assertEquals(RewardSource.PREMIUM, RewardSource.fromReason("tebex_lunacoins_credit"));
        assertEquals(RewardSource.OTHER, RewardSource.fromReason("unknown_system_reason"));
        assertEquals(RewardSource.OTHER, RewardSource.fromReason(null));
    }

    @Test
    void testDiminishingReturnsBrackets() {
        var eco = ProgressionBalanceConfig.get().economy;

        // Bracket 1: 0 to 20k -> 1.0 (100%)
        assertEquals(1.00, eco.getMarginalMultiplier(0L), 0.001);
        assertEquals(1.00, eco.getMarginalMultiplier(10_000L), 0.001);
        assertEquals(1.00, eco.getMarginalMultiplier(19_999L), 0.001);

        // Bracket 2: 20k to 50k -> 0.80 (80%)
        assertEquals(0.80, eco.getMarginalMultiplier(20_000L), 0.001);
        assertEquals(0.80, eco.getMarginalMultiplier(35_000L), 0.001);
        assertEquals(0.80, eco.getMarginalMultiplier(49_999L), 0.001);

        // Bracket 3: 50k to 100k -> 0.50 (50%)
        assertEquals(0.50, eco.getMarginalMultiplier(50_000L), 0.001);
        assertEquals(0.50, eco.getMarginalMultiplier(75_000L), 0.001);
        assertEquals(0.50, eco.getMarginalMultiplier(99_999L), 0.001);

        // Excess Bracket: > 100k -> 0.25 (25%)
        assertEquals(0.25, eco.getMarginalMultiplier(100_000L), 0.001);
        assertEquals(0.25, eco.getMarginalMultiplier(200_000L), 0.001);
        assertEquals(0.25, eco.getMarginalMultiplier(500_000L), 0.001);
    }

    @Test
    void testDiminishingReturnsKillSwitch() {
        var eco = ProgressionBalanceConfig.get().economy;
        eco.diminishingReturnsEnabled = false;

        assertEquals(1.00, eco.getMarginalMultiplier(0L), 0.001);
        assertEquals(1.00, eco.getMarginalMultiplier(100_000L), 0.001);
        assertEquals(1.00, eco.getMarginalMultiplier(500_000L), 0.001);
    }
}
