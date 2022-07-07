create table site_hostnames(
    hostname varchar(255) not null,
    site_id integer
);
alter table site_hostnames add constraint pk_site_hostnames primary key(hostname);

alter table site_hostnames add constraint fk_site_hostnames_2_sites foreign key(site_id) references site(id) on delete cascade;
