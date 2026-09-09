package com.onemace.mixin;

import com.onemace.OneMaceUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SlotAccess;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * BEST EFFORT / LOWEST CONFIDENCE of the mixins in this mod.
 * <p>
 * Bundles don't go through the normal Slot system (SlotMixin) - inserting an
 * item into a bundle is a special click-on-the-item interaction, handled by
 * Item#overrideOtherStackedOnMe. That method's exact name/signature is the
 * one thing in this mod I could not verify against real source for 1.21.11,
 * so if this fails to apply, the mod will simply skip this one rule (see
 * require = 0 in onemace.mixins.json) rather than fail to load.
 * <p>
 * If bundle-blocking doesn't work after building: open BundleItem.class in
 * your IDE (with the mappings applied) and check the actual method that
 * handles "another item was clicked onto this bundle", then update the
 * "method" value below to match.
 */
@Mixin(BundleItem.class)
public abstract class BundleItemMixin {

    @Inject(method = "overrideOtherStackedOnMe", at = @At("HEAD"), cancellable = true, require = 0)
    private void onemace$blockMaceInBundle(ItemStack bundleStack, ItemStack otherStack, Slot slot,
                                            ClickAction action, Player player, SlotAccess access,
                                            CallbackInfoReturnable<Boolean> cir) {
        if (OneMaceUtil.isMace(otherStack)) {
            cir.setReturnValue(false);
        }
    }
}
