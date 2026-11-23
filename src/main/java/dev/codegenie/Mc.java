package dev.codegenie;

import java.util.Optional;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Thin helpers over vanilla. Effects, sounds and items are looked up by their
 * registry id strings (e.g. "jump_boost") so the mod is resilient to Java field renames
 * between Minecraft versions, and a missing id is skipped instead of crashing.
 */
public final class Mc {
	private Mc() {
	}

	public static MutableComponent prefix() {
		return Component.literal("[").withStyle(ChatFormatting.DARK_GRAY)
				.append(Component.literal("GENIE").withStyle(ChatFormatting.GOLD))
				.append(Component.literal("] ").withStyle(ChatFormatting.DARK_GRAY));
	}

	public static void say(ServerPlayer player, Component message) {
		player.sendSystemMessage(prefix().append(message));
	}

	public static void say(ServerPlayer player, String message, ChatFormatting color) {
		say(player, Component.literal(message).withStyle(color));
	}

	public static void banner(ServerPlayer player, String message, ChatFormatting color) {
		player.sendOverlayMessage(Component.literal(message).withStyle(color));
	}

	public static ServerLevel level(ServerPlayer player) {
		return (ServerLevel) player.level();
	}

	public static @Nullable Holder<MobEffect> effect(String id) {
		Optional<Holder.Reference<MobEffect>> ref = BuiltInRegistries.MOB_EFFECT.get(Identifier.withDefaultNamespace(id));

		if (ref.isEmpty()) {
			CodeGenie.LOGGER.warn("Unknown mob effect id minecraft:{}", id);
			return null;
		}

		return ref.get();
	}

	/** Adds an effect. Hidden particles, visible icon; "ambient" so it reads like a beacon effect. */
	public static void give(LivingEntity entity, String effectId, int ticks, int amplifier) {
		give(entity, effectId, ticks, amplifier, true, false);
	}

	public static void give(LivingEntity entity, String effectId, int ticks, int amplifier, boolean ambient, boolean particles) {
		Holder<MobEffect> effect = effect(effectId);

		if (effect != null) {
			entity.addEffect(new MobEffectInstance(effect, ticks, amplifier, ambient, particles, true));
		}
	}

	public static boolean has(LivingEntity entity, String effectId) {
		Holder<MobEffect> effect = effect(effectId);
		return effect != null && entity.hasEffect(effect);
	}

	public static void remove(LivingEntity entity, String effectId) {
		Holder<MobEffect> effect = effect(effectId);

		if (effect != null) {
			entity.removeEffect(effect);
		}
	}

	public static void sound(ServerPlayer player, String soundId, float volume, float pitch) {
		Optional<SoundEvent> sound = BuiltInRegistries.SOUND_EVENT.getOptional(Identifier.withDefaultNamespace(soundId));
		sound.ifPresent(s -> player.level().playSound(null, player.getX(), player.getY(), player.getZ(), s, SoundSource.PLAYERS, volume, pitch));
	}

	public static void giveItem(ServerPlayer player, String itemId, int count) {
		Item item = BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(itemId));
		ItemStack stack = new ItemStack(item, count);

		if (stack.isEmpty()) {
			return;
		}

		if (!player.addItem(stack) && !stack.isEmpty()) {
			ServerLevel level = level(player);
			level.addFreshEntity(new ItemEntity(level, player.getX(), player.getY() + 0.5, player.getZ(), stack));
		}
	}

	/** "minecraft:fall", "minecraft:out_of_world", ... or "" if the type has no key. */
	public static String damageType(DamageSource source) {
		return source.typeHolder().unwrapKey().map(key -> key.identifier().toString()).orElse("");
	}
}
