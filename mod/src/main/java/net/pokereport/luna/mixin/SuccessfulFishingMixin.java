package net.pokereport.luna.mixin;

import com.cobblemon.mod.common.entity.fishing.PokeRodFishingBobberEntity;
import com.cobblemon.mod.common.api.spawning.detail.SpawnAction;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import net.pokereport.luna.progression.OficiosListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PokeRodFishingBobberEntity.class, remap = false)
public abstract class SuccessfulFishingMixin {
    @Unique private boolean luna$fishingCredited;
    @Unique private void luna$credit() {
        var bobber = (PokeRodFishingBobberEntity)(Object)this;
        if (!luna$fishingCredited && bobber.getPlayerOwner() instanceof ServerPlayerEntity player) {
            luna$fishingCredited = true;
            OficiosListener.pescaReal(player);
        }
    }
    @Redirect(method = "use", remap = true, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/World;spawnEntity(Lnet/minecraft/entity/Entity;)Z", remap = true))
    private boolean luna$itemActuallySpawned(World world, Entity entity) {
        boolean spawned = world.spawnEntity(entity);
        if (spawned && entity instanceof ItemEntity item && !item.getStack().isEmpty()) luna$credit();
        return spawned;
    }
    @Inject(method = "spawnPokemonFromFishing", at = @At("RETURN"))
    private void luna$pokemonActuallySpawned(ServerPlayerEntity player, ItemStack rod,
            SpawnAction<?> action, CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.TRUE.equals(cir.getReturnValue())) luna$credit();
    }
}
