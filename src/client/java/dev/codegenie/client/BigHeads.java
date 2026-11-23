package dev.codegenie.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;

import net.fabricmc.fabric.api.client.rendering.v1.FabricModel;

/**
 * DK Mode: after vanilla poses an entity model each frame, blow its "head" part up.
 * Vanilla resets every part's pose (scale included) at the start of the next pose,
 * so turning DK Mode off needs no clean-up.
 */
public final class BigHeads {
	public static final float SCALE = 2.25F;

	/** Set by the server (see {@code DkModePayload}); only affects what this client sees. */
	public static volatile boolean enabled = false;

	private BigHeads() {
	}

	public static void apply(Model<?> model) {
		if (!enabled || !(model instanceof EntityModel<?>)) {
			return;
		}

		ModelPart head = ((FabricModel<?>) model).getChildPart("head");

		if (head != null) {
			head.xScale = SCALE;
			head.yScale = SCALE;
			head.zScale = SCALE;
		}
	}
}
