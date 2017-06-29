ALTER TABLE sites_granted DROP KEY FK_sites_granted_site_id_plugin_id;
ALTER TABLE sites_granted CHANGE application_id site_id_temp int(11) NOT NULL;
ALTER TABLE sites_granted CHANGE site_id application_id int(11) NOT NULL;
ALTER TABLE sites_granted CHANGE site_id_temp site_id int(11) NOT NULL;
ALTER TABLE site_application ADD PRIMARY KEY (application_id, site_id);
ALTER TABLE sites_granted
ADD CONSTRAINT FK__SITES_GRANTED__SITE_APPLICATION
FOREIGN KEY (application_id, site_id)
REFERENCES site_application (application_id, site_id);