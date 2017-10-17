-- APPNG-2027 Rename core application --> privileged
ALTER TABLE application CHANGE `core_application` `privileged` bit default 0;