create table database_connection (
	id int identity not null,
	description varchar(255),
	driver_class varchar(255),
	jdbc_url varchar(255),
	name varchar(255),
	password varbinary(MAX),
	type varchar(255),
	username varchar(255),
	mutable bit,
	version datetime2,
	site_id int,
	primary key (id)
);

alter table database_connection
	add constraint FK12AF98228C469736
	foreign key (site_id)
	references site;

alter table site_plugin add connection_id int;

alter table site_plugin 
	add constraint FKA66C1F2BEFA11DF1
	foreign key (connection_id)
	references database_connection;