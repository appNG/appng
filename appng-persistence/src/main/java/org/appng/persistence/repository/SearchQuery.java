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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;

/**
 * A {@link SearchQuery} can be used to perform JPA-queries for JPA entities. Therefore multiple criteria can be added,
 * which are being used in the WHERE-clause of the query (with an and-concatenation).<br />
 * Example:<br/>
 * To retrieve all male users aged over 30 whose name starts with "John", the following query could be build:
 * 
 * <pre>
 * SearchQuery&lt;User&gt; searchQuery = userRepository.createSearchQuery();
 * searchQuery.startsWith(&quot;name&quot;, &quot;John&quot;).equals(&quot;gender&quot;, &quot;male&quot;).greaterThan(&quot;age&quot;, 30);
 * </pre>
 * 
 * This would result in a JPA-query like this:
 * 
 * <pre>
 * from User e where e.name like ?1 and e.gender = ?2 and e.age > ?3
 * </pre>
 * 
 * The query can then be executed by calling {@link #execute(javax.persistence.EntityManager)} or
 * {@link #execute(org.springframework.data.domain.Pageable, javax.persistence.EntityManager)} .
 * <p/>
 * <b> Note that the methods which are used to add a criteria are null-safe, which means the criteria is ignored if the
 * given value is {@code null}.</b>
 * </p>
 * 
 * @author Matthias Müller
 * @param T
 *            the JPA {@link Entity}-type
 * 
 * @see SearchRepository#search(SearchQuery, Pageable)
 */
public class SearchQuery<T> {

	private List<SearchCriteria> criteria = new ArrayList<SearchCriteria>();
	private Class<T> domainClass;
	private boolean distinct;
	private String joinQuery;
	private boolean appendEntityAlias = true;
	private List<Clause> andClauses = new ArrayList<SearchQuery<T>.Clause>();

	/**
	 * Creates a new {@link SearchQuery} for the given type.
	 * 
	 * @param domainClass
	 *            the type of the JPA {@link Entity}
	 */
	public SearchQuery(Class<T> domainClass) {
		this.domainClass = domainClass;
	}

	/**
	 * Checks whether or not to add the entity alias {@code e.} for each criteria in the JPQL query. Default is
	 * {@code true} .
	 * 
	 * @return {@code true} when entity alias gets appended, {@code false} otherwise
	 */
	public boolean isAppendEntityAlias() {
		return appendEntityAlias;
	}

	/**
	 * Adds a parameterized JPQL AND-clause to this query. Avoid using the same parameter names for different calls of
	 * this method.<br/>
	 * Usage example:
	 * 
	 * <pre>
	 * SearchQuery query= repository.createSearchQuery();
	 * Map&lt;String, Object> params = new HashMap&ltString, Object>();
	 * params.put(&quot;date&quot;, new Date());
	 * query.and(&quot;(e.valid_from before :date or e.valid_from is null)&quot;, params);
	 * </pre>
	 * 
	 * @param clause
	 *            the AND clause
	 * @param params
	 *            the named parameters for the AND-clause
	 * 
	 * @see #and(String)
	 * @see #setAppendEntityAlias(boolean)
	 */
	public void and(String clause, Map<String, Object> params) {
		andClauses.add(new Clause(clause, params));
	}

	/**
	 * Adds a parameterless JPQL AND-clause to this query.<br/>
	 * Usage example:
	 * 
	 * <pre>
	 * SearchQuery query = repository.createSearchQuery();
	 * query.and(&quot;(e.valid_from before now() or e.valid_from is null)&quot;);
	 * </pre>
	 * 
	 * @param clause
	 *            the AND-clause
	 * @see #and(String, Map)
	 * @see #setAppendEntityAlias(boolean)
	 */
	public void and(String clause) {
		and(clause, new HashMap<String, Object>());
	}

	/**
	 * Set to {@code false} to avoid adding the entity alias {@code e.} for each criteria in the JPQL query.
	 * 
	 * @param appendEntityAlias
	 * @see #isAppendEntityAlias()
	 */
	public void setAppendEntityAlias(boolean appendEntityAlias) {
		this.appendEntityAlias = appendEntityAlias;
	}

	/**
	 * Checks if the attribute <b>equals</b> the given value.<br/>
	 * JPQL-fragment:<br/>
	 * 
	 * <pre>
	 * e.name = :value
	 * </pre>
	 * 
	 * @param name
	 *            the name of the attribute
	 * @param value
	 *            the value to check
	 * @return the current {@link SearchQuery}
	 */
	public final SearchQuery<T> equals(String name, Object value) {
		add(name, value, Operand.EQ, true);
		return this;
	}

