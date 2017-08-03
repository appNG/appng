-- APPNG-2027 Rename core --> privileged application
exec sp_rename 'application.core_application', 'privileged' , 'COLUMN';