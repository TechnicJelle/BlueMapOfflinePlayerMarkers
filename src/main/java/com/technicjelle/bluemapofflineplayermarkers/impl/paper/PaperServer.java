package com.technicjelle.bluemapofflineplayermarkers.impl.paper;

import com.technicjelle.bluemapofflineplayermarkers.common.Server;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class PaperServer implements Server {
	final org.bukkit.Server server;

	public PaperServer(JavaPlugin plugin) {
		this.server = plugin.getServer();
	}

	@Override
	public boolean isPlayerOnline(UUID playerUUID) {
		OfflinePlayer op = server.getOfflinePlayer(playerUUID);
		return op.isOnline();
	}

	@Override
	public Path getPlayerDataFolder() {
		//I really don't like "getWorlds().get(0)" as a way to get the main world, but as far as I can tell there is no other way
		return Bukkit.getWorlds().get(0).getWorldFolder().toPath().resolve("playerdata");
	}

	@Override
	public Instant getPlayerLastPlayed(UUID playerUUID) {
		OfflinePlayer op = server.getOfflinePlayer(playerUUID);
		long millisSinceEpoch = op.getLastPlayed();
		return Instant.ofEpochMilli(millisSinceEpoch);
	}

	@Override
	public String getPlayerName(UUID playerUUID) {
		OfflinePlayer op = server.getOfflinePlayer(playerUUID);
		return op.getName();
	}

	@Override
	public Optional<UUID> guessWorldUUID(Object object) {
		if (object instanceof String) {
			String dimensionString = (String) object;
			return Optional.ofNullable(server.getWorld(dimensionString).getUID());
		}

		if (object instanceof Integer) {
			int dimensionInt = (Integer) object;
			for (World world : server.getWorlds()) {
				@SuppressWarnings("deprecation") int worldID = world.getEnvironment().getId();
				if (worldID == dimensionInt) return Optional.ofNullable(world.getUID());
			}
		}

		return Optional.empty();
	}
}
