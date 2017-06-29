CREATE DATABASE <database>;
GRANT ALL ON <database>.* TO '<user>'@'localhost' IDENTIFIED BY '<password>';
GRANT ALL ON <database>.* TO '<user>'@'%' IDENTIFIED BY '<password>';