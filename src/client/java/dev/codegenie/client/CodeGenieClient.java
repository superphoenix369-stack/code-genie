package dev.codegenie.client;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.sdl.SDLScancode;

import net.minecraft.client.KeyMapping;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import dev.codegenie.DkModePayload;

public final class CodeGenieClient implements ClientModInitializer {
	public static final KeyMapping OPEN_GENIE = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.codegenie.open", InputConstants.Type.KEYBOARD, SDLScancode.SDL_SCANCODE_G, KeyMapping.Category.MISC));

	@Override
	public void onInitializeClient() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (OPEN_GENIE.consumeClick()) {
				if (client.player != null && client.getConnection() != null) {
					client.gui.setScreen(new GenieScreen());
				}
			}
		});

		ClientPlayNetworking.registerGlobalReceiver(DkModePayload.TYPE, (payload, context) -> BigHeads.enabled = payload.on());
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> BigHeads.enabled = false);
	}
}