	/**
	 * Checks if the attribute <b>not equals</b> the given value.<br/>
	 * JPQL-fragment:<br/>
	 * 
	 * <pre>
	 * e.name != :value
	 * </pre>
	 * 
	 * @param name
	 *            the name of the attribute
	 * @param value
	 *            the value to check
	 * @return the current {@link SearchQuery}
	 */
	public final SearchQuery<T> notEquals(String name, Object value) {
		add(name, value, Operand.NE, true);
		return this;
	}

	/**
	 * Checks if the attribute is <b>greater than</b> the given value.<br/>
	 * JPQL-fragment:<br/>
	 * 
	 * <pre>
	 * e.name > :value
	 * </pre>
	 * 
	 * @param name
	 *            the name of the attribute
	 * @param value
	 *            the value to check
	 * @return the current {@link SearchQuery}
	 */
	public final SearchQuery<T> greaterThan(String name, Object value) {
		add(name, value, Operand.GT, true);
		return this;
	}

	/**
	 * Checks if the attribute is <b>greater or equals</b> the given value.<br/>
	 * JPQL-fragment:<br/>
	 * 
	 * <pre>
	 * e.name >= :value
	 * </pre>
	 * 
	 * @param name
	 *            the name of the attribute
	 * @param value
	 *            the value to check
	 * @return the current {@link SearchQuery}
	 */
	public final SearchQuery<T> greaterEquals(String name, Object value) {
		add(name, value, Operand.GE, true);
		return this;
	}

	/**
	 * Checks if the attribute is <b>less than</b> the given value.<br/>
	 * JPQL-fragment:<br/>
	 * 
	 * <pre>
	 * e.name < :value
	 * </pre>
	 * 
	 * @param name
	 *            the name of the attribute
	 * @param value
	 *            the value to check
	 * @return the current {@link SearchQuery}
	 */
	public final SearchQuery<T> lessThan(String name, Object value) {
		add(name, value, Operand.LT, true);
		return this;
	}

	/**
	 * Checks if the attribute is <b>less or equals</b> the given value.<br/>
	 * JPQL-fragment:<br/>
	 * 
	 * <pre>
	 * e.name <= :value
	 * </pre>
	 * 
	 * @param name
	 *            the name of the attribute
	 * @param value
	 *            the value to check
	 * @return the current {@link SearchQuery}
	 */
	public final SearchQuery<T> lessEquals(String name, Object value) {
		add(name, value, Operand.LE, true);
		return this;
	}

	/**
	 * Checks if the attribute is <b>in</b> the given values.<br/>
	 * JPQL-fragment:<br/>
	 * 
	 * <pre>
	 * e.name in :value
	 * </pre>
	 * 
	 * @param name
	 *            the name of the attribute
	 * @param values
	 *            the values to check
	 * @return the current {@link SearchQuery}
	 */
	public final SearchQuery<T> in(String name, Collection<?> values) {
		add(name, values == null ? null : (values.isEmpty() ? null : values), Operand.IN, true);
		return this;
	}

	/**
	 * Checks if the attribute is <b>not in</b> the given values.<br/>
	 * JPQL-fragment:<br/>
	 * 
	 * <pre>
	 * e.name not in :value
	 * </pre>
	 * 
	 * @param name
	 *            the name of the attribute
	 * @param values
	 *            the values to check
	 * @return the current {@link SearchQuery}
	 */
	public final SearchQuery<T> notIn(String name, Collection<?> values) {
		add(name, values == null ? null : (values.isEmpty() ? null : values), Operand.NOT_IN, true);
		return this;
	}

	/**
	 * Checks if the attribute is <b>contains</b> the given value.<br/>
	 * JPQL-fragment:<br/>
	 * 
	 * <pre>
	 * e.name like :value
	 * </pre>
	 * 
	 * <b>You don't have to add the wildcard '{@code %}' before and after the {@code value}, this is done
	 * internally!</b>
	 * 
	 * @param name
	 *            the name of the attribute
	 * @param value
	 *            the value to check
	 * @return the current {@link SearchQuery}
	 */
	public final SearchQuery<T> contains(String name, Object value) {
		add(name, value == null ? null : ("%" + value + "%"), Operand.LIKE, true);
		return this;
	}

	/**
	 * Checks if the attribute is <b>like</b> the given value.<br/>
	 * JPQL-fragment:<br/>
	 * 
	 * <pre>
	 * e.name like :value
	 * </pre>
	 * 
	 * <b>The {@code value} should contain some wildcards ('{@code %}').</b>
	 * 
	 * @param name
	 *            the name of the attribute
	 * @param value
	 *            the value to check
	 * @return the current {@link SearchQuery}
	 * @see #startsWith(String, Object)
	 * @see #endsWith(String, Object)
	 */
	public final SearchQuery<T> like(String name, Object value) {
		add(name, value == null ? null : (value), Operand.LIKE, true);
		return this;
	}

