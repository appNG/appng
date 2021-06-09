alter table property add prop_type nvarchar(16);
GO
update property set prop_type='TEXT';
update property set prop_type='MULTILINE' where len(clobValue) > 0;
update property set prop_type='BOOLEAN' where lower(defaultValue) in('true','false');
update property set prop_type='INT' where ISNUMERIC(defaultValue)=1;
update property set prop_type='DECIMAL' where ISNUMERIC(defaultValue)=1 and defaultValue like '%.%';
update property set prop_type='PASSWORD' where name like '%Password';
