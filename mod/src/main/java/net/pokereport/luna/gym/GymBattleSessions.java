package net.pokereport.luna.gym;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** A medal belongs to one battle, not to any victory while a slot is reserved. */
public final class GymBattleSessions {
    private final ConcurrentHashMap<UUID, UUID> battles = new ConcurrentHashMap<>();
    public void start(UUID player, UUID battle) {
        if (battles.putIfAbsent(player, battle) != null) {
            throw new IllegalStateException("Player already has a gym battle session");
        }
    }
    public boolean finish(UUID player, UUID battle) { return battles.remove(player, battle); }
    public void remove(UUID player) { battles.remove(player); }
}
