package dev.codegenie.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

import dev.codegenie.core.GenieCode;

/**
 * The code-entry screen, in two parts:
 *
 * <ul>
 *   <li>GAME GENIE CODES: two rows of eight letters and three code lines, like the original cartridge.</li>
 *   <li>CLASSIC CHEAT CODES: a free-text field for things like IDDQD, HESOYAM or the Konami
 *   code (the arrow keys type arrows). It has the cursor when the screen opens.</li>
 * </ul>
 *
 * START sends {@code /genie cheat <text>} or {@code /genie CODE CODE CODE} to the server.
 * Like most menus, this screen pauses a singleplayer world.
 */
public final class GenieScreen extends Screen {
	private static final int PANEL_W = 288;
	private static final int PANEL_H = 234;
	private static final int LINES = 3;
	private static final int MAX_LEN = 8;
	private static final int MAX_CHEAT_LEN = 48;

	private static final int[] RAINBOW = {
			0xFFFF5555, 0xFFFFAA00, 0xFFFFFF55, 0xFF55FF55, 0xFF55FFFF, 0xFF5599FF, 0xFFAA77FF, 0xFFFF77DD
	};
	private static final ChatFormatting[] LETTER_COLORS = {
			ChatFormatting.RED, ChatFormatting.GOLD, ChatFormatting.YELLOW, ChatFormatting.GREEN,
			ChatFormatting.AQUA, ChatFormatting.BLUE, ChatFormatting.LIGHT_PURPLE, ChatFormatting.WHITE
	};

	// Game Genie lines are kept between openings, like codes staying on the cartridge until you power off.
	private static final StringBuilder[] CODES = {new StringBuilder(), new StringBuilder(), new StringBuilder()};
	private static int activeLine = 0;

	// The cheat field starts empty and focused every time, for quick entry.
	private final StringBuilder cheat = new StringBuilder();
	private boolean cheatFocus = true;

	private int left;
	private int top;

	public GenieScreen() {
		super(Component.literal("Code Genie"));
	}

