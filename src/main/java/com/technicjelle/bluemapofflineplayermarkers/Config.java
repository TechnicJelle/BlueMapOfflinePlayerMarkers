package com.technicjelle.bluemapofflineplayermarkers;

import org.bukkit.GameMode;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Config {
	public static final String MARKER_SET_ID = "offplrs";

	private final BlueMapOfflinePlayerMarkers plugin;

	public String markerSetName;
	public boolean toggleable;
	public boolean defaultHidden;
	public long expireTimeInHours;
	public List<GameMode> hiddenGameModes;

	public Config(BlueMapOfflinePlayerMarkers plugin) {
		this.plugin = plugin;

		if (plugin.getDataFolder().mkdirs()) plugin.getLogger().info("Created plugin config directory");
		File configFile = new File(plugin.getDataFolder(), "config.yml");
		if (!configFile.exists()) {
			try {
				plugin.getLogger().info("Creating config file");
				Files.copy(Objects.requireNonNull(plugin.getResource("config.yml")), configFile.toPath());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		//Load config from disk
		plugin.reloadConfig();

		//Load config values into variables
		markerSetName = configFile().getString("MarkerSetName");
		toggleable = configFile().getBoolean("Toggleable");
		defaultHidden = configFile().getBoolean("DefaultHidden");
		expireTimeInHours = configFile().getLong("ExpireTimeInHours");
		hiddenGameModes = parseGameModes(configFile().getStringList("HiddenGameModes"));
	}

	private List<GameMode> parseGameModes(List<String> hiddenGameModesStrings) {
		ArrayList<GameMode> gameModes = new ArrayList<>();
		for (String gm : hiddenGameModesStrings) {
			try {
				gameModes.add(GameMode.valueOf(gm.toUpperCase()));
			} catch (IllegalArgumentException e) {
				plugin.getLogger().warning("Invalid Game Mode: " + gm);
			}
		}
		return gameModes;
	}

	private FileConfiguration configFile() {
		return plugin.getConfig();
	}
}
