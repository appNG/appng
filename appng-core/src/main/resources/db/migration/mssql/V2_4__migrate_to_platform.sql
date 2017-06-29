-- update properties
update property set name=replace(name,'application.','platform.');
update property set name=replace(name,'.plugin.','.application.');
update property set name=replace(name,'.defaultPlugin','.defaultApplication'),value = 'appng-manager' where name like '%.defaultPlugin%';
update property set name=replace(name,'webadmin-path','manager-path') where name like '%.webadmin-path%';
update property set name='platform.filebasedDeployment' where name='platform.filebasedPluginProvisioning';

-- drop foreign keys
ALTER TABLE authgroup_pluginrole DROP constraint FKD85255919EA59535;
ALTER TABLE pluginpermission DROP constraint FKCCBE114230E912B6;
ALTER TABLE pluginresource DROP constraint FKAFB6504130E912B6;
ALTER TABLE pluginrole DROP constraint FKE8FC1E2930E912B6;
ALTER TABLE pluginrole_pluginpermission DROP constraint FKFE284C989DAC3216;
ALTER TABLE pluginrole_pluginpermission DROP constraint FKFE284C98870DFA61;
ALTER TABLE site_plugin DROP constraint FKA66C1F2B30E912B6;
ALTER TABLE site_plugin DROP constraint FKA66C1F2B8C469736;

DECLARE @SQL VARCHAR(100)
SET @SQL = REPLACE('ALTER TABLE dbo.site_plugin DROP CONSTRAINT |PK_NAME|', '|PK_NAME|', (SELECT name FROM  sys.key_constraints WHERE  [type] = 'PK' AND [parent_object_id] = Object_id('site_plugin')))
EXEC (@SQL)


-- rename tables
exec sp_rename 'authgroup_pluginrole', 'authgroup_role';
exec sp_rename 'plugin', 'application';
exec sp_rename 'pluginpermission', 'permission';
exec sp_rename 'pluginrepository', 'repository';
exec sp_rename 'pluginresource', 'resource';
exec sp_rename 'pluginrole', 'role';
exec sp_rename 'pluginrole_pluginpermission', 'role_permission';
exec sp_rename 'site_plugin', 'site_application';

-- rename colums
exec sp_rename 'application.pluginVersion', 'application_version' , 'COLUMN';
exec sp_rename 'application.corePlugin', 'core_application' , 'COLUMN';
exec sp_rename 'authgroup_role.pluginRoles_id', 'role_id' , 'COLUMN';
exec sp_rename 'permission.plugin_id', 'application_id' , 'COLUMN';
exec sp_rename 'resource.plugin_id', 'application_id' , 'COLUMN';
exec sp_rename 'role.plugin_id', 'application_id' , 'COLUMN';
exec sp_rename 'role_permission.pluginrole_id', 'role_id' , 'COLUMN';
exec sp_rename 'site_application.plugin_id', 'application_id' , 'COLUMN';


-- add new foreign keys an indizes
alter table authgroup_role
	add constraint FKD85255919EA59535
	foreign key (role_id)
	references role (id);

alter table permission
	add constraint FKCCBE114230E912B6
	foreign key (application_id)
	references application (id);

alter table resource
	add constraint FKAFB6504130E912B6
	foreign key (application_id)
	references application (id);

alter table role
	add constraint FKE8FC1E2930E912B6
	foreign key (application_id)
	references application (id);

alter table role_permission
	add constraint FKFE284C989DAC3216
	foreign key (role_id)
	references role (id);

alter table role_permission
	add constraint FKFE284C98870DFA61
	foreign key (permissions_id)
	references permission (id);

alter table site_application
	add constraint FKA66C1F2B30E912B6
	foreign key (application_id)
	references application (id);
	
alter table site_application
	add constraint FKA66C1F2B8C469736
	foreign key (site_id)
	references site (id);
	
alter table site_application
	add constraint PK_site_application
	primary key (site_id, application_ID);
	
create table sites_granted (
	site_id int not null,
	application_id int not null,
	granted_site_id int not null,
	primary key (site_id, application_id, granted_site_id)
);

alter table sites_granted 
	add constraint FK_sites_granted_site_id 
	foreign key (site_id) 
	references site (id);

alter table sites_granted 
	add constraint FK_sites_granted_granted_site_id 
	foreign key (granted_site_id) 
	references site (id);

alter table sites_granted 
	add constraint FK_sites_granted_site_id_application_id 
	foreign key (site_id, application_id) 
	references site_application (site_id, application_id);
	