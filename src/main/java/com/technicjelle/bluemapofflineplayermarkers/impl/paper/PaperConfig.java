package com.technicjelle.bluemapofflineplayermarkers.impl.paper;

import com.technicjelle.MCUtils;
import com.technicjelle.bluemapofflineplayermarkers.common.Config;
import com.technicjelle.bluemapofflineplayermarkers.core.GameMode;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.List;

public class PaperConfig implements Config {
	private String markerSetName;
	private boolean toggleable;
	private boolean defaultHidden;
	private long expireTimeInHours;
	private List<GameMode> hiddenGameModes;

	public PaperConfig(JavaPlugin plugin) {
		loadFromPlugin(plugin);
	}

	public void loadFromPlugin(JavaPlugin plugin) {
		try {
			MCUtils.copyPluginResourceToConfigDir(plugin, "config.yml", "config.yml", false);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		//Load config from disk
		plugin.reloadConfig();

		//Load config values into variables
		markerSetName = plugin.getConfig().getString("MarkerSetName");
		toggleable = plugin.getConfig().getBoolean("Toggleable");
		defaultHidden = plugin.getConfig().getBoolean("DefaultHidden");
		expireTimeInHours = plugin.getConfig().getLong("ExpireTimeInHours");
		hiddenGameModes = Config.parseGameModes(plugin.getConfig().getStringList("HiddenGameModes"));
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
}
