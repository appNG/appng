alter table subject
add last_login timestamp,
add pw_last_changed timestamp,
add locked_since timestamp,
add login_attempts int4 not null default 0,
add allow_change_pw boolean not null default true;

update subject set allow_change_pw=false where type != 'LOCAL_USER';
