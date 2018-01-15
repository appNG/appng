/*
 * Copyright 2011-2018 the original author or authors.
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
package org.appng.search.json;

public class FilterData {

	private String label;
	private boolean active;
	private String value;
	private Integer resultsCount;

	public FilterData() {

	}

	public FilterData(String label, boolean active, String value, Integer resultsCount) {
		super();
		this.label = label;
		this.active = active;
		this.value = value;
		this.resultsCount = resultsCount;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public Integer getResultsCount() {
		return resultsCount;
	}

	public void setResultsCount(Integer resultsCount) {
		this.resultsCount = resultsCount;
	}

}
