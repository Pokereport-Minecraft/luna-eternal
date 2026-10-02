package net.pokereport.luna.world;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import net.pokereport.luna.world.CommandSafetyPolicy.Access;

class CommandSafetyPolicyTest {
    @Test void normalPlayersCannotUseCriticalCommandsOrNamespacedAliases() {
        var player = new Access(false, false, false, false, false, false, true);
        for (String command : new String[]{"op Alice", "gamemode creative", "give @s diamond", "fill ~ ~ ~ ~ ~ ~ stone",
                "setblock ~ ~ ~ stone", "execute as @a run give @s diamond", "function x:y", "luna dar POKEDOLLAR 50",
                "minecraft:fill ~ ~ ~ ~ ~ ~ stone", "  MiNeCrAfT:GiVe @s diamond", "/set stone", "worldedit:brush sphere stone"}) {
            assertFalse(CommandSafetyPolicy.allowed(player, command), command);
        }
        assertTrue(CommandSafetyPolicy.allowed(player, "luna saldo"));
        assertTrue(CommandSafetyPolicy.allowed(player, "login example"));
    }
    @Test void opTwoOrThreeNeverEditEvenWithExplicitGrantsAndCreative() {
        var moderator = new Access(false, true, true, true, true, true, true);
        assertFalse(CommandSafetyPolicy.allowed(moderator, "/set stone"));
        assertFalse(CommandSafetyPolicy.allowed(moderator, "fill ~ ~ ~ ~ ~ ~ stone"));
    }
    @Test void superiorStaffNeedCreativeEditSessionAndAllRequiredGrants() {
        for (var access : new Access[]{
                new Access(true, true, true, true, false, true, true),
                new Access(true, true, true, true, true, false, true),
                new Access(true, false, true, true, true, true, true),
                new Access(true, true, false, true, true, true, true)}) {
            assertFalse(CommandSafetyPolicy.allowed(access, "setblock ~ ~ ~ stone"));
            assertFalse(CommandSafetyPolicy.allowed(access, "/set stone"));
        }
        var authorized = new Access(true, true, true, true, true, true, true);
        assertTrue(CommandSafetyPolicy.allowed(authorized, "setblock ~ ~ ~ stone"));
        assertTrue(CommandSafetyPolicy.allowed(authorized, "/set stone"));
        assertFalse(CommandSafetyPolicy.allowed(new Access(true, true, true, false, true, true, true), "give @s diamond"));
    }
    @Test void teleportAndFunctionCannotBypassTheSafeWorldEditGate() {
        var survivalAdmin = new Access(true, true, true, true, false, false, true);
        assertFalse(CommandSafetyPolicy.allowed(survivalAdmin, "execute in minecraft:overworld run fill ~ ~ ~ ~ ~ ~ stone"));
        assertFalse(CommandSafetyPolicy.allowed(survivalAdmin, "function x:y"));
        assertFalse(CommandSafetyPolicy.allowed(survivalAdmin, "luna gimnasio preparar brock"));
        assertTrue(CommandSafetyPolicy.allowed(new Access(true, true, true, true, false, false, false), "setblock ~ ~ ~ stone"));
    }
    @Test void survivalWorldCannotBeUsedAsSourceForCrossDimensionEditing() {
        var survivalAdmin = new Access(true, true, true, true, false, false, false);
        for (String command : new String[]{
                "execute in lunaeternal:ciudadela run setblock 0 70 0 stone",
                "function maps:ciudadela", "schedule function maps:ciudadela 1t",
                "clone from minecraft:overworld 0 0 0 1 1 1 to lunaeternal:ciudadela 0 70 0",
                "luna gimnasio preparar brock", "luna decorar", "luna naranja preparar"}) {
            assertFalse(CommandSafetyPolicy.allowed(survivalAdmin, command), command);
            assertTrue(CommandSafetyPolicy.allowed(new Access(true, true, true, true, true, true, false), command), command);
        }
        assertTrue(CommandSafetyPolicy.allowed(survivalAdmin, "setblock ~ ~ ~ stone"));
    }
}
