create table database_connection (
	id integer not null auto_increment,
	description varchar(255),
	driver_class varchar(255),
	jdbc_url varchar(255),
	name varchar(255),
	password longblob,
	type varchar(255),
	username varchar(255),
	mutable bit,
	version datetime,
	site_id integer,
	primary key (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

alter table database_connection
	add index FK12AF98228C469736 (site_id),
	add constraint FK12AF98228C469736
	foreign key (site_id)
	references site (id);
	
alter table site_plugin add connection_id INTEGER;

alter table site_plugin
	add index FKA66C1F2BEFA11DF1 (connection_id),
	add constraint FKA66C1F2BEFA11DF1
	foreign key (connection_id)
	references database_connection (id);