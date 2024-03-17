package com.technicjelle.bluemapofflineplayermarkers.core.fileloader;

import com.technicjelle.bluemapofflineplayermarkers.core.Player;
import com.technicjelle.bluemapofflineplayermarkers.core.Singletons;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluenbt.BlueNBT;
import de.bluecolored.bluenbt.NBTReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

public class FileMarkerLoader {
	private static final BlueNBT nbt = new BlueNBT();

	public static void loadOfflineMarkers() {
		Path playerDataFolder = Singletons.getServer().getPlayerDataFolder();

		//Return if playerdata is missing for some reason.
		if (!Files.exists(playerDataFolder) || !Files.isDirectory(playerDataFolder)) {
			Singletons.getLogger().severe("Playerdata folder not found, skipping loading of offline markers from storage");
			return;
		}

		BlueMapAPI api;
		if (Singletons.isBlueMapAPIPresent()) {
			if (BlueMapAPI.getInstance().isPresent())
				api = BlueMapAPI.getInstance().get();
			else {
				Singletons.getLogger().warning("BlueMapAPI not available, skipping loading of offline markers from storage");
				return;
			}
		} else {
			Singletons.getLogger().info("BlueMapAPI not available, probably due to running in a test environment");
			api = null;
		}

		try (Stream<Path> playerDataFiles = Files.list(playerDataFolder)) {
			playerDataFiles.filter(p -> p.toString().endsWith(".dat")).forEach(p -> loadOfflineMarker(p, api));
		} catch (IOException e) {
			Singletons.getLogger().log(Level.SEVERE, "Failed to stream playerdata", e);
		}
	}

	private static void loadOfflineMarker(Path playerDataFile, BlueMapAPI api) {
		final String fileName = playerDataFile.getFileName().toString();
		Singletons.getLogger().info("Loading playerdata file: " + fileName);

		final String uuidString = fileName.replace(".dat", "");
		final UUID playerUUID;
		try {
			playerUUID = UUID.fromString(uuidString);
		} catch (IllegalArgumentException e) {
			Singletons.getLogger().warning("Invalid playerdata filename: " + fileName + ", skipping");
			return;
		}

		if (playerDataFile.toFile().length() == 0) {
			Singletons.getLogger().warning("Playerdata file " + fileName + " is empty, skipping");
			return;
		}

		if (Singletons.getServer().isPlayerOnline(playerUUID)) return; // don't add markers for online players

		if (Singletons.getConfig().checkPlayerLastPlayed(playerUUID)) {
			String playerName = Singletons.getServer().getPlayerName(playerUUID);
			Instant lastPlayed = Singletons.getServer().getPlayerLastPlayed(playerUUID);
			Singletons.getLogger().finer("Player " + playerName + " (" + playerUUID + ") was last online at " + lastPlayed.toString() + ",\n" +
					"which is more than " + Singletons.getConfig().getExpireTimeInHours() + " hours ago, so not adding marker");
			return;
		}

		try (GZIPInputStream in = new GZIPInputStream(Files.newInputStream(playerDataFile))) {
			NBTReader reader = new NBTReader(in);
			PlayerNBTData playerNBTData = nbt.read(reader, PlayerNBTData.class);

			Player player = new Player(playerUUID, playerNBTData);
			Singletons.getMarkerHandler().add(player, api);
		} catch (IOException e) {
			Singletons.getLogger().log(Level.SEVERE, "Failed to read playerdata file " + fileName, e);
		}
	}
}
