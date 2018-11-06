create table job_execution_record (
	id INT identity not null,
	application varchar(255),
	site varchar(255),
	job_name varchar(255),
	start datetime2,
	end datetime2,
	duration INT,
	run_once BIT,
	result varchar(255),
	stacktraces NVARCHAR,
	custom_data NVARCHAR,
	triggername varchar(255),	
	primary key (id)
);
