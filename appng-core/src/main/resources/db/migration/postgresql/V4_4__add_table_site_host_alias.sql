create table site_host_alias (
 alias_name varchar(255) not null,
 site_id int4 not null,
 primary key (alias_name)
);

alter table site_host_alias add constraint FK__SITE_HOST_ALIAS__SITES foreign key (site_id) references site (id) on delete cascade;
