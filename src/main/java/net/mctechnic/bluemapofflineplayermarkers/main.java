package net.mctechnic.bluemapofflineplayermarkers;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.DoubleTag;
import com.flowpowered.nbt.stream.NBTInputStream;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.api.marker.MarkerAPI;
import de.bluecolored.bluemap.api.marker.MarkerSet;
import de.bluecolored.bluemap.api.marker.POIMarker;
import net.mctechnic.bluemapofflineplayermarkers.commands.OfflineMarkers;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.awt.Image;
import java.awt.Graphics2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.function.Consumer;

import static org.bukkit.util.NumberConversions.round;

public final class main extends JavaPlugin implements Listener {

	private boolean useBlueMapSource = true;
	private boolean verboseErrors = true;

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

			addMarker(blueMapAPI, markerAPI, player, player.getLocation());

			try {
				markerAPI.save();
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}

	public void addMarker(BlueMapAPI blueMapAPI, MarkerAPI markerAPI, OfflinePlayer player, Location location) {
		MarkerSet markerSet;

		if (markerAPI.getMarkerSet(markerSetId).isEmpty()) {
			markerSet = markerAPI.createMarkerSet(markerSetId);
			markerSet.setLabel(markerSetName);
		} else {
			markerSet = markerAPI.getMarkerSet(markerSetId).get();
		}

		Optional<BlueMapWorld> blueMapWorld = blueMapAPI.getWorld(location.getWorld().getUID()); //get the player's world

		if (blueMapWorld.isPresent()) //check if the world exists/is loaded
		{
			for (BlueMapMap map : blueMapWorld.get().getMaps()) { //then for every map of the world (worlds can have multiple maps)
				POIMarker marker = markerSet.createPOIMarker(player.getUniqueId().toString(), map,
						location.getX(), location.getY(), location.getZ()); //make the marker
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

	BufferedImage getBImgFromFile(OfflinePlayer player) {
		BufferedImage result;
		File f = new File("bluemap/web/assets/playerheads/" + player.getUniqueId() + ".png"); //TODO: make this work for non-default webroots too
		if(f.exists()) {
			try {
				result = ImageIO.read(f);
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		} else {
			getLogger().warning("Marker for " + player.getName() + " couldn't be added!" + (verboseErrors ? "" : " (config: verboseErrors)"));
			if(verboseErrors) {
				getLogger().warning(" Couldn't find the playerhead image file in BlueMap's resources");
				getLogger().warning(" This is likely due to the fact that BlueMap was installed after they last logged off");
				getLogger().warning(" Falling back to a Steve skin");
			}
			try {
				result = ImageIO.read(new File("bluemap/web/assets/steve.png"));
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}
		return result;
	}

	BufferedImage getBImgFromURL(OfflinePlayer player) {
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

	Consumer<BlueMapAPI> onDisableListener = blueMapAPI -> {
		getLogger().info("API shutting down!");
		//No async on shutdown, otherwise BlueMap might have already shut down
		markAllOnlinePlayers(blueMapAPI);
	};

	@Override
	public void onEnable() {
		getLogger().info("BlueMap Offline Player Markers plugin enabled!");

		getServer().getPluginManager().registerEvents(this, this);
		loadConfig();

		PluginCommand offlineMarkers = Bukkit.getPluginCommand("offlinemarkers");
		OfflineMarkers executor = new OfflineMarkers(this);
		if (offlineMarkers != null) {
			offlineMarkers.setExecutor(executor);
			offlineMarkers.setTabCompleter(executor);
		} else {
			getLogger().warning("offlineMarkers is null. This is not good");
		}

		BlueMapAPI.onEnable(blueMapAPI -> {
			getLogger().info("API ready!");
			loadConfig();

			//Async on startup
			Bukkit.getScheduler().runTaskAsynchronously(this, () ->
					removeAllOnlinePlayers(blueMapAPI));
		});

		BlueMapAPI.onDisable(onDisableListener);
	}

	private void loadConfig() {
		//TODO: Get these from the config later on (see #8)
		useBlueMapSource = true;
		verboseErrors = true;
	}

	private void removeAllOnlinePlayers(BlueMapAPI blueMapAPI) {
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

    for (Player p : Bukkit.getOnlinePlayers()) {
      addMarker(blueMapAPI, markerAPI, p, p.getLocation());
    }
  }

	private void markAllOnlinePlayers(BlueMapAPI blueMapAPI) {
		MarkerAPI markerAPI;

		try {
			markerAPI = blueMapAPI.getMarkerAPI();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		for (Player p : Bukkit.getOnlinePlayers()) {
			addMarker(blueMapAPI, markerAPI, p, p.getLocation());
		}

		try {
			markerAPI.save();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void resetMarkers(){
		//Clear the marker set (called by command or in preparation for loading markers from playerdata)
		Optional<BlueMapAPI> oApi = BlueMapAPI.getInstance();
		if(oApi.isEmpty()) return;
		BlueMapAPI api = oApi.get();
		try {
			MarkerAPI markerAPI = api.getMarkerAPI();
			markerAPI.removeMarkerSet(markerSetId);
			MarkerSet emptySet = markerAPI.createMarkerSet(markerSetId);
			emptySet.setLabel(markerSetName);
			markerAPI.save();
		}catch(IOException e){
			e.printStackTrace();
		}
	}

	public void loadMarkers(){

		Optional<BlueMapAPI> oApi = BlueMapAPI.getInstance();
		if(oApi.isEmpty()) return;
		BlueMapAPI api = oApi.get();
		MarkerAPI markerAPI;
		try {
			markerAPI = api.getMarkerAPI();
		}catch (IOException e){
			e.printStackTrace();
			return;
		}

		//I really don't like "getWorlds().get(0)" as a way to get the main world, but as far as I can tell there is no other way
		File playerDataFolder = new File(Bukkit.getWorlds().get(0).getWorldFolder(), "playerdata");
		//Return if playerdata is missing for some reason.
		if(!playerDataFolder.exists() || !playerDataFolder.isDirectory()) return;

		for(OfflinePlayer op : Bukkit.getOfflinePlayers()){
			//If player is online, ignore (I don't know why the method is called "getOfflinePlayers" when it also contains all online players...)
			if(op.isOnline()) continue;

			File dataFile = new File(playerDataFolder, op.getUniqueId() + ".dat");

			//Failsafe if playerdata doesn't exist (should be impossible but whatever)
			if(!dataFile.exists()) continue;

			CompoundMap nbtData;
			try(FileInputStream fis = new FileInputStream(dataFile);
				NBTInputStream nbtInputStream = new NBTInputStream(fis)) {
				nbtData = ((CompoundTag) nbtInputStream.readTag()).getValue();
			} catch (IOException e) {
				e.printStackTrace();
				continue;
			}

			//Collect data
			long worldUUIDLeast = (long) nbtData.get("WorldUUIDLeast").getValue();
			long worldUUIDMost = (long) nbtData.get("WorldUUIDMost").getValue();
			List<Double> position = ((List<DoubleTag>) nbtData.get("Pos").getValue()).stream().map(DoubleTag::getValue).toList();

			//Convert to location
			UUID worldUUID = new UUID(worldUUIDMost, worldUUIDLeast);
			World w = Bukkit.getWorld(worldUUID);
			//World doesn't exist or position is broken
			if(w == null || position.size() != 3) continue;
			Location loc = new Location(w, position.get(0), position.get(1), position.get(2));

			//Add marker
			addMarker(api, markerAPI, op, loc);
		}
		try {
			markerAPI.save();
		} catch (IOException e) {
			e.printStackTrace();
		}
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
		BlueMapAPI.getInstance().ifPresent(this::markAllOnlinePlayers);
		BlueMapAPI.unregisterListener(onDisableListener);
		getLogger().info("BlueMap Offline Player Markers plugin disabled!");
	}
}
