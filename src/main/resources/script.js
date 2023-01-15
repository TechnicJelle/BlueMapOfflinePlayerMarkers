let smallIcons = false;

bluemap.events.addEventListener("bluemapCameraMoved", event => {
	let farAway = event.detail.controlsManager.distance > 1000;
	if (smallIcons !== farAway) {
		smallIcons = farAway;

		let elements = document.getElementsByClassName("bmopm-offline-player");
		for (let i = 0; i < elements.length; i++) {
			let el = elements.item(i);
			let icon = el.getElementsByClassName("bm-marker-poi-icon").item(0);
			if (farAway) {
				icon.classList.add("bmopm-small");
			} else {
				icon.classList.remove("bmopm-small");
			}
		}
	}
});

class LocaleDateTime extends HTMLElement {
	constructor() {
		super();
		const timestamp = this.getAttribute("data-timestamp");
		const dateString = new Date(parseInt(timestamp, 10)).toLocaleString();
		this.innerText = dateString;
	}
}


customElements.define("bmopm-datetime", LocaleDateTime);
