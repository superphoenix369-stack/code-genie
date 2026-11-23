package dev.codegenie.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

import dev.codegenie.client.BigHeads;

/**
 * DK Mode for humanoids (players, zombies, skeletons, piglins, armor stands...).
 * Runs after HumanoidModel has posed the model for this frame.
 * {@code require = 0}: if a future version changes this method, DK Mode just does nothing.
 */
@Mixin(HumanoidModel.class)
abstract class HumanoidModelMixin {
	@Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("TAIL"), require = 0)
	private void codegenie$bigHead(HumanoidRenderState state, CallbackInfo ci) {
		BigHeads.apply((Model<?>) (Object) this);
	}
}
