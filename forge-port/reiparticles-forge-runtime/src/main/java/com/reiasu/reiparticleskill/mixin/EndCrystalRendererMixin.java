// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.mixin;

import com.reiasu.reiparticleskill.config.SkillClientConfig;
import net.minecraft.client.renderer.entity.EndCrystalRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Suppresses the vanilla End Crystal beam so the mod's custom
 * particle effects are not obscured by the bright white beam.
 */
@Mixin(EndCrystalRenderer.class)
public abstract class EndCrystalRendererMixin {

    @Redirect(
            method = "render(Lnet/minecraft/world/entity/boss/enderdragon/EndCrystal;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/boss/enderdragon/EndCrystal;getBeamTarget()Lnet/minecraft/core/BlockPos;")
    )
    private BlockPos reiparticleskill$hideBeam(EndCrystal crystal) {
        if (!SkillClientConfig.INSTANCE.isSuppressCrystalBeam()) {
            return crystal.getBeamTarget();
        }
        return null;
    }
}
