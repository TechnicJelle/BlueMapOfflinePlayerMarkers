import com.technicjelle.bluemapofflineplayermarkers.core.Singletons;
import com.technicjelle.bluemapofflineplayermarkers.core.fileloader.FileMarkerLoader;
import mockery.*;
import org.junit.After;
import org.junit.Test;

public class LoadOfflineMarkersTests {
	@After
	public void cleanup() {
		Singletons.cleanup();
	}

	@Test
	public void extract_info_from_playerdata_files() {
		Singletons.init(
				new MockServer("test_playerdata"),
				ConsoleLogger.createLogger("extract_info_from_playerdata_files"),
				new MockConfig(),
				new MockMarkerHandler(),
				new MockBMApiStatus()
		);
		FileMarkerLoader.loadOfflineMarkers();
	}
}
