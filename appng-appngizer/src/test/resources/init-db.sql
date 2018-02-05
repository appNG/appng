INSERT INTO database_connection
(description,driver_class,jdbc_url,name,password,type,username,site_id,min_connections,max_connections,managed,active,validation_query)
VALUES ('appNG Root Database','org.hsqldb.jdbc.JDBCDriver','jdbc:hsqldb:mem:testdb','appNG HSQL','','HSQL','sa',null,1,20,false,false,'select 1 from INFORMATION_SCHEMA.SYSTEM_USERS');
