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
package org.appng.persistence.repository;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * 
 * A {@link SearchRepository} provides several search-methods for a persistent domain-class.
 * 
 * @author Matthias Müller
 * 
 * @param <T>
 *            the domain class
 * @param <ID>
 *            the type of the Id of the domain class
 * 
 * @see JpaRepository
 * @see JpaSpecificationExecutor
 */
@NoRepositoryBean
public interface SearchRepository<T, ID extends Serializable>
		extends JpaRepository<T, ID>, JpaSpecificationExecutor<T> {

	/**
	 * Checks whether the given property is unique for this domain type.
	 * 
	 * @param id
	 *            the ID of the entity to check, may be {@code null} if it's a new entity
	 * @param property
	 *            the name of the property to check uniqueness for, must not be {@code null}
	 * @param value
	 *            the value of the property to check uniqueness for, must not be {@code null}
	 * @return {@code true}, if there is no other entity where the given property has the given value, {@code false}
	 *         otherwise
	 */
	boolean isUnique(ID id, String property, Object value);

	/**
	 * Checks whether the given properties are unique for this domain type.
	 * 
	 * @param id
	 *            the ID of the entity to check, may be {@code null} if it's a new entity
	 * @param properties
	 *            the names of the properties to check uniqueness for, must not be {@code null}
	 * @param values
	 *            the values of the properties to check uniqueness for, must not be {@code null}
	 * @return {@code true}, if there is no other entity where the given properties have the given values, {@code false}
	 *         otherwise
	 */
	boolean isUnique(ID id, String[] properties, Object[] values);

	/**
	 * Performs a paginated search.<br/>
	 * In contrast to {@link PagingAndSortingRepository#findAll(Pageable)}, this method is able to re-calculate the
	 * pagination. This means, if {@link Pageable#getOffset()} returns a higher number than
	 * {@link Page#getTotalElements()}, a new {@link Pageable} is created, starting with page 0.
	 * 
	 * @param pageable
	 *            a {@link Pageable}
	 * @return a {@link Page} containing the result
	 */
	Page<T> search(Pageable pageable);

	/**
	 * Performs a paginated search based upon the given query-String.<b>Note that the query string must start with "from
	 * &lt;Entity> &lt;entityName>"!</b>
	 * 
	 * @param queryString
	 *            the JPQL-query, starting with "from &lt;Entity> &lt;entityName>"
	 * @param entityName
	 *            the alias used for the entity within the query.<br/>
	 *            E.g. if your query is {@code from Foo f}, then the entityName needs to be {@code f}.
	 * @param pageable
	 *            a {@link Pageable}
	 * @param params
	 *            the parameters to be applied to the {@link Query}, using {@link Query#setParameter(int, Object)}.
	 * @return a {@link Page} containing the result
	 * 
	 */
	Page<T> search(String queryString, String entityName, Pageable pageable, Object... params);

	/**
	 * Performs a search with the given query-String.
	 * 
	 * @param queryString
	 *            the JPQL-query
	 * @param params
	 *            the parameters to be applied to the {@link Query}, using {@link Query#setParameter(int, Object)}.
	 * @return a {@link Page} containing the result
	 * 
	 */
	List<T> search(String queryString, Object... params);

	/**
	 * Performs a paginated search with the given {@link SearchQuery}.
	 * 
	 * @param searchQuery
	 *            the {@link SearchQuery}
	 * @param pageable
	 *            a {@link Pageable} (optional)
	 * @return a {@link Page} containing the result. When {@code pageable} is null, a single page containing all results
	 *         will be returned
	 */
	Page<T> search(SearchQuery<T> searchQuery, Pageable pageable);

	/**
	 * Returns all previous revisions (starting with the newest) for the entity with the given ID (if the domain-class
	 * is audited).
	 * 
	 * @param id
	 *            the ID of the entity
	 * @return the previous revisions of the entity, if any
	 */
	Collection<T> getHistory(ID id);

	/**
	 * Returns the requested revision of the entity with the requested ID (if the domain-class is audited);
	 * 
	 * @param id
	 *            the ID of the entity
	 * @param revision
	 *            the revision of the entity
	 * @return the requested revision of the entity with the requested ID. If either the entity or the revision does not
	 *         exist, {@code null} is returned.
	 */
	T getRevision(ID id, Number revision);

	/**
	 * If the domain-class is audited, the latest revision number of the entity is returned.
	 * 
	 * @param id
	 *            the ID of the entity to get the revision for.
	 * @return the revision of the entity, or {@code null} if the domain-class is not audited or an entity with this ID
	 *         does not exist
	 */
	Number getRevisionNumber(ID id);

	/**
	 * Detaches the entity from the underlying {@link EntityManager}.
	 * 
	 * @param entity
	 *            the entity to detach
	 */
	void detach(T entity);

	/**
	 * Creates and returns a new {@link SearchQuery}.
	 * 
	 * @return the {@link SearchQuery}
	 */
	SearchQuery<T> createSearchQuery();

}
