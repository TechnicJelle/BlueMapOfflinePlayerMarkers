package com.technicjelle.bluemapofflineplayermarkers;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.DoubleTag;
import com.flowpowered.nbt.stream.NBTInputStream;
import com.technicjelle.BMUtils;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;


public class MarkerHandler {
	private final BlueMapOfflinePlayerMarkers plugin;

	MarkerHandler(BlueMapOfflinePlayerMarkers plugin) {
		this.plugin = plugin;
	}

	/**
	 * Adds a player marker to the map.
	 *
	 * @param player The player to add the marker for.
	 */
	public void add(Player player) {
		add(player, player.getLocation(), player.getGameMode());
	}

	/**
	 * Adds a player marker to the map.
	 *
	 * @param player   The player to add the marker for.
	 * @param location The location to put the marker at.
	 * @param gameMode The game mode of the player.
	 */
	public void add(OfflinePlayer player, Location location, GameMode gameMode) {
		Optional<BlueMapAPI> optionalApi = BlueMapAPI.getInstance();
		if (optionalApi.isEmpty()) {
			plugin.getLogger().warning("Tried to add a marker, but BlueMap wasn't loaded!");
			return;
		}
		BlueMapAPI api = optionalApi.get();

		//If this player's visibility is disabled on the map, don't add the marker.
		if (!api.getWebApp().getPlayerVisibility(player.getUniqueId())) return;

		//If this player's game mode is disabled on the map, don't add the marker.
		if (plugin.getCurrentConfig().hiddenGameModes.contains(gameMode)) return;

		// Get BlueMapWorld for the location
		BlueMapWorld blueMapWorld = api.getWorld(location.getWorld()).orElse(null);
		if (blueMapWorld == null) return;

		// Create marker-template
		// (add 1.8 to y to place the marker at the head-position of the player, like BlueMap does with its player-markers)
		POIMarker.Builder markerBuilder = POIMarker.builder()
				.label(player.getName())
				.detail(player.getName() + " <i>(offline)</i><br>"
						+ "<bmopm-datetime data-timestamp=" + player.getLastPlayed() + "></bmopm-datetime>")
				.styleClasses("bmopm-offline-player")
				.position(location.getX(), location.getY() + 1.8, location.getZ());

		// Create an icon and marker for each map of this world
		// We need to create a separate marker per map, because the map-storage that the icon is saved in
		// is different for each map
		for (BlueMapMap map : blueMapWorld.getMaps()) {
			markerBuilder.icon(BMUtils.getPlayerHeadIconAddress(api, player.getUniqueId(), map), 0, 0); // centered with CSS instead

			// get marker-set (or create new marker set if none found)
			MarkerSet markerSet = map.getMarkerSets().computeIfAbsent(Config.MARKER_SET_ID, id -> MarkerSet.builder()
					.label(plugin.getCurrentConfig().markerSetName)
					.toggleable(plugin.getCurrentConfig().toggleable)
					.defaultHidden(plugin.getCurrentConfig().defaultHidden)
					.build());

			// add marker
			markerSet.put(player.getUniqueId().toString(), markerBuilder.build());
		}

		plugin.getLogger().info("Marker for " + player.getName() + " added");
	}


	/**
	 * Removes a player marker from the map.
	 *
	 * @param player The player to remove the marker for.
	 */
	public void remove(Player player) {
		Optional<BlueMapAPI> optionalApi = BlueMapAPI.getInstance();
		if (optionalApi.isEmpty()) {
			plugin.getLogger().warning("Tried to remove a marker, but BlueMap wasn't loaded!");
			return;
		}
		BlueMapAPI api = optionalApi.get();

		// remove all markers with the players uuid
		for (BlueMapMap map : api.getMaps()) {
			MarkerSet set = map.getMarkerSets().get(Config.MARKER_SET_ID);
			if (set != null) set.remove(player.getUniqueId().toString());
		}

		plugin.getLogger().info("Marker for " + player.getName() + " removed");
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

			long timeSinceLastPlayed = System.currentTimeMillis() - op.getLastPlayed();
//			logger.info("Player " + op.getName() + " was last seen " + timeSinceLastPlayed + "ms ago");
			if (plugin.getCurrentConfig().expireTimeInHours > 0 && timeSinceLastPlayed > plugin.getCurrentConfig().expireTimeInHours * 60 * 60 * 1000) {
				plugin.getLogger().fine("Player " + op.getName() + " was last seen too long ago, skipping");
				continue;
			}

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
			int gameModeInt = (int) nbtData.get("playerGameType").getValue();
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

			//Convert to game mode
			@SuppressWarnings("deprecation")
			GameMode gameMode = GameMode.getByValue(gameModeInt);

			//Add marker
			add(op, loc, gameMode);
		}
	}
}
