create table authgroup (
	id integer not null auto_increment,
	description varchar(8192),
	name varchar(64) not null unique,
	version datetime,
	primary key (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table plugin (
	id integer not null auto_increment,
	corePlugin boolean not null,
	description varchar(8192),
	displayName varchar(64),
	fileBased boolean not null,
	hidden boolean not null,
	longDescription longtext,
	name varchar(64) not null unique,
	pluginVersion varchar(64),
	snapshot boolean not null,
	version datetime,
	primary key (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table pluginrole (
	id integer not null auto_increment,
	description varchar(8192),
	name varchar(64) not null,
	version datetime,
	plugin_id integer,
	primary key (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table authgroup_pluginrole (
	authgroup_id integer not null,
	pluginRoles_id integer not null,
	primary key (authgroup_id, pluginRoles_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table pluginpermission (
	id integer not null auto_increment,
	description varchar(8192),
	name varchar(255) not null,
	version datetime,
	plugin_id integer,
	primary key (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table pluginrepository (
	id integer not null auto_increment,
	active boolean not null,
	description varchar(8192),
	name varchar(64) not null,
	published boolean not null,
	mode varchar(255),
	type varchar(255),
	uri tinyblob,
	version datetime,
	primary key (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table pluginresource (
	id integer not null auto_increment,
	bytes longblob,
	checksum varchar(255),
	description varchar(8192),
	name varchar(64),
	type varchar(255),
	version datetime,
	plugin_id integer,
	primary key (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table pluginrole_pluginpermission (
	pluginrole_id integer not null,
	permissions_id integer not null,
	primary key (pluginrole_id, permissions_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table property (
	name varchar(255) not null,
	blobValue longblob,
	clobValue longtext,
	mandatory boolean not null,
	value varchar(255),
	version datetime,
	primary key (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table site (
	id integer not null auto_increment,
	active boolean not null,
	description varchar(8192),
	domain varchar(255) not null unique,
	host varchar(255) not null unique,
	name varchar(64) not null unique,
	version datetime,
	parentId integer,
	primary key (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table site_plugin (
	site_id integer not null,
	plugin_id integer not null,
	primary key (site_id, plugin_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table subject (
	id integer not null auto_increment,
	description varchar(8192),
	digest varchar(255),
	email varchar(255) not null,
	language varchar(3) not null,
	name varchar(64) not null unique,
	realname varchar(64) not null,
	salt varchar(255),
	timezone varchar(255),
	type varchar(255),
	version datetime,
	primary key (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table subject_authgroup (
	subject_Id integer not null,
	group_id integer not null
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

alter table authgroup_pluginrole
	add index FKD85255919EF9C126 (authgroup_id), 
	add constraint FKD85255919EF9C12
	foreign key (authgroup_id)
	references authgroup (id);

alter table authgroup_pluginrole
	add index FKD85255919EA59535 (pluginRoles_id), 
	add constraint FKD85255919EA59535
	foreign key (pluginRoles_id)
	references pluginrole (id);

alter table pluginpermission
	add index FKCCBE114230E912B6 (plugin_id), 
	add constraint FKCCBE114230E912B6
	foreign key (plugin_id)
	references plugin (id);

alter table pluginresource
	add index FKAFB6504130E912B6 (plugin_id), 
	add constraint FKAFB6504130E912B6
	foreign key (plugin_id)
	references plugin (id);

alter table pluginrole
	add index FKE8FC1E2930E912B6 (plugin_id), 
	add constraint FKE8FC1E2930E912B6
	foreign key (plugin_id)
	references plugin (id);

alter table pluginrole_pluginpermission
	add index FKFE284C989DAC3216 (pluginrole_id), 
	add constraint FKFE284C989DAC3216
	foreign key (pluginrole_id)
	references pluginrole (id);

alter table pluginrole_pluginpermission
	add index FKFE284C98870DFA61 (permissions_id), 
	add constraint FKFE284C98870DFA61
	foreign key (permissions_id)
	references pluginpermission (id);

alter table site
	add index FK35DF475526B168 (parentId), 
	add constraint FK35DF475526B168
	foreign key (parentId)
	references site (id);

alter table site_plugin
	add index FKA66C1F2B30E912B6 (plugin_id), 
	add constraint FKA66C1F2B30E912B6
	foreign key (plugin_id)
	references plugin (id);

alter table site_plugin
	add index FKA66C1F2B8C469736 (site_id), 
	add constraint FKA66C1F2B8C469736
	foreign key (site_id)
	references site (id);

alter table subject_authgroup
	add index FKC9972A8488D688DE (subject_Id), 
	add constraint FKC9972A8488D688DE
	foreign key (subject_Id)
	references subject (id);

alter table subject_authgroup
	add index FKC9972A84BB100B7E (group_id), 
	add constraint FKC9972A84BB100B7E
	foreign key (group_id)
	references authgroup (id);
