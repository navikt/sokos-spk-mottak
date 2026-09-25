export class ApiError extends Error {}

export class HttpStatusCodeError extends Error {
	constructor(readonly statusCode: number) {
		super(`HTTP ${statusCode}`);
	}
}
