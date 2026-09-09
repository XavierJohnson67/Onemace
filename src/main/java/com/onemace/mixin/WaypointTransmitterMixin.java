package com.onemace.mixin;

import com.onemace.OneMaceMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.waypoints.WaypointTransmitter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla's locator bar (added 1.21.6) lets a player hide their waypoint from
 * others by sneaking, wearing a mob head/carved pumpkin, or being invisible.
 * That check lives in the static WaypointTransmitter.cannotReceive(source, receiver)
 * method. We force it to say "no, don't hide them" whenever the source is the
 * current mace holder, regardless of what vanilla's own checks decided.
 */
@Mixin(WaypointTransmitter.class)
public abstract class WaypointTransmitterMixin {

    @Inject(method = "cannotReceive", at = @At("HEAD"), cancellable = true, require = 0)
    private static void onemace$forceMaceHolderVisible(LivingEntity source, ServerPlayer receiver,
                                                         CallbackInfoReturnable<Boolean> cir) {
        if (OneMaceMod.STATE.exists
                && OneMaceMod.STATE.holderUuid != null
                && source.getUUID().equals(OneMaceMod.STATE.holderUuid)) {
            cir.setReturnValue(false);
        }
    }
}
