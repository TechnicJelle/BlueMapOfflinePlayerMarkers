package com.technicjelle.bluemapofflineplayermarkers;

import de.bluecolored.bluemap.api.BlueMapAPI;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import com.technicjelle.UpdateChecker;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Main extends JavaPlugin implements Listener {
	public static Logger logger;
	public static Config config;

	@Override
	public void onEnable() {
		new Metrics(this, 16425);

		logger = getLogger();

		UpdateChecker.checkAsync("TechnicJelle", "BlueMapOfflinePlayerMarkers", getDescription().getVersion());

		getServer().getPluginManager().registerEvents(this, this);

		//all actual startup and shutdown logic moved to BlueMapAPI enable/disable methods, so `/bluemap reload` also reloads this plugin
		BlueMapAPI.onEnable(onEnableListener);
		BlueMapAPI.onDisable(onDisableListener);
	}

	Consumer<BlueMapAPI> onEnableListener = api -> {
		logger.info("API Ready! BlueMap Offline Player Markers plugin enabled!");
		UpdateChecker.logUpdateMessage(logger);

		config = new Config(this);

		// "registerStyle" has to be invoked inside the consumer (=> not in the async scheduled task below)
		try {
			copyResourceToBlueMapWebApp(api, "style.css", "bmopm.css");
			copyResourceToBlueMapWebApp(api, "script.js", "bmopm.js");
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Failed to copy resources to BlueMap webapp!", e);
		}

		//create marker handler and add all offline players in a separate thread, so the server doesn't hang up while it's going
		Bukkit.getScheduler().runTaskAsynchronously(this, MarkerHandler::loadOfflineMarkers);
	};

	private void copyResourceToBlueMapWebApp(BlueMapAPI api, String fromResource, String toAsset) throws IOException {
		Path toPath = api.getWebApp().getWebRoot().resolve("assets").resolve(toAsset);
		Files.createDirectories(toPath.getParent());
		try (
				InputStream in = getResource(fromResource);
				OutputStream out = Files.newOutputStream(toPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
		){
			if (in == null) throw new IOException("Resource not found: " + fromResource);
			in.transferTo(out);
		}
		String assetPath = "assets/" + toAsset;
		if (toAsset.endsWith(".js")) api.getWebApp().registerScript(assetPath);
		if (toAsset.endsWith(".css")) api.getWebApp().registerStyle(assetPath);

	}

	Consumer<BlueMapAPI> onDisableListener = api -> {
		logger.info("API disabled! BlueMap Offline Player Markers shutting down...");
		//not much to do here, actually...
	};

	@Override
	public void onDisable() {
		BlueMapAPI.unregisterListener(onEnableListener);
		BlueMapAPI.unregisterListener(onDisableListener);
		logger.info("BlueMap Offline Player Markers plugin disabled!");
	}


	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		Bukkit.getScheduler().runTaskAsynchronously(this, () -> MarkerHandler.remove(e.getPlayer()));
	}

	@EventHandler
	public void onLeave(PlayerQuitEvent e) {
		Bukkit.getScheduler().runTaskAsynchronously(this, () -> MarkerHandler.add(e.getPlayer()));
	}
}
