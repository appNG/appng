alter table subject
add last_login datetime(3),
add pw_last_changed datetime(3),
add locked_since datetime(3),
add login_attempts integer not null default 0,
add pw_change_policy integer not null default 0;

update subject set pw_change_policy=2 where type != 'LOCAL_USER';
