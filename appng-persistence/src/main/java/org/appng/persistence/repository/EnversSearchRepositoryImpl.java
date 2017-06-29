/*
 * Copyright 2011-2017 the original author or authors.
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
import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.DefaultRevisionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.envers.repository.support.EnversRevisionRepositoryImpl;
import org.springframework.data.envers.repository.support.ReflectionRevisionEntityInformation;
import org.springframework.data.history.Revision;
import org.springframework.data.history.Revisions;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.data.repository.history.support.RevisionEntityInformation;

/**
 * Default {@link EnversSearchRepository} implementation.<br/>
 * Use this class as the base class for your {@link EnversSearchRepository} implementations:
 * 
 * <pre>
 * &lt;repositories base-class="org.appng.persistence.repository.EnversSearchRepositoryImpl"&gt;
 * </pre>
 * 
 * See <a href=
 * "http://docs.spring.io/spring-data/jpa/docs/1.11.0.RELEASE/reference/html/#repositories.custom-behaviour-for-all-repositories">
 * 4.6.2. Adding custom behavior to all repositories</a> from the reference Documentation for further details.
 * 
 * @author Claus Stuemke
 * 
 * @param <T>
 *            the domain class
 * @param <ID>
 *            the type of the Id of the domain class
 * @param <N>
 *            the type of the revision
 * 
 */
public class EnversSearchRepositoryImpl<T, ID extends Serializable, N extends Number & Comparable<N>>
		extends SearchRepositoryImpl<T, ID> implements EnversSearchRepository<T, ID, N> {

	RevisionRepository<T, ID, N> revisionRepository;

	private EntityInformation<T, ?> entityInformation;

	/**
	 * Can be overwritten by extending class to support alternative RevisionEntity implementations
	 * 
	 */
	protected Class<?> getRevisionEntity() {
		return DefaultRevisionEntity.class;
	}

	public EnversSearchRepositoryImpl(JpaEntityInformation<T, ID> entityInformation, EntityManager entityManager) {
		super(entityInformation, entityManager);
		this.entityInformation = entityInformation;
		RevisionEntityInformation revisionEntityInformation = new ReflectionRevisionEntityInformation(
				getRevisionEntity());
		this.revisionRepository = new EnversRevisionRepositoryImpl<T, ID, N>(entityInformation,
				revisionEntityInformation, entityManager);
	}

	public Revision<N, T> findRevision(ID id, N revisionNumber) {
		return revisionRepository.findRevision(id, revisionNumber);
	}

	public Revision<N, T> findLastChangeRevision(ID id) {
		return revisionRepository.findLastChangeRevision(id);
	}

	private boolean entityHasRevisions(ID id) {
		Class<T> type = entityInformation.getJavaType();
		AuditReader reader = AuditReaderFactory.get(entityManager);
		List<Number> revisionNumbers = reader.getRevisions(type, id);
		return !revisionNumbers.isEmpty();
	}

	public Page<Revision<N, T>> findRevisions(ID id, Pageable pageable) {
		// this is a awful workaround for an issue inside EnversRevisionRepository. When there is no revision at all,
		// what might happen if you extend existing data with auditoring, a sql query is generated with wrong syntax
		// https://github.com/spring-projects/spring-data-envers/issues/7
		if (entityHasRevisions(id)) {
			Page<Revision<N, T>> findRevisions = revisionRepository.findRevisions(id, pageable);
			return findRevisions;
		}
		// otherwise return empty page
		return new PageImpl<Revision<N, T>>(Collections.<Revision<N, T>> emptyList(), pageable, 0);
	}

	public Revisions<N, T> findRevisions(ID id) {
		Revisions<N, T> findRevisions = revisionRepository.findRevisions(id);
		return findRevisions;
	}
}
