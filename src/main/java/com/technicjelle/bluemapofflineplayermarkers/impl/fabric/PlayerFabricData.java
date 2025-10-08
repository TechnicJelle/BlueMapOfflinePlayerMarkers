package com.technicjelle.bluemapofflineplayermarkers.impl.fabric;

import com.flowpowered.math.vector.Vector3d;
import com.technicjelle.bluemapofflineplayermarkers.common.PlayerData;
import com.technicjelle.bluemapofflineplayermarkers.core.GameMode;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;
import java.util.UUID;

public class PlayerFabricData implements PlayerData {

    final ServerPlayerEntity player;

    public PlayerFabricData(ServerPlayerEntity player) {
        this.player = player;
    }

    @Override
    public GameMode getGameMode() {
        return GameMode.getByValue(player.interactionManager.getGameMode().getIndex());
    }

    @Override
    public Vector3d getPosition() {
        Vec3d location = player.getEntityPos();
        return new Vector3d(location.getX(), location.getY(), location.getZ());
    }

    @Override
    public Optional<UUID> getWorldUUID() {
        return Optional.empty();
    }

    @Override
    public Optional<String> getDimension() {
        return Optional.of(player.getEntityWorld().getDimensionEntry().getKey().get().getValue().toString());
    }
}
