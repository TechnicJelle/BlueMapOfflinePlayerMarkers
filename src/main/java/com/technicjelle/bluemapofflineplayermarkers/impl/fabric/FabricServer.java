package com.technicjelle.bluemapofflineplayermarkers.impl.fabric;

import com.mojang.authlib.GameProfile;
import com.technicjelle.bluemapofflineplayermarkers.common.Server;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;
import org.jetbrains.annotations.Nullable;

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
        return server.getPlayerManager().getPlayer(playerUUID) != null;
    }

    @Override
    public Path getConfigFolder() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public Path getPlayerDataFolder() {
        return server.getSavePath(WorldSavePath.PLAYERDATA);
    }

    @Override
    public Instant getPlayerLastPlayed(UUID playerUUID) {
        try {
            NbtCompound nbt = NbtIo.readCompressed(getPlayerDataFolder().resolve(playerUUID + ".dat"), NbtSizeTracker.ofUnlimitedBytes());
            long millisSinceEpoch = nbt != null ? nbt.getCompoundOrEmpty("bukkit").getLong("lastPlayed", 0L) : 0;
            return Instant.ofEpochMilli(millisSinceEpoch);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getPlayerName(UUID playerUUID) {
        Optional<GameProfile> profile = server.getUserCache().getByUuid(playerUUID);

        if (profile.isEmpty()) throw new RuntimeException("Can't get player from cache with id: " + playerUUID);

        @Nullable String name = profile.get().getName();
        if (name != null) return name;

        try {
            return Server.nameFromMojangAPI(playerUUID);
        } catch (IOException e) {
            //If the player is not found, return the UUID as a string
            return playerUUID.toString();
        }
    }

    @Override
    public Optional<UUID> guessWorldUUID(Object object) {
        // Unused in Fabric
        return Optional.empty();
    }

    @Override
    public boolean isPlayerBanned(UUID playerUUID) {
        return server.getPlayerManager().getUserBanList().toString().contains(playerUUID.toString());
    }
}
