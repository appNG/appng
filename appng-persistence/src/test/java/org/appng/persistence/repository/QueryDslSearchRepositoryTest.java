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

import org.appng.persistence.model.QTestEntity;
import org.appng.persistence.model.TestEntity;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.querydsl.core.types.dsl.BooleanExpression;

public class QueryDslSearchRepositoryTest {

	private QueryDslSearchRepository<TestEntity, Integer> repo;
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
		repo.save(t1);
		TestEntity t2 = new TestEntity();
		t2.setName("name2");
		t2.setIntegerValue(2);
		repo.save(t2);
		TestEntity t3 = new TestEntity();
		t3.setName("name3");
		t3.setIntegerValue(3);
		repo.save(t3);
	}

	@Test
	public void testRepo() {
		BooleanExpression like = QTestEntity.testEntity.name.like("%ame1");
		TestEntity bob1 = repo.findOne(like);
		TestEntity bob2 = repo.findOne(like);
		Assert.assertEquals(bob1, bob2);

		Iterable<TestEntity> byNameLike = repo.findAll(QTestEntity.testEntity.name.like("name%"));
		int i = 0;
		for (TestEntity testEntity : byNameLike) {
			Assert.assertEquals("name" + (++i), testEntity.getName());
		}
		Assert.assertEquals(3, i);
	}

	@After
	public void tearDown() {
		ctx.close();
	}
}
