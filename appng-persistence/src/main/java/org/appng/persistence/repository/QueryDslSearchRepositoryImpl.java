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
package org.appng.persistence.repository;

import java.io.Serializable;

import javax.persistence.EntityManager;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.JpaEntityInformationSupport;
import org.springframework.data.jpa.repository.support.QueryDslJpaRepository;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;

/**
 * Default {@link QueryDslSearchRepository} implementation.<br/>
 * Use this class as the base class for your {@link QueryDslSearchRepository} implementations:
 * 
 * <pre>
 * &lt;repositories base-class="org.appng.persistence.repository.QueryDslSearchRepositoryImpl"&gt;
 * </pre>
 * 
 * See <a href=
 * "http://docs.spring.io/spring-data/jpa/docs/1.11.0.RELEASE/reference/html/#repositories.custom-behaviour-for-all-repositories">
 * 4.6.2. Adding custom behavior to all repositories</a> from the reference Documentation for further details.
 *
 * @author Matthias Müller
 * 
 * @param <T>
 *            the domain class
 * @param <ID>
 *            the type of the Id of the domain class
 */
public class QueryDslSearchRepositoryImpl<T, ID extends Serializable> extends SearchRepositoryImpl<T, ID>
		implements QueryDslSearchRepository<T, ID> {

	private QueryDslJpaRepository<T, ID> queryDslJpaRepository;

	public QueryDslSearchRepositoryImpl(JpaEntityInformation<T, ID> entityInformation, EntityManager entityManager) {
		super(entityInformation, entityManager);
		this.queryDslJpaRepository = new QueryDslJpaRepository<T, ID>(entityInformation, entityManager);
	}

	@SuppressWarnings("unchecked")
	public QueryDslSearchRepositoryImpl(Class<T> domainType, EntityManager entityManager) {
		super(domainType, entityManager);
		JpaEntityInformation<T, ID> entityInformation = (JpaEntityInformation<T, ID>) JpaEntityInformationSupport
				.getEntityInformation(domainClass, entityManager);
		this.queryDslJpaRepository = new QueryDslJpaRepository<T, ID>(entityInformation, entityManager);
	}

	public T findOne(Predicate predicate) {
		return queryDslJpaRepository.findOne(predicate);
	}

	public Iterable<T> findAll(Predicate predicate) {
		return queryDslJpaRepository.findAll(predicate);
	}

	public Iterable<T> findAll(Predicate predicate, OrderSpecifier<?>... orders) {
		return queryDslJpaRepository.findAll(predicate, orders);
	}

	public Page<T> findAll(Predicate predicate, Pageable pageable) {
		return queryDslJpaRepository.findAll(predicate, pageable);
	}

	public long count(Predicate predicate) {
		return queryDslJpaRepository.count(predicate);
	}

	public Iterable<T> findAll(Predicate predicate, Sort sort) {
		return queryDslJpaRepository.findAll(predicate, sort);
	}

	public Iterable<T> findAll(OrderSpecifier<?>... orders) {
		return queryDslJpaRepository.findAll(orders);
	}

	public boolean exists(Predicate predicate) {
		return queryDslJpaRepository.exists(predicate);
	}

}
