create table `site_hostnames` (
    `hostname` varchar(255) not null,
    `site_id` int not null,
    primary key (`hostname`),
    constraint `fk_site_hostnames_2_sites` foreign key (`site_id`) references `site` (`id`) on delete cascade
);
