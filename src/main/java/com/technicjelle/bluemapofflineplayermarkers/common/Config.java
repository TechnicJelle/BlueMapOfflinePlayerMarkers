package com.technicjelle.bluemapofflineplayermarkers.common;

import com.technicjelle.bluemapofflineplayermarkers.core.GameMode;
import com.technicjelle.bluemapofflineplayermarkers.core.Singletons;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public interface Config {
	String MARKER_SET_ID = "offline-players";

	String getMarkerSetName();

	boolean isToggleable();

	boolean isDefaultHidden();

	/**
	 * If you want to show only players who have joined in the last X hours, set this to a number greater than 0.<p>
	 * If you want to show all players, set this to 0.
	 */
	long getExpireTimeInHours();

	List<GameMode> getHiddenGameModes();

	default boolean isGameModeHidden(GameMode gameMode) {
		return getHiddenGameModes().contains(gameMode);
	}

	boolean hideBannedPlayers();

	/**
	 * @param playerUUID The player to check.
	 * @return true if the player should be hidden
	 */
	default boolean checkPlayerLastPlayed(UUID playerUUID) {
		if (getExpireTimeInHours() <= 0) return false; // don't hide players if the expiry time is 0 or less

		Instant lastPlayed = Singletons.getServer().getPlayerLastPlayed(playerUUID);
		Instant expireTime = Instant.now().minusSeconds(getExpireTimeInHours() * 60 * 60);
		return lastPlayed.isBefore(expireTime);
	}

	static List<GameMode> parseGameModes(List<String> hiddenGameModesStrings) throws IllegalArgumentException {
		ArrayList<GameMode> gameModes = new ArrayList<>();
		for (String hiddenGameModeString : hiddenGameModesStrings) {
			try {
				GameMode parsedGameMode = GameMode.getById(hiddenGameModeString);
				if (parsedGameMode == null) throw new IllegalArgumentException("Invalid Game Mode: " + hiddenGameModeString);
				gameModes.add(parsedGameMode);
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException("Invalid Game Mode: " + hiddenGameModeString);
			}
		}
		return gameModes;
	}
}
