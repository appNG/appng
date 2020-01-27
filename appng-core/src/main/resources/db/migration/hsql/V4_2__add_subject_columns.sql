alter table subject add column last_login timestamp;
alter table subject add column pw_last_changed timestamp;
alter table subject add column locked_since timestamp;
alter table subject add column login_attempts integer default 0 not null;
alter table subject add column allow_change_pw boolean default 1 not null;
