package org.appng.core.domain;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

import lombok.Data;

@Data
@Entity
@Table(name = "job_execution_record")
public class JobExecutionRecord {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	private String site;
	private String application;
	@Column(name = "job_name")
	private String jobName;
	private String result;
	@Lob
	private String stacktraces;
	@Lob
	@Column(name = "custom_data")
	private String customData;
	private String triggername;
	private Integer duration;
	@Column(name = "run_once")
	private boolean runOnce;
	@Column(name = "start_time")
	private Date startTime;
	@Column(name = "end_time")
	private Date endTime;
}
