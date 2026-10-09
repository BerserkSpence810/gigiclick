package com.gigiclick;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Config {
	public static final int MIN_ALLOWED = 1;
	public static final int MAX_ALLOWED = 40;
	public static final int DEFAULT_MIN = 8;
	public static final int DEFAULT_MAX = 12;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("gigiclick.json");

	public boolean enabled = true;
	public int minCps = DEFAULT_MIN;
	public int maxCps = DEFAULT_MAX;
	public boolean showHud = true;

	public static Config load() {
		Config config = new Config();
		if (Files.exists(PATH)) {
			try {
				Config loaded = GSON.fromJson(Files.readString(PATH), Config.class);
				if (loaded != null) {
					config = loaded;
				}
			} catch (Exception e) {
				GigiClick.LOGGER.warn("Failed to read {}, using defaults", PATH, e);
			}
		}
		config.setRange(config.minCps, config.maxCps);
		config.save();
		return config;
	}

	public void save() {
		try {
			Files.writeString(PATH, GSON.toJson(this));
		} catch (IOException e) {
			GigiClick.LOGGER.warn("Failed to save {}", PATH, e);
		}
	}

	public void setRange(int min, int max) {
		min = Math.clamp(min, MIN_ALLOWED, MAX_ALLOWED);
		max = Math.clamp(max, MIN_ALLOWED, MAX_ALLOWED);
		minCps = Math.min(min, max);
		maxCps = Math.max(min, max);
	}

	public void resetToDefaults() {
		enabled = true;
		showHud = true;
		setRange(DEFAULT_MIN, DEFAULT_MAX);
	}
}
