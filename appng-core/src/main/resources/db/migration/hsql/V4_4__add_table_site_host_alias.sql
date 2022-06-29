create table site_host_alias(
    alias_name varchar(255) not null,
    site_id integer
);
alter table site_host_alias add constraint pk_site_host_alias primary key(alias_name);

alter table site_host_alias add constraint fk_site_host_alias_2_sites foreign key(site_id) references site(id) on delete cascade;
