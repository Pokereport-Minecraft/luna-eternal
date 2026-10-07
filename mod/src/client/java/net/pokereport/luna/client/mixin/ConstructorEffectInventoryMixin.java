package net.pokereport.luna.client.mixin;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.pokereport.luna.world.ConstructorBuffs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import java.util.Collection;
/** Presentation only: never removes effects from the player or hides normal potions. */
@Mixin(AbstractInventoryScreen.class)
public abstract class ConstructorEffectInventoryMixin {
 @Redirect(method="drawStatusEffects",at=@At(value="INVOKE",target="Lnet/minecraft/client/network/ClientPlayerEntity;getStatusEffects()Ljava/util/Collection;"))
 private Collection<StatusEffectInstance> pokereport$visibleEffects(ClientPlayerEntity player){
  return player.getStatusEffects().stream().filter(e->!ConstructorBuffs.hiddenManagedEffect(e)).toList();
 }
}
