package com.technicjelle.bluemapofflineplayermarkers;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.DoubleTag;
import com.flowpowered.nbt.stream.NBTInputStream;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


public class MarkerHandler {

	private final Map<UUID, MarkerSet> markerSets;

	/**
	 * Creates a new MarkerHandler.
	 */
	public MarkerHandler() {
		markerSets = new HashMap<>();
		init();
	}

	/**
	 * Initializes the internal data for the MarkerHandler.
	 * <p>
	 * (Makes a MarkerSet for each world.)
	 */
	private void init() {
		//loop through all worlds
		List<World> worlds = Bukkit.getWorlds();
		for (World world : worlds) {
			//check that this world doesn't already have a markerset
			if (!markerSets.containsKey(world.getUID())) {
				//make a new markerset
				MarkerSet markerSet = new MarkerSet(Main.config.markerSetName);
				markerSet.setDefaultHidden(false);
				markerSet.setToggleable(true);

				//add the markerset to the global world,markerset collection, so it can be accessed later
				markerSets.put(world.getUID(), markerSet);
			}
		}
	}

	/**
	 * Puts all the MarkerSets onto BlueMap.
	 *
	 * @param api The BlueMapAPI to attach with.
	 */
	public void attachToBlueMap(BlueMapAPI api) {
		//loop through the markerSets
		for (UUID worldUID : markerSets.keySet()) {
			//add that markerset to each bluemap map of every world
			api.getWorld(worldUID).ifPresent(bmWorld -> {
				for (BlueMapMap map : bmWorld.getMaps()) {
					map.getMarkerSets().put(Config.MARKER_SET_ID, markerSets.get(worldUID));
				}
			});
		}
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

		POIMarker marker = new POIMarker(player.getName(), new Vector3d(location.getX(), location.getY(), location.getZ()));

		BufferedImage image;
		if (Main.config.useBlueMapSource) {
			image = ImageUtils.GetBImgFromAPI(player, api);
		} else {
			image = ImageUtils.GetBImgFromURL(player);
		}
		if (image == null) return;

		ImageUtils.Recolour(image);
		image = ImageUtils.Resize(image, 32, 32);

		try {
			String imagePath = api.getWebApp().createImage(image, "offlineplayerheads/" + player.getUniqueId());
			marker.setIcon(imagePath, image.getWidth() / 2, image.getHeight() / 2);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		markerSets.get(location.getWorld().getUID()).getMarkers().put(player.getUniqueId().toString(), marker);

		Main.logger.info("Marker for " + player.getName() + " added");
	}

	/**
	 * Removes a player marker from the map.
	 *
	 * @param player The player to remove the marker for.
	 */
	public void remove(Player player) {
		World world = player.getWorld();
		MarkerSet markerSet = markerSets.get(world.getUID());
		if (markerSet == null) {
			Main.logger.warning("Tried to remove a marker, but there is no marker set for the world " + world.getName());
			return;
		}
		markerSet.getMarkers().remove(player.getUniqueId().toString());

		Main.logger.info("Marker for " + player.getName() + " removed");
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
