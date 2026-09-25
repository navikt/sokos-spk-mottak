const hostname = window.location.hostname;
const isProduction = hostname.endsWith(".intern.nav.no");
const isDevelopment = hostname.endsWith(".intern.dev.nav.no");

type Environment = "production" | "development" | "local";

export function getEnvironment(): Environment {
	if (isProduction) {
		return "production";
	}

	if (isDevelopment) {
		return "development";
	}

	return "local";
}
