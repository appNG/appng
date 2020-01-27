alter table subject
add last_login datetime(3),
add pw_last_changed datetime(3),
add locked_since datetime(3),
add login_attempts integer not null default 0,
add allow_change_pw bit not null default 1;

update subject set allow_change_pw=0 where type != 'LOCAL_USER';
