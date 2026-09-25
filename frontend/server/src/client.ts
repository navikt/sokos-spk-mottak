import type { Request, Response } from "express";
import { logger } from "./logger.ts";
import { getOboToken } from "./token.ts";

export async function sendRequest(
	req: Request,
	res: Response,
	backendUrl: string,
	logMessage: string,
) {
	const oboToken = await getOboToken(req);
	const path = new URL(backendUrl).pathname;
	const endpoint = path.split("/").pop();
	logger.info({ method: req.method, path }, logMessage);

	const response = await fetch(backendUrl, {
		method: req.method,
		headers: {
			Authorization: `Bearer ${oboToken}`,
			"Content-Type": "application/json",
		},
		...(req.method === "POST" && { body: JSON.stringify(req.body) }),
	});

	const level =
		response.status >= 500 ? "error" : response.status >= 400 ? "warn" : "info";
	logger[level](
		{ method: req.method, path, status: response.status },
		`Svar fra backend: ${endpoint}`,
	);

	const responseData = await response.text();
	res.status(response.status);

	const contentType = response.headers.get("content-type");
	if (contentType) {
		res.type(contentType);
	}

	if (responseData) {
		res.send(responseData);
	} else {
		res.end();
	}
}
