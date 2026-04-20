package com.technicjelle.bluemapofflineplayermarkers.impl.fabric;

import com.technicjelle.bluemapofflineplayermarkers.common.Server;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class FabricServer implements Server {

    MinecraftServer server;

    public FabricServer(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public void startUp() {
        // Override with empty because Vanilla Fabric already has cache file
    }

    @Override
    public void shutDown() {
        // Override with empty because Vanilla Fabric already has cache file
    }

    @Override
    public boolean isPlayerOnline(UUID playerUUID) {
        return server.getPlayerList().getPlayer(playerUUID) != null;
    }

    @Override
    public Path getConfigFolder() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public Path getPlayerDataFolder() {
        return server.getWorldPath(LevelResource.PLAYER_DATA_DIR);
    }

    @Override
    public Instant getPlayerLastPlayed(UUID playerUUID) {
        try {
            CompoundTag nbt = NbtIo.readCompressed(getPlayerDataFolder().resolve(playerUUID + ".dat"), NbtAccounter.unlimitedHeap());
            long millisSinceEpoch = nbt.getCompoundOrEmpty("bukkit").getLongOr("lastPlayed", 0L);
            return Instant.ofEpochMilli(millisSinceEpoch);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getPlayerName(UUID playerUUID) {
        Optional<NameAndId> profile = server.services().nameToIdCache().get(playerUUID);

        if (profile.isEmpty()) throw new RuntimeException("Can't get player from cache with id: " + playerUUID);

        return profile.get().name();
    }

    @Override
    public Optional<UUID> guessWorldUUID(Object object) {
        // Unused in Fabric
        return Optional.empty();
    }

    @Override
    public boolean isPlayerBanned(UUID playerUUID) {
        return server.getPlayerList().getBans().toString().contains(playerUUID.toString());
    }
}
