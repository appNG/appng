alter table database_connection add managed bit;
update database_connection set managed=1;

alter table site_plugin add deletion_mark bit not null default 0;
alter table site_plugin add reload_required bit not null default 0;
alter table site_plugin add active bit not null default 1;

alter table site add startup_time datetime;