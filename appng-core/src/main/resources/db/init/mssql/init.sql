CREATE DATABASE <database>;
CREATE LOGIN <user> WITH PASSWORD = '<password>', DEFAULT_DATABASE = <database>;
USE <database>;
CREATE USER <user> FOR LOGIN <user> WITH DEFAULT_SCHEMA = dbo;
EXEC sp_addrolemember N'db_owner', N'<user>';
EXEC sp_addrolemember N'db_datawriter', N'<user>';