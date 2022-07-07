create table site_hostnames (
 hostname varchar(255) not null,
 site_id int4 not null,
 primary key (hostname)
);

alter table site_hostnames add constraint FK__SITE_HOSTNAMES__SITES foreign key (site_id) references site (id) on delete cascade;
