import { init } from "@nais/apm";

// Må være likt app-navnet i Nais, ellers finner ikke Nais APM dataene.
function getAppName(): string {
	return window.location.hostname.startsWith("sokos-spk-mottak-admin-qx.")
		? "sokos-spk-mottak-admin-qx"
		: "sokos-spk-mottak-admin";
}

export function initApm() {
	init({
		namespace: "okonomi",
		app: getAppName(),
		tracing: true,
		devConsoleEcho: false,
	});
}
