package dev.codegenie;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import dev.codegenie.core.GenieCode;

/**
 * On the real cartridge, a random-but-valid code patched some random byte of the game,
 * with unpredictable results. Here, an unknown code picks a harmless "glitch" from its
 * decoded address/value, so the same code always does the same weird thing.
 */
final class Glitches {
	private record Glitch(String effect, int ticks, int amplifier, String sound, String message) {
	}

	private static final Glitch[] GLITCHES = {
			new Glitch("levitation", 40, 0, "entity.chicken.egg", "You floated off the top of the screen!"),
			new Glitch("nausea", 140, 0, "block.beacon.deactivate", "The screen is scrolling the wrong way..."),
			new Glitch("speed", 200, 1, "entity.firework_rocket.launch", "Your sprite got the zoomies."),
			new Glitch("jump_boost", 200, 1, "entity.chicken.egg", "The jump physics byte flipped."),
			new Glitch("glowing", 300, 0, "block.beacon.activate", "Sprite flicker!"),
			new Glitch("night_vision", 600, 0, "block.beacon.activate", "The palette table changed."),
			new Glitch("slow_falling", 200, 0, "entity.chicken.egg", "Gravity got patched."),
			new Glitch("invisibility", 200, 0, "entity.enderman.teleport", "You're now an invisible sprite."),
			new Glitch("dolphins_grace", 300, 0, "entity.cat.ambient", "Swim physics everywhere? Close enough."),
			new Glitch("luck", 1200, 0, "block.note_block.chime", "Something got luckier. Hard to say what."),
			new Glitch(null, 0, 0, "entity.villager.no", "Nothing happened. That byte must not have mattered."),
	};

	private Glitches() {
	}

	static void apply(ServerPlayer player, GenieCode code, boolean allowed) {
		Component head = Component.literal(code.letters() + " " + code.patchString() + "  ").withStyle(ChatFormatting.DARK_AQUA);

		if (!allowed) {
			Mc.say(player, Component.empty().append(head).append(Component.literal("Unknown code. Nothing happened.").withStyle(ChatFormatting.GRAY)));
			return;
		}

		Glitch glitch = GLITCHES[code.fingerprint() % GLITCHES.length];

		if (glitch.effect() != null) {
			Mc.give(player, glitch.effect(), glitch.ticks(), glitch.amplifier(), false, true);
		}

		Mc.sound(player, glitch.sound(), 0.8F, 1.0F);
		Mc.say(player, Component.empty().append(head)
				.append(Component.literal("Unknown code. ").withStyle(ChatFormatting.LIGHT_PURPLE))
				.append(Component.literal(glitch.message()).withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY)));
	}
}
