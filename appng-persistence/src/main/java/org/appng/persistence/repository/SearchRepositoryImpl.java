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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.commons.lang3.Validate;
import org.appng.api.model.RevisionAware;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

/**
 * Default {@link SearchRepository} implementation.<br/>
 * Use this class as the base class for your {@link SearchRepository} implementations:
 * 
 * <pre>
 * &lt;repositories base-class="org.appng.persistence.repository.SearchRepositoryImpl"&gt;
 * </pre>
 * 
 * See <a href=
 * "http://docs.spring.io/spring-data/jpa/docs/1.11.0.RELEASE/reference/html/#repositories.custom-behaviour-for-all-repositories">
 * 4.6.2. Adding custom behavior to all repositories</a> from the reference Documentation for further details.
 *
 * @author Matthias Müller
 * 
 * @param <T>
 *             the domain class
 * @param <ID>
 *             the type of the Id of the domain class
 */
public class SearchRepositoryImpl<T, ID extends Serializable> extends SimpleJpaRepository<T, ID>
		implements SearchRepository<T, ID> {

	protected EntityManager entityManager;
	protected Class<T> domainClass;

	public SearchRepositoryImpl(JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager) {
		super(entityInformation, entityManager);
		this.domainClass = entityInformation.getJavaType();
		this.entityManager = entityManager;
	}

	public SearchRepositoryImpl(Class<T> domainType, EntityManager entityManager) {
		super(domainType, entityManager);
		this.domainClass = domainType;
		this.entityManager = entityManager;
	}

	@Override
	public Page<T> findAll(Pageable pageable) {
		return search(pageable);
	}

	public Page<T> search(Pageable pageable) {
		Page<T> page = super.findAll(pageable);
		if (pageable.getOffset() >= page.getTotalElements()) {
			Pageable newPageable = new PageRequest(0, pageable.getPageSize(), pageable.getSort());
			page = super.findAll(newPageable);
		}
		return page;
	}

	private void appendOrder(Pageable pageable, StringBuilder queryBuilder, String entityName) {
		Sort pageSort = pageable.getSort();
		if (null != pageSort) {
			boolean firstOrder = true;
			for (Order order : pageSort) {
				queryBuilder.append(firstOrder ? " order by " : ", ");
				queryBuilder.append(entityName + "." + order.getProperty() + " " + order.getDirection().name());
				firstOrder = false;
			}
		}
	}

	public Page<T> search(String queryString, String entityName, Pageable pageable, Object... args) {
		StringBuilder queryBuilder = new StringBuilder(queryString);
		StringBuilder countQueryBuilder = new StringBuilder("select count(" + entityName + ") ");
		countQueryBuilder.append(queryBuilder.toString());
		TypedQuery<Long> countQuery = entityManager.createQuery(countQueryBuilder.toString(), Long.class);

		appendOrder(pageable, queryBuilder, entityName);
		TypedQuery<T> query = entityManager.createQuery(queryBuilder.toString(), domainClass);
		if (null != args) {
			for (int i = 1; i <= args.length; i++) {
				query.setParameter(i, args[i - 1]);
				countQuery.setParameter(i, args[i - 1]);
			}
		}
		Long total = countQuery.getSingleResult();
		if (pageable.getOffset() >= total) {
			pageable = new PageRequest(0, pageable.getPageSize(), pageable.getSort());
		}
		query.setFirstResult(pageable.getOffset());
		query.setMaxResults(pageable.getPageSize());
		List<T> content = query.getResultList();
		return new PageImpl<T>(content, pageable, total);
	}

	public List<T> search(String queryString, Object... args) {
		TypedQuery<T> query = entityManager.createQuery(queryString, domainClass);
		if (null != args) {
			for (int i = 1; i <= args.length; i++) {
				query.setParameter(i, args[i - 1]);
			}
		}
		return query.getResultList();
	}

	public Page<T> search(SearchQuery<T> searchQuery, Pageable pageable) {
		return searchQuery.execute(pageable, entityManager);
	}

	@SuppressWarnings("unchecked")
	public Collection<T> getHistory(ID id) {
		AuditReader auditReader = AuditReaderFactory.get(entityManager);
		boolean entityClassAudited = auditReader.isEntityClassAudited(domainClass);
		if (!entityClassAudited) {
			return Collections.emptyList();
		}
		AuditQuery auditQuery = auditReader.createQuery().forRevisionsOfEntity(domainClass, false, false);
		auditQuery.add(AuditEntity.id().eq(id));
		List<Object[]> revisions = auditQuery.getResultList();
		List<T> result = new ArrayList<>();
		// take all revision except the last (latest) one
		for (int index = 0; index < revisions.size() - 1; index++) {
			T t = (T) revisions.get(index)[0];
			DefaultRevisionEntity entry = (DefaultRevisionEntity) revisions.get(index)[1];
			setRevision(t, entry.getId());
			result.add(t);
		}
		Collections.reverse(result);
		return result;
	}

	private void setRevision(T t, Number number) {
		if (RevisionAware.class.isAssignableFrom(domainClass)) {
			((RevisionAware) t).setRevision(number);
		}
	}

	public T getRevision(ID id, Number revision) {
		AuditReader auditReader = AuditReaderFactory.get(entityManager);
		boolean entityClassAudited = auditReader.isEntityClassAudited(domainClass);
		if (!entityClassAudited) {
			return null;
		}
		T result = auditReader.find(domainClass, id, revision);
		setRevision(result, revision);
		return result;
	}

	public Number getRevisionNumber(ID id) {
		AuditReader auditReader = AuditReaderFactory.get(entityManager);
		boolean entityClassAudited = auditReader.isEntityClassAudited(domainClass);
		if (!entityClassAudited) {
			return null;
		}
		AuditQuery auditQuery = auditReader.createQuery().forRevisionsOfEntity(domainClass, true, false);
		auditQuery.add(AuditEntity.id().eq(id));

		Set<Number> revisionNumbers = new HashSet<>(auditReader.getRevisions(domainClass, id));
		final Number number = (new ArrayList<>(revisionNumbers)).get(revisionNumbers.size() - 1);
		return number;
	}

	public boolean isUnique(ID id, String property, Object value) {
		return isUnique(id, new String[] { property }, new Object[] { value });
	}

	public boolean isUnique(ID id, String[] properties, Object[] values) {
		Validate.notEmpty(properties, "properties can not be empty!");
		Validate.notEmpty(values, "values can not be empty!");
		if (properties.length != values.length) {
			throw new IllegalArgumentException(String.format("properties has a size of %s, but values a size of %s!",
					properties.length, values.length));
		}
		SearchQuery<T> searchQuery = new SearchQuery<T>(domainClass);
		for (int i = 0; i < properties.length; i++) {
			searchQuery.equals(properties[i], values[i]);
		}
		List<T> results = searchQuery.execute(entityManager);
		if (results.isEmpty()) {
			return true;
		} else if (id != null) {
			T current = findOne(id);
			return results.size() == 1 && current.equals(results.get(0));
		} else {
			return false;
		}
	}

	public void detach(T entity) {
		entityManager.detach(entity);
	}

	public SearchQuery<T> createSearchQuery() {
		return new SearchQuery<T>(domainClass);
	}
}
