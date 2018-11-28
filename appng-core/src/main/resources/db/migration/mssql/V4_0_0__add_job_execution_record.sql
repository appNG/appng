create table job_execution_record (
	id INT identity not null,
	application varchar(255),
	site varchar(255),
	job_name varchar(255),
	start_time datetime2,
	end_time datetime2,
	duration INT,
	run_once BIT,
	result varchar(255),
	stacktraces NVARCHAR(max),
	custom_data NVARCHAR(max),
	triggername varchar(255),	
	primary key (id)
);
