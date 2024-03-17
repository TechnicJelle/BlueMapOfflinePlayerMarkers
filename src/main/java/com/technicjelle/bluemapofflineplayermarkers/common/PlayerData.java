package com.technicjelle.bluemapofflineplayermarkers.common;

import com.flowpowered.math.vector.Vector3d;
import com.technicjelle.bluemapofflineplayermarkers.core.GameMode;

import java.util.Optional;
import java.util.UUID;

public interface PlayerData {
	GameMode getGameMode();

	Vector3d getPosition();

	Optional<UUID> getWorldUUID();
}
