import { captureException } from "@nais/apm";
import {
	Box,
	Heading,
	InlineMessage,
	Skeleton,
	VStack,
} from "@navikt/ds-react";
import { useEffect, useState } from "react";
import {
	postAvstemming,
	postReadAndParseFile,
	postSendAvregningsretur,
	postSendTrekkTransaksjon,
	postSendUtbetalingTransaksjon,
	useGetJobTaskInfo,
} from "../api/apiService";
import { useStore } from "../store/AppState";
import { toIsoDate } from "../util/datoUtil";
import DateRangePicker from "./components/DateRangePicker";
import JobCard from "./components/JobCard";
import styles from "./Dashboard.module.css";

const JOB_COOLDOWN_MS = 30000;
const JOB_CARD_SKELETONS = [
	"read",
	"utbetaling",
	"trekk",
	"avstemming",
	"avregning",
];

const Dashboard = () => {
	const { data, error, isLoading, mutate } = useGetJobTaskInfo();

	const [alert, setAlert] = useState<{
		id: string;
		type: "success" | "error";
	} | null>(null);

	const { taskInfoStateRecord, setTaskInfoStateItem, removeTaskInfoStateItem } =
		useStore();
	const [loadingButtons, setLoadingButtons] = useState<{
		[key: string]: boolean;
	}>({});
	const [alertVisibility, setalertVisibility] = useState<{
		[key: string]: boolean;
	}>({});

	const [dateRange, setDateRange] = useState({
		fromDate: null as string | null,
		toDate: null as string | null,
	});

	const taskMap = new Map(data?.map((task) => [task.taskName, task]));

	const jobDefinitions = [
		{
			title: "Les inn fil og valider transaksjoner",
			taskName: "readParseFileAndValidateTransactions",
			start: postReadAndParseFile,
			hasDatePicker: false,
		},
		{
			title: "Send utbetalingstransaksjoner",
			taskName: "sendUtbetalingTransaksjonToOppdragZ",
			start: postSendUtbetalingTransaksjon,
			hasDatePicker: false,
		},
		{
			title: "Send trekktransaksjoner",
			taskName: "sendTrekkTransaksjonToOppdragZ",
			start: postSendTrekkTransaksjon,
			hasDatePicker: false,
		},
		{
			title: "Send avregningsretur",
			taskName: "writeAvregningsreturFile",
			start: postSendAvregningsretur,
			hasDatePicker: false,
		},
		{
			title: "Grensesnittavstemming",
			taskName: "grensesnittAvstemming",
			start: () =>
				postAvstemming({
					fromDate: dateRange.fromDate
						? toIsoDate(dateRange.fromDate)
						: undefined,
					toDate: dateRange.toDate ? toIsoDate(dateRange.toDate) : undefined,
				}),
			hasDatePicker: true,
		},
	] as const;

	const handleStartJob = async (
		taskId: string,
		startJob: () => Promise<unknown>,
	) => {
		setLoadingButtons((prev) => ({ ...prev, [taskId]: true }));
		setalertVisibility((prev) => ({ ...prev, [taskId]: true }));

		const currentTime = Date.now();
		setTaskInfoStateItem(taskId, { disabled: true, timestamp: currentTime });

		await startJob()
			.then(() => {
				setAlert({ id: taskId, type: "success" });
				mutate();
			})
			.catch((error) => {
				captureException(error, { context: { taskId } });
				setAlert({ id: taskId, type: "error" });
			});
	};

	useEffect(() => {
		// Track timeouts for cleanup
		const timeouts: Record<string, number> = {};

		// Single function to handle button state reset
		const resetButtonState = (key: string) => {
			setLoadingButtons((prev) => ({ ...prev, [key]: false }));
			setalertVisibility((prev) => ({ ...prev, [key]: false }));
			removeTaskInfoStateItem(key);
		};

		const processTask = (taskId: string, timestamp: number) => {
			const now = Date.now();
			const elapsedTime = now - timestamp;

			if (elapsedTime < JOB_COOLDOWN_MS) {
				// Still within disabled period - set UI state
				setLoadingButtons((prev) => ({ ...prev, [taskId]: true }));
				setalertVisibility((prev) => ({ ...prev, [taskId]: true }));

				// Schedule reset
				const remainingTime = JOB_COOLDOWN_MS - elapsedTime;
				timeouts[taskId] = window.setTimeout(
					() => resetButtonState(taskId),
					remainingTime,
				);
			} else {
				// Already expired
				resetButtonState(taskId);
			}
		};

		// Process tasks from state record
		if (taskInfoStateRecord) {
			Object.entries(taskInfoStateRecord).forEach(([taskId, state]) => {
				if (state.disabled) {
					processTask(taskId, state.timestamp);
				}
			});
		}

		// Clean up all timeouts on unmount
		return () => {
			Object.values(timeouts).forEach((timeoutId) => {
				window.clearTimeout(timeoutId);
			});
		};
	}, [taskInfoStateRecord, removeTaskInfoStateItem]);

	return (
		<>
			<VStack align="center">
				<Box paddingBlock="space-16">
					<Heading size="medium" level="1">
						SPK Mottak Dashboard
					</Heading>
				</Box>
			</VStack>
			{isLoading ? (
				<VStack gap="space-16" align="stretch" aria-busy="true">
					{JOB_CARD_SKELETONS.map((key) => (
						<Skeleton
							key={key}
							variant="rounded"
							height={130}
							className={styles["job-card-skeleton"]}
						/>
					))}
				</VStack>
			) : error ? (
				<VStack align="center" justify="center" gap="space-32">
					<InlineMessage status="error">
						Det oppstod en feil ved henting av data fra serveren. Vennligst prøv
						igjen senere.
					</InlineMessage>
				</VStack>
			) : (
				<VStack gap="space-16" align="stretch">
					{jobDefinitions.map((job) => {
						const taskInfo = taskMap.get(job.taskName);
						const jobCard = (
							<JobCard
								key={job.taskName}
								title={job.title}
								attributes={{
									alertType: alert?.id === job.taskName ? alert.type : "info",
									isAlertVisible: alertVisibility[job.taskName] ?? false,
									isJobRunning: taskInfo?.isPicked ?? false,
									isLoading: loadingButtons[job.taskName] ?? false,
									isButtonDisabled:
										taskInfoStateRecord[job.taskName]?.disabled ?? false,
								}}
								jobTaskInfo={taskInfo}
								onStartClick={() => {
									if (taskInfo) {
										void handleStartJob(taskInfo.taskName, job.start);
									}
								}}
							>
								{job.hasDatePicker && (
									<div className={styles.datePickerWrapper}>
										<DateRangePicker
											onDateChange={(fromDate, toDate) => {
												if (
													fromDate !== dateRange.fromDate ||
													toDate !== dateRange.toDate
												) {
													setDateRange({ fromDate, toDate });
												}
											}}
										/>
									</div>
								)}
							</JobCard>
						);

						return job.hasDatePicker ? (
							<div key={job.taskName} className={styles.bottomSpacing}>
								{jobCard}
							</div>
						) : (
							jobCard
						);
					})}
				</VStack>
			)}
		</>
	);
};

export default Dashboard;
