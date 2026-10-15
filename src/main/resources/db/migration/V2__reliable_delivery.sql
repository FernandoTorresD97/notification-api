alter table notifications add column idempotency_key varchar(120) unique;
alter table notifications add column attempt_count integer not null default 0;
alter table notifications add column next_attempt_at timestamp(6);
alter table notifications add column last_error varchar(80);
alter table notifications add column dead_letter_pending boolean not null default false;
alter table notifications drop constraint if exists notifications_status_check;
alter table notifications add constraint notifications_status_check check (status in ('PENDING','SCHEDULED','RETRYING','SENT','FAILED','CANCELLED'));
create index idx_notifications_dispatch on notifications(status,next_attempt_at,scheduled_at);
create table delivery_attempts (
 id bigserial primary key, notification_id bigint not null references notifications(id),
 attempt_number integer not null, outcome varchar(30) not null, error_code varchar(80),
 attempted_at timestamp(6) not null, unique(notification_id,attempt_number)
);
