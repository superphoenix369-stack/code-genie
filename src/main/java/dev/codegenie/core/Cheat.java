package dev.codegenie.core;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The codebook, in two sections:
 *
 * <ul>
 *   <li>{@link Section#GAME_GENIE}: real, published NES Game Genie codes (6 or 8 letters,
 *   up to three at once).</li>
 *   <li>{@link Section#CLASSIC}: famous typed-in cheats from other games (Konami code,
 *   IDDQD, HESOYAM, ...), entered as a phrase. Spaces, punctuation and case are ignored,
 *   and arrow characters count as U/D/L/R.</li>
 * </ul>
 *
 * <p>{@link Kind#TOGGLE} cheats stay on (through death, relogs and restarts) until the
 * same code is entered again or the player runs {@code /genie off}. {@link Kind#INSTANT}
 * cheats fire once each time they are entered.
 *
 * <p>Pure Java (no Minecraft classes) so it can be unit-tested on its own.
 */
public enum Cheat {
	// ============================================================== Game Genie (NES)
	INFINITE_LIVES(Section.GAME_GENIE, Kind.TOGGLE, "Infinite Lives", "Super Mario Bros.", "Infinite lives",
			"A fatal blow becomes a 1-UP: you pop back up at full health.",
			List.of(List.of("SXIOPO")), List.of()),
	MEGA_JUMP(Section.GAME_GENIE, Kind.TOGGLE, "Mega Jump", "Super Mario Bros.", "Super jump",
			"Jump Boost IV and no fall damage.",
			List.of(List.of("APZLGG"), List.of("APZLGK")), List.of()),
	SUPER_SPEED(Section.GAME_GENIE, Kind.TOGGLE, "Turbo Mode", "Super Mario Bros.", "Super speed game",
			"Speed III and Haste II.",
			List.of(List.of("YZXLNY")), List.of()),
	STARMAN(Section.GAME_GENIE, Kind.INSTANT, "Starman", "Super Mario Bros.", "Invincibility, walk right through enemies",
			"20 seconds of invincibility. Hostile mobs you touch are knocked out.",
			List.of(List.of("UATIUE")), List.of()),
	WARP_WORLD_8(Section.GAME_GENIE, Kind.INSTANT, "Warp to World 8", "Super Mario Bros.", "Start on World 8 (enter all three codes)",
			"Warps you to The End. Enter it again in The End to warp back.",
			List.of(List.of("YSAOPE", "YEAOZA", "YEAPYA")), List.of()),
	INFINITE_HEALTH(Section.GAME_GENIE, Kind.TOGGLE, "Infinite Health", "The Legend of Zelda", "Infinite health",
			"Regeneration III that never runs out.",
			List.of(List.of("SXVUZUSE")), List.of()),
	INFINITE_RUPEES(Section.GAME_GENIE, Kind.INSTANT, "Rupee Jackpot", "The Legend of Zelda", "Infinite rupees",
			"A stack of emeralds, every time you enter it.",
			List.of(List.of("SZVXASVK")), List.of()),
	INFINITE_ENERGY(Section.GAME_GENIE, Kind.TOGGLE, "Infinite Energy", "Mega Man 2", "Infinite energy",
			"Your hunger bar never drains.",
			List.of(List.of("SXXTPSSE")), List.of()),
	INVINCIBILITY(Section.GAME_GENIE, Kind.TOGGLE, "Invincibility", "Mega Man 2", "Invincibility",
			"You take no damage (except /kill and the void).",
			List.of(List.of("EIUGVTEY")), List.of()),
	THIRTY_LIVES(Section.GAME_GENIE, Kind.INSTANT, "30 Lives", "Contra", "Players start with 30 lives",
			"Banks 30 extra lives. Each one saves you from a death.",
			List.of(List.of("IPVGZGZE")), List.of()),
	FREEZE_ENEMIES(Section.GAME_GENIE, Kind.INSTANT, "Ice Beam", "Metroid", "Enemies are frozen",
			"Hostile mobs within 16 blocks are frozen in place for 15 seconds.",
			List.of(List.of("XXUAAA")), List.of()),
	INFINITE_HEARTS(Section.GAME_GENIE, Kind.TOGGLE, "Infinite Hearts", "Castlevania", "Infinite hearts",
			"Ten golden hearts that refill every 10 seconds.",
			List.of(List.of("KZUIUZKA")), List.of()),
	ONE_PUNCH_KO(Section.GAME_GENIE, Kind.TOGGLE, "One-Punch KO", "Mike Tyson's Punch-Out!!", "Knock out opponent with 1 punch",
			"Strength so high almost anything goes down in one hit.",
			List.of(List.of("AAVETLGA")), List.of()),
	P_WING(Section.GAME_GENIE, Kind.TOGGLE, "P-Wing", "Super Mario Bros. 3", "Fly forever until hit",
			"You can fly in Survival, until something hits you.",
			List.of(List.of("NNOXXGEE")), List.of()),

	// ============================================================== Classic cheat codes
	KONAMI(Section.CLASSIC, Kind.INSTANT, "Konami Code", "Contra", "30 lives",
			"Banks 30 extra lives (up to 99). Use your arrow keys, then B A.",
			List.of(), List.of("\u2191\u2191\u2193\u2193\u2190\u2192\u2190\u2192BA", "UUDDLRLRBASTART", "UPUPDOWNDOWNLEFTRIGHTLEFTRIGHTBA", "UPUPDOWNDOWNLEFTRIGHTLEFTRIGHTBASTART")),
	JUSTIN_BAILEY(Section.CLASSIC, Kind.INSTANT, "Justin Bailey", "Metroid", "Start mid-game, powered up, suit off",
			"10 minutes of extra hearts (energy tanks), speed and jump, plus a crossbow and rocket \"missiles\".",
			List.of(), List.of("JUSTIN BAILEY")),
	BLOOD_CODE(Section.CLASSIC, Kind.TOGGLE, "Blood Code", "Mortal Kombat (Genesis)", "Turns the blood back on",
			"Anything you hit bleeds, and your kills burst in red.",
			List.of(), List.of("ABACABB")),
	DK_MODE(Section.CLASSIC, Kind.TOGGLE, "DK Mode", "GoldenEye 007", "Big heads",
			"Every creature's head is huge. (What you see; needs the mod on your client.)",
			List.of(), List.of("DK MODE")),
	GOD_MODE(Section.CLASSIC, Kind.TOGGLE, "God Mode", "Doom / StarCraft", "Invulnerability",
			"You take no damage (except /kill and the void).",
			List.of(), List.of("IDDQD", "POWER OVERWHELMING")),
	IDKFA(Section.CLASSIC, Kind.INSTANT, "Keys, Full Ammo", "Doom", "All weapons, full ammo, all keys, armor",
			"Full diamond armor, a diamond sword, bow, crossbow, shield and arrows.",
			List.of(), List.of("IDKFA")),
	WHOS_YOUR_DADDY(Section.CLASSIC, Kind.TOGGLE, "Who's Your Daddy", "Warcraft III", "Invulnerable, one-hit kills",
			"No damage, and almost anything dies in one hit.",
			List.of(), List.of("WHOSYOURDADDY")),
	HESOYAM(Section.CLASSIC, Kind.INSTANT, "Health, Armor, $250k", "GTA: San Andreas", "Health, armor and $250,000",
			"Full health and food, 8 golden hearts of \"armor\", and 25 emeralds.",
			List.of(), List.of("HESOYAM", "INEEDSOMEHELP")),
	BAGUVIX(Section.CLASSIC, Kind.TOGGLE, "Semi-Infinite Health", "GTA: San Andreas", "Bulletproof, fireproof, explosion-proof",
			"Immune to arrows and projectiles, fire, lava and explosions. Falls and drowning still hurt.",
			List.of(), List.of("BAGUVIX", "NOONECANHURTME")),
	AEZAKMI(Section.CLASSIC, Kind.TOGGLE, "Never Wanted", "GTA: San Andreas", "Never get a wanted level",
			"Permanent invisibility, so mobs mostly leave you alone.",
			List.of(), List.of("AEZAKMI", "IDOASIPLEASE")),
	ROCKETMAN(Section.CLASSIC, Kind.INSTANT, "Jetpack", "GTA: San Andreas", "Spawn a jetpack",
			"An elytra and 32 firework rockets.",
			List.of(), List.of("ROCKETMAN", "YECGAA")),
	KANGAROO(Section.CLASSIC, Kind.TOGGLE, "Kangaroo", "GTA: San Andreas", "Mega jump",
			"Jump Boost VII and no fall damage.",
			List.of(), List.of("KANGAROO")),
	CVWKXAM(Section.CLASSIC, Kind.TOGGLE, "Infinite Oxygen", "GTA: San Andreas", "Infinite oxygen",
			"Permanent Water Breathing.",
			List.of(), List.of("CVWKXAM")),
	AIYPWZQP(Section.CLASSIC, Kind.INSTANT, "Parachute", "GTA: San Andreas", "Spawn a parachute",
			"60 seconds of Slow Falling.",
			List.of(), List.of("AIYPWZQP", "LETSGOBASEJUMPING")),
	SHOW_ME_THE_MONEY(Section.CLASSIC, Kind.INSTANT, "Show Me the Money", "StarCraft", "10,000 minerals and gas",
			"16 diamonds and 64 iron ingots.",
			List.of(), List.of("SHOW ME THE MONEY")),
	BLACK_SHEEP_WALL(Section.CLASSIC, Kind.INSTANT, "Black Sheep Wall", "StarCraft", "Reveal the map",
			"Night Vision for 2 minutes, and every creature within 96 blocks glows for 1 minute.",
			List.of(), List.of("BLACK SHEEP WALL")),
	OPERATION_CWAL(Section.CLASSIC, Kind.TOGGLE, "Operation CWAL", "StarCraft", "Build faster",
			"Haste V.",
			List.of(), List.of("OPERATION CWAL")),
	NO_COW_LEVEL(Section.CLASSIC, Kind.INSTANT, "There Is No Cow Level", "StarCraft / Diablo", "Instant win",
			"There is now. Eight cows appear around you.",
			List.of(), List.of("THERE IS NO COW LEVEL")),
	ROSEBUD(Section.CLASSIC, Kind.INSTANT, "Rosebud", "The Sims", "+1,000 Simoleons",
			"10 emeralds.",
			List.of(), List.of("ROSEBUD", "KACHING", "KLAPAUCIUS")),
	MOTHERLODE(Section.CLASSIC, Kind.INSTANT, "Motherlode", "The Sims", "+50,000 Simoleons",
			"16 emerald blocks.",
			List.of(), List.of("MOTHERLODE")),
	GREED_IS_GOOD(Section.CLASSIC, Kind.INSTANT, "Greed Is Good", "Warcraft III", "Gold and lumber",
			"64 gold ingots and 64 oak logs.",
			List.of(), List.of("GREEDISGOOD"));

	public enum Section { GAME_GENIE, CLASSIC }

	public enum Kind { TOGGLE, INSTANT }

	public final Section section;
	public final Kind kind;
	public final String title;
	public final String game;
	public final String originalEffect;
	public final String minecraftEffect;
	/** Game Genie only: each alternative is a set of codes that must all be entered together. */
	public final List<List<String>> alternatives;
	/** Classic only: accepted phrases as shown to players (first one is the "display" spelling). */
	public final List<String> phrases;

	Cheat(Section section, Kind kind, String title, String game, String originalEffect, String minecraftEffect,
			List<List<String>> alternatives, List<String> phrases) {
		this.section = section;
		this.kind = kind;
		this.title = title;
		this.game = game;
		this.originalEffect = originalEffect;
		this.minecraftEffect = minecraftEffect;
		this.alternatives = alternatives;
		this.phrases = phrases;
	}

	/** Primary code(s) for display: "YSAOPE + YEAOZA + YEAPYA" or "POWER OVERWHELMING". */
	public String displayCode() {
		if (section == Section.CLASSIC) {
			return phrases.isEmpty() ? "" : phrases.get(0);
		}

		return alternatives.isEmpty() ? "" : String.join(" + ", alternatives.get(0));
	}

	/** Scoreboard-tag-safe id, e.g. {@code codegenie.infinite_lives}. */
	public String tag() {
		return "codegenie." + name().toLowerCase(Locale.ROOT);
	}

	/**
	 * Normalizes a typed cheat phrase: arrows become U/D/L/R, then everything except
	 * letters and digits is dropped and the rest upper-cased.
	 * {@code "power overwhelming"} and {@code "Power-Overwhelming!"} both become {@code POWEROVERWHELMING}.
	 */
	public static String normalizePhrase(String raw) {
		StringBuilder sb = new StringBuilder(raw.length());

		for (int i = 0; i < raw.length(); i++) {
			char c = raw.charAt(i);

			switch (c) {
				case '\u2191' -> sb.append('U');
				case '\u2193' -> sb.append('D');
				case '\u2190' -> sb.append('L');
				case '\u2192' -> sb.append('R');
				default -> {
					if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) {
						sb.append(Character.toUpperCase(c));
					}
				}
			}
		}

		return sb.toString();
	}

	public static Optional<Cheat> findClassic(String raw) {
		String key = normalizePhrase(raw);

		if (key.isEmpty()) {
			return Optional.empty();
		}

		for (Cheat cheat : values()) {
			for (String phrase : cheat.phrases) {
				if (normalizePhrase(phrase).equals(key)) {
					return Optional.of(cheat);
				}
			}
		}

		return Optional.empty();
	}
}
