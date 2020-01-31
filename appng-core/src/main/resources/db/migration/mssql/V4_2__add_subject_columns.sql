alter table subject
add last_login datetime2,
pw_last_changed datetime2,
expiry_date datetime2,
locked bit not null default 0,
login_attempts int not null default 0,
pw_change_policy int not null default 0;
GO
update subject set pw_change_policy=2 where type != 'LOCAL_USER';