	/**
	 * Checks if the attribute is <b>not like</b> the given value.<br/>
	 * JPQL-fragment:<br/>
	 * 
	 * <pre>
	 * e.name not like :value
	 * </pre>
	 * 
	 * <b>The {@code value} should contain some wildcards ('{@code %}').</b>
	 * 
	 * @param name
	 *            the name of the attribute
	 * @param value
	 *            the value to check
	 * @return the current {@link SearchQuery}
	 * @see #startsWith(String, Object)
	 * @see #endsWith(String, Object)
	 */
	public final SearchQuery<T> notLike(String name, Object value) {
		add(name, value == null ? null : (value), Operand.NOT_LIKE, true);
		return this;
	}

	/**
	 * Checks if the attribute is <b>starts with</b> the given value.<br/>
	 * JPQL-fragment:<br/>
	 * 
	 * <pre>
	 * e.name like :value
	 * </pre>
	 * 
	 * <b>You don't have to add the wildcard '{@code %}' after the {@code value}, this is done internally!</b>
	 * 
	 * @param name
	 *            the name of the attribute
	 * @param value
	 *            the value to check
	 * @return the current {@link SearchQuery}
	 * @see #like(String, Object)
	 */
	public final SearchQuery<T> startsWith(String name, Object value) {
		add(name, value == null ? null : (value + "%"), Operand.LIKE, true);
		return this;
	}

	/**
	 * Checks if the attribute is <b>ends with</b> the given value.<br/>
	 * JPQL-fragment:<br/>
	 * 
	 * <pre>
	 * e.name like :value
	 * </pre>
	 * 
	 * <b>You don't have to add the wildcard '{@code %}' before the {@code value}, this is done internally!</b>
	 * 
	 * @param name
	 *            the name of the attribute
	 * @param value
	 *            the value to check
	 * @return the current {@link SearchQuery}
	 * @see #like(String, Object)
	 */
	public final SearchQuery<T> endsWith(String name, Object value) {
		add(name, value == null ? null : ("%" + value), Operand.LIKE, true);
		return this;
	}

	/**
	 * Checks if the attribute is <b>not null</b>.<br/>
	 * JPQL-fragment:<br/>
	 * 
	 * <pre>
	 * e.name is not null
	 * </pre>
	 * 
	 * @param name
	 *            the name of the attribute
	 * @return the current {@link SearchQuery}
	 */
	public final SearchQuery<T> isNotNull(String name) {
		add(name, null, Operand.NOT_NULL, false);
		return this;
	}

	/**
	 * Checks if the attribute is <b>null</b>.<br/>
	 * JPQL-fragment:<br/>
	 * 
	 * <pre>
	 * e.name is null
	 * </pre>
	 * 
	 * @param name
	 *            the name of the attribute
	 * @return the current {@link SearchQuery}
	 */
	public final SearchQuery<T> isNull(String name) {
		add(name, null, Operand.NULL, false);
		return this;
	}

	private void add(String name, Object value, Operand operand, boolean valueMandatory) {
		SearchCriteria sc = new SearchCriteria(name, value, operand, valueMandatory);
		if (sc.isValid()) {
			criteria.add(sc);
		}
	}

	private void appendOrder(Pageable pageable, StringBuilder queryBuilder, String entityName) {
		if (null != pageable) {
			Sort pageSort = pageable.getSort();
			if (null != pageSort) {
				boolean firstOrder = true;
				for (Order order : pageSort) {
					queryBuilder.append(firstOrder ? " order by " : ", ");
					queryBuilder.append(StringUtils.isBlank(entityName) ? entityName : entityName + ".");
					queryBuilder.append(order.getProperty() + " " + order.getDirection().name());
					firstOrder = false;
				}
			}
		}
	}

	/**
	 * Adds one or more joins to the resulting JPQL-query.<br/>
	 * The alias for the entity is {@code e}, so you could add a join like
	 * 
	 * <pre>
	 * join e.addresses a
	 * </pre>
	 * 
	 * @param joinQuery
	 *            the join-part of a JPQL query
	 */
	public void join(String joinQuery) {
		this.joinQuery = joinQuery;
	}

	/**
	 * Causes the JPQL-query to select {@code distinct} entities only.
	 */
	public void distinct() {
		this.distinct = true;
	}

	@Override
	public String toString() {
		return criteria.toString() + " " + andClauses.toString();
	}

