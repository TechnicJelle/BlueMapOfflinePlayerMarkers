package com.technicjelle.bluemapofflineplayermarkers;

import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Objects;

import static com.technicjelle.bluemapofflineplayermarkers.Main.logger;

public class Config {

	public static final String MARKER_SET_ID = "offplrs";

	private final Main plugin;

	@NotNull
	private FileConfiguration configFile() {
		return plugin.getConfig();
	}

	public String markerSetName;
	public boolean useBlueMapSource;
	public URL skinURL;
	public boolean verboseErrors;

	public Config(Main plugin) {
		this.plugin = plugin;

		if(plugin.getDataFolder().mkdirs()) logger.info("Created plugin config directory");
		File configFile = new File(plugin.getDataFolder(), "config.yml");
		if (!configFile.exists()) {
			try {
				logger.info("Creating config file");
				Files.copy(Objects.requireNonNull(plugin.getResource("config.yml")), configFile.toPath());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		//Load config from disk
		plugin.reloadConfig();

		//Load config values into variables
		markerSetName = configFile().getString("MarkerSetName");
		useBlueMapSource = configFile().getBoolean("UseBlueMapSource");

		try {
			//Check if the skinURL is a valid URL
			skinURL = new URL(configFile().getString("SkinURL"));
		} catch (MalformedURLException e) {
			logger.warning("Invalid skin URL: " + skinURL);
			e.printStackTrace();
		}

		verboseErrors = configFile().getBoolean("VerboseErrors");
	}
}
