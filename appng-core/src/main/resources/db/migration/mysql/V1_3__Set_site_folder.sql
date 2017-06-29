alter table site 
	add column create_repository boolean not null default false;
alter table property 
	add column defaultValue  varchar(255);
alter table property 
	add column description  varchar(255);
