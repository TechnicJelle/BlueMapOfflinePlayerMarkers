package com.technicjelle.bluemapofflineplayermarkers;

import com.flowpowered.math.vector.Vector3d;
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
import java.io.IOException;
import java.util.*;


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
					map.getMarkerSets().put(Main.config.markerSetId, markerSets.get(worldUID));
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
}
