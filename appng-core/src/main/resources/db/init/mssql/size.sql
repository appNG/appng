SELECT sum(size*8/1024) FROM sys.master_files WHERE DB_NAME(database_id)='<database>';
