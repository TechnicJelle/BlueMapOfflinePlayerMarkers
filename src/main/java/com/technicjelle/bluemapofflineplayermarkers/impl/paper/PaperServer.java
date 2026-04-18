package com.technicjelle.bluemapofflineplayermarkers.impl.paper;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.technicjelle.bluemapofflineplayermarkers.common.Server;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class PaperServer implements Server {
	final JavaPlugin plugin;
	final org.bukkit.Server server;

	public PaperServer(JavaPlugin plugin) {
		this.plugin = plugin;
		this.server = plugin.getServer();
	}

	@Override
	public boolean isPlayerOnline(UUID playerUUID) {
		OfflinePlayer op = server.getOfflinePlayer(playerUUID);
		return op.isOnline();
	}

	@Override
	public Path getConfigFolder() {
		return plugin.getDataFolder().toPath();
	}

	@Override
	public Path getPlayerDataFolder() {
		//I really don't like "getWorlds().getFirst()" as a way to get the main world, but as far as I can tell, there is no other way
		Path dimensionFolder = Bukkit.getWorlds().getFirst().getWorldFolder().toPath();

		//This is how BlueMap does it too... https://github.com/BlueMap-Minecraft/BlueMap/blob/c115f26d7b2330b83c368396b4f84c9ce53945ae/implementations/paper/src/main/java/de/bluecolored/bluemap/bukkit/BukkitWorld.java#L55
		Path worldFolder = dimensionFolder.getParent().getParent().getParent();

		Path newPlayerDataFolder = worldFolder.resolve("players").resolve("data");
		if (Files.exists(newPlayerDataFolder)) return newPlayerDataFolder;

		//Pre 26.1 format:
		Path oldPlayerDataFolder = dimensionFolder.resolve("playerdata");
		if (Files.exists(oldPlayerDataFolder)) return oldPlayerDataFolder;

		return Path.of("");
	}

	@Override
	public Instant getPlayerLastPlayed(UUID playerUUID) {
		OfflinePlayer op = server.getOfflinePlayer(playerUUID);
		long millisSinceEpoch = op.getLastSeen();
		return Instant.ofEpochMilli(millisSinceEpoch);
	}

	@Override
	public String getPlayerName(UUID playerUUID) {
		OfflinePlayer op = server.getOfflinePlayer(playerUUID);
		@Nullable String name = op.getName();
		if (name != null) return name;

		PlayerProfile playerProfile = server.createProfile(playerUUID);
		if (playerProfile.complete(false)) {
			name = playerProfile.getName();
			if (name != null && !name.isBlank()) return name;
		}

		try {
			return Server.nameFromMojangAPI(playerUUID);
		} catch (IOException e) {
			//If the player is not found, return the UUID as a string
			return playerUUID.toString();
		}
	}

	@Override
	public Optional<UUID> guessWorldUUID(Object object) {
		if (object instanceof String dimensionString) {
			//Try to get world by name
			{
				@Nullable World world = server.getWorld(dimensionString);
				if (world != null) {
					return Optional.of(world.getUID());
				}
			}

			//Try to get world by dimension
			for (World world : server.getWorlds()) {
				switch (world.getEnvironment()) {
					case NORMAL:
						if (dimensionString.contains("overworld")) return Optional.of(world.getUID());
					case NETHER:
						if (dimensionString.contains("the_nether")) return Optional.of(world.getUID());
					case THE_END:
						if (dimensionString.contains("the_end")) return Optional.of(world.getUID());
				}
			}
		}

		if (object instanceof Integer) {
			int dimensionInt = (Integer) object;
			for (World world : server.getWorlds()) {
				@SuppressWarnings("deprecation") int worldID = world.getEnvironment().getId();
				if (worldID == dimensionInt) return Optional.of(world.getUID());
			}
		}

		return Optional.empty();
	}

	@Override
	public boolean isPlayerBanned(UUID playerUUID) {
		OfflinePlayer op = server.getOfflinePlayer(playerUUID);
		return op.isBanned();
	}
}
