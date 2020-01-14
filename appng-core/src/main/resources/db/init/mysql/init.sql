CREATE DATABASE <database>;
CREATE USER '<user>'@'%' IDENTIFIED BY '<password>';
CREATE USER '<user>'@'localhost' IDENTIFIED BY '<password>';
GRANT ALL ON <database>.* TO '<user>'@'%';
GRANT ALL ON <database>.* TO '<user>'@'localhost';
