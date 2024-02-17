package com.technicjelle.bluemapofflineplayermarkers.impl.paper;

import com.flowpowered.math.vector.Vector3d;
import com.technicjelle.bluemapofflineplayermarkers.common.PlayerData;
import com.technicjelle.bluemapofflineplayermarkers.core.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

public class PlayerBukkitData implements PlayerData {
	final Player player;

	public PlayerBukkitData(Player player) {
		this.player = player;
	}

	@Override
	public GameMode getGameMode() {
		org.bukkit.GameMode bukkitGameMode = player.getGameMode();
		@SuppressWarnings("deprecation") GameMode gameMode = GameMode.getByValue(bukkitGameMode.getValue());
		return gameMode;

	}

	@Override
	public Vector3d getPosition() {
		Location location = player.getLocation();
		return new Vector3d(location.getX(), location.getY(), location.getZ());
	}

	@Override
	public Optional<UUID> getWorldUUID() {
		return Optional.of(player.getWorld().getUID());
	}
}
