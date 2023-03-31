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
package org.appng.persistence.repository;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.appng.persistence.model.TestEntity;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.domain.Specifications;

public class SearchRepositoryTest {

	private SearchRepository<TestEntity, Integer> repo;
	private Pageable pageable;
	private Sort sort;
	private SearchQuery<TestEntity> searchQuery;

	private AnnotationConfigApplicationContext ctx;

	@Before
	public void setup() {
		ctx = new AnnotationConfigApplicationContext();
		ctx.register(RepositoryConfiguration.class);
		ctx.refresh();
		repo = ctx.getBean(TestEntityRepo.class);
		TestEntity t1 = new TestEntity();
		t1.setName("name1");
		t1.setIntegerValue(1);
		TestEntity t2 = new TestEntity();
		t2.setName("name2");
		t2.setIntegerValue(2);
		TestEntity t3 = new TestEntity();
		t3.setName("name3");
		t3.setIntegerValue(3);
		repo.save(Arrays.asList(t1, t2, t3));

		Assert.assertEquals(1, repo.getRevisionNumber(t1.getId()));
		Assert.assertEquals(1, repo.getRevisionNumber(t2.getId()));
		Assert.assertEquals(1, repo.getRevisionNumber(t3.getId()));
		searchQuery = new SearchQuery<TestEntity>(TestEntity.class);
		searchQuery.contains("name", "me");
		searchQuery.startsWith("name", "na");
		searchQuery.greaterEquals("integerValue", 1);

		sort = new Sort(new Sort.Order(Direction.DESC, "name"), new Sort.Order(Direction.DESC, "integerValue"));
		pageable = new PageRequest(0, 10, sort);
	}

	@After
	public void tearDown() {
		ctx.close();
	}

	@Test
	public void testSpecifications() {
		Page<TestEntity> page = repo.findAll(Specifications.where(new Specification<TestEntity>() {
			public Predicate toPredicate(Root<TestEntity> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
				Path<Integer> path = root.get("integerValue");
				return cb.gt(path, new Integer(0));
			}
		}).and(new Specification<TestEntity>() {

			public Predicate toPredicate(Root<TestEntity> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
				Path<String> path = root.get("name");
				return cb.like(path, "name%");
			}
		}), pageable);
		validate(page);
	}

	@Test
	public void testQueryStringUnpaged() {
		List<TestEntity> items = repo.search("from TestEntity e where e.name like ?1", "%name%");
		Assert.assertEquals(3, items.size());
	}

	@Test
	public void testQueryStringPageableParams() {
		Page<TestEntity> result = repo.search("from TestEntity e where e.id > ?1", "e", pageable, -1);
		validate(result);
	}

	@Test
	public void testQueryStringPageableParamsXAlias() {
		Pageable pageRequest = new PageRequest(0, pageable.getPageSize(), new Sort(Direction.DESC, "name"));
		Page<TestEntity> result = repo.search("from TestEntity x where x.id > ?1", "x", pageRequest, -1);
		validate(result, pageRequest.getSort());
	}

	@Test
	public void testSearchQueryPageable() {
		Page<TestEntity> result = repo.search(searchQuery, pageable);
		validate(result);
		searchQuery.distinct();
		result = repo.search(searchQuery, pageable);
		validate(result);
	}

	@Test
	public void testSearchQueryPageableNoAlias() {
		searchQuery.setAppendEntityAlias(false);
		Pageable pageRequest = new PageRequest(0, pageable.getPageSize(), new Sort(Direction.DESC, "e.name"));
		Page<TestEntity> result = repo.search(searchQuery, pageRequest);
		validate(result, pageRequest.getSort());
	}

	@Test
	public void testSearchPageable() {
		Page<TestEntity> page = repo.search(new PageRequest(5, 2, sort));
		Assert.assertEquals(0, page.getNumber());
		Assert.assertEquals(2, page.getNumberOfElements());
		Assert.assertEquals(2, page.getSize());
		Assert.assertEquals(3, page.getTotalElements());
		Assert.assertEquals(2, page.getTotalPages());
		Assert.assertEquals(sort, page.getSort());
	}

