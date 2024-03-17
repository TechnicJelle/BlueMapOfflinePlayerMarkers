package com.technicjelle.bluemapofflineplayermarkers.core;

import com.technicjelle.bluemapofflineplayermarkers.common.PlayerData;

import java.time.Instant;
import java.util.UUID;

public class Player {
	private final UUID playerUUID;
	private final String playerName;
	/**
	 * The last time the player was online.
	 * In milliseconds since epoch.
	 */
	private final Instant lastPlayed;
	private final PlayerData playerData;

	public Player(UUID uuid, PlayerData playerData) {
		this.playerUUID = uuid;
		this.playerName = Singletons.getServer().getPlayerName(uuid);
		this.lastPlayed = Singletons.getServer().getPlayerLastPlayed(uuid);
		this.playerData = playerData;
	}

	public UUID getPlayerUUID() {
		return playerUUID;
	}

	public String getPlayerName() {
		return playerName;
	}

	public Instant getLastPlayed() {
		return lastPlayed;
	}

	public PlayerData getPlayerData() {
		return playerData;
	}
}
