import { readdirSync, readFileSync, writeFileSync } from "node:fs";
import { join, resolve } from "node:path";
import { brotliCompressSync, constants, gzipSync } from "node:zlib";
import react from "@vitejs/plugin-react";
import { defineConfig, type Plugin } from "vite";

const isQ1 = (mode: string) => mode.endsWith("-q1");

const COMPRESSIBLE = /\.(js|css|html|svg|json|ico)$/;

/**
 * Kreves av `express-static-gzip` i server/src/server.ts, som stille faller
 * tilbake til ukomprimert levering hvis .br/.gz mangler.
 */
function precompressAssets(): Plugin {
	let outDir = "";

	return {
		name: "precompress-assets",
		apply: "build",
		configResolved(config) {
			outDir = resolve(config.root, config.build.outDir);
		},
		closeBundle() {
			const entries = readdirSync(outDir, {
				recursive: true,
				withFileTypes: true,
			});

			for (const entry of entries) {
				if (!entry.isFile() || !COMPRESSIBLE.test(entry.name)) continue;

				const file = join(entry.parentPath, entry.name);
				const source = readFileSync(file);

				writeFileSync(`${file}.gz`, gzipSync(source, { level: 9 }));
				writeFileSync(
					`${file}.br`,
					brotliCompressSync(source, {
						params: { [constants.BROTLI_PARAM_QUALITY]: 11 },
					}),
				);
			}
		},
	};
}

export default defineConfig(({ command, mode }) => {
	const isProductionBuild = command === "build";
	const useProxy = mode === "backend" || isQ1(mode);

	return {
		plugins: [react(), precompressAssets()],

		build: {
			rolldownOptions: {
				output: {
					codeSplitting: {
						groups: [
							{
								test: /node_modules[\\/](react|react-dom|scheduler)[\\/]/,
								name: "react",
							},
							{ test: /node_modules[\\/]@navikt[\\/]/, name: "aksel" },
							{
								test: /node_modules[\\/](@grafana|@nais)[\\/]/,
								name: "apm",
							},
							{ test: /node_modules[\\/]/, name: "vendor" },
						],
					},
				},
			},
		},

		css: {
			modules: {
				// Hashet i prod så kildefilnavn ikke lekker til klienten.
				generateScopedName: isProductionBuild
					? "[hash:base64:6]"
					: "[name]__[local]___[hash:base64:5]",
			},
		},

		server: {
			proxy: useProxy
				? {
						"/spk-mottak-api/api/v1": {
							target: isQ1(mode)
								? "https://sokos-spk-mottak.intern.dev.nav.no"
								: "http://localhost:8080",
							rewrite: (path: string) => path.replace(/^\/spk-mottak-api/, ""),
							changeOrigin: true,
							secure: isQ1(mode),
						},
					}
				: undefined,
		},
	};
});
