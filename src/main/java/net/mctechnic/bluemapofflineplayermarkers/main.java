package net.mctechnic.bluemapofflineplayermarkers;

import com.google.common.net.UrlEscapers;
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

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

public final class main extends JavaPlugin implements Listener {

	boolean isDebugBuild = true; // Enables some extra debugging code when true. Disable in production builds

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
				marker.setIcon(createMarkerImage(blueMapAPI, player), 16, 16);
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

	String createMarkerImage (BlueMapAPI blueMapAPI, Player player) {
		String pathToModifiedPlayerhead = "offlineplayerheads/" + player.getUniqueId().toString();
		BufferedImage image = null;

		// Some debugging
		if (isDebugBuild) {
			getLogger().info("UUID of player " + player.getName() + " is " + player.getUniqueId().toString());
			getLogger().info("Wanted path to marker image: " + pathToModifiedPlayerhead);
		}

		// Set the url as the crafatar endpoint
		URL imageUrl = null;
		try {
			imageUrl = new URL("https://crafatar.com/avatars/" + player.getUniqueId().toString() +".png?size=32&overlay=true");
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		// Read the image from the url to a BufferedImage
		try {
			InputStream in = imageUrl.openStream();
			image = ImageIO.read(in);
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		image = convertToGrayScale(image);

		// Make the image file from the BufferedImage
		try {
			pathToModifiedPlayerhead = blueMapAPI.createImage(image, pathToModifiedPlayerhead);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Debugging
		if (isDebugBuild) {
			getLogger().info("Actual path to marker image: " + pathToModifiedPlayerhead);
		}

		// Return the path to the image file
		return pathToModifiedPlayerhead;

	}

	// Adapted from https://stackoverflow.com/questions/3106269/how-to-use-type-byte-gray-to-efficiently-create-a-grayscale-bufferedimage-using/12860219#12860219
	BufferedImage convertToGrayScale(BufferedImage image) {
		BufferedImage result = new BufferedImage(
				image.getWidth(),
				image.getHeight(),
				BufferedImage.TYPE_BYTE_GRAY);
		Graphics g = result.getGraphics();
		g.drawImage(image, 0, 0, null);
		g.dispose();
		return result;
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