	@Test
	public void testGetHistory() {
		createEntityRevisions();
		Collection<TestEntity> history = repo.getHistory(1);
		Assert.assertEquals(0, history.size());
		history = repo.getHistory(3);
		Assert.assertEquals(2, history.size());
		Iterator<TestEntity> iterator = history.iterator();
		TestEntity v1 = iterator.next();
		TestEntity v2 = iterator.next();
		Assert.assertEquals("name3", v1.getName());
		Assert.assertEquals("name3", v2.getName());
		Assert.assertEquals(Boolean.TRUE, v1.getBooleanValue());
		Assert.assertNull(v2.getBooleanValue());
		Number rev = repo.getRevisionNumber(3);
		Assert.assertEquals(Integer.valueOf(3), rev);
		TestEntity latest = repo.getRevision(3, rev);
		Assert.assertEquals("foo", latest.getName());
		Assert.assertEquals(Integer.valueOf(3), latest.getIntegerValue());
		Assert.assertEquals(Boolean.TRUE, latest.getBooleanValue());
	}

	private void createEntityRevisions() {
		TestEntity e = repo.findOne(3);
		e.setBooleanValue(true);
		repo.save(e);

		e = repo.findOne(3);
		e.setName("foo");
		repo.save(e);
	}

	@Test
	public void testGetRevision() {
		createEntityRevisions();

		TestEntity revision = repo.getRevision(3, 1);
		Assert.assertEquals(Integer.valueOf(1), revision.getRevision());
		Assert.assertEquals("name3", revision.getName());
		Assert.assertEquals(null, revision.getBooleanValue());

		revision = repo.getRevision(3, 2);
		Assert.assertEquals(Integer.valueOf(2), revision.getRevision());
		Assert.assertEquals("name3", revision.getName());
		Assert.assertEquals(true, revision.getBooleanValue().booleanValue());

		revision = repo.getRevision(3, 3);
		Assert.assertEquals(Integer.valueOf(3), revision.getRevision());
		Assert.assertEquals("foo", revision.getName());
		Assert.assertEquals(true, revision.getBooleanValue().booleanValue());
	}

	@Test
	public void testGetRevisionNumber() {
		createEntityRevisions();
		Assert.assertEquals(1, repo.getRevisionNumber(1));
		Assert.assertEquals(1, repo.getRevisionNumber(2));
		Assert.assertEquals(3, repo.getRevisionNumber(3));
	}

	@Test
	public void testUniqueSameEntity() {
		Assert.assertTrue(repo.isUnique(3, "name", "name3"));
	}

	@Test
	public void testUniqueNewEntity() {
		Assert.assertFalse(repo.isUnique(null, "name", "name3"));
	}

	@Test
	public void testUniqueOtherEntity() {
		Assert.assertFalse(repo.isUnique(2, "name", "name3"));
	}

	@Test
	public void testUnique() {
		Assert.assertTrue(repo.isUnique(4, "name", "name4"));
	}

	@Test(expected = InvalidDataAccessApiUsageException.class)
	public void testUniqueArgMismatch() {
		repo.isUnique(4, new String[] { "name" }, new String[] { "a", "b" });
	}

	@Test
	public void testSearchQuery() {
		Page<TestEntity> page = repo.search(repo.createSearchQuery().like("name", "%name%"), new PageRequest(0, 10));
		Assert.assertEquals(3L, page.getTotalElements());
	}

	private void validate(Page<TestEntity> result) {
		validate(result, sort);
	}

	private void validate(Page<TestEntity> result, Sort sort) {
		Assert.assertEquals(0, result.getNumber());
		Assert.assertEquals(3, result.getNumberOfElements());
		Assert.assertEquals(10, result.getSize());
		Assert.assertEquals(3, result.getTotalElements());
		Assert.assertEquals(1, result.getTotalPages());
		Assert.assertEquals(sort, result.getSort());
		Assert.assertEquals("name3", result.getContent().get(0).getName());
		Assert.assertEquals("name2", result.getContent().get(1).getName());
		Assert.assertEquals("name1", result.getContent().get(2).getName());
	}
}
