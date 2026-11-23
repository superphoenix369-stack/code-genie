package dev.codegenie.core;

import java.util.Locale;
import java.util.Optional;

/**
 * A single NES-style patch code: six or eight letters from the 16-letter alphabet
 * the original cartridge used. Decoding follows the real NES Game Genie bit layout,
 * so every valid code maps to the same ROM address/value it did back in 1990.
 *
 * <p>Pure Java (no Minecraft classes) so it can be unit-tested on its own.
 */
public record GenieCode(String letters, int address, int value, int compare) {
	/** The 16 letters in nybble order: A=0x0 ... N=0xF. */
	public static final String ALPHABET = "APZLGITYEOXUKSVN";

	/** Row layout of the original code-entry screen. */
	public static final String ROW_ONE = "APZLGITY";
	public static final String ROW_TWO = "EOXUKSVN";

	public boolean isEightLetter() {
		return letters.length() == 8;
	}

	/** Formats the decoded patch the way code hackers write it: {@code $91D9:AD} or {@code $94A7?03:02}. */
	public String patchString() {
		if (isEightLetter()) {
			return String.format(Locale.ROOT, "$%04X?%02X:%02X", address, compare, value);
		}

		return String.format(Locale.ROOT, "$%04X:%02X", address, value);
	}

	public static boolean isGenieLetter(char c) {
		return ALPHABET.indexOf(Character.toUpperCase(c)) >= 0;
	}

	/** Upper-cases and strips spaces/dashes. Does not validate. */
	public static String normalize(String raw) {
		StringBuilder sb = new StringBuilder(raw.length());

		for (int i = 0; i < raw.length(); i++) {
			char c = raw.charAt(i);

			if (c == ' ' || c == '-' || c == '_') {
				continue;
			}

			sb.append(Character.toUpperCase(c));
		}

		return sb.toString();
	}

	public static Optional<GenieCode> parse(String raw) {
		String code = normalize(raw);

		if (code.length() != 6 && code.length() != 8) {
			return Optional.empty();
		}

		int[] n = new int[code.length()];

		for (int i = 0; i < code.length(); i++) {
			int v = ALPHABET.indexOf(code.charAt(i));

			if (v < 0) {
				return Optional.empty();
			}

			n[i] = v;
		}

		int address = 0x8000
				| ((n[3] & 7) << 12)
				| ((n[5] & 7) << 8)
				| ((n[4] & 8) << 8)
				| ((n[2] & 7) << 4)
				| ((n[1] & 8) << 4)
				| (n[4] & 7)
				| (n[3] & 8);

		int value;
		int compare = -1;

		if (code.length() == 6) {
			value = ((n[1] & 7) << 4) | ((n[0] & 8) << 4) | (n[0] & 7) | (n[5] & 8);
		} else {
			value = ((n[1] & 7) << 4) | ((n[0] & 8) << 4) | (n[0] & 7) | (n[7] & 8);
			compare = ((n[7] & 7) << 4) | ((n[6] & 8) << 4) | (n[6] & 7) | (n[5] & 8);
		}

		return Optional.of(new GenieCode(code, address, value, compare));
	}

	/** A stable 31-bit fingerprint of the decoded patch, used to pick a "glitch" for unknown codes. */
	public int fingerprint() {
		int h = address * 31 + value;
		h = h * 31 + compare;
		h ^= (h >>> 16);
		h *= 0x45d9f3b;
		h ^= (h >>> 16);
		return h & 0x7FFFFFFF;
	}
}
