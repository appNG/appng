create table sites_granted (
	site_id integer not null,
	plugin_id integer not null,
	granted_site_id integer not null,
	primary key (site_id, plugin_id, granted_site_id)
) ENGINE=InnoDB;

alter table sites_granted 
	add index FK_sites_granted_granted_site_id (granted_site_id), 
	add constraint FK_sites_granted_granted_site_id 
	foreign key (granted_site_id) 
	references site (id);

alter table sites_granted 
	add index FK_sites_granted_site_id_plugin_id (site_id, plugin_id), 
	add constraint FK_sites_granted_site_id_plugin_id 
	foreign key (site_id, plugin_id) 
	references site_plugin (plugin_id, site_id);