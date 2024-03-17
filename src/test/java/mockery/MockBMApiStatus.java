package mockery;

import com.technicjelle.bluemapofflineplayermarkers.core.BMApiStatus;

public class MockBMApiStatus extends BMApiStatus {
	@Override
	public boolean isBlueMapAPIPresent() {
		return false;
	}
}
