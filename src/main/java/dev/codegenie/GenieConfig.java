package dev.codegenie;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;

import dev.codegenie.core.Cheat;

/** {@code config/codegenie.json}. Created with defaults on first launch. */
public final class GenieConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	/** If true, only operators (permission level 2) can enter codes. Permission node: {@code codegenie:use}. */
	public boolean requireOp = false;

	/** Unknown-but-valid codes produce a harmless random "glitch", like poking random bytes on the real thing. */
	public boolean allowGlitchCodes = true;

	/** Allow the World 8 warp (teleports players to The End). */
	public boolean allowWarp = true;

	/** Cheats to switch off, by name, e.g. ["INVINCIBILITY", "P_WING"]. */
	public List<String> disabledCheats = new ArrayList<>();

	public boolean isDisabled(Cheat cheat) {
		if (cheat == Cheat.WARP_WORLD_8 && !allowWarp) {
			return true;
		}

		for (String name : disabledCheats) {
			if (name != null && name.trim().toUpperCase(Locale.ROOT).equals(cheat.name())) {
				return true;
			}
		}

		return false;
	}

	public static GenieConfig load() {
		Path path = FabricLoader.getInstance().getConfigDir().resolve("codegenie.json");
		GenieConfig config = null;

		if (Files.exists(path)) {
			try (Reader reader = Files.newBufferedReader(path)) {
				config = GSON.fromJson(reader, GenieConfig.class);
			} catch (Exception e) {
				CodeGenie.LOGGER.warn("Could not read {}, using defaults", path, e);
			}
		}

		if (config == null) {
			config = new GenieConfig();
		}

		if (config.disabledCheats == null) {
			config.disabledCheats = new ArrayList<>();
		}

		try {
			Files.createDirectories(path.getParent());

			try (Writer writer = Files.newBufferedWriter(path)) {
				GSON.toJson(config, writer);
			}
		} catch (IOException e) {
			CodeGenie.LOGGER.warn("Could not write {}", path, e);
		}

		return config;
	}
}
