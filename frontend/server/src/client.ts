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
	const endpoint = new URL(backendUrl).pathname.split("/").pop();
	logger.info(
		{
			method: req.method,
			url: backendUrl,
		},
		logMessage,
	);

	const response = await fetch(backendUrl, {
		method: req.method,
		headers: {
			Authorization: `Bearer ${oboToken}`,
			"Content-Type": "application/json",
		},
		...(req.method === "POST" && { body: JSON.stringify(req.body) }),
	});

	logger.info(
		{
			url: response.url,
			status: response.status,
		},
		`Svar fra backend: ${endpoint}`,
	);

	const responseData = await response.text();
	res.status(response.status);

	if (responseData) {
		res.send(responseData);
	} else {
		res.end();
	}
}
