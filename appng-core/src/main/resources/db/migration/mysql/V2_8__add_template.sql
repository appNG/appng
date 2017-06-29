create table template (
	id integer not null auto_increment,
	appng_version varchar(255),
	description varchar(255),
	displayName varchar(255),
	long_desc varchar(255),
	name varchar(255),
	template_version varchar(255),
	timestamp varchar(255),
	version datetime(3),
	primary key (id)
);

create table template_resource (
	id integer not null auto_increment,
	bytes longblob,
	checksum varchar(255),
	file_version datetime(3),
	name varchar(255),
	version datetime(3),
	template_id integer,
	primary key (id)
);

alter table template_resource 
	add constraint FK__TEMPLATE_RESOURCE__TEMPLATE_ID 
	foreign key (template_id) 
	references template (id);