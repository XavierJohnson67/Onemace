package com.onemace.mixin;

import com.onemace.OneMaceMod;
import com.onemace.OneMaceUtil;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * BEST EFFORT: every GUI-based storage slot (chest, barrel, shulker box, ender
 * chest, hopper, dispenser, dropper...) is a plain vanilla Slot wrapping some
 * Container that isn't the player's own Inventory, so blocking it here at the
 * Slot level covers all of them in one place rather than mixing into every
 * individual block entity.
 * <p>
 * This mixin is marked require = 0 in onemace.mixins.json: if Mojang ever
 * renames these methods, the mod simply won't apply this specific rule
 * instead of failing to load. The core "one mace" tracking in MaceTicker
 * does not depend on this mixin at all.
 */
@Mixin(Slot.class)
public abstract class SlotMixin {

    @Shadow
    public Container container;

    @Shadow
    public abstract ItemStack getItem();

    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true, require = 0)
    private void onemace$blockMaceStorage(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (OneMaceUtil.isMace(stack) && !(this.container instanceof Inventory)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true, require = 0)
    private void onemace$blockDuplicateCraft(Player player, CallbackInfoReturnable<Boolean> cir) {
        // Only touches crafting-output slots, and only while a mace already exists -
        // this is purely to save the player's rare ingredients on an obviously-doomed
        // craft. If it doesn't fire (wrong method name on a future version), the
        // periodic inventory scan in MaceTicker will still confiscate the duplicate
        // a moment later; ingredients just won't be saved in that fallback case.
        if ((Object) this instanceof ResultSlot
                && OneMaceMod.STATE.exists
                && OneMaceUtil.isMace(this.getItem())) {
            cir.setReturnValue(false);
        }
    }
}
