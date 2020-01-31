alter table subject
add last_login timestamp,
add pw_last_changed timestamp,
add expiry_date timestamp,
add locked boolean not null default false,
add login_attempts int4 not null default 0,
add pw_change_policy int4 not null default 0;

update subject set pw_change_policy=2 where type != 'LOCAL_USER';
