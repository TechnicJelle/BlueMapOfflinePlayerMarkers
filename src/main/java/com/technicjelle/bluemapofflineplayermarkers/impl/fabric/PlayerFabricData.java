package com.technicjelle.bluemapofflineplayermarkers.impl.fabric;

import com.flowpowered.math.vector.Vector3d;
import com.technicjelle.bluemapofflineplayermarkers.common.PlayerData;
import com.technicjelle.bluemapofflineplayermarkers.core.GameMode;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

public class PlayerFabricData implements PlayerData {

    final ServerPlayer player;

    public PlayerFabricData(ServerPlayer player) {
        this.player = player;
    }

    @Override
    public GameMode getGameMode() {
        return GameMode.getByValue(player.gameMode.getGameModeForPlayer().getId());
    }

    @Override
    public Vector3d getPosition() {
        Vec3 location = player.position();
        return new Vector3d(location.x(), location.y(), location.z());
    }

    @Override
    public Optional<UUID> getWorldUUID() {
        return Optional.empty();
    }

    @Override
    public Optional<String> getDimension() {
        try {
            // NEVER USE TRY WITH RESOURCE WITH LEVEL!!!
            @SuppressWarnings("resource") var level = player.level();
            return level.dimensionTypeRegistration().unwrapKey().map(dimensionTypeResourceKey -> dimensionTypeResourceKey.identifier().toString());
        } catch (Exception e) {
            BluemapOfflinePlayerMarkers.LOGGER.error("Failed to get dimension", e);
            return Optional.empty();
        }
    }
}
