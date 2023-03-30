/*
 * Copyright 2011-2023 the original author or authors.
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

import java.io.Serializable;

/**
 * A {@link ScheduledJobResult} can be provided by a {@link ScheduledJob}. The {@link ScheduledJobResult} contains some
 * information which will be stored by the scheduler application in the appNG database per job execution.
 * 
 * @author Claus Stümke
 */
@SuppressWarnings("serial")
public class ScheduledJobResult implements Serializable {

	private ExecutionResult result;
	private String customData;

	/**
	 * simple enum reflecting the results a {@link ScheduledJob} execution can have.
	 * 
	 * @author Claus Stümke
	 */
	public enum ExecutionResult {
		FAIL, SUCCESS;
	}

	/**
	 * returns the general result of the job in a semantic manner. Job abortions, because of thrown Exceptions, are
	 * handles another way.
	 * 
	 * @return execution result as {@code enum} {@link ExecutionResult}
	 */
	public ExecutionResult getResult() {
		return result;
	}

	/**
	 * a {@link ScheduledJob} can provide some application specific custom information such as statistics or log
	 * messages to be stored in the appNG Database. Serialization and de-serialization has to be done by the
	 * {@link ScheduledJob} and any application that consumes the custom information. The size is limited to the size of
	 * a TEXT field in the database type used by the running appNG instance. (~64kB on MySQL)
	 * 
	 * @return customData as {@code String}
	 */
	public String getCustomData() {
		return customData;
	}

	/**
	 * sets the general semantic result of the {@link ScheduledJob}
	 * 
	 * @param result
	 *               {@code result}
	 */
	public void setResult(ExecutionResult result) {
		this.result = result;
	}

	/**
	 * sets the custom data of a {@link ScheduledJob} execution
	 * 
	 * @param customData
	 *                   {@code customData}
	 */
	public void setCustomData(String customData) {
		this.customData = customData;
	}

}
