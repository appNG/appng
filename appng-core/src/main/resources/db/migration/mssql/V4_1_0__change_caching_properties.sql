update property set name=replace(name,'.ehcache','.cache') where name like '%.ehcache%';
delete from property where name like 'platform.site.%.ehcacheBlockingTimeout';
