import { captureException } from "@nais/apm";
import useSWRImmutable from "swr/immutable";
import type { JobTaskInfoList } from "../types/JobTaskInfo";
import { JobTaskInfoListSchema } from "../types/schema/JobTaskInfoSchema";
import { axiosFetcher, axiosPostFetcher } from "./config/apiConfig";
import type { AvstemmingRequest } from "./models/AvstemmingRequest";

const BASE_URI = {
	BACKEND_API: "/spk-mottak-api/api/v1",
};

function swrConfig<T>(fetcher: (uri: string) => Promise<T>) {
	return {
		fetcher,
		suspense: false,
		revalidateOnFocus: false,
		refreshInterval: 30000,
		onError: (error: unknown, key: string) =>
			captureException(error, { context: { url: key } }),
	};
}

export function useGetJobTaskInfo() {
	const { data, error, isLoading, mutate } = useSWRImmutable<JobTaskInfoList>(
		`/jobTaskInfo`,
		swrConfig<JobTaskInfoList>(async (url) =>
			JobTaskInfoListSchema.parse(
				await axiosFetcher<unknown>(BASE_URI.BACKEND_API, url),
			),
		),
	);

	return { data, error, isLoading, mutate };
}

export async function postReadAndParseFile() {
	return await axiosPostFetcher(
		BASE_URI.BACKEND_API,
		"/readParseFileAndValidateTransactions",
	);
}

export async function postSendUtbetalingTransaksjon() {
	return await axiosPostFetcher(
		BASE_URI.BACKEND_API,
		"/sendUtbetalingTransaksjonToOppdragZ",
	);
}

export async function postSendTrekkTransaksjon() {
	return await axiosPostFetcher(
		BASE_URI.BACKEND_API,
		"/sendTrekkTransaksjonToOppdragZ",
	);
}

export async function postSendAvregningsretur() {
	return await axiosPostFetcher(
		BASE_URI.BACKEND_API,
		"/writeAvregningsreturFile",
	);
}

export async function postAvstemming(request: AvstemmingRequest) {
	return await axiosPostFetcher<AvstemmingRequest, null>(
		BASE_URI.BACKEND_API,
		"/avstemming",
		request,
	);
}
