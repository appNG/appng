ALTER TABLE authgroup ADD default_admin boolean default 0 not null;
INSERT INTO authgroup (name,description,default_admin,version) VALUES ('Administrators','appNG Administrators group',true,now());
