package net.mctechnic.bluemapofflineplayermarkers;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.marker.MarkerAPI;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

public final class main extends JavaPlugin implements Listener {

	@Override
	public void onEnable() {
		// Plugin startup logic
		getLogger().info("BlueMap Offline Player Markers plugin enabled!");

		getServer().getPluginManager().registerEvents(this, this);

	}

	@Override
	public void onDisable() {
		// Plugin shutdown logic
		getLogger().info("BlueMap Offline Player Markers plugin disabled!");
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		Player p = e.getPlayer();
		getLogger().info(p.getName() + " has joined the server!");
		//TODO: BlueMap remove player marker

		// Directly using the API if it is enabled
		BlueMapAPI.getInstance().ifPresent(api -> {
			//code executed when the api is enabled (skipped if the api is not enabled)
			try {
				MarkerAPI markerApi = api.getMarkerAPI();
				getLogger().info("BlueMap marker API loaded successfully!");
				markerApi.save();
			} catch (IOException ioException) {
				getLogger().info("BlueMap marker API failed to load!");
				ioException.printStackTrace();
			}
		});
	}

	@EventHandler
	public void onLeave(PlayerQuitEvent e) {
		Player p = e.getPlayer();
		Location l = p.getLocation();
		getLogger().info(p.getName() + " has left the server at " + l);

		//TODO: BlueMap save player marker
	}
}
