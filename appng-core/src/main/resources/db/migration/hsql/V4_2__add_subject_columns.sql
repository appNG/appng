alter table subject add column last_login timestamp;
alter table subject add column pw_last_changed timestamp;
alter table subject add column locked_since timestamp;
alter table subject add column expiry_date timestamp;
alter table subject add column locked bit default 0 not null;
alter table subject add column login_attempts integer default 0 not null;
alter table subject add column pw_change_policy integer default 0 not null;

update subject set pw_change_policy=2 where type != 'LOCAL_USER';
