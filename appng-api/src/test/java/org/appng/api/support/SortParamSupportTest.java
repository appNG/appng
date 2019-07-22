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
package org.appng.api.support;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;

/**
 * Test for {@link SortParamSupport}
 * 
 * @author Matthias Müller
 * 
 */
public class SortParamSupportTest {

	static String pageId = "pageId";
	static String dsId = "dsId";
	private static final String SESS_PARAM_ID = "sort_" + pageId + "_" + dsId;

	@SuppressWarnings("unchecked")
	Map<String, String> sessionMap = Mockito.mock(Map.class);
	SortParamSupport sortParamSupport = new SortParamSupport(sessionMap, pageId, dsId, 10);

	@Test
	public void testFromSession() {
		Mockito.when(sessionMap.get(SESS_PARAM_ID)).thenReturn("lastname:asc;name:desc");
		Pageable sortParams = sortParamSupport.getPageable(null);
		Sort sort = sortParams.getSort();
		Assert.assertEquals(new Order(Direction.ASC, "lastname"), sort.getOrderFor("lastname"));
		Assert.assertEquals(new Order(Direction.DESC, "name"), sort.getOrderFor("name"));
		Assert.assertEquals("lastname:asc;name:desc;page:0;pageSize:10", sortParamSupport.getSortString(sortParams));
	}

	@Test
	public void testWithPageParams() {
		Mockito.when(sessionMap.get(SESS_PARAM_ID)).thenReturn("type:asc;lastname:asc;name:asc;id:asc;page:4;pageSize:5");
		Pageable fromSession = sortParamSupport.getPageable(null);
		Assert.assertEquals(new Sort(Direction.ASC, "type", "lastname", "name", "id"), fromSession.getSort());
		Assert.assertEquals(4, fromSession.getPageNumber());
		Assert.assertEquals(5, fromSession.getPageSize());
		Mockito.when(sessionMap.get(SESS_PARAM_ID)).thenReturn("type:desc");
		Pageable pageable = sortParamSupport.getPageable("name:desc;lastname:desc;id:desc;page:5;pageSize:20");
		Assert.assertEquals(new Sort(Direction.DESC, "type", "name", "lastname", "id"), pageable.getSort());
		Assert.assertEquals(5, pageable.getPageNumber());
		Assert.assertEquals(20, pageable.getPageSize());

		Assert.assertEquals("type:desc;name:desc;lastname:desc;id:desc;page:5;pageSize:20",
				sortParamSupport.getSortString(pageable));

		Mockito.when(sessionMap.get(SESS_PARAM_ID)).thenReturn("name:asc");
		Pageable pageable2 = sortParamSupport.getPageable("name;page:3;pageSize:50");
		Assert.assertNull(pageable2.getSort());
		Assert.assertEquals(3, pageable2.getPageNumber());
		Assert.assertEquals(50, pageable2.getPageSize());

		Pageable pageable3 = sortParamSupport.getPageable("reset");
		Assert.assertNull(pageable3.getSort());
		Assert.assertEquals(0, pageable3.getPageNumber());
		Assert.assertEquals(10, pageable3.getPageSize());
	}

	@Test
	public void test() {
		Mockito.when(sessionMap.get(SESS_PARAM_ID)).thenReturn("lastname:asc;name:desc;");
		Pageable sortParams = sortParamSupport.getPageable("name:desc;lastname;id:asc");
		Sort sort = sortParams.getSort();
		Assert.assertEquals(new Order(Direction.DESC, "name"), sort.getOrderFor("name"));
		Assert.assertEquals(new Order(Direction.ASC, "id"), sort.getOrderFor("id"));
		Assert.assertEquals("name:desc;id:asc;page:0;pageSize:10", sortParamSupport.getSortString(sortParams));
	}

	@Test
	public void testKeepPrio() {
		Mockito.when(sessionMap.get(SESS_PARAM_ID)).thenReturn("lastname:asc;name:desc;");
		Pageable sortParams = sortParamSupport.getPageable("lastname:desc");
		Sort sort = sortParams.getSort();
		Assert.assertEquals(new Order(Direction.DESC, "name"), sort.getOrderFor("name"));
		Assert.assertEquals(new Order(Direction.DESC, "lastname"), sort.getOrderFor("lastname"));
		Assert.assertEquals("lastname:desc;name:desc;page:0;pageSize:10", sortParamSupport.getSortString(sortParams));
	}

	@Test
	public void testOverridePrio() {
		Mockito.when(sessionMap.get(SESS_PARAM_ID)).thenReturn("lastname:asc;name:desc;");
		Pageable sortParams = sortParamSupport.getPageable("name:asc;lastname:desc;reset");
		Sort sort = sortParams.getSort();
		Assert.assertEquals(new Order(Direction.ASC, "name"), sort.getOrderFor("name"));
		Assert.assertEquals(new Order(Direction.DESC, "lastname"), sort.getOrderFor("lastname"));
		Assert.assertEquals("name:asc;lastname:desc;page:0;pageSize:10", sortParamSupport.getSortString(sortParams));
	}

	@Test
	public void testToggle() {
		Mockito.when(sessionMap.get(SESS_PARAM_ID)).thenReturn("lastname:asc");
		Pageable sortParams = sortParamSupport.getPageable("lastname:desc");
		Sort sort = sortParams.getSort();
		Assert.assertEquals(new Order(Direction.DESC, "lastname"), sort.getOrderFor("lastname"));
		Assert.assertEquals("lastname:desc;page:0;pageSize:10", sortParamSupport.getSortString(sortParams));
	}

	@Test
	public void testClear() {
		Mockito.when(sessionMap.get(SESS_PARAM_ID)).thenReturn("lastname:asc;name:desc;id:desc");
		Pageable sortParams = sortParamSupport.getPageable("lastname:;name:;id:");
		Assert.assertNull(sortParams.getSort());
	}

}
