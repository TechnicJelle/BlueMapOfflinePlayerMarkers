package com.technicjelle.bluemapofflineplayermarkers;

import de.bluecolored.bluemap.api.BlueMapAPI;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
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

	@Nullable
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

		// "registerStyle" has to be invoked inside the consumer (=> not in the async scheduled task below)
		api.getWebApp().registerStyle("assets/bmopm.css");
		api.getWebApp().registerScript("assets/bmopm.js");

		Path webroot = api.getWebApp().getWebRoot();
		Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
			try {
				// load steve
				BufferedImage steve = ImageIO.read(new File(webroot + "/assets/steve.png"));

				markers = new MarkerHandler(steve);
				markers.loadOfflineMarkers();
			} catch (IOException ex) {
				Main.logger.log(Level.SEVERE, "Failed to load steve from BlueMap's webroot!", ex);
			}

			// update custom style
			Path stylePath = webroot.resolve("assets").resolve("bmopm.css");
			try (
					InputStream in = getResource("style.css");
					OutputStream out = Files.newOutputStream(stylePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
			){
				in.transferTo(out);
			} catch (IOException ex) {
				Main.logger.log(Level.SEVERE, "Failed to update bmopm.css in BlueMap's webroot!", ex);
			}

			// update custom script
			Path scriptPath = webroot.resolve("assets").resolve("bmopm.js");
			try (
					InputStream in = getResource("script.js");
					OutputStream out = Files.newOutputStream(scriptPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
			){
				in.transferTo(out);
			} catch (IOException ex) {
				Main.logger.log(Level.SEVERE, "Failed to update bmopm.js in BlueMap's webroot!", ex);
			}
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
		Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
			if (markers != null) markers.remove(e.getPlayer());
		});
	}

	@EventHandler
	public void onLeave(PlayerQuitEvent e) {
		Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
			if (markers != null) markers.add(e.getPlayer());
		});
	}
}
