alter table site 
	add create_repository bit not null default 0;
alter table property 
	add defaultValue  nvarchar(255);
alter table property 
	add description  nvarchar(255);