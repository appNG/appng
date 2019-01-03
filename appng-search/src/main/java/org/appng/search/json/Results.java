/*
 * Copyright 2011-2019 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlType;

@XmlType(propOrder = { "time", "layout", "pagination", "data" })
public class Results {

	private long time;
	private Page pagination;
	private FilterItem layout;
	private List<Result> data;

	public List<Result> getData() {
		if (null == data) {
			data = new ArrayList<Result>();
		}
		return data;
	}

	public void setData(List<Result> data) {
		this.data = data;
	}

	public Page getPagination() {
		return pagination;
	}

	public void setPagination(Page pagination) {
		this.pagination = pagination;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public FilterItem getLayout() {
		return layout;
	}

	public void setLayout(FilterItem layout) {
		this.layout = layout;
	}

}
