package com.technicjelle.bluemapofflineplayermarkers.impl.paper;

import com.technicjelle.BMUtils;
import com.technicjelle.UpdateChecker;
import com.technicjelle.bluemapofflineplayermarkers.core.BMApiStatus;
import com.technicjelle.bluemapofflineplayermarkers.core.Player;
import com.technicjelle.bluemapofflineplayermarkers.core.Singletons;
import com.technicjelle.bluemapofflineplayermarkers.core.fileloader.FileMarkerLoader;
import com.technicjelle.bluemapofflineplayermarkers.core.markerhandler.BlueMapMarkerHandler;
import de.bluecolored.bluemap.api.BlueMapAPI;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class BlueMapOfflinePlayerMarkers extends JavaPlugin implements Listener {
	private PaperConfig config;
	private UpdateChecker updateChecker;

	@Override
	public void onLoad() {
		getLogger().info("BlueMap Offline Player Markers plugin (on)loading...");
		BlueMapAPI.onEnable(api -> {
			getLogger().info("BlueMap is enabled! Copying resources to BlueMap webapp and registering them...");
			try {
				BMUtils.copyJarResourceToBlueMap(api, getClassLoader(), "style.css", "bmopm.css", false);
				BMUtils.copyJarResourceToBlueMap(api, getClassLoader(), "script.js", "bmopm.js", false);
			} catch (IOException e) {
				Singletons.getLogger().log(Level.SEVERE, "Failed to copy resources to BlueMap webapp!", e);
			}

		});
	}

	@Override
	public void onEnable() {
		new Metrics(this, 16425);

		updateChecker = new UpdateChecker("TechnicJelle", "BlueMapOfflinePlayerMarkers", getDescription().getVersion());
		updateChecker.checkAsync();

		getServer().getPluginManager().registerEvents(this, this);

		config = new PaperConfig(this);

		Singletons.init(new PaperServer(this), getLogger(), config, new BlueMapMarkerHandler(), new BMApiStatus());
		Singletons.getServer().startUp();

		//all actual startup and shutdown logic moved to BlueMapAPI enable/disable methods, so `/bluemap reload` also reloads this plugin
		BlueMapAPI.onEnable(onEnableListener);
		BlueMapAPI.onDisable(onDisableListener);
	}

	final Consumer<BlueMapAPI> onEnableListener = api -> {
		getLogger().info("API Ready! BlueMap Offline Player Markers plugin enabled!");
		updateChecker.logUpdateMessage(Singletons.getLogger());

		config.loadFromPlugin(this);

		//create marker handler and add all offline players in a separate thread, so the server doesn't hang up while it's going
		//with a delay, so any potential BlueMap SkinProviders have time to load
		Bukkit.getScheduler().runTaskLaterAsynchronously(this, FileMarkerLoader::loadOfflineMarkers, 20 * 5);
	};

	final Consumer<BlueMapAPI> onDisableListener = api -> {
		Singletons.getLogger().info("API disabled! BlueMap Offline Player Markers shutting down...");
		//not much to do here, actually...
	};

	@Override
	public void onDisable() {
		BlueMapAPI.unregisterListener(onEnableListener);
		BlueMapAPI.unregisterListener(onDisableListener);
		Singletons.getServer().shutDown();
		Singletons.getLogger().info("BlueMap Offline Player Markers plugin disabled!");
		Singletons.cleanup();
	}


	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
			org.bukkit.entity.Player player = e.getPlayer();
			UUID playerUUID = player.getUniqueId();

			Optional<BlueMapAPI> api = BlueMapAPI.getInstance();
			if (api.isEmpty()) {
				Singletons.getLogger().warning("BlueMap is not loaded, not removing marker for " + player.getName());
				return;
			}

			Singletons.getMarkerHandler().remove(playerUUID, api.get());
		});
	}

	@EventHandler
	public void onLeave(PlayerQuitEvent e) {
		Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
			org.bukkit.entity.Player player = e.getPlayer();
			UUID playerUUID = player.getUniqueId();

			PlayerBukkitData playerBukkitData = new PlayerBukkitData(player);
			Player playerToAdd = new Player(playerUUID, playerBukkitData);

			Optional<BlueMapAPI> api = BlueMapAPI.getInstance();
			if (api.isEmpty()) {
				Singletons.getLogger().warning("BlueMap is not loaded, not adding marker for " + player.getName());
				return;
			}

			Singletons.getMarkerHandler().add(playerToAdd, api.get());
		});
	}
}
