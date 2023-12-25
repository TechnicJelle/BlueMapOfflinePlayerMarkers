package com.technicjelle.bluemapofflineplayermarkers;

import com.technicjelle.BMUtils;
import com.technicjelle.UpdateChecker;
import de.bluecolored.bluemap.api.BlueMapAPI;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class BlueMapOfflinePlayerMarkers extends JavaPlugin implements Listener {
	private Config config;
	private UpdateChecker updateChecker;
	private MarkerHandler markerHandler;

	@Override
	public void onEnable() {
		new Metrics(this, 16425);

		updateChecker = new UpdateChecker("TechnicJelle", "BlueMapOfflinePlayerMarkers", getDescription().getVersion());
		updateChecker.checkAsync();

		getServer().getPluginManager().registerEvents(this, this);

		markerHandler = new MarkerHandler(this);

		//all actual startup and shutdown logic moved to BlueMapAPI enable/disable methods, so `/bluemap reload` also reloads this plugin
		BlueMapAPI.onEnable(onEnableListener);
		BlueMapAPI.onDisable(onDisableListener);
	}

	Consumer<BlueMapAPI> onEnableListener = api -> {
		getLogger().info("API Ready! BlueMap Offline Player Markers plugin enabled!");
		updateChecker.logUpdateMessage(getLogger());

		config = new Config(this);

		try {
			BMUtils.copyJarResourceToBlueMap(api, getClassLoader(), "style.css", "bmopm.css", false);
			BMUtils.copyJarResourceToBlueMap(api, getClassLoader(), "script.js", "bmopm.js", false);
		} catch (IOException e) {
			getLogger().log(Level.SEVERE, "Failed to copy resources to BlueMap webapp!", e);
		}

		//create marker handler and add all offline players in a separate thread, so the server doesn't hang up while it's going
		//with a delay, so any potential BlueMap SkinProviders have time to load
		Bukkit.getScheduler().runTaskLaterAsynchronously(this, markerHandler::loadOfflineMarkers, 20 * 5);
	};

	Consumer<BlueMapAPI> onDisableListener = api -> {
		getLogger().info("API disabled! BlueMap Offline Player Markers shutting down...");
		//not much to do here, actually...
	};

	@Override
	public void onDisable() {
		BlueMapAPI.unregisterListener(onEnableListener);
		BlueMapAPI.unregisterListener(onDisableListener);
		getLogger().info("BlueMap Offline Player Markers plugin disabled!");
	}


	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		Bukkit.getScheduler().runTaskAsynchronously(this, () -> markerHandler.remove(e.getPlayer()));
	}

	@EventHandler
	public void onLeave(PlayerQuitEvent e) {
		Bukkit.getScheduler().runTaskAsynchronously(this, () -> markerHandler.add(e.getPlayer()));
	}

	/**
	 * The config instance may change when the plugin is reloaded, so this method should be used to get the current config
	 *
	 * @return the current config
	 */
	public Config getCurrentConfig() {
		return config;
	}
}
