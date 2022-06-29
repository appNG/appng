create table `site_host_alias` (
    `alias_name` varchar(255) not null,
    `site_id` int not null,
    primary key (`alias_name`),
    constraint `fk_site_host_alias_2_sites` foreign key (`site_id`) references `site` (`id`) on delete cascade
);
