package net.pokereport.luna.mixin.battle;

import com.gitlab.srcmc.rctapi.api.errors.RCTErrors;
import com.gitlab.srcmc.rctapi.api.models.Gimmicks;
import com.gitlab.srcmc.rctapi.api.models.PokemonModel;
import com.gitlab.srcmc.rctapi.api.models.converter.PokemonModelConverter;
import com.gitlab.srcmc.rctapi.api.util.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;

/**
 * Corrige fallos por NullPointerException en PokemonModelConverter.toTarget().
 *
 * <p>En RCT API 0.16.0-beta, {@code toTarget} invoca directamente {@code model.getNickname().getLiteral()}
 * sin comprobar nulidad previa. Si un datapack, entrenador personalizado o generador omite el nickname,
 * el género o los artilugios (gimmicks), se genera un NPE no capturado que aborta el inicio del combate
 * y deja la entidad del NPC bloqueada.
 */
@Mixin(value = PokemonModelConverter.class, remap = false)
public abstract class PokemonModelConverterFixMixin {

    @Inject(method = "toTarget", at = @At("HEAD"), remap = false)
    private void pokeReport$sanitizeModel(PokemonModel model, RCTErrors<?> errors, CallbackInfoReturnable<?> cir) {
        if (model == null) return;
        try {
            if (model.getNickname() == null) {
                Field f = PokemonModel.class.getDeclaredField("nickname");
                f.setAccessible(true);
                f.set(model, Text.empty());
            }
            if (model.getGender() == null) {
                Field f = PokemonModel.class.getDeclaredField("gender");
                f.setAccessible(true);
                f.set(model, "GENDERLESS");
            }
            if (model.getGimmicks() == null) {
                Field f = PokemonModel.class.getDeclaredField("gimmicks");
                f.setAccessible(true);
                f.set(model, new Gimmicks());
            }
        } catch (Throwable ignored) {}
    }
}
