ALTER TABLE authgroup ADD default_admin boolean not null default false;
UPDATE authgroup SET default_admin=true WHERE name='Administrators';
INSERT IGNORE INTO authgroup (name,description,default_admin,version) VALUES ('Administrators','appNG Administrators group',true,now());