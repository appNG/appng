alter table subject
add last_login datetime2,
pw_last_changed datetime2,
locked_since datetime2,
login_attempts int not null default 0,
allow_change_pw bit not null default 1;
GO
update subject set allow_change_pw=0 where type != 'LOCAL_USER';