	/**
	 * Executes this {@link SearchQuery} with the given {@link EntityManager}.
	 * 
	 * @param entityManager
	 *            the {@link EntityManager}
	 * @return the result-list
	 */
	public final List<T> execute(EntityManager entityManager) {
		return execute(null, entityManager).getContent();
	}

	/**
	 * Executes this {@link SearchQuery} with the given {@link EntityManager}, applying the given {@link Pageable} (if
	 * any) for paging and sorting the results.
	 * 
	 * @param pageable
	 *            a {@link Pageable} (optional)
	 * @param entityManager
	 *            the {@link EntityManager}
	 * @return the result-{@link Page}
	 * 
	 * @see SearchRepository#search(SearchQuery, Pageable)
	 */
	public final Page<T> execute(Pageable pageable, EntityManager entityManager) {
		StringBuilder sb = new StringBuilder();
		sb.append("from " + domainClass.getSimpleName() + " e");
		if (StringUtils.isNotBlank(joinQuery)) {
			sb.append(" " + joinQuery.trim() + " ");
		}
		for (int i = 0; i < criteria.size(); i++) {
			SearchCriteria criterion = criteria.get(i);
			sb.append(i == 0 ? " where " : " and ");
			if (appendEntityAlias) {
				sb.append("e.");
			}
			sb.append(criterion.getName() + " " + criterion.getOperand().getPresentation());
			if (null != criterion.getValue()) {
				sb.append(" ?" + i);
			}
		}
		boolean addWhere = criteria.size() == 0;
		for (Clause clause : andClauses) {
			sb.append(addWhere ? " where " : " and ");
			sb.append(" " + clause.clause + " ");
			addWhere = false;
		}

		String distinctPart = distinct ? "distinct" : "";
		TypedQuery<Long> countQuery = entityManager.createQuery("select count(" + distinctPart + " e) " + sb.toString(),
				Long.class);
		appendOrder(pageable, sb, appendEntityAlias ? "e" : "");
		TypedQuery<T> query = entityManager.createQuery("select " + distinctPart + " e " + sb.toString(), domainClass);
		for (int i = 0; i < criteria.size(); i++) {
			SearchCriteria criterion = criteria.get(i);
			Object value = criterion.getValue();
			if (null != value) {
				countQuery.setParameter(i, value);
				query.setParameter(i, value);
			}
		}
		for (Clause clause : andClauses) {
			for (Entry<String, Object> entry : clause.params.entrySet()) {
				countQuery.setParameter(entry.getKey(), entry.getValue());
				query.setParameter(entry.getKey(), entry.getValue());
			}
		}

		Long total = null;
		if (null != pageable) {
			total = countQuery.getSingleResult();
			if (pageable.getOffset() >= total) {
				pageable = new PageRequest(0, pageable.getPageSize(), pageable.getSort());
			}
			query.setFirstResult(pageable.getOffset());
			query.setMaxResults(pageable.getPageSize());
		}
		List<T> content = query.getResultList();
		if (null == total) {
			total = new Integer(content.size()).longValue();
		}

		Page<T> page = new PageImpl<T>(content, pageable, total);
		return page;
	}

	enum Operand {
		EQ("="), NE("!="), LE("<="), GE(">="), LT("<"), GT(">"), IN("in"), NOT_IN("not in"), LIKE("like"), NOT_LIKE(
				"not like"), NOT_NULL("is not null"), NULL("is null");

		private final String presentation;

		private Operand(String presentation) {
			this.presentation = presentation;
		}

		String getPresentation() {
			return presentation;
		}
	}

	private class Clause {
		String clause;
		Map<String, Object> params;

		Clause(String clause, Map<String, Object> params) {
			this.clause = clause.trim();
			this.params = params;
		}

		@Override
		public String toString() {
			return clause;
		}
	}

	private class SearchCriteria {

		private final String name;
		private final Object value;
		private final Operand operand;
		private final boolean valueMandatory;

		SearchCriteria(String name, Object value, Operand operand, boolean valueMandatory) {
			this.name = name;
			this.value = value;
			this.operand = operand;
			this.valueMandatory = valueMandatory;
		}

		String getName() {
			return name;
		}

		Object getValue() {
			return value;
		}

		Operand getOperand() {
			return operand;
		}

		boolean isValueMandatory() {
			return valueMandatory;
		}

		boolean isValid() {
			return null != value || !isValueMandatory();
		}

		@Override
		public String toString() {
			return "e." + getName() + " " + getOperand().getPresentation() + " "
					+ (isValueMandatory() ? getValue() : "");
		}

	}

}
