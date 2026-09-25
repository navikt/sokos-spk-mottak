const isProduction = process.env.NODE_ENV === "production";

function required(name: string): string {
	const value = process.env[name];
	if (!value) {
		throw new Error(`Mangler miljøvariabel ${name}`);
	}
	return value;
}

export const config = {
	isProduction,
	port: process.env.PORT || 8080,
	backendUrl: required("SOKOS_SPK_MOTTAK_BACKEND_URL"),
	backendAudience: isProduction
		? required("SOKOS_SPK_MOTTAK_BACKEND_AUDIENCE")
		: "",
};
