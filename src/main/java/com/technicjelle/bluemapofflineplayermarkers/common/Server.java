package com.technicjelle.bluemapofflineplayermarkers.common;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface Server {
	boolean isPlayerOnline(UUID playerUUID);

	Path getPlayerDataFolder();

	/**
	 * @param playerUUID The UUID of the player to get the last played time for.
	 * @return The last time the player was online in amount of milliseconds since epoch (January 1, 1970, 00:00:00 GMT).
	 */
	Instant getPlayerLastPlayed(UUID playerUUID);

	String getPlayerName(UUID playerUUID);

	Optional<UUID> guessWorldUUID(Object object);
}
