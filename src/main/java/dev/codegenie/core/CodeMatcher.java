package dev.codegenie.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Turns what a player typed into matched cheats, unknown-but-valid Game Genie codes
 * (which "glitch"), and invalid input.
 *
 * <p>A whole-input match against the classic cheat phrases is tried first, so
 * {@code /genie power overwhelming} and {@code /genie IDDQD} work as well as Game Genie codes.
 *
 * <p>Pure Java (no Minecraft classes) so it can be unit-tested on its own.
 */
public final class CodeMatcher {
	public static final int MAX_CODES = 3;

	/**
	 * @param unknownCheat set when the input was entered as a classic cheat phrase and
	 *                     matched nothing; holds what was typed.
	 */
	public record Result(List<Cheat> cheats, List<GenieCode> unknown, List<GenieCode> decoded, List<String> invalid,
			boolean tooMany, String unknownCheat) {
		public boolean isEmpty() {
			return cheats.isEmpty() && unknown.isEmpty() && invalid.isEmpty() && unknownCheat == null;
		}
	}

	private CodeMatcher() {
	}

	/** Input from the Classic Cheat Codes field ({@code /genie cheat <phrase>}). */
	public static Result matchClassic(String input) {
		Optional<Cheat> classic = Cheat.findClassic(input);

		if (classic.isPresent()) {
			return new Result(List.of(classic.get()), List.of(), List.of(), List.of(), false, null);
		}

		// Someone typing a Game Genie code into the cheat field still gets it.
		Result genie = match(input);

		if (!genie.cheats().isEmpty() || !genie.unknown().isEmpty()) {
			return genie;
		}

		return new Result(List.of(), List.of(), List.of(), List.of(), false, input.trim());
	}

	/** Input from {@code /genie <codes>}: a classic phrase, or up to three Game Genie codes. */
	public static Result match(String input) {
		Optional<Cheat> classic = Cheat.findClassic(input);

		if (classic.isPresent()) {
			return new Result(List.of(classic.get()), List.of(), List.of(), List.of(), false, null);
		}

		List<String> tokens = new ArrayList<>();

		for (String part : input.trim().split("[\\s,+]+")) {
			if (!part.isBlank()) {
				tokens.add(GenieCode.normalize(part));
			}
		}

		List<Cheat> cheats = new ArrayList<>();
		List<String> invalid = new ArrayList<>();
		Set<String> valid = new LinkedHashSet<>();
		List<GenieCode> decoded = new ArrayList<>();

		for (String token : tokens) {
			GenieCode.parse(token).ifPresentOrElse(code -> {
				if (valid.add(code.letters())) {
					decoded.add(code);
				}
			}, () -> invalid.add(token));
		}

		if (valid.size() > MAX_CODES) {
			return new Result(List.of(), List.of(), List.of(), invalid, true, null);
		}

		// Match multi-code cheats first so YSAOPE + YEAOZA + YEAPYA wins over any single-code use.
		record Candidate(Cheat cheat, List<String> codes) { }
		List<Candidate> candidates = new ArrayList<>();

		for (Cheat cheat : Cheat.values()) {
			for (List<String> alt : cheat.alternatives) {
				candidates.add(new Candidate(cheat, alt));
			}
		}

		candidates.sort(Comparator.comparingInt((Candidate c) -> c.codes().size()).reversed());
		Set<String> remaining = new LinkedHashSet<>(valid);

		for (Candidate candidate : candidates) {
			if (!candidate.codes().isEmpty() && remaining.containsAll(candidate.codes()) && !cheats.contains(candidate.cheat())) {
				remaining.removeAll(candidate.codes());
				cheats.add(candidate.cheat());
			}
		}

		List<GenieCode> unknown = new ArrayList<>();

		for (GenieCode code : decoded) {
			if (remaining.contains(code.letters())) {
				unknown.add(code);
			}
		}

		return new Result(cheats, unknown, decoded, invalid, false, null);
	}
}
