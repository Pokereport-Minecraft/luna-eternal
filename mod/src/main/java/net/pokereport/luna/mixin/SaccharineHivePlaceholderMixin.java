package net.pokereport.luna.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.entity.BeehiveBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import java.util.function.Function;

/** Let Cobblemon's existing RETURN handler construct its saccharine hive occupant.
 * Vanilla cannot deserialize this deliberate, unregistered marker. Each failed
 * exit retries that load and emits a warning before Cobblemon handles the null.
 * Do not cancel loadEntity: that would bypass Cobblemon's positioning/nectar code.
 */
@Mixin(BeehiveBlockEntity.BeeData.class)
public abstract class SaccharineHivePlaceholderMixin {
    @WrapOperation(method = "loadEntity", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/EntityType;loadEntityWithPassengers(Lnet/minecraft/nbt/NbtCompound;Lnet/minecraft/world/World;Ljava/util/function/Function;)Lnet/minecraft/entity/Entity;"))
    private Entity pokeReport$deferPlaceholder(NbtCompound nbt, World world,
            Function<Entity, Entity> processor, Operation<Entity> original) {
        if ("cobblemon_pokemon_entity_placeholder".equals(nbt.getString("id"))) {
            return null;
        }
        return original.call(nbt, world, processor);
    }
}
