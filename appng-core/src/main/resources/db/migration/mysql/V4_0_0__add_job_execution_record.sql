create table job_execution_record (
	id integer not null auto_increment,
	application varchar(255),
	site varchar(255),
	job_name varchar(255),
	start datetime(3),
	end datetime(3),
	duration INT,
	run_once boolean,
	result varchar(255),
	stacktraces MEDIUMTEXT,
	custom_data MEDIUMTEXT,
	triggername varchar(255),	
	primary key (id)
);
