package com.technicjelle.bluemapofflineplayermarkers.impl.fabric;

import com.technicjelle.BMUtils.BMCopy;
import com.technicjelle.bluemapofflineplayermarkers.core.BMApiStatus;
import com.technicjelle.bluemapofflineplayermarkers.core.Player;
import com.technicjelle.bluemapofflineplayermarkers.core.Singletons;
import com.technicjelle.bluemapofflineplayermarkers.core.fileloader.FileMarkerLoader;
import com.technicjelle.bluemapofflineplayermarkers.core.markerhandler.BlueMapMarkerHandler;
import de.bluecolored.bluemap.api.BlueMapAPI;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;

public class BluemapOfflinePlayerMarkers implements DedicatedServerModInitializer {

    public static Logger LOGGER = LogManager.getLogger();

    @Override
    public void onInitializeServer() {

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            LOGGER.info("BlueMap Offline Player Markers plugin enabled!");
            FabricConfig config = new FabricConfig();
            config.createAndReadConfig();
            Singletons.init(new FabricServer(server), null, config, new BlueMapMarkerHandler(), new BMApiStatus());
            Singletons.getServer().startUp();
            BlueMapAPI.onEnable(onEnableListener);
            BlueMapAPI.onDisable(onDisableListener);
        });

        LOGGER.info("BlueMap Offline Player Markers plugin (on)loading...");

        BlueMapAPI.onEnable(api -> {
            LOGGER.info("BlueMap is enabled! Copying resources to BlueMap webapp and registering them...");
            try {
                BMCopy.jarResourceToWebApp(api, getClass().getClassLoader(), "assets/technicjelle/style.css", "bmopm.css", false);
                BMCopy.jarResourceToWebApp(api, getClass().getClassLoader(), "assets/technicjelle/script.js", "bmopm.js", false);
            } catch (IOException e) {
                LOGGER.error("Failed to copy resources to BlueMap webapp!", e);
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, _, _) -> new Thread(() -> {
            Optional<BlueMapAPI> api = BlueMapAPI.getInstance();
            if (api.isEmpty()) {
                LOGGER.warn("BlueMap is not loaded, not removing marker for {}", handler.player.getName());
                return;
            }
            Singletons.getMarkerHandler().remove(handler.player.getUUID(), api.get());
        }).start());

        ServerPlayConnectionEvents.DISCONNECT.register((handler, _) -> new Thread(() -> {
            try {
                Thread.sleep(100);
                PlayerFabricData playerFabricData = new PlayerFabricData(handler.player);
                Player playerToAdd = new Player(handler.player.getUUID(), playerFabricData);

                Optional<BlueMapAPI> api = BlueMapAPI.getInstance();
                if (api.isEmpty()) {
                    LOGGER.warn("BlueMap is not loaded, not adding marker for {}", handler.player.getName());
                    return;
                }

                Singletons.getMarkerHandler().add(playerToAdd, api.get());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start());

        ServerLifecycleEvents.SERVER_STOPPING.register(_ -> {
            BlueMapAPI.unregisterListener(onEnableListener);
            BlueMapAPI.unregisterListener(onDisableListener);
            Singletons.getServer().shutDown();
            LOGGER.info("BlueMap Offline Player Markers plugin disabled!");
            Singletons.cleanup();
        });
    }

    private final Consumer<BlueMapAPI> onEnableListener = _ -> {
        LOGGER.info("API Ready! BlueMap Offline Player Markers plugin enabled!");

        new Thread(() -> {
            try {
                Thread.sleep(100);
                FileMarkerLoader.loadOfflineMarkers();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();
    };

    final Consumer<BlueMapAPI> onDisableListener = _ -> LOGGER.info("API disabled! BlueMap Offline Player Markers shutting down...");
}
