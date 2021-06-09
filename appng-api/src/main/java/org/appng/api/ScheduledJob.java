/*
 * Copyright 2011-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.appng.api;

import java.util.Map;

import org.appng.api.ScheduledJobResult.ExecutionResult;
import org.appng.api.model.Application;
import org.appng.api.model.Site;

/**
 * A {@link ScheduledJob} is a (periodically or manually triggered) task that can be defined by an {@link Application}.
 * The job's {@code jobDataMap} can contain any information that the job needs to be executed. For periodical execution,
 * there are two pre-defined entries for the {@code jobDataMap}:
 * <ul>
 * <li>{@code cronExpression}<br/>
 * A <a href="https://en.wikipedia.org/wiki/Cron#CRON_expression">cron-expression</a> describing when the
 * {@link ScheduledJob} should be executed.
 * <li>{@code enabled}<br/>
 * If set to {@code true} and the {@code cronExpression} property is present (and valid), the {@link ScheduledJob} will
 * automatically be scheduled when the platform starts.
 * <li>{@code runOnce}<br/>
 * If set to {@code true}, this job will only run once per cluster, i.e. it is not executed on each node, which is the
 * default behavior.
 * <li>{@code hardInterruptable}<br/>
 * If set to {@code true}, this job can safely be interrupted, e.g when a {@link Site} is being reloaded. This is
 * achieved by running the job in a separate thread and calling {@link Thread#interrupt()}.
 * <li>{@code allowConcurrentExecutions}<br/>
 * If set to {@code true}, multiple instances of this job can run concurrently (default is {@code false}).
 * </ul>
 * 
 * @author Matthias Müller
 */
public interface ScheduledJob {

	/**
	 * Returns the description for this job.
	 * 
	 * @return the description
	 */
	String getDescription();

	/**
	 * Sets the description for this job.
	 * 
	 * @param description
	 *                    the description
	 */
	void setDescription(String description);

	/**
	 * Returns a {@link Map} containing some data needed the execute the job
	 * 
	 * @return the {@code jobDataMap}
	 */
	Map<String, Object> getJobDataMap();

	/**
	 * Sets the {@code jobDataMap} for this job.
	 * 
	 * @param map
	 *            {@code jobDataMap}
	 */
	void setJobDataMap(Map<String, Object> map);

	/**
	 * This method actually executes the job.
	 * 
	 * @param site
	 *                    the {@link Site} to run within
	 * @param application
	 *                    the {@link Application} to run within
	 * 
	 * @throws Exception
	 *                   if any error occurs during job execution
	 */
	void execute(Site site, Application application) throws Exception;

	/**
	 * This method is called after execution of the job. It has to provide the result and optionally some custom
	 * information to be stored in the appNG database.
	 * 
	 * @return the {@code ScheduledJobResult}
	 */
	default ScheduledJobResult getResult() {
		ScheduledJobResult scheduledJobResult = new ScheduledJobResult();
		scheduledJobResult.setResult(ExecutionResult.SUCCESS);
		return scheduledJobResult;
	}
}
