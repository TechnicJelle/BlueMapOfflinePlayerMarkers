package mockery;

import com.technicjelle.bluemapofflineplayermarkers.common.Config;
import com.technicjelle.bluemapofflineplayermarkers.core.GameMode;

import java.util.List;

public class MockConfig implements Config {
	@Override
	public String getMarkerSetName() {
		return MARKER_SET_ID;
	}

	@Override
	public boolean isToggleable() {
		return true;
	}

	@Override
	public boolean isDefaultHidden() {
		return false;
	}

	@Override
	public long getExpireTimeInHours() {
		return 0;
	}

	@Override
	public List<GameMode> getHiddenGameModes() {
		return List.of();
	}

	@Override
	public boolean hideBannedPlayers() {
		return false;
	}
}
