package dev.codegenie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import dev.codegenie.core.Cheat;
import dev.codegenie.core.CodeMatcher;
import dev.codegenie.core.GenieCode;

/**
 * Everything the codes actually do.
 *
 * <p>Toggle cheats are stored as scoreboard tags on the player ({@code codegenie.<cheat>}),
 * so they survive relogs and server restarts, and are copied across respawns. Every second
 * the engine re-applies their effects, so milk or death only interrupts them briefly.
 */
public final class CheatEngine {
	private static final String LIVES_PREFIX = "codegenie.lives.";
	private static final int MAX_LIVES = 99;
	private static final int STAR_TICKS = 20 * 20;

	/** BAGUVIX: "bulletproof, fireproof, explosion-proof". */
	private static final Set<String> BAGUVIX_IMMUNE = Set.of(
			"minecraft:arrow", "minecraft:trident", "minecraft:mob_projectile", "minecraft:thrown", "minecraft:spit",
			"minecraft:wind_charge", "minecraft:fireball", "minecraft:unattributed_fireball", "minecraft:wither_skull",
			"minecraft:fireworks", "minecraft:in_fire", "minecraft:on_fire", "minecraft:campfire", "minecraft:lava",
			"minecraft:hot_floor", "minecraft:explosion", "minecraft:player_explosion");

	private static final DustParticleOptions BLOOD_DARK = new DustParticleOptions(0xFF7A0000, 1.6F);
	private static final DustParticleOptions BLOOD_BRIGHT = new DustParticleOptions(0xFFC40000, 1.0F);

	private static final Map<UUID, Integer> STAR_TIMERS = new HashMap<>();
	private static final Map<UUID, WarpOrigin> WARP_ORIGINS = new HashMap<>();

	private record WarpOrigin(ResourceKey<Level> dimension, double x, double y, double z, float yRot, float xRot) {
	}

	private CheatEngine() {
	}

	// ------------------------------------------------------------------ state

	public static boolean isOn(ServerPlayer player, Cheat cheat) {
		return player.entityTags().contains(cheat.tag());
	}

	/** On, and not disabled in the server config. */
	private static boolean isActive(ServerPlayer player, Cheat cheat) {
		return isOn(player, cheat) && !CodeGenie.config().isDisabled(cheat);
	}

	public static int lives(ServerPlayer player) {
		for (String tag : player.entityTags()) {
			if (tag.startsWith(LIVES_PREFIX)) {
				try {
					return Integer.parseInt(tag.substring(LIVES_PREFIX.length()));
				} catch (NumberFormatException ignored) {
					return 0;
				}
			}
		}

		return 0;
	}

	private static void setLives(ServerPlayer player, int lives) {
		for (String tag : new ArrayList<>(player.entityTags())) {
			if (tag.startsWith(LIVES_PREFIX)) {
				player.removeTag(tag);
			}
		}

		if (lives > 0) {
			player.addTag(LIVES_PREFIX + Math.min(lives, MAX_LIVES));
		}
	}

	public static int starTicks(ServerPlayer player) {
		return STAR_TIMERS.getOrDefault(player.getUUID(), 0);
	}

	public static List<Cheat> activeToggles(ServerPlayer player) {
		List<Cheat> list = new ArrayList<>();

		for (Cheat cheat : Cheat.values()) {
			if (cheat.kind == Cheat.Kind.TOGGLE && isOn(player, cheat)) {
				list.add(cheat);
			}
		}

		return list;
	}

	/** {@code /genie off}: pull the cartridge out. */
	public static int unplugAll(ServerPlayer player) {
		int count = 0;

		for (Cheat cheat : activeToggles(player)) {
			player.removeTag(cheat.tag());
			cleanUp(player, cheat);
			count++;
		}

		return count;
	}

	/** Copies the codes (and banked lives) to the new player object after death or leaving The End. */
	public static void copyFrom(ServerPlayer oldPlayer, ServerPlayer newPlayer) {
		for (String tag : oldPlayer.entityTags()) {
			if (tag.startsWith("codegenie.")) {
				newPlayer.addTag(tag);
			}
		}
	}

