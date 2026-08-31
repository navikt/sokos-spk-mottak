import path from "node:path";
import express, {
	type NextFunction,
	type Request,
	type Response,
} from "express";
import expressStaticGzip from "express-static-gzip";
import { collectDefaultMetrics, register } from "@prometheus-io/client";
import { logger } from "./logger.ts";
import { proxyRoutes } from "./proxy.ts";

const BUILD_PATH = path.resolve(import.meta.dirname, "../dist");
const PORT = process.env.PORT || 8080;
const SOKOS_SPK_MOTTAK_BACKEND_URL = process.env.SOKOS_SPK_MOTTAK_BACKEND_URL;

collectDefaultMetrics();

const server = express();

server.use(express.static(BUILD_PATH, { index: false }));
server.use(express.json());
server.use(express.urlencoded({ extended: true }));
server.use(
	expressStaticGzip(BUILD_PATH, {
		enableBrotli: true,
		orderPreference: ["br"],
	}),
);

function asyncHandler(
	fn: (req: Request, res: Response, next: NextFunction) => Promise<unknown>,
) {
	return (req: Request, res: Response, next: NextFunction) => {
		Promise.resolve(fn(req, res, next)).catch(next);
	};
}

server.post(
	`/spk-mottak-api/api/v1/readParseFileAndValidateTransactions`,
	asyncHandler(async (req: Request, res: Response) => {
		await proxyRoutes(
			req,
			res,
			`${SOKOS_SPK_MOTTAK_BACKEND_URL}/api/v1/readParseFileAndValidateTransactions`,
		);
	}),
);

server.post(
	`/spk-mottak-api/api/v1/sendUtbetalingTransaksjonToOppdragZ`,
	asyncHandler(async (req: Request, res: Response) => {
		await proxyRoutes(
			req,
			res,
			`${SOKOS_SPK_MOTTAK_BACKEND_URL}/api/v1/sendUtbetalingTransaksjonToOppdragZ`,
		);
	}),
);

server.post(
	`/spk-mottak-api/api/v1/sendTrekkTransaksjonToOppdragZ`,
	asyncHandler(async (req: Request, res: Response) => {
		await proxyRoutes(
			req,
			res,
			`${SOKOS_SPK_MOTTAK_BACKEND_URL}/api/v1/sendTrekkTransaksjonToOppdragZ`,
		);
	}),
);

server.post(
	`/spk-mottak-api/api/v1/writeAvregningsreturFile`,
	asyncHandler(async (req: Request, res: Response) => {
		await proxyRoutes(
			req,
			res,
			`${SOKOS_SPK_MOTTAK_BACKEND_URL}/api/v1/writeAvregningsreturFile`,
		);
	}),
);

server.post(
	`/spk-mottak-api/api/v1/avstemming`,
	asyncHandler(async (req: Request, res: Response) => {
		await proxyRoutes(
			req,
			res,
			`${SOKOS_SPK_MOTTAK_BACKEND_URL}/api/v1/avstemming`,
		);
	}),
);

server.get(
	`/spk-mottak-api/api/v1/jobTaskInfo`,
	asyncHandler(async (req: Request, res: Response) => {
		await proxyRoutes(
			req,
			res,
			`${SOKOS_SPK_MOTTAK_BACKEND_URL}/api/v1/jobTaskInfo`,
		);
	}),
);

const internalRouter = express.Router();

internalRouter.get("/isAlive", (_req: Request, res: Response) => {
	res.sendStatus(200);
});

internalRouter.get("/isReady", (_req: Request, res: Response) => {
	res.sendStatus(200);
});

internalRouter.get(
	"/metrics",
	asyncHandler(async (_req: Request, res: Response) => {
		res.set("Content-Type", register.contentType);
		res.send(await register.metrics());
	}),
);

server.use("/internal", internalRouter);

server.use((err: Error, _req: Request, res: Response, _next: NextFunction) => {
	logger.error({ err }, "Request error occurred");

	res.status(500).json({
		message: err.message,
		...(process.env.NODE_ENV !== "production" && { stack: err.stack }),
	});
});

server.listen(PORT, () => logger.info(`Server listening on port ${PORT}`));
