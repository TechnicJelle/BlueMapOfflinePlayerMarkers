package com.technicjelle.bluemapofflineplayermarkers.models;

import de.bluecolored.bluenbt.NBTName;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@SuppressWarnings({"unused", "MismatchedReadAndWriteOfArray"})
public class PlayerNBT {
	@NBTName("playerGameType")
	private int gameMode;

	@NBTName("Pos")
	private double[] position;

	@NBTName("WorldUUIDLeast")
	private long worldUUIDLeast;

	@NBTName("WorldUUIDMost")
	private long worldUUIDMost;

	public GameMode getGameMode() {
		//noinspection deprecation
		return GameMode.getByValue(gameMode);
	}

	public @Nullable Location getLocation() {
		UUID worldUUID = new UUID(worldUUIDMost, worldUUIDLeast);
		World world = Bukkit.getWorld(worldUUID);

		//World doesn't exist or position is broken
		if (world == null || position.length != 3) return null;
		return new Location(world, position[0], position[1], position[2]);
	}
}