	// ------------------------------------------------------------------ entering codes

	public static void enter(ServerPlayer player, CodeMatcher.Result result) {
		GenieConfig config = CodeGenie.config();

		for (String bad : result.invalid()) {
			Mc.say(player, Component.literal(bad).withStyle(ChatFormatting.RED)
					.append(Component.literal(" isn't a valid code. Codes are 6 or 8 letters from: " + GenieCode.ALPHABET).withStyle(ChatFormatting.GRAY)));
		}

		if (result.unknownCheat() != null) {
			Mc.sound(player, "entity.villager.no", 0.8F, 1.0F);
			Mc.say(player, Component.literal("\"" + result.unknownCheat() + "\"").withStyle(ChatFormatting.WHITE)
					.append(Component.literal(" doesn't work in this game... yet. /genie list shows the codes that do.").withStyle(ChatFormatting.GRAY)));
		}

		for (Cheat cheat : result.cheats()) {
			String patch = patchFor(cheat, result);

			if (config.isDisabled(cheat)) {
				Mc.say(player, line(cheat, patch).append(Component.literal(" (disabled on this server)").withStyle(ChatFormatting.RED)));
				continue;
			}

			if (cheat.kind == Cheat.Kind.TOGGLE) {
				boolean on = !isOn(player, cheat);

				if (on) {
					player.addTag(cheat.tag());
					maintain(player, cheat, true);
					switchedOn(player, cheat);
					Mc.sound(player, "block.note_block.chime", 1.0F, 1.5F);
				} else {
					player.removeTag(cheat.tag());
					cleanUp(player, cheat);
					Mc.sound(player, "block.note_block.chime", 1.0F, 0.7F);
				}

				Mc.say(player, line(cheat, patch).append(on
						? Component.literal(" ON").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
						: Component.literal(" OFF").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)));
			} else {
				Mc.say(player, line(cheat, patch));
				fire(player, cheat);
			}
		}

