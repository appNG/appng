create table `site_hostalias` (
    `hostname` varchar(255) not null,
    `site_id` int not null,
    primary key (`hostname`),
    constraint `FK__SITE_HOSTALIAS__SITE` foreign key (`site_id`) references `site` (`id`) on delete cascade
);
