ALTER TABLE authgroup ADD default_admin bit not null default 0;
GO
UPDATE authgroup SET default_admin=1 WHERE name='Administrators';
INSERT INTO authgroup (name,description,default_admin,version) VALUES ('Administrators','appNG Administrators group',1,getdate());