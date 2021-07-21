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

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

import static org.bukkit.util.NumberConversions.round;

public final class main extends JavaPlugin implements Listener {

	private boolean useBlueMapSource = true;

	public final String markerSetId = "offplrs";
	public final String markerSetName = "Offline Players";

	public static void recolour(BufferedImage image) {
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				int rgb = image.getRGB(x, y);
				int red = (rgb >> 16) & 0x0ff;
				int green = (rgb >> 8) & 0x0ff;
				int blue = (rgb) & 0x0ff;

				//https://tannerhelland.com/2011/10/01/grayscale-image-algorithm-vb6.html
//				int grey = (red + green + blue) / 3; //average
				int grey = round(red * 0.3 + green * 0.59 + blue * 0.11); //luma
				Color c = new Color(grey, grey, grey);
				image.setRGB(x, y, c.getRGB());
			}
		}
	}

	public static BufferedImage resize(BufferedImage image, int newW, int newH) {
		Image scaled = image.getScaledInstance(newW, newH, Image.SCALE_FAST);
		BufferedImage result = new BufferedImage(newW, newH, BufferedImage.TYPE_BYTE_GRAY);

		Graphics2D g2d = result.createGraphics();
		g2d.drawImage(scaled, 0, 0, null);
		g2d.dispose();

		return result;
	}

	public void addMarker(Player player) {
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

	public void addMarker(BlueMapAPI blueMapAPI, MarkerAPI markerAPI, Player player) {
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

				BufferedImage image;
				if(useBlueMapSource) {
					image = getBImgFromFile(player);
				} else {
					image = getBImgFromURL(player);
				}
				if (image == null) return;

				recolour(image);
				image = resize(image, 32, 32);

				try {
					String imagePath = blueMapAPI.createImage(image, "offlineplayerheads/" + player.getUniqueId());
					marker.setIcon(imagePath, image.getWidth() / 2, image.getHeight() / 2);
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}
			}
		}

		getLogger().info("Marker for " + player.getName() + " added");
	}

	public void removeMarker(Player player) {
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

	public void removeMarker(MarkerAPI markerAPI, Player player) {
		markerAPI.getMarkerSet(markerSetId).ifPresent(markerSet ->
				markerSet.removeMarker(player.getUniqueId().toString()));

		getLogger().info("Marker for " + player.getName() + " removed");
	}

	BufferedImage getBImgFromFile(Player player) {
		BufferedImage result;
		File f = new File("bluemap/web/assets/playerheads/" + player.getUniqueId() + ".png"); //TODO: make this work for non-default webroots too
		try {
			result = ImageIO.read(f);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return result;
	}

	BufferedImage getBImgFromURL(Player player) {
		BufferedImage result;
		try {
			URL imageUrl = new URL("https://crafatar.com/avatars/" + player.getUniqueId() + ".png?size=8&overlay=true"); //TODO: get from config later on (see #8)
			try {
				InputStream in = imageUrl.openStream();
				result = ImageIO.read(in);
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		}
		return result;
	}

	@Override
	public void onEnable() {
		getLogger().info("BlueMap Offline Player Markers plugin enabled!");

		getServer().getPluginManager().registerEvents(this, this);

		BlueMapAPI.onEnable(blueMapAPI -> {
			getLogger().info("API ready!");
			useBlueMapSource = true; //TODO: Get from config later on (see #8)
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
