package net.pokereport.luna.rank;

import java.util.UUID;

/**
 * Ficha de Rango (RankToken): moneda de agradecimiento ligada permanentemente al jugador (BOUND).
 *
 * <p>Reglas estrictas de diseño:
 * <ul>
 *   <li>NO vendible a NPCs ni en GTS.</li>
 *   <li>NO transferible entre jugadores.</li>
 *   <li>NO convertible a PokéDólares ni LunaCoins.</li>
 *   <li>Canjeable exclusivamente en la Supporter Shop por cosméticos y utilidades menores.</li>
 * </ul>
 */
public final class RankToken {

    private final UUID ownerUuid;
    private long balance;

    public RankToken(UUID ownerUuid, long balance) {
        this.ownerUuid = ownerUuid;
        this.balance = Math.max(0, balance);
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public long getBalance() {
        return balance;
    }

    public boolean isBound() {
        return true;
    }

    public boolean canTrade() {
        return false;
    }

    public boolean canSell() {
        return false;
    }

    public boolean canConvertToCurrency() {
        return false;
    }

    public boolean debit(long amount) {
        if (amount <= 0) return false;
        if (balance >= amount) {
            balance -= amount;
            return true;
        }
        return false;
    }

    public void credit(long amount) {
        if (amount > 0) {
            balance += amount;
        }
    }
}
