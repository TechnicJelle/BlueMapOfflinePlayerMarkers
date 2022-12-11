package com.technicjelle.bluemapofflineplayermarkers;

import de.bluecolored.bluemap.api.BlueMapAPI;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.function.Consumer;
import java.util.logging.Logger;

public final class Main extends JavaPlugin implements Listener {
	public static Logger logger;
	public static Config config;

	public MarkerHandler markers;

	@Override
	public void onEnable() {
		new Metrics(this, 16425);

		if(getDataFolder().mkdirs()) getLogger().info("Created plugin config directory");
		File configFile = new File(getDataFolder(), "config.yml");
		if (!configFile.exists()) {
			try {
				Files.copy(getResource("config.yml"), configFile.toPath());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		//all actual startup and shutdown logic moved to BlueMapAPI enable/disable methods, so `/bluemap reload` also reloads this plugin
		BlueMapAPI.onEnable(onEnableListener);
		BlueMapAPI.onDisable(onDisableListener);
	}

	Consumer<BlueMapAPI> onEnableListener = api -> {
		logger = getLogger();
		logger.info("API Ready! BlueMap Offline Player Markers plugin enabled!");

		getServer().getPluginManager().registerEvents(this, this);

		config = new Config(this);

		markers = new MarkerHandler();

		Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
			markers.attachToBlueMap(api);
			markers.loadOfflineMarkers();
		});
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
		Bukkit.getScheduler().runTaskAsynchronously(this, () ->
				markers.remove(e.getPlayer())
		);
	}

	@EventHandler
	public void onLeave(PlayerQuitEvent e) {
		Bukkit.getScheduler().runTaskAsynchronously(this, () ->
				markers.add(e.getPlayer())
		);
	}
}
