-- update properties
update property set name=replace(name,'application.','platform.');
update property set name=replace(name,'.plugin.','.application.');
update property set name=replace(name,'.defaultPlugin','.defaultApplication'),value = 'appng-manager' where name like '%.defaultPlugin%';
update property set name=replace(name,'webadmin-path','manager-path') where name like '%.webadmin-path%';
update property set name='platform.filebasedDeployment' where name='platform.filebasedPluginProvisioning';

-- drop foreign keys
ALTER TABLE authgroup_pluginrole DROP FOREIGN KEY FKD85255919EA59535;
ALTER TABLE authgroup_pluginrole DROP INDEX FKD85255919EA59535;

ALTER TABLE pluginpermission DROP FOREIGN KEY FKCCBE114230E912B6;
ALTER TABLE pluginpermission DROP INDEX FKCCBE114230E912B6;

ALTER TABLE pluginresource DROP FOREIGN KEY FKAFB6504130E912B6;
ALTER TABLE pluginresource DROP INDEX FKAFB6504130E912B6;

ALTER TABLE pluginrole DROP FOREIGN KEY FKE8FC1E2930E912B6;
ALTER TABLE pluginrole DROP INDEX FKE8FC1E2930E912B6;

ALTER TABLE pluginrole_pluginpermission DROP FOREIGN KEY FKFE284C989DAC3216;
ALTER TABLE pluginrole_pluginpermission DROP INDEX FKFE284C989DAC3216;

ALTER TABLE pluginrole_pluginpermission DROP FOREIGN KEY FKFE284C98870DFA61;
ALTER TABLE pluginrole_pluginpermission DROP INDEX FKFE284C98870DFA61;

ALTER TABLE sites_granted DROP FOREIGN KEY FK_sites_granted_site_id_plugin_id;
ALTER TABLE sites_granted DROP PRIMARY KEY;

ALTER TABLE site_plugin DROP PRIMARY KEY;
ALTER TABLE site_plugin DROP FOREIGN KEY FKA66C1F2B30E912B6;
ALTER TABLE site_plugin DROP FOREIGN KEY FKA66C1F2B8C469736;
ALTER TABLE site_plugin DROP INDEX FKA66C1F2B30E912B6;
ALTER TABLE site_plugin DROP INDEX FKA66C1F2B8C469736;

-- ALTER TABLE site_plugin DROP INDEX FKA66C1F2B30E912B6;

-- rename tables
ALTER TABLE authgroup_pluginrole RENAME authgroup_role;
ALTER TABLE plugin RENAME application;
ALTER TABLE pluginpermission RENAME permission;
ALTER TABLE pluginrepository RENAME repository;
ALTER TABLE pluginresource RENAME resource;
ALTER TABLE pluginrole RENAME role;
ALTER TABLE pluginrole_pluginpermission RENAME role_permission;
ALTER TABLE site_plugin RENAME site_application;

-- rename colums
ALTER TABLE  application CHANGE `pluginVersion` `application_version` varchar(64);
ALTER TABLE  application CHANGE `corePlugin` `core_application` bit default 0;
ALTER TABLE  authgroup_role CHANGE `pluginRoles_id` `role_id` integer not null;
ALTER TABLE  permission CHANGE `plugin_id` `application_id` integer;
ALTER TABLE  resource CHANGE `plugin_id` `application_id` integer;
ALTER TABLE  role CHANGE `plugin_id` `application_id` integer;
ALTER TABLE  role_permission CHANGE `pluginrole_id` `role_id` integer not null;
ALTER TABLE  site_application CHANGE `plugin_id` `application_id` integer not null;
ALTER TABLE  sites_granted CHANGE `plugin_id` `application_id` integer not null;


-- add new foreign keys an indizes
alter table authgroup_role
	add index FKD85255919EA59535 (role_id), 
	add constraint FKD85255919EA59535
	foreign key (role_id)
	references role (id);

alter table permission
	add index FKCCBE114230E912B6 (application_id), 
	add constraint FKCCBE114230E912B6
	foreign key (application_id)
	references application (id);

alter table resource
	add index FKAFB6504130E912B6 (application_id), 
	add constraint FKAFB6504130E912B6
	foreign key (application_id)
	references application (id);

alter table role
	add index FKE8FC1E2930E912B6 (application_id), 
	add constraint FKE8FC1E2930E912B6
	foreign key (application_id)
	references application (id);

alter table role_permission
	add index FKFE284C989DAC3216 (role_id), 
	add constraint FKFE284C989DAC3216
	foreign key (role_id)
	references role (id);

alter table role_permission
	add index FKFE284C98870DFA61 (permissions_id), 
	add constraint FKFE284C98870DFA61
	foreign key (permissions_id)
	references permission (id);

alter table site_application
	add index FKA66C1F2B30E912B6 (application_id), 
	add constraint FKA66C1F2B30E912B6
	foreign key (application_id)
	references application (id);
	
alter table site_application
	add index FKA66C1F2B8C469736 (site_id), 
	add constraint FKA66C1F2B8C469736
	foreign key (site_id)
	references site (id);

alter table sites_granted add primary key (site_id, application_id, granted_site_id);
	