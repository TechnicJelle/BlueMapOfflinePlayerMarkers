package com.technicjelle.bluemapofflineplayermarkers;

import com.technicjelle.bluemapofflineplayermarkers.commands.OfflineMarkers;
import de.bluecolored.bluemap.api.BlueMapAPI;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.Consumer;
import java.util.logging.Logger;

public final class Main extends JavaPlugin implements Listener {
	public static Logger logger;
	public static Config config;

	public MarkerHandler markers;

	@Override
	public void onEnable() {
		logger = getLogger();
		logger.info("BlueMap Offline Player Markers plugin enabled!");

		getServer().getPluginManager().registerEvents(this, this);

		config = new Config();

		PluginCommand offlineMarkers = Bukkit.getPluginCommand("offlinemarkers");
		OfflineMarkers executor = new OfflineMarkers(this);
		if (offlineMarkers != null) {
			offlineMarkers.setExecutor(executor);
			offlineMarkers.setTabCompleter(executor);
		} else {
			getLogger().warning("offlineMarkers is null. This is not good");
		}

		markers = new MarkerHandler();

		BlueMapAPI.onEnable(onEnableListener);

		//TODO: Load persistent markers from JSON
	}

	Consumer<BlueMapAPI> onEnableListener = api -> {
		getLogger().info("API ready!");
		markers.attachToBlueMap(api);
	};

	@Override
	public void onDisable() {
		//TODO: Save persistent markers to JSON

		BlueMapAPI.unregisterListener(onEnableListener);
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