		for (GenieCode code : result.unknown()) {
			Glitches.apply(player, code, config.allowGlitchCodes);
		}
	}

	private static String patchFor(Cheat cheat, CodeMatcher.Result result) {
		List<String> parts = new ArrayList<>();

		for (GenieCode code : result.decoded()) {
			for (List<String> alt : cheat.alternatives) {
				if (alt.contains(code.letters())) {
					parts.add(code.letters() + " " + code.patchString());
					break;
				}
			}
		}

		return String.join("  ", parts);
	}

	private static net.minecraft.network.chat.MutableComponent line(Cheat cheat, String patch) {
		return Component.literal(patch.isEmpty() ? "" : patch + "  ").withStyle(ChatFormatting.DARK_AQUA)
				.append(Component.literal(cheat.title).withStyle(ChatFormatting.YELLOW))
				.append(Component.literal(" (" + cheat.game + ")").withStyle(ChatFormatting.GRAY));
	}

	// ------------------------------------------------------------------ instant cheats

	private static void fire(ServerPlayer player, Cheat cheat) {
		switch (cheat) {
			case STARMAN -> {
				STAR_TIMERS.put(player.getUUID(), STAR_TICKS);
				Mc.give(player, "speed", STAR_TICKS, 1);
				Mc.give(player, "glowing", STAR_TICKS, 0);
				Mc.banner(player, "\u2605 INVINCIBLE! \u2605", ChatFormatting.GOLD);
			}
			case WARP_WORLD_8 -> warp(player);
			case INFINITE_RUPEES -> {
				Mc.giveItem(player, "emerald", 64);
				Mc.sound(player, "entity.experience_orb.pickup", 1.0F, 1.2F);
				Mc.say(player, "+64 emeralds. Spend them wisely (or don't).", ChatFormatting.GREEN);
			}
			case THIRTY_LIVES, KONAMI -> {
				int lives = Math.min(MAX_LIVES, lives(player) + 30);
				setLives(player, lives);
				Mc.sound(player, "entity.player.levelup", 1.0F, 1.0F);
				Mc.banner(player, "LIVES x" + lives, ChatFormatting.GREEN);
				Mc.say(player, "You now have " + lives + " extra lives.", ChatFormatting.GREEN);
			}
			case FREEZE_ENEMIES -> freezeEnemies(player);
			case JUSTIN_BAILEY -> {
				int tenMinutes = 20 * 60 * 10;
				Mc.give(player, "health_boost", tenMinutes, 3);
				Mc.give(player, "speed", tenMinutes, 0);
				Mc.give(player, "jump_boost", tenMinutes, 1);
				player.setHealth(player.getMaxHealth());
				Mc.giveItem(player, "crossbow", 1);
				Mc.giveItem(player, "firework_rocket", 32);
				Mc.sound(player, "entity.player.levelup", 1.0F, 1.4F);
				Mc.banner(player, "SUIT OFF. POWERED UP.", ChatFormatting.LIGHT_PURPLE);
			}
			case IDKFA -> {
				for (String item : List.of("diamond_helmet", "diamond_chestplate", "diamond_leggings", "diamond_boots",
						"diamond_sword", "bow", "crossbow", "shield")) {
					Mc.giveItem(player, item, 1);
				}

				Mc.giveItem(player, "arrow", 64);
				Mc.giveItem(player, "arrow", 64);
				Mc.sound(player, "item.armor.equip_diamond", 1.0F, 1.0F);
				Mc.banner(player, "VERY HAPPY AMMO ADDED", ChatFormatting.RED);
			}
			case HESOYAM -> {
				player.setHealth(player.getMaxHealth());
				player.getFoodData().setFoodLevel(20);
				Mc.remove(player, "absorption");
				Mc.give(player, "absorption", 20 * 60 * 2, 3);
				Mc.giveItem(player, "emerald", 25);
				Mc.sound(player, "entity.experience_orb.pickup", 1.0F, 0.8F);
				Mc.banner(player, "CHEAT ACTIVATED", ChatFormatting.WHITE);
			}
			case ROCKETMAN -> {
				Mc.giveItem(player, "elytra", 1);
				Mc.giveItem(player, "firework_rocket", 32);
				Mc.sound(player, "item.armor.equip_elytra", 1.0F, 1.0F);
				Mc.banner(player, "CHEAT ACTIVATED", ChatFormatting.WHITE);
			}
			case AIYPWZQP -> {
				Mc.give(player, "slow_falling", 20 * 60, 0);
				Mc.banner(player, "CHEAT ACTIVATED", ChatFormatting.WHITE);
			}
			case SHOW_ME_THE_MONEY -> {
				Mc.giveItem(player, "diamond", 16);
				Mc.giveItem(player, "iron_ingot", 64);
				Mc.sound(player, "entity.experience_orb.pickup", 1.0F, 1.0F);
			}
			case BLACK_SHEEP_WALL -> revealMap(player);
			case NO_COW_LEVEL -> cowLevel(player);
			case ROSEBUD -> {
				Mc.giveItem(player, "emerald", 10);
				Mc.sound(player, "entity.experience_orb.pickup", 1.0F, 1.5F);
			}
			case MOTHERLODE -> {
				Mc.giveItem(player, "emerald_block", 16);
				Mc.sound(player, "entity.player.levelup", 1.0F, 1.2F);
				Mc.banner(player, "+50,000 SIMOLEONS", ChatFormatting.GREEN);
			}
			case GREED_IS_GOOD -> {
				Mc.giveItem(player, "gold_ingot", 64);
				Mc.giveItem(player, "oak_log", 64);
				Mc.sound(player, "entity.experience_orb.pickup", 1.0F, 1.0F);
			}
			default -> {
			}
		}
	}

	private static void revealMap(ServerPlayer player) {
		Mc.give(player, "night_vision", 20 * 120, 0);
		ServerLevel level = Mc.level(player);
		List<Entity> found = level.getEntities(player, player.getBoundingBox().inflate(96.0, 48.0, 96.0), e -> e instanceof LivingEntity);

		for (Entity entity : found) {
			Mc.give((LivingEntity) entity, "glowing", 20 * 60, 0, false, false);
		}

		Mc.sound(player, "block.beacon.activate", 1.0F, 1.2F);
		Mc.say(player, "Fog of war lifted. " + found.size() + " creatures revealed.", ChatFormatting.AQUA);
	}

	private static void cowLevel(ServerPlayer player) {
		ServerLevel level = Mc.level(player);
		EntityType<?> cow = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("cow"));
		int spawned = 0;

		for (int i = 0; i < 8; i++) {
			double angle = i * Math.PI / 4.0;
			BlockPos pos = new BlockPos((int) Math.floor(player.getX() + Math.cos(angle) * 3.0), (int) Math.floor(player.getY()),
					(int) Math.floor(player.getZ() + Math.sin(angle) * 3.0));

			if (cow.spawn(level, pos, EntitySpawnReason.TRIGGERED) != null) {
				spawned++;
			}
		}

		Mc.sound(player, "entity.cow.ambient", 1.0F, 1.0F);
		Mc.say(player, spawned > 0 ? "There is a cow level now." : "There really is no cow level here.", ChatFormatting.YELLOW);
	}

	private static void freezeEnemies(ServerPlayer player) {
		ServerLevel level = Mc.level(player);
		List<Entity> targets = level.getEntities(player, player.getBoundingBox().inflate(16.0, 8.0, 16.0),
				e -> e instanceof Enemy && e instanceof LivingEntity);

		for (Entity entity : targets) {
			LivingEntity mob = (LivingEntity) entity;
			Mc.give(mob, "slowness", 300, 6, false, true);
			Mc.give(mob, "weakness", 300, 4, false, true);
			Mc.give(mob, "glowing", 300, 0, false, false);
			level.sendParticles(ParticleTypes.END_ROD, mob.getX(), mob.getY() + 1.0, mob.getZ(), 10, 0.3, 0.6, 0.3, 0.01);
		}

		Mc.sound(player, "block.amethyst_block.chime", 1.0F, 0.6F);
		Mc.say(player, targets.isEmpty() ? "No enemies in range. Brr." : "Froze " + targets.size() + " enemies for 15 seconds.", ChatFormatting.AQUA);
	}

	/** World 8: build a little obsidian landing pad in The End and warp there. Enter again in The End to warp back. */
	private static void warp(ServerPlayer player) {
		MinecraftServer server = Mc.level(player).getServer();

		if (player.level().dimension() == Level.END) {
			WarpOrigin origin = WARP_ORIGINS.remove(player.getUUID());
			ServerLevel target = origin == null ? null : server.getLevel(origin.dimension());

			if (origin != null && target != null) {
				player.teleportTo(target, origin.x(), origin.y(), origin.z(), Set.of(), origin.yRot(), origin.xRot(), true);
			} else {
				warpToSurface(server, player);
			}

			Mc.sound(player, "entity.enderman.teleport", 1.0F, 1.0F);
			Mc.banner(player, "WORLD 1-1", ChatFormatting.WHITE);
			return;
		}

		ServerLevel end = server.getLevel(Level.END);

		if (end == null) {
			Mc.say(player, "World 8 doesn't exist on this server (The End is disabled).", ChatFormatting.RED);
			return;
		}

		WARP_ORIGINS.put(player.getUUID(), new WarpOrigin(player.level().dimension(), player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot()));
		buildLandingPad(end);
		player.teleportTo(end, 100.5, 49.0, 0.5, Set.of(), 90.0F, 0.0F, true);
		Mc.give(player, "slow_falling", 100, 0);
		Mc.sound(player, "entity.enderman.teleport", 1.0F, 0.8F);
		Mc.banner(player, "WORLD 8-1", ChatFormatting.LIGHT_PURPLE);
		Mc.say(player, "Welcome to World 8. Your princess is in another castle. Enter the code again to warp back.", ChatFormatting.LIGHT_PURPLE);
	}

	/** Same 5x5 platform vanilla builds for End portal arrivals. */
	private static void buildLandingPad(ServerLevel end) {
		for (int x = 98; x <= 102; x++) {
			for (int z = -2; z <= 2; z++) {
				end.setBlockAndUpdate(new BlockPos(x, 48, z), Blocks.OBSIDIAN.defaultBlockState());

				for (int y = 49; y <= 51; y++) {
					end.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
				}
			}
		}
	}

	private static void warpToSurface(MinecraftServer server, ServerPlayer player) {
		ServerLevel overworld = server.overworld();
		BlockPos top = overworld.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.ZERO);
		player.teleportTo(overworld, top.getX() + 0.5, top.getY() + 1.0, top.getZ() + 0.5, Set.of(), player.getYRot(), 0.0F, true);
	}

	// ------------------------------------------------------------------ toggle upkeep

	/** Called every second for every online player, and once immediately when a toggle is switched on. */
	public static void maintain(ServerPlayer player, Cheat cheat, boolean refresh) {
		switch (cheat) {
			case MEGA_JUMP -> keep(player, "jump_boost", 3, refresh);
			case SUPER_SPEED -> {
				keep(player, "speed", 2, refresh);
				keep(player, "haste", 1, refresh);
			}
			case INFINITE_HEALTH -> keep(player, "regeneration", 2, refresh);
			case ONE_PUNCH_KO -> keep(player, "strength", 29, refresh);
			case INFINITE_ENERGY -> player.getFoodData().setFoodLevel(20);
			case INFINITE_HEARTS -> {
				// Re-adding absorption is what refills the golden hearts.
				if (refresh || !Mc.has(player, "absorption")) {
					Mc.remove(player, "absorption");
					Mc.give(player, "absorption", 20 * 60 * 5, 4);
				}
			}
			case KANGAROO -> keep(player, "jump_boost", 6, refresh);
			case CVWKXAM -> keep(player, "water_breathing", 0, refresh);
			case AEZAKMI -> keep(player, "invisibility", 0, refresh);
			case OPERATION_CWAL -> keep(player, "haste", 4, refresh);
			case WHOS_YOUR_DADDY -> keep(player, "strength", 29, refresh);
			case P_WING -> {
				if (!player.isCreative() && !player.isSpectator() && !player.getAbilities().mayfly) {
					player.getAbilities().mayfly = true;
					player.onUpdateAbilities();
				}
			}
			default -> {
			}
		}
	}

	private static void keep(ServerPlayer player, String effect, int amplifier, boolean refresh) {
		if (refresh || !Mc.has(player, effect)) {
			Mc.give(player, effect, 20 * 15, amplifier);
		}
	}

	private static void cleanUp(ServerPlayer player, Cheat cheat) {
		switch (cheat) {
			case MEGA_JUMP -> Mc.remove(player, "jump_boost");
			case SUPER_SPEED -> {
				Mc.remove(player, "speed");
				Mc.remove(player, "haste");
			}
			case INFINITE_HEALTH -> Mc.remove(player, "regeneration");
			case ONE_PUNCH_KO -> Mc.remove(player, "strength");
			case INFINITE_HEARTS -> Mc.remove(player, "absorption");
			case P_WING -> clipWings(player);
			case KANGAROO -> Mc.remove(player, "jump_boost");
			case CVWKXAM -> Mc.remove(player, "water_breathing");
			case AEZAKMI -> Mc.remove(player, "invisibility");
			case OPERATION_CWAL -> Mc.remove(player, "haste");
			case WHOS_YOUR_DADDY -> Mc.remove(player, "strength");
			case DK_MODE -> sendDkMode(player, false);
			default -> {
			}
		}
	}

	/** One-time hooks when a toggle is switched on (upkeep is in {@link #maintain}). */
	private static void switchedOn(ServerPlayer player, Cheat cheat) {
		if (cheat == Cheat.DK_MODE && !sendDkMode(player, true)) {
			Mc.say(player, "DK Mode is on, but you'll only see big heads with Code Genie installed on your client.", ChatFormatting.GRAY);
		}
	}

	private static boolean sendDkMode(ServerPlayer player, boolean on) {
		if (ServerPlayNetworking.canSend(player, DkModePayload.TYPE)) {
			ServerPlayNetworking.send(player, new DkModePayload(on));
			return true;
		}

		return false;
	}

	/** {@code ServerPlayConnectionEvents.JOIN}: restore DK Mode on the client after a relog. */
	public static void onJoin(ServerPlayer player) {
		if (isActive(player, Cheat.DK_MODE)) {
			sendDkMode(player, true);
		}
	}

	private static void clipWings(ServerPlayer player) {
		if (!player.isCreative() && !player.isSpectator()) {
			player.getAbilities().mayfly = false;
			player.getAbilities().flying = false;
			player.onUpdateAbilities();
		}
	}

	// ------------------------------------------------------------------ events

	public static void tick(MinecraftServer server) {
		int tick = server.getTickCount();

		if (tick % 20 == 0) {
			// Effects last 15 s; refresh every 10 s so the icon never blinks out.
			boolean refresh = tick % 200 == 0;

			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				for (Cheat cheat : activeToggles(player)) {
					if (!CodeGenie.config().isDisabled(cheat)) {
						maintain(player, cheat, refresh);
					}
				}
			}
		}

		Iterator<Map.Entry<UUID, Integer>> it = STAR_TIMERS.entrySet().iterator();

		while (it.hasNext()) {
			Map.Entry<UUID, Integer> entry = it.next();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			int left = entry.getValue() - 1;

			if (player == null || left <= 0) {
				it.remove();

				if (player != null) {
					Mc.say(player, "Starman wore off.", ChatFormatting.GRAY);
				}

				continue;
			}

			entry.setValue(left);
			starTick(player, left);
		}
	}

	private static void starTick(ServerPlayer player, int left) {
		ServerLevel level = Mc.level(player);

		for (Entity entity : level.getEntities(player, player.getBoundingBox().inflate(0.6, 0.3, 0.6),
				e -> e instanceof Enemy && e instanceof LivingEntity)) {
			LivingEntity mob = (LivingEntity) entity;

			// Bosses (Ender Dragon, Wither, Warden) shrug it off, like Bowser.
			if (mob.isAlive() && mob.getMaxHealth() <= 100.0F) {
				mob.hurtServer(level, level.damageSources().magic(), 100.0F);
			}
		}

		if (left % 2 == 0) {
			level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(), 3, 0.4, 0.7, 0.4, 0.02);
		}

		if (left % 5 == 0) {
			// A little bouncy arpeggio while the star lasts.
			float[] pitches = {1.0F, 1.26F, 1.5F, 1.26F};
			Mc.sound(player, "block.note_block.bell", 0.4F, pitches[(left / 5) % pitches.length]);
		}
	}

	/** {@code ServerLivingEntityEvents.ALLOW_DAMAGE}. Return false to cancel the hit. */
	public static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
		if (!(entity instanceof ServerPlayer player)) {
			return true;
		}

		String type = Mc.damageType(source);
		boolean unstoppable = type.equals("minecraft:generic_kill") || type.equals("minecraft:out_of_world");
		boolean fall = type.equals("minecraft:fall");

		if (!unstoppable && (STAR_TIMERS.containsKey(player.getUUID()) || isActive(player, Cheat.INVINCIBILITY)
				|| isActive(player, Cheat.GOD_MODE) || isActive(player, Cheat.WHOS_YOUR_DADDY))) {
			return false;
		}

		if (fall && (isActive(player, Cheat.MEGA_JUMP) || isActive(player, Cheat.KANGAROO) || isActive(player, Cheat.P_WING))) {
			return false;
		}

		if (BAGUVIX_IMMUNE.contains(type) && isActive(player, Cheat.BAGUVIX)) {
			return false;
		}

		if (amount > 0.0F && isOn(player, Cheat.P_WING) && !player.isCreative() && !player.isSpectator()) {
			player.removeTag(Cheat.P_WING.tag());
			clipWings(player);
			Mc.sound(player, "entity.player.hurt", 1.0F, 0.6F);
			Mc.banner(player, "OUCH! P-Wing lost", ChatFormatting.RED);
			Mc.say(player, "You got hit and lost your P-Wing. Re-enter NNOXXGEE to fly again.", ChatFormatting.RED);
		}

		return true;
	}

	/** {@code ServerLivingEntityEvents.ALLOW_DEATH}. Return false to cancel the death. */
	public static boolean allowDeath(LivingEntity entity, DamageSource source, float amount) {
		if (!(entity instanceof ServerPlayer player)) {
			return true;
		}

		String type = Mc.damageType(source);

		if (type.equals("minecraft:generic_kill")) {
			return true; // /kill always works
		}

		boolean infinite = isActive(player, Cheat.INFINITE_LIVES);
		int banked = lives(player);

		if (!infinite && banked <= 0) {
			return true;
		}

		if (!infinite) {
			setLives(player, banked - 1);
		}

		player.setHealth(player.getMaxHealth());
		Mc.give(player, "resistance", 60, 4);
		Mc.give(player, "fire_resistance", 200, 0);
		Mc.give(player, "regeneration", 100, 1);

		if (type.equals("minecraft:out_of_world")) {
			if (player.level().dimension() == Level.END) {
				ServerLevel end = Mc.level(player);
				buildLandingPad(end);
				player.teleportTo(end, 100.5, 49.0, 0.5, Set.of(), player.getYRot(), 0.0F, true);
			} else {
				warpToSurface(Mc.level(player).getServer(), player);
			}
		}

		Mc.level(player).sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.5, 0.8, 0.5, 0.1);
		Mc.sound(player, "item.totem.use", 0.8F, 1.3F);
		Mc.banner(player, infinite ? "1-UP!" : "1-UP!  Lives left: " + (banked - 1), ChatFormatting.GREEN);
		return false;
	}

	// ------------------------------------------------------------------ ABACABB

	private static boolean bloodFor(LivingEntity victim, DamageSource source) {
		if (source.getEntity() instanceof ServerPlayer attacker && isActive(attacker, Cheat.BLOOD_CODE)) {
			return true;
		}

		return victim instanceof ServerPlayer player && isActive(player, Cheat.BLOOD_CODE);
	}

	/** {@code ServerLivingEntityEvents.AFTER_DAMAGE}: a spray of red on every hit. */
	public static void afterDamage(LivingEntity entity, DamageSource source, float baseDamage, float damageTaken, boolean blocked) {
		if (blocked || damageTaken <= 0.0F || !(entity.level() instanceof ServerLevel level) || !bloodFor(entity, source)) {
			return;
		}

		int amount = Math.min(40, 6 + (int) (damageTaken * 3.0F));
		double y = (entity.getY() + entity.getEyeY()) / 2.0;
		level.sendParticles(BLOOD_DARK, entity.getX(), y, entity.getZ(), amount, 0.25, 0.3, 0.25, 0.05);
		level.sendParticles(BLOOD_BRIGHT, entity.getX(), y, entity.getZ(), amount / 2, 0.35, 0.35, 0.35, 0.1);
	}

	/** {@code ServerLivingEntityEvents.AFTER_DEATH}: a big red burst. */
	public static void afterDeath(LivingEntity entity, DamageSource source) {
		if (!(entity.level() instanceof ServerLevel level) || !bloodFor(entity, source)) {
			return;
		}

		double y = (entity.getY() + entity.getEyeY()) / 2.0;
		level.sendParticles(BLOOD_DARK, entity.getX(), y, entity.getZ(), 80, 0.5, 0.5, 0.5, 0.15);
		level.sendParticles(BLOOD_BRIGHT, entity.getX(), y, entity.getZ(), 50, 0.7, 0.6, 0.7, 0.2);
		level.sendParticles(BLOOD_DARK, entity.getX(), entity.getY() + 0.1, entity.getZ(), 40, 0.8, 0.05, 0.8, 0.0);
	}
}
