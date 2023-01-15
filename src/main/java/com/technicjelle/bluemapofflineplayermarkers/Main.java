package com.technicjelle.bluemapofflineplayermarkers;

import de.bluecolored.bluemap.api.BlueMapAPI;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

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

		getServer().getPluginManager().registerEvents(this, this);

		//all actual startup and shutdown logic moved to BlueMapAPI enable/disable methods, so `/bluemap reload` also reloads this plugin
		BlueMapAPI.onEnable(onEnableListener);
		BlueMapAPI.onDisable(onDisableListener);
	}

	Consumer<BlueMapAPI> onEnableListener = api -> {
		logger.info("API Ready! BlueMap Offline Player Markers plugin enabled!");

		config = new Config(this);

		Path webroot = api.getWebApp().getWebRoot();

		// "registerStyle" has to be invoked inside the consumer (=> not in the async scheduled task below)
		copyResourceToBlueMapWebApp(webroot, "style.css", "bmopm.css");
		api.getWebApp().registerStyle("assets/bmopm.css");

		copyResourceToBlueMapWebApp(webroot, "script.js", "bmopm.js");
		api.getWebApp().registerScript("assets/bmopm.js");

		//create marker handler and add all offline players in a separate thread, so the server doesn't hang up while it's going
		Bukkit.getScheduler().runTaskAsynchronously(this, MarkerHandler::loadOfflineMarkers);
	};

	private void copyResourceToBlueMapWebApp(Path webroot, String fromResource, String toAsset) {
		Path toPath = webroot.resolve("assets").resolve(toAsset);
		try (
				InputStream in = getResource(fromResource);
				OutputStream out = Files.newOutputStream(toPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
		){
			in.transferTo(out);
		} catch (IOException ex) {
			logger.log(Level.SEVERE, "Failed to update " + toAsset + " in BlueMap's webroot!", ex);
		}
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
