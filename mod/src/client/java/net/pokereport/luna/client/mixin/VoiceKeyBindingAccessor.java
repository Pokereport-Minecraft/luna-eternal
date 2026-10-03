package net.pokereport.luna.client.mixin;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
/** Queue the exact native action without changing bindings or triggering colliding keys. */
@Mixin(KeyBinding.class)
public interface VoiceKeyBindingAccessor {
 @Accessor("timesPressed") int pokevoice$getTimesPressed();
 @Accessor("timesPressed") void pokevoice$setTimesPressed(int value);
}
