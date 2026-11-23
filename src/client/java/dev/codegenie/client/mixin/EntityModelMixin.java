package dev.codegenie.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

import dev.codegenie.client.BigHeads;

/**
 * DK Mode for every other creature with a "head" part (villagers, cows, creepers...).
 * EntityModel#setupAnim resets the pose; scaling right after it lets the subclass animate
 * a big head. {@code require = 0}: if this method isn't there, only humanoids get big heads.
 */
@Mixin(EntityModel.class)
abstract class EntityModelMixin {
	@Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;)V", at = @At("TAIL"), require = 0)
	private void codegenie$bigHead(EntityRenderState state, CallbackInfo ci) {
		BigHeads.apply((Model<?>) (Object) this);
	}
}
