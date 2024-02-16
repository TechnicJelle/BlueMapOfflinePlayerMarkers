package com.technicjelle.bluemapofflineplayermarkers.core;

import de.bluecolored.bluemap.api.BlueMapAPI;

public class BMApiStatus {
	public boolean isBlueMapAPIPresent() {
		return BlueMapAPI.getInstance().isPresent();
	}
}
