package com.technicjelle.bluemapofflineplayermarkers;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.DoubleTag;
import com.flowpowered.nbt.stream.NBTInputStream;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;


public class MarkerHandler {

	private final BufferedImage fallbackIcon;

	/**
	 * Creates a new MarkerHandler.
	 */
	public MarkerHandler(BufferedImage fallbackIcon) {
		this.fallbackIcon = fallbackIcon;
	}

	/**
	 * Adds a player marker to the map.
	 *
	 * @param player The player to add the marker for.
	 */
	public void add(Player player) {
		add(player, player.getLocation());
	}

	/**
	 * Adds a player marker to the map.
	 *
	 * @param player   The player to add the marker for.
	 * @param location The location to put the marker at.
	 */
	public void add(OfflinePlayer player, Location location) {
		Optional<BlueMapAPI> optionalApi = BlueMapAPI.getInstance();
		if (optionalApi.isEmpty()) {
			Main.logger.warning("Tried to add a marker, but BlueMap wasn't loaded!");
			return;
		}
		BlueMapAPI api = optionalApi.get();

		// Get BlueMapWorld for the location
		BlueMapWorld blueMapWorld = api.getWorld(location.getWorld()).orElse(null);
		if (blueMapWorld == null) return;

		// Create marker-template
		// (add 1.8 to y to place the marker at the head-position of the player, like bluemap does with it's player-markers)
		POIMarker.Builder markerBuilder = POIMarker.builder()
				.label(player.getName())
				.detail(player.getName() + " <i>(offline)</i>")
				.styleClasses("bmopm-offline-player")
				.position(location.getX(), location.getY() + 1.8, location.getZ());

		// Create an icon and marker for each map of this world
		// We need to create a separate marker per map, because the map-storage that the icon is saved in
		// is different for each map
		for (BlueMapMap map : blueMapWorld.getMaps()) {
			BufferedImage image = Main.config.useBlueMapSource ?
					ImageUtils.GetBImgFromAPI(player, map.getAssetStorage()) :
					ImageUtils.GetBImgFromURL(player);

			// if no image is found, use default
			if (image == null) {
				Main.logger.warning("Playerhead image for " + player.getName() + " couldn't be loaded!" + (Main.config.verboseErrors ? "" : " (config: verboseErrors)"));
				if (Main.config.verboseErrors) {
					Main.logger.warning(" Couldn't find the playerhead image file in BlueMap's resources");
					Main.logger.warning(" This is likely due to the fact that BlueMap was installed after they last logged off");
					Main.logger.warning(" Falling back to a Steve skin...");
				}

				image = this.fallbackIcon;
			}

			// if image is still null, skip
			if (image == null) {
				if (Main.config.verboseErrors) {
					Main.logger.warning(" Couldn't load the default steve image!");
					Main.logger.warning(" No marker will be created.");
				}
				continue;
			}

			// process image
			//ImageUtils.Recolour(image);
			//image = ImageUtils.Resize(image, 32, 32); // moved to css

			// write image to asset storage
			String assetName = "offlineplayerheads/" + player.getUniqueId() + ".png";
			try (OutputStream out = map.getAssetStorage().writeAsset(assetName)) {
				ImageIO.write(image, "png", out);
			} catch (IOException ex) {
				Main.logger.log(Level.SEVERE, "Failed to write playerhead image to BlueMaps AssetStorage!", ex);
				continue;
			}

			// set marker-icon
			String imagePath = map.getAssetStorage().getAssetUrl(assetName);
			markerBuilder.icon(imagePath, 0, 0);

			// get marker-set (or create new marker set if none found)
			MarkerSet markerSet = map.getMarkerSets().computeIfAbsent(Config.MARKER_SET_ID, id -> MarkerSet.builder()
					.label(Main.config.markerSetName)
					.defaultHidden(false)
					.toggleable(true)
					.build());

			// add marker
			markerSet.put(player.getUniqueId().toString(), markerBuilder.build());
		}

		Main.logger.info("Marker for " + player.getName() + " added");
	}

	/**
	 * Removes a player marker from the map.
	 *
	 * @param player The player to remove the marker for.
	 */
	public void remove(Player player) {
		BlueMapAPI.getInstance().ifPresent(api -> {
			// remove all markers with the players uuid
			for (BlueMapMap map : api.getMaps()) {
				MarkerSet set = map.getMarkerSets().get(Config.MARKER_SET_ID);
				if (set != null) set.remove(player.getUniqueId().toString());
			}

			Main.logger.info("Marker for " + player.getName() + " removed");
		});
	}

	/**
	 * Load in markers of all offline players by going through the playerdata NBT
	 */
	public void loadOfflineMarkers() {
		//I really don't like "getWorlds().get(0)" as a way to get the main world, but as far as I can tell there is no other way
		File playerDataFolder = new File(Bukkit.getWorlds().get(0).getWorldFolder(), "playerdata");
		//Return if playerdata is missing for some reason.
		if (!playerDataFolder.exists() || !playerDataFolder.isDirectory()) return;

		for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
			//If player is online, ignore (I don't know why the method is called "getOfflinePlayers" when it also contains all online players...)
			if (op.isOnline()) continue;

			File dataFile = new File(playerDataFolder, op.getUniqueId() + ".dat");

			//Failsafe if playerdata doesn't exist (should be impossible but whatever)
			if (!dataFile.exists()) continue;

			CompoundMap nbtData;
			try (FileInputStream fis = new FileInputStream(dataFile);
				 NBTInputStream nbtInputStream = new NBTInputStream(fis)) {
				nbtData = ((CompoundTag) nbtInputStream.readTag()).getValue();
			} catch (IOException e) {
				e.printStackTrace();
				continue;
			}

			//Collect data
			long worldUUIDLeast = (long) nbtData.get("WorldUUIDLeast").getValue();
			long worldUUIDMost = (long) nbtData.get("WorldUUIDMost").getValue();
			@SuppressWarnings("unchecked") //Apparently this is just how it should be https://discord.com/channels/665868367416131594/771451216499965953/917450319259115550
			List<Double> position = ((List<DoubleTag>) nbtData.get("Pos").getValue()).stream().map(DoubleTag::getValue).collect(Collectors.toList());

			//Convert to location
			UUID worldUUID = new UUID(worldUUIDMost, worldUUIDLeast);
			World w = Bukkit.getWorld(worldUUID);
			//World doesn't exist or position is broken
			if (w == null || position.size() != 3) continue;
			Location loc = new Location(w, position.get(0), position.get(1), position.get(2));

			//Add marker
			add(op, loc);
		}
	}
}
