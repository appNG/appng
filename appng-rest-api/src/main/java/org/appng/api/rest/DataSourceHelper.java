/*
 * Copyright 2011-2020 the original author or authors.
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
package org.appng.api.rest;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.rest.model.Datasource;
import org.appng.api.rest.model.Element;
import org.appng.api.rest.model.FieldValue;
import org.appng.api.rest.model.Filter;
import org.appng.api.rest.model.Link;
import org.appng.api.rest.model.Page;
import org.springframework.http.ResponseEntity;

/**
 * Utility class that helps dealing with {@link Datasource}s.
 */
public class DataSourceHelper {

	private ResponseEntity<Datasource> dataSource;

	public DataSourceHelper(ResponseEntity<Datasource> dataSource) {
		this.dataSource = dataSource;
	}

	public static DataSourceHelper create(ResponseEntity<Datasource> dataSource) {
		return new DataSourceHelper(dataSource);
	}

	@Deprecated
	public Optional<Element> getResult(int i) {
		return getElement(i);
	}

	public Optional<Element> getElement(int i) {
		Page page = dataSource.getBody().getPage();
		if (null != page && null != page.getElements() && i > -1 && page.getElements().size() > i) {
			return Optional.of(page.getElements().get(i));
		}
		return Optional.empty();
	}

	public Optional<Link> getLink(Element item, String linkId) {
		return item.getLinks().stream().filter(l -> l.getId().equals(linkId)).findFirst();
	}

	public Optional<FieldValue> getField(Element item, String name) {
		return getField(item.getFields(), name);
	}

	private Optional<FieldValue> getField(List<FieldValue> fields, String name) {
		if (!name.contains(".")) {
			return fields.stream().filter(f -> f.getName().equals(name)).findFirst();
		}
		String[] segments = name.split("\\.");
		Optional<FieldValue> root = fields.stream().filter(f -> f.getName().equals(segments[0])).findFirst();
		if (root.isPresent()) {
			return getField(root.get().getValues(),
					StringUtils.join(Arrays.copyOfRange(segments, 1, segments.length), "."));
		}
		return Optional.empty();
	}

	/**
	 * Retrieves the filter with the given name, if any
	 * 
	 * @param name
	 *            the name of the filter
	 * @return an {@code Optional<Filter>}
	 */
	public Optional<Filter> getFilter(String name) {
		List<Filter> filters = dataSource.getBody().getFilters();
		if (null == filters) {
			return Optional.empty();
		}
		return filters.parallelStream().filter(f -> f.getName().equals(name)).findFirst();
	}

}