	@Override
	protected void init() {
		this.left = (this.width - PANEL_W) / 2;
		this.top = Math.max(2, (this.height - PANEL_H) / 2);
		int cx = this.width / 2;

		addLetterRow(GenieCode.ROW_ONE, cx, this.top + 30);
		addLetterRow(GenieCode.ROW_TWO, cx, this.top + 52);

		for (int i = 0; i < LINES; i++) {
			final int line = i;
			this.addRenderableWidget(Button.builder(Component.literal(String.valueOf(i + 1)), b -> focusLine(line))
					.pos(this.left + 8, lineY(i) - 1)
					.size(20, 20)
					.tooltip(Tooltip.create(Component.literal("Edit Game Genie line " + (i + 1))))
					.build());
		}

		this.addRenderableWidget(Button.builder(Component.literal("CHEAT"), b -> this.cheatFocus = true)
				.pos(this.left + 8, fieldY())
				.size(40, 20)
				.tooltip(Tooltip.create(Component.literal("Type a classic cheat code (Tab also switches)")))
				.build());

		int by = this.top + 192;
		this.addRenderableWidget(Button.builder(Component.literal("\u2190 DEL"), b -> backspace()).pos(cx - 106, by).size(60, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal("CLEAR"), b -> clearAll()).pos(cx - 40, by).size(60, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal("START").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), b -> start())
				.pos(cx + 26, by).size(80, 20)
				.tooltip(Tooltip.create(Component.literal("Send the codes (Enter)")))
				.build());
	}

	private void addLetterRow(String letters, int cx, int y) {
		int x0 = cx - (8 * 24 - 2) / 2;

		for (int i = 0; i < letters.length(); i++) {
			char letter = letters.charAt(i);
			int value = GenieCode.ALPHABET.indexOf(letter);
			ChatFormatting color = LETTER_COLORS[i % LETTER_COLORS.length];
			this.addRenderableWidget(Button.builder(Component.literal(String.valueOf(letter)).withStyle(color, ChatFormatting.BOLD), b -> {
				this.cheatFocus = false;
				typeGenie(letter);
			})
					.pos(x0 + i * 24, y)
					.size(22, 20)
					.tooltip(Tooltip.create(Component.literal(String.format(Locale.ROOT, "%c = 0x%X", letter, value))))
					.build());
		}
	}

	private int lineY(int line) {
		return this.top + 78 + line * 22;
	}

	private int fieldY() {
		return this.top + 163;
	}

	private int fieldLeft() {
		return this.left + 52;
	}

	private int fieldRight() {
		return this.left + PANEL_W - 8;
	}

	// ------------------------------------------------------------------ editing

	private void focusLine(int line) {
		this.cheatFocus = false;
		activeLine = line;
	}

	private static void typeGenie(char letter) {
		StringBuilder code = CODES[activeLine];

		if (code.length() >= MAX_LEN) {
			if (activeLine >= LINES - 1) {
				return;
			}

			activeLine++;
			code = CODES[activeLine];

			if (code.length() >= MAX_LEN) {
				return;
			}
		}

		code.append(letter);

		if (code.length() == MAX_LEN && activeLine < LINES - 1) {
			activeLine++;
		}
	}

	private void typeCheat(String text) {
		if (this.cheat.length() + text.length() <= MAX_CHEAT_LEN) {
			this.cheat.append(text);
		}
	}

	private void backspace() {
		if (this.cheatFocus) {
			if (!this.cheat.isEmpty()) {
				this.cheat.setLength(this.cheat.length() - 1);
			}

			return;
		}

		if (CODES[activeLine].isEmpty() && activeLine > 0) {
			activeLine--;
		}

		StringBuilder code = CODES[activeLine];

		if (!code.isEmpty()) {
			code.setLength(code.length() - 1);
		}
	}

	private void clearAll() {
		for (StringBuilder code : CODES) {
			code.setLength(0);
		}

		activeLine = 0;
		this.cheat.setLength(0);
	}

	/** Tab order: cheat field, line 1, line 2, line 3, back to the cheat field. */
	private void nextField() {
		if (this.cheatFocus) {
			focusLine(0);
		} else if (activeLine < LINES - 1) {
			activeLine++;
		} else {
			this.cheatFocus = true;
		}
	}

	private void start() {
		if (this.minecraft.player == null) {
			return;
		}

		List<String> codes = new ArrayList<>();

		for (StringBuilder code : CODES) {
			if (!code.isEmpty()) {
				codes.add(code.toString());
			}
		}

		String typed = this.cheat.toString().trim();
		String command;

		if (!typed.isEmpty() && (this.cheatFocus || codes.isEmpty())) {
			command = "genie cheat " + typed;
		} else if (!codes.isEmpty()) {
			command = "genie " + String.join(" ", codes);
		} else {
			return;
		}

		this.minecraft.player.connection.sendCommand(command);
		this.onClose();
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int key = event.key();

		if (key == InputConstants.KEY_BACKSPACE) {
			backspace();
			return true;
		}

		if (key == InputConstants.KEY_RETURN) {
			start();
			return true;
		}

		if (key == InputConstants.KEY_TAB) {
			nextField();
			return true;
		}

		if (this.cheatFocus) {
			// The arrow keys type arrows, so the Konami code can be entered the way it was meant to be.
			if (key == InputConstants.KEY_UP) {
				typeCheat("\u2191");
				return true;
			}

			if (key == InputConstants.KEY_DOWN) {
				typeCheat("\u2193");
				return true;
			}

			if (key == InputConstants.KEY_LEFT) {
				typeCheat("\u2190");
				return true;
			}

			if (key == InputConstants.KEY_RIGHT) {
				typeCheat("\u2192");
				return true;
			}
		}

		return super.keyPressed(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		String typed = event.codepointAsString();

		if (typed.length() != 1) {
			return super.charTyped(event);
		}

		char c = typed.charAt(0);

		if (this.cheatFocus) {
			if (c >= ' ' && c != '\u00a7' && c != 127) {
				typeCheat(typed);
				return true;
			}
		} else if (GenieCode.isGenieLetter(c)) {
			typeGenie(Character.toUpperCase(c));
			return true;
		}

		return super.charTyped(event);
	}

	// ------------------------------------------------------------------ drawing

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);

		int right = this.left + PANEL_W;
		int bottom = this.top + PANEL_H;

		// Cartridge body: gold rim, navy face.
		graphics.fill(this.left - 2, this.top - 2, right + 2, bottom + 2, 0xFFC8A040);
		graphics.fill(this.left, this.top, right, bottom, 0xF0101030);
		graphics.fill(this.left, this.top + 18, right, this.top + 19, 0xFF3A3A70);
		graphics.fill(this.left, this.top + 145, right, this.top + 146, 0xFF3A3A70);
		graphics.fill(this.left, this.top + 187, right, this.top + 188, 0xFF3A3A70);

		for (int line = 0; line < LINES; line++) {
			int y = lineY(line);
			boolean lineActive = !this.cheatFocus && line == activeLine;

			for (int slot = 0; slot < MAX_LEN; slot++) {
				int x = boxX(slot);
				boolean current = lineActive && slot == Math.min(CODES[line].length(), MAX_LEN - 1);
				int border = lineActive ? (current ? 0xFF55FFFF : 0xFF5577AA) : 0xFF333355;
				graphics.fill(x - 1, y - 1, x + 15, y + 19, border);
				graphics.fill(x, y, x + 14, y + 18, 0xFF000000);
			}
		}

		int fy = fieldY();
		graphics.fill(fieldLeft() - 1, fy - 1, fieldRight() + 1, fy + 21, this.cheatFocus ? 0xFF55FFFF : 0xFF333355);
		graphics.fill(fieldLeft(), fy, fieldRight(), fy + 20, 0xFF000000);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);

		// Rainbow title.
		String title = "CODE GENIE";
		int x = (this.width - this.font.width(title)) / 2;

		for (int i = 0; i < title.length(); i++) {
			String ch = String.valueOf(title.charAt(i));

			if (title.charAt(i) != ' ') {
				graphics.text(this.font, ch, x, this.top + 6, RAINBOW[i % RAINBOW.length], true);
			}

			x += this.font.width(ch);
		}

		boolean blink = (System.currentTimeMillis() / 400) % 2 == 0;

		graphics.text(this.font, "GAME GENIE CODES", this.left + 8, this.top + 21, 0xFFC8A040, false);

		for (int line = 0; line < LINES; line++) {
			int y = lineY(line);
			String code = CODES[line].toString();
			boolean lineActive = !this.cheatFocus && line == activeLine;

			for (int slot = 0; slot < MAX_LEN; slot++) {
				String ch;
				int color;

				if (slot < code.length()) {
					ch = String.valueOf(code.charAt(slot));
					color = RAINBOW[GenieCode.ALPHABET.indexOf(code.charAt(slot)) % RAINBOW.length];
				} else if (lineActive && slot == code.length() && blink) {
					ch = "_";
					color = 0xFFFFFFFF;
				} else {
					continue;
				}

				graphics.centeredText(this.font, ch, boxX(slot) + 7, y + 5, color);
			}

			Optional<GenieCode> decoded = GenieCode.parse(code);
			String info = decoded.map(GenieCode::patchString).orElse(code.isEmpty() ? "" : code.length() < 6 ? "..." : "?");
			graphics.text(this.font, info, boxX(MAX_LEN) + 6, y + 5, decoded.isPresent() ? 0xFF55FF55 : 0xFF777799, false);
		}

		graphics.text(this.font, "CLASSIC CHEAT CODES", this.left + 8, this.top + 151, 0xFFC8A040, false);

		int fy = fieldY() + 6;
		int maxWidth = fieldRight() - fieldLeft() - 12;

		if (this.cheat.isEmpty() && !this.cheatFocus) {
			graphics.text(this.font, "IDDQD, HESOYAM, \u2191\u2191\u2193\u2193\u2190\u2192\u2190\u2192BA...", fieldLeft() + 4, fy, 0xFF555577, false);
		} else {
			// Show the end of the text if it's wider than the field.
			String shown = this.cheat.toString();

			while (!shown.isEmpty() && this.font.width(shown) > maxWidth) {
				shown = shown.substring(1);
			}

			graphics.text(this.font, shown, fieldLeft() + 4, fy, 0xFFFFFFFF, false);

			if (this.cheatFocus && blink) {
				graphics.text(this.font, "_", fieldLeft() + 4 + this.font.width(shown), fy, 0xFFFFFFFF, false);
			}
		}

		graphics.centeredText(this.font, "Tab = switch field \u2022 Enter = START \u2022 /genie list",
				this.width / 2, this.top + 219, 0xFF9999BB);
	}

	private int boxX(int slot) {
		return this.left + 36 + slot * 18;
	}
}
