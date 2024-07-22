package com.technicjelle.bluemapofflineplayermarkers.impl.paper;

import com.technicjelle.MCUtils.ConfigUtils;
import com.technicjelle.bluemapofflineplayermarkers.common.Config;
import com.technicjelle.bluemapofflineplayermarkers.core.GameMode;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PaperConfig implements Config {
	private String markerSetName;
	private boolean toggleable;
	private boolean defaultHidden;
	private long expireTimeInHours;
	private List<GameMode> hiddenGameModes;
	private boolean hideBannedPlayers;

	public PaperConfig(JavaPlugin plugin) {
		loadFromPlugin(plugin);
	}

	public void loadFromPlugin(JavaPlugin plugin) {
		try {
			ConfigUtils.copyPluginResourceToConfigDir(plugin, "config.yml", "config.yml", false);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		//Load config from disk
		plugin.reloadConfig();

		//Load config values into variables
		markerSetName = plugin.getConfig().getString("MarkerSetName", "Offline Players");
		toggleable = plugin.getConfig().getBoolean("Toggleable", true);
		defaultHidden = plugin.getConfig().getBoolean("DefaultHidden", false);
		expireTimeInHours = plugin.getConfig().getLong("ExpireTimeInHours", 0);
		hiddenGameModes = Config.parseGameModes(getStringList(plugin, "HiddenGameModes", List.of("spectator")));
		hideBannedPlayers = plugin.getConfig().getBoolean("HideBannedPlayers", true);
	}

	//Copied/Adapted from org.bukkit.configuration.MemorySection.java
	@SuppressWarnings("SameParameterValue")
	private List<String> getStringList(JavaPlugin plugin, String path, List<String> def) {
		List<?> list = plugin.getConfig().getList(path, def);

		if (list == null) {
			return new ArrayList<>(0);
		}

		List<String> result = new ArrayList<>();

		for (Object object : list) {
			if ((object instanceof String) || (isPrimitiveWrapper(object))) {
				result.add(String.valueOf(object));
			}
		}

		return result;
	}

	//Copied/Adapted from org.bukkit.configuration.MemorySection.java
	private boolean isPrimitiveWrapper(Object input) {
		return input instanceof Integer || input instanceof Boolean ||
				input instanceof Character || input instanceof Byte ||
				input instanceof Short || input instanceof Double ||
				input instanceof Long || input instanceof Float;
	}

	@Override
	public String getMarkerSetName() {
		return markerSetName;
	}

	@Override
	public boolean isToggleable() {
		return toggleable;
	}

	@Override
	public boolean isDefaultHidden() {
		return defaultHidden;
	}

	@Override
	public long getExpireTimeInHours() {
		return expireTimeInHours;
	}

	@Override
	public List<GameMode> getHiddenGameModes() {
		return hiddenGameModes;
	}

	@Override
	public boolean hideBannedPlayers() {
		return hideBannedPlayers;
	}
}
