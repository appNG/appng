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
package org.appng.search.json;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.appng.api.model.Named;

public class FilterItem {

	private FilterConfig config;
	private List<FilterData> data = new ArrayList<>();

	public FilterConfig getConfig() {
		return config;
	}

	public void setConfig(FilterConfig config) {
		this.config = config;
	}

	public List<FilterData> getData() {
		return data;
	}

	public void setData(List<FilterData> data) {
		this.data = data;
	}

	public interface DataCounter {
		int count(Serializable itemId);

		int getMaxItems();
	}

	public static <ID extends Serializable, T extends Named<ID>> FilterItem getFilterItem(String name, String label,
			String type, Iterable<T> items, Collection<ID> selectedList, DataCounter counter) {
		FilterItem item = new FilterItem();
		FilterConfig filterConfig = new FilterConfig();
		filterConfig.setLabel(label);
		filterConfig.setName(name);
		filterConfig.setType(type);
		filterConfig.setVisibleItemsCount(counter.getMaxItems());
		item.setConfig(filterConfig);
		int count = 0;
		for (T named : items) {
			Serializable id = named.getId();
			int itemCount = counter.count(id);
			if (itemCount > 0) {
				FilterData data = new FilterData();
				data.setActive(selectedList.contains(id));
				data.setLabel(named.getName());
				data.setValue(id.toString());
				data.setResultsCount(itemCount);
				item.getData().add(data);
			}
			count++;
		}
		filterConfig.setItemsCount(count);
		return item;
	}

}
