package com.totemanywhere.mixin;

import com.totemanywhere.TotemFinder;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    /** Si vanilla no encontró tótem en las manos, buscamos en el resto del inventario. */
    @Inject(method = "tryUseTotem", at = @At("RETURN"), cancellable = true)
    private void totemAnywhere$search(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) return;
        if ((Object) this instanceof ServerPlayerEntity player && TotemFinder.consumeTotem(player, source)) {
            TotemFinder.applyTotemEffects(player);
            cir.setReturnValue(true);
        }
    }
}
