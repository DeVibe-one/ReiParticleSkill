// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.extend;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Extension utilities for {@link ItemStack}.
 * Originally Kotlin extension functions, ported as static utility methods.
 */
public final class ItemStackExtendKt {

    private ItemStackExtendKt() {
    }

    /**
     * Checks whether this stack's item matches the given {@link Item}.
     */
    public static boolean isOf(ItemStack stack, Item item) {
        return stack.is(item);
    }
}
