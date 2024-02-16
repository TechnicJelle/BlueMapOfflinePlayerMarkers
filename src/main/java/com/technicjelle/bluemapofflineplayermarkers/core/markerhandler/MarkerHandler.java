package com.technicjelle.bluemapofflineplayermarkers.core.markerhandler;

import com.technicjelle.bluemapofflineplayermarkers.core.Player;
import de.bluecolored.bluemap.api.BlueMapAPI;

import java.util.UUID;

public interface MarkerHandler {
	void add(Player player, BlueMapAPI api);

	void remove(UUID playerUUID, BlueMapAPI api);
}
