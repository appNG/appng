create table authgroup (
    id int identity not null,
    description nvarchar(MAX),
    name nvarchar(64) not null unique,
    version datetime2,
    primary key (id)
)

create table authgroup_pluginrole (
    authgroup_id int not null,
    pluginRoles_id int not null,
    primary key (authgroup_id, pluginRoles_id)
)

create table plugin (
    id int identity not null,
    corePlugin bit not null,
    description nvarchar(MAX),
    displayName nvarchar(64),
    fileBased bit not null,
    hidden bit not null,
    longDescription nvarchar(MAX),
    name nvarchar(64) not null unique,
    pluginVersion nvarchar(64),
    snapshot bit not null,
    version datetime2,
    primary key (id)
)

create table pluginpermission (
    id int identity not null,
    description nvarchar(MAX),
    name nvarchar(255) not null,
    version datetime2,
    plugin_id int,
    primary key (id)
)

create table pluginrepository (
    id int identity not null,
    active bit not null,
    description nvarchar(MAX),
    name nvarchar(64) not null unique,
    published bit not null,
    mode nvarchar(255),
    type nvarchar(255),
    uri varbinary(255),
    version datetime2,
    primary key (id)
)

create table pluginresource (
    id int identity not null,
    bytes varbinary(MAX),
    checksum nvarchar(255),
    description nvarchar(MAX),
    name nvarchar(64),
    type nvarchar(255),
    version datetime2,
    plugin_id int,
    primary key (id)
)

create table pluginrole (
    id int identity not null,
    description nvarchar(MAX),
    name nvarchar(64) not null,
    version datetime2,
    plugin_id int,
    primary key (id)
)

create table pluginrole_pluginpermission (
    pluginrole_id int not null,
    permissions_id int not null,
    primary key (pluginrole_id, permissions_id)
)

create table property (
    name nvarchar(255) not null,
    blobValue varbinary(MAX),
    clobValue nvarchar(MAX),
    mandatory bit not null,
    value nvarchar(255),
    version datetime2,
    primary key (name)
)

create table site (
    id int identity not null,
    active bit not null,
    description nvarchar(MAX),
    domain nvarchar(255) not null unique,
    host nvarchar(255) not null unique,
    name nvarchar(64) not null unique,
    version datetime2,
    parentId int,
    primary key (id)
)

create table site_plugin (
    site_id int not null,
    plugin_id int not null,
    primary key (site_id, plugin_id)
)

create table subject (
    id int identity not null,
    description nvarchar(MAX),
    digest nvarchar(255),
    email nvarchar(255) not null,
    language nvarchar(3) not null,
    name nvarchar(64) not null unique,
    realname nvarchar(64) not null,
    salt nvarchar(255),
    timezone nvarchar(255),
    type nvarchar(255),
    version datetime2,
    primary key (id)
)

create table subject_authgroup (
    subject_Id int not null,
    group_id int not null
)

alter table authgroup_pluginrole 
    add constraint FKD85255919EF9C126 
    foreign key (authgroup_id) 
    references authgroup
    
alter table authgroup_pluginrole 
    add constraint FKD85255919EA59535 
    foreign key (pluginRoles_id) 
    references pluginrole
    
alter table pluginpermission 
    add constraint FKCCBE114230E912B6 
    foreign key (plugin_id) 
    references plugin
    
alter table pluginresource 
    add constraint FKAFB6504130E912B6 
    foreign key (plugin_id) 
    references plugin
    
alter table pluginrole 
    add constraint FKE8FC1E2930E912B6 
    foreign key (plugin_id) 
    references plugin
    
alter table pluginrole_pluginpermission 
    add constraint FKFE284C989DAC3216 
    foreign key (pluginrole_id) 
    references pluginrole
    
alter table pluginrole_pluginpermission 
    add constraint FKFE284C98870DFA61 
    foreign key (permissions_id) 
    references pluginpermission
    
alter table site 
    add constraint FK35DF475526B168 
    foreign key (parentId) 
    references site
    
alter table site_plugin 
    add constraint FKA66C1F2B30E912B6 
    foreign key (plugin_id) 
    references plugin
    
alter table site_plugin 
    add constraint FKA66C1F2B8C469736 
    foreign key (site_id) 
    references site
    
alter table subject_authgroup 
    add constraint FKC9972A8488D688DE 
    foreign key (subject_Id) 
    references subject
    
alter table subject_authgroup 
    add constraint FKC9972A84BB100B7E 
    foreign key (group_id) 
    references authgroup