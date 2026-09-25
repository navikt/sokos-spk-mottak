import React from "react";
import ReactDOM from "react-dom/client";
import "./index.css";
import Dashboard from "./pages/Dashboard";
import { initApm } from "./util/apm";

initApm();

const startMsw = async () => {
	if (import.meta.env.MODE !== "mock") return;

	try {
		const { worker } = await import("../mock/browser");
		await worker.start({
			onUnhandledRequest: "bypass",
		});
	} catch (error) {
		// biome-ignore lint/suspicious/noConsole: <in case of error>
		console.error("Failed to start MSW", error);
	}
};

startMsw().then(() =>
	ReactDOM.createRoot(document.getElementById("root") as HTMLElement).render(
		<React.StrictMode>
			<div className="page-wrapper">
				<div className="page-layout">
					<main>
						<Dashboard />
					</main>
				</div>
			</div>
		</React.StrictMode>,
	),
);
