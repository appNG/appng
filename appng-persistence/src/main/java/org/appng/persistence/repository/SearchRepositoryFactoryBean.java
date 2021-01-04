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

import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

/**
 * A custom {@link JpaRepositoryFactoryBean}.<br/>
 * See <a href=
 * "http://docs.spring.io/spring-data/jpa/docs/1.11.0.RELEASE/reference/html/#repositories.custom-behaviour-for-all-repositories">
 * 4.6.2. Adding custom behavior to all repositories</a> from the reference Documentation for further details.
 * 
 * @author Matthias Müller
 */
public class SearchRepositoryFactoryBean<R extends SearchRepository<T, I>, T, I extends Serializable>
		extends JpaRepositoryFactoryBean<R, T, I> {

	public SearchRepositoryFactoryBean(Class<? extends R> repositoryInterface) {
		super(repositoryInterface);
	}

	@Override
	protected RepositoryFactorySupport createRepositoryFactory(EntityManager entityManager) {
		return new SearchRepositoryFactory(entityManager);
	}

	class SearchRepositoryFactory extends JpaRepositoryFactory {

		public SearchRepositoryFactory(EntityManager entityManager) {
			super(entityManager);
		}

		@Override
		@SuppressWarnings("unchecked")
		protected SimpleJpaRepository<T, I> getTargetRepository(RepositoryInformation information,
				EntityManager entityManager) {
			Class<T> domainType = (Class<T>) information.getDomainType();
			if (QueryDslSearchRepository.class.equals(information.getRepositoryBaseClass())) {
				return new QueryDslSearchRepositoryImpl<T, I>(domainType, entityManager);
			}
			if (SearchRepository.class.equals(information.getRepositoryBaseClass())) {
				return new SearchRepositoryImpl<T, I>(domainType, entityManager);
			}
			return (SimpleJpaRepository<T, I>) super.getTargetRepository(information, entityManager);
		}

		@Override
		protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
			if (QueryDslSearchRepository.class.isAssignableFrom(metadata.getRepositoryInterface())) {
				return QueryDslSearchRepository.class;
			}
			if (SearchRepository.class.isAssignableFrom(metadata.getRepositoryInterface())) {
				return SearchRepository.class;
			}
			return super.getRepositoryBaseClass(metadata);
		}

	}

}
