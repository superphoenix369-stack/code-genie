package dev.codegenie;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionLevel;

import net.fabricmc.fabric.api.permission.v1.PermissionContextOwner;

import dev.codegenie.core.Cheat;
import dev.codegenie.core.CodeMatcher;

/**
 * {@code /genie <code> [code] [code]}, {@code /genie list}, {@code /genie active}, {@code /genie off}.
 * Also registered as {@code /codegenie}. Works with vanilla clients; the mod's client
 * screen just sends this command.
 */
final class GenieCommand {
	static final Identifier USE_PERMISSION = Identifier.fromNamespaceAndPath(CodeGenie.MOD_ID, "use");

	private GenieCommand() {
	}

	static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(build("genie"));
		dispatcher.register(build("codegenie"));
		dispatcher.register(Commands.literal("cheat")
				.executes(GenieCommand::help)
				.then(Commands.argument("phrase", StringArgumentType.greedyString()).executes(ctx -> enter(ctx, "phrase", true))));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> build(String name) {
		return Commands.literal(name)
				.executes(GenieCommand::help)
				.then(Commands.literal("list").executes(GenieCommand::list))
				.then(Commands.literal("active").executes(GenieCommand::active))
				.then(Commands.literal("off").executes(GenieCommand::off))
				.then(Commands.literal("cheat").then(Commands.argument("phrase", StringArgumentType.greedyString()).executes(ctx -> enter(ctx, "phrase", true))))
				.then(Commands.argument("codes", StringArgumentType.greedyString()).executes(ctx -> enter(ctx, "codes", false)));
	}

	private static boolean allowed(CommandSourceStack source) {
		PermissionLevel needed = CodeGenie.config().requireOp ? PermissionLevel.GAMEMASTERS : PermissionLevel.ALL;
		return ((PermissionContextOwner) source).checkPermission(USE_PERMISSION, needed);
	}

	private static int enter(CommandContext<CommandSourceStack> ctx, String argument, boolean classicField) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();

		if (!allowed(ctx.getSource())) {
			Mc.say(player, "The cartridge won't seat. (You don't have permission to enter codes here.)", ChatFormatting.RED);
			return 0;
		}

		String input = StringArgumentType.getString(ctx, argument);
		CodeMatcher.Result result = classicField ? CodeMatcher.matchClassic(input) : CodeMatcher.match(input);

		if (result.tooMany()) {
			Mc.say(player, "The cartridge only holds " + CodeMatcher.MAX_CODES + " codes at a time.", ChatFormatting.RED);
			return 0;
		}

		if (result.isEmpty()) {
			return help(ctx);
		}

		CheatEngine.enter(player, result);
		return 1;
	}

	private static int help(CommandContext<CommandSourceStack> ctx) {
		send(ctx, Mc.prefix().append(Component.literal("Code Genie").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)));
		send(ctx, Component.literal("  /genie <code> [code] [code]").withStyle(ChatFormatting.WHITE)
				.append(Component.literal("  enter up to 3 codes (enter a toggle again to switch it off)").withStyle(ChatFormatting.GRAY)));
		send(ctx, Component.literal("  /cheat <phrase>").withStyle(ChatFormatting.WHITE)
				.append(Component.literal("  classic cheat codes, e.g. /cheat iddqd or /cheat power overwhelming").withStyle(ChatFormatting.GRAY)));
		send(ctx, Component.literal("  /genie list").withStyle(ChatFormatting.WHITE)
				.append(Component.literal("  the codebook").withStyle(ChatFormatting.GRAY)));
		send(ctx, Component.literal("  /genie active").withStyle(ChatFormatting.WHITE)
				.append(Component.literal("  what's plugged in right now").withStyle(ChatFormatting.GRAY)));
		send(ctx, Component.literal("  /genie off").withStyle(ChatFormatting.WHITE)
				.append(Component.literal("  pull the cartridge (turn off all toggles)").withStyle(ChatFormatting.GRAY)));
		send(ctx, Component.literal("  Tip: with the mod on your client, press G for the code-entry screen.").withStyle(ChatFormatting.DARK_GRAY));
		return 1;
	}

	private static int list(CommandContext<CommandSourceStack> ctx) {
		send(ctx, Mc.prefix().append(Component.literal("Codebook").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)
				.append(Component.literal("  (\u21ba = toggle, \u2605 = instant)").withStyle(ChatFormatting.DARK_GRAY))));

		for (Cheat cheat : Cheat.values()) {
			if (cheat.ordinal() == 0 || cheat.section != Cheat.values()[cheat.ordinal() - 1].section) {
				send(ctx, Component.literal(cheat.section == Cheat.Section.GAME_GENIE ? "\u2500\u2500 Game Genie (NES) \u2500\u2500" : "\u2500\u2500 Classic Cheat Codes \u2500\u2500")
						.withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
			}

			boolean disabled = CodeGenie.config().isDisabled(cheat);
			MutableComponent line = Component.literal(cheat.kind == Cheat.Kind.TOGGLE ? " \u21ba " : " \u2605 ").withStyle(ChatFormatting.GOLD)
					.append(Component.literal(cheat.displayCode()).withStyle(disabled ? ChatFormatting.DARK_GRAY : ChatFormatting.AQUA))
					.append(Component.literal("  " + cheat.title).withStyle(ChatFormatting.YELLOW))
					.append(Component.literal(" \u2014 " + cheat.game + ": \"" + cheat.originalEffect + "\"").withStyle(ChatFormatting.GRAY));
			send(ctx, line);
			send(ctx, Component.literal("      " + cheat.minecraftEffect + (disabled ? " [disabled]" : "")).withStyle(ChatFormatting.WHITE));
		}

		return 1;
	}

	private static int active(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		var toggles = CheatEngine.activeToggles(player);
		int lives = CheatEngine.lives(player);
		int star = CheatEngine.starTicks(player);

		if (toggles.isEmpty() && lives == 0 && star == 0) {
			Mc.say(player, "No codes active.", ChatFormatting.GRAY);
			return 1;
		}

		for (Cheat cheat : toggles) {
			Mc.say(player, Component.literal(cheat.displayCode() + "  ").withStyle(ChatFormatting.AQUA)
					.append(Component.literal(cheat.title).withStyle(ChatFormatting.YELLOW)));
		}

		if (lives > 0) {
			Mc.say(player, "Banked lives: " + lives, ChatFormatting.GREEN);
		}

		if (star > 0) {
			Mc.say(player, "Starman: " + (star / 20) + "s left", ChatFormatting.GOLD);
		}

		return 1;
	}

	private static int off(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		int count = CheatEngine.unplugAll(player);
		Mc.sound(player, "block.note_block.chime", 1.0F, 0.5F);
		Mc.say(player, count == 0 ? "Nothing was plugged in." : "Cartridge pulled. " + count + " code" + (count == 1 ? "" : "s") + " switched off.", ChatFormatting.GRAY);
		return 1;
	}

	private static void send(CommandContext<CommandSourceStack> ctx, Component message) {
		ctx.getSource().sendSuccess(() -> message, false);
	}
}
