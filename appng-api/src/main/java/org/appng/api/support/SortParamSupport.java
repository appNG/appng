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
package org.appng.api.support;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;

/**
 * 
 * Supports converting a {@link Pageable} to its {@link String}-representation and vice versa.<br/>
 * The following example illustrates how this works:
 * 
 * <pre>
 * Sort sort = Sort.by(new Order(Direction.DESC, &quot;name&quot;), new Order(Direction.ASC, &quot;id&quot;));
 * PageRequest pageable = PageRequest.of(1, 20, sort);
 * org.junit.Assert.assertEquals(&quot;name:desc;id:asc;page:1;pageSize:20&quot;, sortParamSupport.getSortString(pageable));
 * </pre>
 * 
 * @author Matthias Müller
 * 
 */
public class SortParamSupport {

	private static final String ALPHA_NUM = "[a-zA-Z]";
	private static final String DIGIT = "\\d+";
	private static final String ASSIGN = ":";
	private static final String MODE_RESET = "reset";
	private static final String PARAM_PAGE_SIZE = "pageSize";
	private static final String PARAM_PAGE = "page";
	private static final int DEFAULT_PAGE = 0;
	private static final String SORT_PREFIX = "sort";
	private static final String SEPARATOR = ";";
	private Map<String, String> sessionMap;
	private String pageId;
	private String dsId;
	private boolean reset = false;
	private int defaultPagesize;

	private boolean isPageSet = false;
	private boolean isPageSizeSet = false;
	private List<String> removedProperties = new ArrayList<>();

	private Pattern paramPattern = Pattern.compile("((" + ALPHA_NUM + "+(\\." + ALPHA_NUM + "+)*)(" + ASSIGN
			+ "(asc|desc))?)" + SEPARATOR + "?");
	private Pattern pagePattern = Pattern.compile("(" + PARAM_PAGE + ASSIGN + ")(" + DIGIT + ")");
	private Pattern pageSizePattern = Pattern.compile("(" + PARAM_PAGE_SIZE + ASSIGN + ")(" + DIGIT + ")");

	SortParamSupport(final Map<String, String> applicationSessionParams, String pageId, String dsId,
			Integer defaultPageSize) {
		this.sessionMap = applicationSessionParams;
		this.pageId = pageId;
		this.dsId = dsId;
		this.defaultPagesize = defaultPageSize;
	}

	Pageable getPageable(String string) {
		if (MODE_RESET.equals(string) || StringUtils.endsWith(string, MODE_RESET)) {
			reset = true;
		}
		String key = getSessionKey();
		String currentOrder = sessionMap.get(key);
		Pageable currentParams = parseParamsToPageable(currentOrder, false);
		Pageable parseParams = parseParamsToPageable(string, true);
		Pageable mergeOrderParams = mergeOrderParams(currentParams, parseParams);
		String sortString = getSortString(mergeOrderParams);
		sessionMap.put(key, sortString);
		return mergeOrderParams;
	}

	private Pageable parseParamsToPageable(String string, boolean writeAttributes) {
		int pageSize = defaultPagesize;
		int page = DEFAULT_PAGE;

		List<Order> orders = new ArrayList<>();
		if (null != string) {
			String pagePart = findGroup(pagePattern, string);
			if (null != pagePart) {
				page = Integer.parseInt(pagePart);
				isPageSet = writeAttributes;
			}
			String pageSizePart = findGroup(pageSizePattern, string);
			if (null != pageSizePart) {
				pageSize = Integer.parseInt(pageSizePart);
				isPageSizeSet = writeAttributes;
			}
			Matcher matcher = paramPattern.matcher(string);
			while (matcher.find()) {
				String property = matcher.group(2);
				if (null != property) {
					String direction = matcher.group(5);
					if (StringUtils.isNotBlank(direction)) {
						Order order = new Order(Direction.valueOf(direction.toUpperCase()), property);
						orders.add(order);
					} else if (writeAttributes) {
						removedProperties.add(property);
					}
				}
			}
		}
		if (orders.isEmpty()) {
			return PageRequest.of(page, pageSize);
		} else {
			return PageRequest.of(page, pageSize, Sort.by(orders));
		}
	}

	private String getSessionKey() {
		return SORT_PREFIX + "_" + pageId + "_" + dsId;
	}

	String getSortString(Pageable pageable) {
		StringBuilder sb = new StringBuilder();
		Sort sort = pageable.getSort();
		if (null != sort) {
			for (Order order : sort) {
				sb.append(order.getProperty());
				sb.append(ASSIGN);
				sb.append(order.getDirection().name().toLowerCase());
				sb.append(SEPARATOR);
			}
		}
		sb.append(PARAM_PAGE + ASSIGN);
		sb.append(pageable.getPageNumber());
		sb.append(SEPARATOR);
		sb.append(PARAM_PAGE_SIZE + ASSIGN);
		sb.append(pageable.getPageSize());
		return sb.toString();
	}

	private Pageable mergeOrderParams(Pageable currentParams, Pageable parseParams) {
		Map<String, Integer> positions = new HashMap<>();
		List<Order> mergedOrders = new ArrayList<>(0);
		int pos = 0;

		Sort currentSort = currentParams.getSort();
		if (!(reset || null == currentSort)) {
			for (Order order : currentSort) {
				String property = order.getProperty();
				if (!removedProperties.contains(property)) {
					positions.put(property, pos++);
					mergedOrders.add(new Order(order.getDirection(), property));
				}
			}
		}
		pos = 0;
		Sort parsedSort = parseParams.getSort();
		if (null != parsedSort) {
			for (Order order : parsedSort) {
				Integer index = positions.get(order.getProperty());
				if (index != null) {
					mergedOrders.set(index.intValue(), order);
				} else {
					mergedOrders.add(order);
				}
			}
		}
		int pageNumber = isPageSet ? parseParams.getPageNumber() : currentParams.getPageNumber();
		int pageSize = isPageSizeSet ? parseParams.getPageSize() : currentParams.getPageSize();
		if (mergedOrders.isEmpty()) {
			return PageRequest.of(pageNumber, pageSize);
		} else {
			return PageRequest.of(pageNumber, pageSize, Sort.by(mergedOrders));
		}

	}

	private String findGroup(Pattern pattern, String string) {
		if (null != string) {
			Matcher matcher = pattern.matcher(string);
			if (matcher.find()) {
				return matcher.group(2);
			}
		}
		return null;
	}

	String getRequestKey() {
		return SORT_PREFIX + StringUtils.capitalize(dsId);
	}

}
