alter table database_connection add min_connections INTEGER;
alter table database_connection add max_connections INTEGER;
update database_connection set min_connections = 1;
update database_connection set max_connections = 20;