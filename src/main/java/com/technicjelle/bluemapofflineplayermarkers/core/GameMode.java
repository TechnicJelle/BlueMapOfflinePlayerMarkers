package com.technicjelle.bluemapofflineplayermarkers.core;

import java.util.HashMap;
import java.util.Map;

public enum GameMode {
	SURVIVAL(0, "survival"),
	CREATIVE(1, "creative"),
	ADVENTURE(2, "adventure"),
	SPECTATOR(3, "spectator");

	private static final Map<Integer, GameMode> BY_VALUE = new HashMap<>();
	private static final Map<String, GameMode> BY_ID = new HashMap<>();

	static {
		for (GameMode c : values()) {
			BY_VALUE.put(c.value, c);
			BY_ID.put(c.id, c);
		}
	}

	private final int value;
	private final String id;

	GameMode(int value, String id) {
		this.value = value;
		this.id = id;
	}

	public static GameMode getByValue(int value) {
		return BY_VALUE.get(value);
	}

	public static GameMode getById(String id) {
		return BY_ID.get(id);
	}
}
