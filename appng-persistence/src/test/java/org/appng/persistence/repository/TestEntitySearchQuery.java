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
package org.appng.persistence.repository;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.appng.persistence.model.TestEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class TestEntitySearchQuery extends SearchQuery<TestEntity> {

	public TestEntitySearchQuery() {
		super(TestEntity.class);
		setEntityAlias("l");
	}

	@Override
	public Page<TestEntity> execute(Pageable pageable, EntityManager entityManager) {
		return super.execute(pageable, entityManager);
	}

	public List<TestEntity> execute(EntityManager entityManager, Sort sort) {
		StringBuilder sb = buildQueryString();
		String distinctPart = distinct ? "distinct " + entityAlias : entityAlias;
		appendOrder(sort, sb, appendEntityAlias ? entityAlias : "");
		TypedQuery<TestEntity> query = entityManager.createQuery("select " + distinctPart + " " + sb.toString(),
				domainClass);
		setQueryParameters(query);
		return query.getResultList();
	}
}
