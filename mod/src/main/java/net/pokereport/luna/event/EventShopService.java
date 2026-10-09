package net.pokereport.luna.event;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.pokereport.luna.LunaEternal;
import net.pokereport.luna.economy.Currency;
import net.pokereport.luna.progression.ProgressionBalanceConfig;
import net.pokereport.luna.progression.TrainerLicense;
import net.pokereport.luna.progression.TrainerLicenseService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tienda de Temporada / Eventos (EventShopService).
 *
 * <p>Reglas estrictas de diseño:
 * <ul>
 *   <li>Control de límites por jugador (anti-hoarding y anti-alt).</li>
 *   <li>Requisito de Licencia de Entrenador mínima para desbloquear artículos de alto valor.</li>
 *   <li>Integración con moneda configurable (PokéDólares o LunaCoins).</li>
 *   <li>Catálogo extensible con soporte para eventos estacionales (ej. Halloween).</li>
 * </ul>
 */
public final class EventShopService {

    public record SeasonalItem(
            String id,
            String displayName,
            String itemIdentifier,
            long price,
            Currency currency,
            TrainerLicense minLicense,
            int limitPerPlayer
    ) {}

    public record PurchaseResult(boolean success, String message, SeasonalItem item, int amountBought) {}

    private final Map<String, SeasonalItem> catalog = new ConcurrentHashMap<>();
    // playerUuid -> (itemId -> countPurchased)
    private final Map<UUID, Map<String, Integer>> playerPurchases = new ConcurrentHashMap<>();

    public EventShopService() {
        initDefaultSeasonalCatalog();
    }

    private void initDefaultSeasonalCatalog() {
        // Catálogo temático estacional (Halloween / Especial)
        registerItem(new SeasonalItem(
                "dusk_ball_pack",
                "§2Pack x5 Dusk Ball",
                "cobblemon:dusk_ball",
                1_500L,
                Currency.POKEDOLLAR,
                TrainerLicense.NOVATO,
                10
        ));
        registerItem(new SeasonalItem(
                "rare_candy_seasonal",
                "§dCaramelo Raro Festivo",
                "cobblemon:rare_candy",
                12_000L,
                Currency.POKEDOLLAR,
                TrainerLicense.BRONCE,
                3
        ));
        registerItem(new SeasonalItem(
                "dusk_stone_seasonal",
                "§5Piedra Noche Mística",
                "cobblemon:dusk_stone",
                25_000L,
                Currency.POKEDOLLAR,
                TrainerLicense.PLATA,
                2
        ));
        registerItem(new SeasonalItem(
                "halloween_lantern",
                "§6Farol de las Ánimas",
                "minecraft:soul_lantern",
                300L,
                Currency.REPORTCOIN,
                TrainerLicense.NOVATO,
                5
        ));
    }

    public void registerItem(SeasonalItem item) {
        if (item != null) {
            catalog.put(item.id(), item);
        }
    }

    public Optional<SeasonalItem> getItem(String id) {
        return Optional.ofNullable(catalog.get(id));
    }

    public Collection<SeasonalItem> getCatalog() {
        return Collections.unmodifiableCollection(catalog.values());
    }

    public int getPurchasedCount(UUID playerUuid, String itemId) {
        Map<String, Integer> purchases = playerPurchases.get(playerUuid);
        if (purchases == null) return 0;
        return purchases.getOrDefault(itemId, 0);
    }

    /**
     * Procesa la compra de un artículo de la tienda estacional.
     */
    public PurchaseResult purchase(UUID playerUuid, long playerId, String itemId, int quantity) {
        if (quantity <= 0) {
            return new PurchaseResult(false, "La cantidad debe ser mayor a cero.", null, 0);
        }

        var cfg = ProgressionBalanceConfig.get().events;
        if (!cfg.active) {
            return new PurchaseResult(false, "La tienda de eventos no está disponible (evento inactivo).", null, 0);
        }

        SeasonalItem item = catalog.get(itemId);
        if (item == null) {
            return new PurchaseResult(false, "Artículo no encontrado en el catálogo del evento.", null, 0);
        }

        // 1. Verificación de Licencia mínima
        TrainerLicense playerLicense = TrainerLicenseService.getLicense(playerUuid);
        if (playerLicense.ordinal() < item.minLicense().ordinal()) {
            return new PurchaseResult(false, "Requiere Licencia " + item.minLicense().formatted + " §co superior.", item, 0);
        }

        // 2. Verificación de Límite por jugador
        Map<String, Integer> purchases = playerPurchases.computeIfAbsent(playerUuid, k -> new ConcurrentHashMap<>());
        int alreadyBought = purchases.getOrDefault(itemId, 0);
        if (alreadyBought + quantity > item.limitPerPlayer()) {
            int remaining = Math.max(0, item.limitPerPlayer() - alreadyBought);
            return new PurchaseResult(false, "Supera el límite de compra. Te quedan: " + remaining + " unidades.", item, 0);
        }

        // 3. Cobro
        long totalCost = Math.multiplyExact(item.price(), (long) quantity);
        try {
            if (LunaEternal.economy() != null) {
                String idempotencyKey = "event_shop_" + itemId + "_" + playerUuid + "_" + System.currentTimeMillis();
                LunaEternal.economy().debit(playerId, item.currency(), totalCost, "event_shop_buy", idempotencyKey);
            }
        } catch (Exception e) {
            return new PurchaseResult(false, "Saldo insuficiente para completar la compra (" + e.getMessage() + ").", item, 0);
        }

        // 4. Registro exitoso
        purchases.put(itemId, alreadyBought + quantity);
        return new PurchaseResult(true, "¡Has adquirido " + quantity + "x " + item.displayName() + "!", item, quantity);
    }

    public void clearPurchases() {
        playerPurchases.clear();
    }
}
