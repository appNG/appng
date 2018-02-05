SELECT round(sum(data_length + index_length) /1024 /1024, 2)
FROM information_schema.TABLES
WHERE TABLE_SCHEMA='<database>';
