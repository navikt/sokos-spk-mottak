drop index if exists idx_scheduled_tasks_history_task_name_timestamp;
drop table if exists scheduled_tasks_history;

create table scheduled_tasks_history
(
    id        uuid                     not null DEFAULT gen_random_uuid(),
    ident     varchar(50)              not null,
    timestamp timestamp with time zone not null,
    task_name varchar(100)             not null,
    PRIMARY KEY (id)
);

CREATE INDEX idx_scheduled_tasks_history_task_name_timestamp ON scheduled_tasks_history (task_name, timestamp);