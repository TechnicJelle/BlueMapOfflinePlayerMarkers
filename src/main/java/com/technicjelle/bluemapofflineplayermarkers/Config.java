package com.technicjelle.bluemapofflineplayermarkers;

import java.net.MalformedURLException;
import java.net.URL;

public class Config {

	public static final String MARKER_SET_ID = "offplrs";

	private final Main plugin;

	public String markerSetName;
	public boolean useBlueMapSource;
	public URL skinURL;
	public boolean verboseErrors;

	public Config(Main plugin) {
		this.plugin = plugin;
		plugin.reloadConfig();
		loadConfig();
	}

	private void loadConfig() {
		markerSetName = plugin.getConfig().getString("MarkerSetName");
		useBlueMapSource = plugin.getConfig().getBoolean("UseBlueMapSource");

		try {
			//Check if the skinURL is a valid URL
			skinURL = new URL(plugin.getConfig().getString("SkinURL"));
		} catch (MalformedURLException e) {
			Main.logger.warning("Invalid skin URL: " + skinURL);
			e.printStackTrace();
		}

		verboseErrors = plugin.getConfig().getBoolean("VerboseErrors");
	}
}
