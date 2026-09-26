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
		beforeSend: (item) => {
			if (item.meta?.page?.url) {
				try {
					const url = new URL(item.meta.page.url);
					url.search = "";
					item.meta.page.url = url.toString();
				} catch {
					return item;
				}
			}
			return item;
		},
	});
}
