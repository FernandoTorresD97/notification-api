create table channels (
 id bigserial primary key, name varchar(30) not null unique, active boolean not null, description varchar(255)
);
create table notifications (
 id bigserial primary key, title varchar(120) not null, message varchar(1000) not null,
 recipient varchar(120) not null, channel_id bigint not null references channels(id),
 status varchar(20) not null, scheduled_at timestamp(6), sent_at timestamp(6),
 created_at timestamp(6) not null, updated_at timestamp(6)
);
