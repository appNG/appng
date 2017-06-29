ALTER TABLE authgroup ADD default_admin bit not null default 0;
UPDATE authgroup SET default_admin=1 WHERE name='Administrators';
INSERT IGNORE INTO authgroup (name,description,default_admin,version) VALUES ('Administrators','appNG Administrators group',1,getdate());