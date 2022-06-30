create table site_host_alias (
    alias_name nvarchar(255) not null,
    site_id int not null,
    primary key (alias_name)
);

alter table site_host_alias add constraint FK__SITE_HOST_ALIAS__SITES foreign key (site_id) references site (id) on delete cascade;
