package com.technicjelle.bluemapofflineplayermarkers.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.technicjelle.bluemapofflineplayermarkers.core.Singletons;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface Server {
	Gson _gson = new GsonBuilder()
			.setLenient()
//			.setPrettyPrinting() //Disabled to discourage people from editing the file by hand
			.enableComplexMapKeySerialization()
			.create();

	Map<UUID, String> _cachedPlayerNames = new HashMap<>();
	String _cacheFileName = "cachedPlayerNames.json";

	default void startUp() {
		//load cached player names
		Path cacheFolder = Singletons.getServer().getConfigFolder();
		File cacheFile = new File(cacheFolder.toFile(), _cacheFileName);
		if (cacheFile.exists()) {
			try (InputStreamReader reader = new FileReader(cacheFile)) {
				Map<UUID, String> map = _gson.fromJson(reader, new TypeToken<Map<UUID, String>>() {}.getType());
				if (map != null) {
					_cachedPlayerNames.putAll(map);
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	default void shutDown() {
		//save cached player names
		Path cacheFolder = Singletons.getServer().getConfigFolder();
		File cacheFile = new File(cacheFolder.toFile(), _cacheFileName);
		try (Writer writer = new FileWriter(cacheFile)) {
			_gson.toJson(_cachedPlayerNames, writer);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	boolean isPlayerOnline(UUID playerUUID);

	Path getConfigFolder();

	Path getPlayerDataFolder();

	/**
	 * @param playerUUID The UUID of the player to get the last played time for.
	 * @return The last time the player was online in amount of milliseconds since epoch (January 1, 1970, 00:00:00 GMT).
	 */
	Instant getPlayerLastPlayed(UUID playerUUID);

	String getPlayerName(UUID playerUUID);

	/**
	 * Requests the player's name from the Mojang API. May be slow.
	 *
	 * @throws IOException If there was an error with the connection.
	 */
	static String nameFromMojangAPI(UUID playerUUID) throws IOException {
		String name = _cachedPlayerNames.get(playerUUID);
		if (name != null) return name;

		URL url = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + playerUUID);
		URLConnection request = url.openConnection();
		request.connect();

		JsonObject response = _gson.fromJson(new InputStreamReader(request.getInputStream()), JsonObject.class);
		if (response == null) throw new IOException("No response from Mojang API");
		name = response.get("name").getAsString();
		_cachedPlayerNames.put(playerUUID, name);
		return name;
	}

	Optional<UUID> guessWorldUUID(Object object);

	boolean isPlayerBanned(UUID playerUUID);
}
