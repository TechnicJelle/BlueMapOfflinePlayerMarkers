package com.technicjelle.bluemapofflineplayermarkers.core.markerhandler;

import com.flowpowered.math.vector.Vector3d;
import com.technicjelle.BMUtils;
import com.technicjelle.bluemapofflineplayermarkers.common.Config;
import com.technicjelle.bluemapofflineplayermarkers.common.Server;
import com.technicjelle.bluemapofflineplayermarkers.core.Player;
import com.technicjelle.bluemapofflineplayermarkers.core.Singletons;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;

import java.util.Optional;
import java.util.UUID;

public class BlueMapMarkerHandler implements MarkerHandler {
	@Override
	public void add(Player player, BlueMapAPI api) {
		//If this player's visibility is disabled on the map, don't add the marker.
		if (!api.getWebApp().getPlayerVisibility(player.getPlayerUUID())) return;

		Config config = Singletons.getConfig();
		//If this player's game mode is disabled on the map, don't add the marker.
		if (config.isGameModeHidden(player.getPlayerData().getGameMode())) return;

		Server server = Singletons.getServer();
		//If this player is banned and the config is set to hide banned players, don't add the marker.
		if (config.hideBannedPlayers() && server.isPlayerBanned(player.getPlayerUUID())) return;

		// Get BlueMapWorld for the position
		Optional<UUID> worldUUID = player.getPlayerData().getWorldUUID();
		if (worldUUID.isEmpty()) return;
		BlueMapWorld blueMapWorld = api.getWorld(worldUUID.get()).orElse(null);
		if (blueMapWorld == null) return;
		Vector3d position = player.getPlayerData().getPosition();
		if (position == null) return;

		// Add 1.8 to y to place the marker at the head-position of the player, like BlueMap does with its player-markers
		position = position.add(0, 1.8, 0);

		// Create marker-template
		POIMarker.Builder markerBuilder = POIMarker.builder()
				.label(player.getPlayerName())
				.detail(player.getPlayerName() + " <i>(offline)</i><br>"
						+ "<bmopm-datetime data-timestamp=" + player.getLastPlayed().toEpochMilli() + "></bmopm-datetime>")
				.styleClasses("bmopm-offline-player")
				.position(position);

		// Create an icon and marker for each map of this world
		// We need to create a separate marker per map, because the map-storage that the icon is saved in
		// is different for each map
		for (BlueMapMap map : blueMapWorld.getMaps()) {
			markerBuilder.icon(BMUtils.getPlayerHeadIconAddress(api, player.getPlayerUUID(), map), 0, 0); // centered with CSS instead

			// get marker-set (or create new marker set if none found)
			MarkerSet markerSet = map.getMarkerSets().computeIfAbsent(Config.MARKER_SET_ID, id -> MarkerSet.builder()
					.label(config.getMarkerSetName())
					.toggleable(config.isToggleable())
					.defaultHidden(config.isDefaultHidden())
					.build());

			// add marker
			markerSet.put(player.getPlayerUUID().toString(), markerBuilder.build());
		}

		Singletons.getLogger().info("Marker for " + player.getPlayerName() + " added");
	}

	@Override
	public void remove(UUID playerUUID, BlueMapAPI api) {
		// remove all markers with the players uuid
		for (BlueMapMap map : api.getMaps()) {
			MarkerSet set = map.getMarkerSets().get(Config.MARKER_SET_ID);
			if (set != null) set.remove(playerUUID.toString());
		}

		Singletons.getLogger().info("Marker for " + Singletons.getServer().getPlayerName(playerUUID) + " removed");
	}
}
