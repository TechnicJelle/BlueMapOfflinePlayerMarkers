package com.technicjelle.bluemapofflineplayermarkers;

import com.technicjelle.BMUtils;
import com.technicjelle.bluemapofflineplayermarkers.models.PlayerNBT;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;
import de.bluecolored.bluenbt.BlueNBT;
import de.bluecolored.bluenbt.NBTReader;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;


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
	public void add(@NotNull Player player) {
		add(player, player.getLocation(), player.getGameMode(), System.currentTimeMillis());
	}

	/**
	 * Adds a player marker to the map.
	 *
	 * @param player   The player to add the marker for.
	 * @param location The location to put the marker at.
	 * @param gameMode The game mode of the player.
	 */
	public void add(@NotNull OfflinePlayer player, @NotNull Location location, @NotNull GameMode gameMode) {
		add(player, location, gameMode, player.getLastPlayed());
	}

	/**
	 * Adds a player marker to the map.
	 *
	 * @param player     The player to add the marker for.
	 * @param location   The location to put the marker at.
	 * @param gameMode   The game mode of the player.
	 * @param lastPlayed The last time the player was online.
	 */
	private void add(@NotNull OfflinePlayer player, @NotNull Location location, @NotNull GameMode gameMode, long lastPlayed) {
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

		String playerName = player.getName();

		// Create marker-template
		// (add 1.8 to y to place the marker at the head-position of the player, like BlueMap does with its player-markers)
		POIMarker.Builder markerBuilder = POIMarker.builder()
				.label(playerName)
				.detail(playerName + " <i>(offline)</i><br>"
						+ "<bmopm-datetime data-timestamp=" + lastPlayed + "></bmopm-datetime>")
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

		plugin.getLogger().info("Marker for " + playerName + " added");
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
		Path playerDataFolder = Bukkit.getWorlds().get(0).getWorldFolder().toPath().resolve("playerdata");
		//Return if playerdata is missing for some reason.
		if (!Files.exists(playerDataFolder) || !Files.isDirectory(playerDataFolder)) return;

		BlueNBT nbt = new BlueNBT();
		for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
			//If player is online, ignore (I don't know why the method is called "getOfflinePlayers" when it also contains all online players...)
			if (op.isOnline()) continue;

			long timeSinceLastPlayed = System.currentTimeMillis() - op.getLastPlayed();
//			logger.info("Player " + op.getName() + " was last seen " + timeSinceLastPlayed + "ms ago");
			if (plugin.getCurrentConfig().expireTimeInHours > 0 && timeSinceLastPlayed > plugin.getCurrentConfig().expireTimeInHours * 60 * 60 * 1000) {
				plugin.getLogger().fine("Player " + op.getName() + " was last seen too long ago, skipping");
				continue;
			}

			Path dataFile = playerDataFolder.resolve(op.getUniqueId().toString() + ".dat");

			//Failsafe if playerdata doesn't exist (should be impossible but whatever)
			if (!Files.exists(dataFile)) continue;

			plugin.getLogger().info("Processing playerdata file " + dataFile.getFileName());
			try (GZIPInputStream in = new GZIPInputStream(Files.newInputStream(dataFile))) {
				NBTReader reader = new NBTReader(in);
				PlayerNBT playerNBT = nbt.read(reader, PlayerNBT.class);

				if (playerNBT.getGameMode() == null || playerNBT.getLocation() == null) {
					plugin.getLogger().warning("Failed to read GameMode or Location from " + dataFile.getFileName());
					continue;
				}

				add(op, playerNBT.getLocation(), playerNBT.getGameMode());
			} catch (IOException e) {
				plugin.getLogger().log(Level.WARNING, "Failed to read playerdata file " + dataFile.getFileName(), e);
			}
		}
	}
}
