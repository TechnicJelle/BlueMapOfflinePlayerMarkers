package net.mctechnic.bluemapofflineplayermarkers;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.api.marker.MarkerAPI;
import de.bluecolored.bluemap.api.marker.MarkerSet;
import de.bluecolored.bluemap.api.marker.POIMarker;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.Optional;

public final class main extends JavaPlugin implements Listener {

	final String markerSetId = "offplrs";
	final String markerSetName = "Offline Players";

	void addMarker(Player player) {
		BlueMapAPI.getInstance().ifPresent(blueMapAPI -> {
			MarkerAPI markerAPI;

			try {
				markerAPI = blueMapAPI.getMarkerAPI();
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}

			addMarker(blueMapAPI, markerAPI, player);

			try {
				markerAPI.save();
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}

	void addMarker(BlueMapAPI blueMapAPI, MarkerAPI markerAPI, Player player) {
		MarkerSet markerSet;

		if (markerAPI.getMarkerSet(markerSetId).isEmpty()) {
			markerSet = markerAPI.createMarkerSet(markerSetId);
			markerSet.setLabel(markerSetName);
		} else {
			markerSet = markerAPI.getMarkerSet(markerSetId).get();
		}

		Optional<BlueMapWorld> blueMapWorld = blueMapAPI.getWorld(player.getLocation().getWorld().getUID()); //get the player's world

		if (blueMapWorld.isPresent()) //check if the world exists/is loaded
		{
			for (BlueMapMap map : blueMapWorld.get().getMaps()) { //then for every map of the world (worlds can have multiple maps)
				POIMarker marker = markerSet.createPOIMarker(player.getUniqueId().toString(), map,
						player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ()); //make the marker
				marker.setLabel(player.getName());
				marker.setIcon("assets/playerheads/" + player.getUniqueId() + ".png", 4, 4); //images
			}
		}

		getLogger().info("Marker for " + player.getName() + " added");
	}

	void removeMarker(Player player) {
		BlueMapAPI.getInstance().ifPresent(blueMapAPI -> {
			MarkerAPI markerAPI;

			try {
				markerAPI = blueMapAPI.getMarkerAPI();
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}

			removeMarker(markerAPI, player);

			try {
				markerAPI.save();
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}

	void removeMarker(MarkerAPI markerAPI, Player player) {
		markerAPI.getMarkerSet(markerSetId).ifPresent(markerSet ->
				markerSet.removeMarker(player.getUniqueId().toString()));

		getLogger().info("Marker for " + player.getName() + " removed");
	}

	@Override
	public void onEnable() {
		getLogger().info("BlueMap Offline Player Markers plugin enabled!");

		getServer().getPluginManager().registerEvents(this, this);

		BlueMapAPI.onEnable(blueMapAPI -> {
			getLogger().info("API ready!");
			Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
				MarkerAPI markerAPI;

				try {
					markerAPI = blueMapAPI.getMarkerAPI();
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}

				for (Player p : Bukkit.getOnlinePlayers()) {
					removeMarker(markerAPI, p);
				}

				try {
					markerAPI.save();
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		});

		BlueMapAPI.onDisable(blueMapAPI -> {
			getLogger().info("API shutting down!");
			//No async on shutdown, otherwise BlueMap might have already shut down
			MarkerAPI markerAPI;

			try {
				markerAPI = blueMapAPI.getMarkerAPI();
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}

			for (Player p : Bukkit.getOnlinePlayers()) {
				addMarker(blueMapAPI, markerAPI, p);
			}

			try {
				markerAPI.save();
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
			Player p = e.getPlayer();
			removeMarker(p);
		});
	}

	@EventHandler
	public void onLeave(PlayerQuitEvent e) {
		Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
			Player p = e.getPlayer();
			addMarker(p);
		});
	}

	@Override
	public void onDisable() {
		getLogger().info("BlueMap Offline Player Markers plugin disabled!");
	}
}
