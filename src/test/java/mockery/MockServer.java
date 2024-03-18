package mockery;

import com.technicjelle.bluemapofflineplayermarkers.common.Server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class MockServer implements Server {
	final String playerDataFolderName;

	public MockServer(String playerDataFolderName) {
		this.playerDataFolderName = playerDataFolderName;
	}

	@Override
	public boolean isPlayerOnline(UUID playerUUID) {
		return false;
	}

	@Override
	public Path getConfigFolder() {
		return getPlayerDataFolder();
	}

	@Override
	public Path getPlayerDataFolder() {
		Path path = Paths.get("").resolve("src/test/resources/" + playerDataFolderName);
		assert Files.exists(path);
		return path;
	}

	@Override
	public Instant getPlayerLastPlayed(UUID playerUUID) {
		return Instant.now();
	}

	@Override
	public String getPlayerName(UUID playerUUID) {
		try {
			return Server.nameFromMojangAPI(playerUUID);
		} catch (IOException e) {
			return playerUUID.toString();
		}
	}

	@Override
	public Optional<UUID> guessWorldUUID(Object object) {
		return Optional.empty();
	}

	@Override
	public boolean isPlayerBanned(UUID playerUUID) {
		return false;
	}
}
