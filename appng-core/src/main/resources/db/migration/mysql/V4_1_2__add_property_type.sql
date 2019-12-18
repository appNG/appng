alter table property add prop_type varchar(16);

update property set prop_type='TEXT';
update property set prop_type='MULTILINE' where length(clobValue) > 0;
update property set prop_type='BOOLEAN' where lower(defaultValue) in('true','false');
update property set prop_type='DECIMAL' where defaultValue REGEXP '^[0-9]+\\.[0-9]+$';
update property set prop_type='INT' where defaultValue REGEXP '^[0-9]+$';
update property set prop_type='PASSWORD' where name like '%Password';
