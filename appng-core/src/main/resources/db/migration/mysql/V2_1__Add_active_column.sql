alter table database_connection add active bit;
update database_connection set active=1;

alter table database_connection drop column mutable;