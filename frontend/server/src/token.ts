import { getToken, requestOboToken, validateToken } from "@navikt/oasis";
import type { Request } from "express";
import { config } from "./config.ts";

export class UnauthorizedError extends Error {
	constructor(message: string) {
		super(message);
		this.name = "UnauthorizedError";
	}
}

async function getRequiredToken(req: Request): Promise<string> {
	if (!config.isProduction) {
		return "mock-token";
	}

	const initialToken = getToken(req);
	if (!initialToken) {
		throw new UnauthorizedError("Missing wonderwall cookie");
	}

	const tokenValidationResult = await validateToken(initialToken);
	if (!tokenValidationResult.ok) {
		throw new UnauthorizedError(
			`Token validation failed: ${tokenValidationResult.error}`,
		);
	}

	const oboTokenResult = await requestOboToken(
		initialToken,
		config.backendAudience,
	);
	if (!oboTokenResult.ok) {
		throw new Error(`Token exchange failed: ${oboTokenResult.error}`);
	}

	return oboTokenResult.token;
}

export async function getOboToken(req: Request): Promise<string> {
	if (!req.headers.authorization) {
		throw new UnauthorizedError("Authorization header is missing");
	}

	return await getRequiredToken(req);
}
