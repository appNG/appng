create table site_hostalias (
 hostname varchar(255) not null,
 site_id int4 not null,
 primary key (hostname)
);

alter table site_hostalias add constraint FK__SITE_HOSTALIAS__SITE foreign key (site_id) references site (id) on delete cascade;
