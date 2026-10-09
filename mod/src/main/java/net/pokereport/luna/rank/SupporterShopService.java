package net.pokereport.luna.rank;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tienda de Colaboradores / Supporter Shop.
 *
 * <p>Reglas estrictas de diseño:
 * <ul>
 *   <li>Usa exclusivamente {@link RankToken}.</li>
 *   <li>Artículos 100% cosméticos: auras, títulos de chat, marcos y estilos visuales de PokéPad.</li>
 *   <li>CERO ventaja competitiva (sin estadísticas, sin IVs, sin EVs, sin objetos de combate).</li>
 *   <li>Desbloqueos permanentes ligados al UUID del jugador.</li>
 * </ul>
 */
public final class SupporterShopService {

    public enum CosmeticType {
        AURA,
        TITULO,
        POKEPAD_STYLE,
        CHAT_COLOR
    }

    public record CosmeticItem(
            String id,
            String displayName,
            CosmeticType type,
            long tokenCost,
            String description
    ) {}

    public record PurchaseResult(boolean success, String message, CosmeticItem item) {}

    private final Map<String, CosmeticItem> catalog = new ConcurrentHashMap<>();
    // playerUuid -> Set<unlockedCosmeticId>
    private final Map<UUID, Set<String>> unlockedCosmetics = new ConcurrentHashMap<>();

    public SupporterShopService() {
        initCatalog();
    }

    private void initCatalog() {
        registerItem(new CosmeticItem(
                "aura_luna_aurora",
                "§bAura de Aurora Polar",
                CosmeticType.AURA,
                5L,
                "Partículas celestes y brillantes al caminar."
        ));
        registerItem(new CosmeticItem(
                "aura_carmesi_fuego",
                "§cAura Carmesí Volcánica",
                CosmeticType.AURA,
                5L,
                "Llamas sutiles que envuelven tus pasos."
        ));
        registerItem(new CosmeticItem(
                "titulo_pionero",
                "§6[Pionero de Luna]",
                CosmeticType.TITULO,
                3L,
                "Título honorífico visible en chat y tablist."
        ));
        registerItem(new CosmeticItem(
                "titulo_veterano",
                "§e[Veterano]",
                CosmeticType.TITULO,
                3L,
                "Título de reconocimiento para veteranos del servidor."
        ));
        registerItem(new CosmeticItem(
                "pokepad_neon_cyan",
                "§3PokéPad Neón Cyber",
                CosmeticType.POKEPAD_STYLE,
                4L,
                "Tema visual futurista y cian para la interfaz de PokéPad."
        ));
        registerItem(new CosmeticItem(
                "pokepad_retro_gold",
                "§6PokéPad Vintage Dorado",
                CosmeticType.POKEPAD_STYLE,
                4L,
                "Tema visual dorado clásico de Coleccionista."
        ));
    }

    public void registerItem(CosmeticItem item) {
        if (item != null) {
            catalog.put(item.id(), item);
        }
    }

    public Optional<CosmeticItem> getItem(String id) {
        return Optional.ofNullable(catalog.get(id));
    }

    public Collection<CosmeticItem> getCatalog() {
        return Collections.unmodifiableCollection(catalog.values());
    }

    public boolean hasUnlocked(UUID playerUuid, String cosmeticId) {
        Set<String> unlocked = unlockedCosmetics.get(playerUuid);
        return unlocked != null && unlocked.contains(cosmeticId);
    }

    public Set<String> getUnlocked(UUID playerUuid) {
        Set<String> unlocked = unlockedCosmetics.get(playerUuid);
        return unlocked != null ? Collections.unmodifiableSet(unlocked) : Collections.emptySet();
    }

    /**
     * Adquiere un cosmético utilizando RankTokens del jugador.
     *
     * @param token Objeto RankToken del jugador
     * @param cosmeticId ID del cosmético deseado
     * @return Resultado de la compra
     */
    public PurchaseResult purchase(RankToken token, String cosmeticId) {
        if (token == null) {
            return new PurchaseResult(false, "No se encontró el balance de RankTokens.", null);
        }

        CosmeticItem item = catalog.get(cosmeticId);
        if (item == null) {
            return new PurchaseResult(false, "Cosmético no encontrado en el catálogo.", null);
        }

        UUID playerUuid = token.getOwnerUuid();
        Set<String> unlocked = unlockedCosmetics.computeIfAbsent(playerUuid, k -> ConcurrentHashMap.newKeySet());
        if (unlocked.contains(cosmeticId)) {
            return new PurchaseResult(false, "Ya has desbloqueado este cosmético anteriormente.", item);
        }

        if (!token.debit(item.tokenCost())) {
            return new PurchaseResult(false, "RankTokens insuficientes. Cuesta " + item.tokenCost() + " tokens (tienes " + token.getBalance() + ").", item);
        }

        unlocked.add(cosmeticId);
        return new PurchaseResult(true, "¡Has desbloqueado " + item.displayName() + " con éxito!", item);
    }

    public void unlockDirect(UUID playerUuid, String cosmeticId) {
        unlockedCosmetics.computeIfAbsent(playerUuid, k -> ConcurrentHashMap.newKeySet()).add(cosmeticId);
    }

    public void clear() {
        unlockedCosmetics.clear();
    }
}
