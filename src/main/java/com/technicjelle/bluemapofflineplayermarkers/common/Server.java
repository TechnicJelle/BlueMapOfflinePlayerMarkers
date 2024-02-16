package com.technicjelle.bluemapofflineplayermarkers.common;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface Server {
	Gson _gson = new Gson();

	boolean isPlayerOnline(UUID playerUUID);

	Path getPlayerDataFolder();

	/**
	 * @param playerUUID The UUID of the player to get the last played time for.
	 * @return The last time the player was online in amount of milliseconds since epoch (January 1, 1970, 00:00:00 GMT).
	 */
	Instant getPlayerLastPlayed(UUID playerUUID);

	String getPlayerName(UUID playerUUID);

	/**
	 * Requests the player's name from the Mojang API. May be slow.
	 * @throws IOException If there was an error with the connection.
	 */
	static String nameFromMojangAPI(UUID playerUUID) throws IOException {
		URL url = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + playerUUID);
		URLConnection request = url.openConnection();
		request.connect();

		JsonObject response = _gson.fromJson(new InputStreamReader(request.getInputStream()), JsonObject.class);
		return response.get("name").getAsString();
	}

	Optional<UUID> guessWorldUUID(Object object);
}
