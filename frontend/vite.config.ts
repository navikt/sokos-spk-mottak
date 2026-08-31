import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

export default defineConfig(({ mode }) => ({
	base: "/",
	build: {
		outDir: "./dist",
		target: "esnext",
	},
	css: {
		modules: {
			generateScopedName: "[name]__[local]___[hash:base64:5]",
		},
	},
	server: {
		proxy: {
			...((mode === "backend" || /^.*-q1$/.test(mode)) && {
				"/spk-mottak-api/api/v1": {
					target: /^.*-q1$/.test(mode)
						? "https://sokos-spk-mottak.intern.dev.nav.no"
						: "http://localhost:8080",
					rewrite: (path: string) => path.replace(/^\/spk-mottak-api/, ""),
					changeOrigin: true,
					secure: /^.*-q1$/.test(mode),
				},
			}),
		},
	},
	plugins: [react()],
}));

