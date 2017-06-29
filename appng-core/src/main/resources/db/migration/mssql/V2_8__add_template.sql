create table template (
	id int identity not null,
	appng_version nvarchar(255),
	description nvarchar(255),
	displayName nvarchar(255),
	long_desc nvarchar(255),
	name nvarchar(255),
	template_version nvarchar(255),
	timestamp nvarchar(255),
	version datetime2,
	primary key (id)
);

create table template_resource (
	id int identity not null,
	bytes varbinary(MAX),
	checksum nvarchar(255),
	file_version datetime2,
	name nvarchar(255),
	version datetime2,
	template_id int,
	primary key (id)
);

alter table template_resource 
	add constraint FK__TEMPLATE_RESOURCE__TEMPLATE_ID 
	foreign key (template_id) 
	references template (id);