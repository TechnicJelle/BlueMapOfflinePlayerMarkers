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

	@NBTName("Dimension")
	private Object dimension;

	public @Nullable GameMode getGameMode() {
		//noinspection deprecation
		return GameMode.getByValue(gameMode);
	}

	public @Nullable Location getLocation() {
		World world = getWorld();

		//World couldn't be found, or position is broken
		if (world == null || position.length != 3) return null;

		return new Location(world, position[0], position[1], position[2]);
	}

	private @Nullable World getWorld() {
		UUID worldUUID = new UUID(worldUUIDMost, worldUUIDLeast);
		if (Bukkit.getWorld(worldUUID) != null) return Bukkit.getWorld(worldUUID);

		//If world doesn't exist, try to find it some other way...

		//By legacy dimension int
		if (dimension instanceof Integer) {
			int dimensionInt = (Integer) this.dimension;
			for (World world : Bukkit.getWorlds()) {
				//noinspection deprecation
				if (world.getEnvironment().getId() == dimensionInt) return world;
			}
		}

		return null;
	}
}
