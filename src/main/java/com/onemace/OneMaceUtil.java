package com.onemace;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class OneMaceUtil {

    private OneMaceUtil() {
    }

    /** True if this stack is a (non-empty) Mace. */
    public static boolean isMace(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.is(Items.MACE);
    }
}
