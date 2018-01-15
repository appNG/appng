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
package org.appng.persistence.repository;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.appng.persistence.model.TestEntity;
import org.appng.testsupport.persistence.ConnectionHelper;
import org.appng.testsupport.persistence.HsqlServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

public class SearchQueryTest {

	private static final String BOOLEAN_VALUE = "booleanValue";

	private static final String DOUBLE_VALUE = "doubleValue";

	private static final String NAME = "name";

	private static final String INTEGER_VALUE = "integerValue";

	private int hsqlPort;

	private EntityManager em;

	private TestEntity testEntity;

	@Before
	public void setup() {
		this.hsqlPort = ConnectionHelper.getHsqlPort();
		HsqlServer.start(hsqlPort);
		EntityManagerFactory emf = Persistence.createEntityManagerFactory("hsql-testdb");
		em = emf.createEntityManager();
		em.getTransaction().begin();

		testEntity = new TestEntity();
		testEntity.setName("abcdef");
		testEntity.setIntegerValue(5);
		em.persist(testEntity);
	}

	@After
	public void tearDown() {
		em.getTransaction().commit();
		em.close();
		HsqlServer.stop(hsqlPort);
	}

	@Test
	public void test() {
		SearchQuery<TestEntity> searchQuery = getSearchQuery(true);
		searchQuery.isNull(BOOLEAN_VALUE);
		Collection<TestEntity> page = searchQuery.execute(em);
		Assert.assertEquals(testEntity, page.iterator().next());
		Assert.assertEquals(1, page.size());
	}

	@Test
	public void testAdditionalClausesOnly() {
		SearchQuery<TestEntity> searchQuery = new SearchQuery<TestEntity>(TestEntity.class);
		searchQuery.and("1=1");

		Map<String, Object> params = new HashMap<String, Object>();
		params.put("param1", Integer.MIN_VALUE);
		searchQuery.and("e." + INTEGER_VALUE + " >= :param1", params);

		Collection<TestEntity> page = searchQuery.execute(em);
		Assert.assertEquals(testEntity, page.iterator().next());
		Assert.assertEquals(1, page.size());
	}

	@Test
	public void testAdditionalClauses() {
		SearchQuery<TestEntity> searchQuery = getSearchQuery(true);
		searchQuery.isNull(BOOLEAN_VALUE);

		Map<String, Object> params = new HashMap<String, Object>();
		params.put("param1", Integer.MIN_VALUE);
		searchQuery.and("e." + INTEGER_VALUE + " >= :param1", params);

		Collection<TestEntity> page = searchQuery.execute(em);
		Assert.assertEquals(testEntity, page.iterator().next());
		Assert.assertEquals(1, page.size());
	}

	@Test
	public void testAdditionalClauseWithOneCriteria() {
		SearchQuery<TestEntity> searchQuery = new SearchQuery<TestEntity>(TestEntity.class);
		searchQuery.isNull(BOOLEAN_VALUE);

		Map<String, Object> params = new HashMap<String, Object>();
		params.put("param1", Integer.MIN_VALUE);
		searchQuery.and("e." + INTEGER_VALUE + " >= :param1", params);

		Collection<TestEntity> page = searchQuery.execute(em);
		Assert.assertEquals(testEntity, page.iterator().next());
		Assert.assertEquals(1, page.size());
	}

	@Test
	public void testPage() {
		testEntity.setBooleanValue(true);
		SearchQuery<TestEntity> searchQuery = getSearchQuery(false);
		searchQuery.isNotNull(BOOLEAN_VALUE);
		PageRequest pageable = new PageRequest(5, 1000, new Sort(new Sort.Order(Direction.ASC, NAME), new Sort.Order(
				Direction.ASC, INTEGER_VALUE)));
		Page<TestEntity> page = searchQuery.execute(pageable, em);
		Assert.assertEquals(testEntity, page.iterator().next());
		Assert.assertEquals(1, page.getTotalElements());
	}

	private SearchQuery<TestEntity> getSearchQuery(boolean manualAppendAlias) {
		SearchQuery<TestEntity> searchQuery = new SearchQuery<TestEntity>(TestEntity.class);
		String alias = "";
		if (manualAppendAlias) {
			alias = "e.";
			searchQuery.setAppendEntityAlias(false);
		}
		searchQuery.equals(alias + NAME, null);

		searchQuery.notEquals(alias + NAME, "123");
		searchQuery.equals(alias + NAME, "abcdef");
		searchQuery.contains(alias + NAME, "cd");
		searchQuery.startsWith(alias + NAME, "ab");
		searchQuery.endsWith(alias + NAME, "ef");
		searchQuery.greaterThan(alias + INTEGER_VALUE, 4);
		searchQuery.greaterEquals(alias + INTEGER_VALUE, 5);
		searchQuery.lessThan(alias + INTEGER_VALUE, 6);
		searchQuery.lessEquals(alias + INTEGER_VALUE, 5);
		searchQuery.isNotNull(alias + INTEGER_VALUE);
		searchQuery.isNull(alias + DOUBLE_VALUE);
		searchQuery.in(alias + INTEGER_VALUE, Arrays.asList(4, 5, 6));
		searchQuery.notIn(alias + INTEGER_VALUE, Arrays.asList(10, 11, 12));
		return searchQuery;
	}

}
