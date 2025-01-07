insert into scheduled_tasks (task_name, task_instance, task_data, execution_time, picked, picked_by, last_success, last_failure, consecutive_failures, last_heartbeat, version)
values ('grensesnittAvstemming', 'recurring', '0xACED000574003C7B226669727374223A2257313137323331222C227365636F6E64223A7B2266726F6D44617465223A6E756C6C2C22746F44617465223A6E756C6C7D7D',
        '2025-01-08 04:00:00.000000 +00:00', false, null, '2025-01-07 04:00:08.072133 +00:00', null, 0, null, 230),
       ('sendTrekkTransaksjonToOppdragZ', 'recurring', '0xACED000574000757313137323331', '2025-01-07 11:00:00.000000 +00:00', false, null, '2025-01-07 10:00:04.067947 +00:00', null, 0, null,
        15069),
       ('sendUtbetalingTransaksjonToOppdragZ', 'recurring', '0xACED000574000757313137323331', '2025-01-07 11:00:00.000000 +00:00', false, null, '2025-01-07 10:00:04.064479 +00:00', null, 0, null,
        15240),
       ('readParseFileAndValidateTransactions', 'recurring', '0xACED000574000757313137323331', '2025-01-07 11:00:00.000000 +00:00', false, null, '2025-01-07 10:00:04.439488 +00:00', null, 0,
        null, 1424);