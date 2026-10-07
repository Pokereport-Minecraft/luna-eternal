package net.pokereport.luna.world;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PublicGameplayTest {
 @Test void personalPokemonDevicesAllowed(){assertTrue(SafeInteractionPolicy.publicGameplay("cobblemon:pc"));assertTrue(SafeInteractionPolicy.publicGameplay("cobblemon:healing_machine"));}
 @Test void seatingAllowedButStorageAndUnknownModsAreNot(){assertTrue(SafeInteractionPolicy.publicGameplay("handcrafted:oak_chair"));assertTrue(SafeInteractionPolicy.publicGameplay("handcrafted:oak_dining_bench"));assertFalse(SafeInteractionPolicy.publicGameplay("handcrafted:oak_drawer"));assertFalse(SafeInteractionPolicy.publicGameplay("minecraft:chest"));assertFalse(SafeInteractionPolicy.publicGameplay("malicious:oak_chair"));}
}
