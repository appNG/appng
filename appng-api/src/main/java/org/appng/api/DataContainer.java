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
package org.appng.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.comparators.ComparatorChain;
import org.apache.commons.lang3.StringUtils;
import org.appng.xml.platform.Data;
import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.FieldType;
import org.appng.xml.platform.Result;
import org.appng.xml.platform.Resultset;
import org.appng.xml.platform.Selection;
import org.appng.xml.platform.SelectionGroup;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;

/**
 * A container for the data returned by a {@link DataProvider}. A {@link DataProvider} must call one of the following
 * methods in order to provide some {@link Data}:
 * <ul>
 * <li>{@link #setItem(Object)} - in case of a single object</li>
 * <li>{@link #setItems(Collection)} - in case of a (non-sortable and non-pageable) collection of items</li>
 * <li>{@link #setPage(Page)} - in case of a (potentially sorted/paginated) {@link Page}</li>
 * <li>{@link #setPage(Collection, Pageable)}/ {@link #setPage(Collection, Pageable, boolean)} - in case where paging
 * and sorting should be applied to a collection of items</li>
 * </ul>
 * A {@link Resultset} (in case {@link #isSingleResult()} returns {@code false}) or a {@link Result} is being added to
 * the {@link Data} returned by {@link #getWrappedData()} using a {@link ResultService}.
 * 
 * @author Matthias Müller
 */
public final class DataContainer {

	private final FieldProcessor fieldProcessor;
	private final Data data;
	private Object item;
	private Collection<?> items;
	private boolean singleResult;
	private Page<?> page;
	private Pageable pageable;

	/**
	 * Creates a new {@link DataContainer} using the given {@link FieldProcessor}.
	 * 
	 * @param fieldProcessor
	 *                       the {@link FieldProcessor} containing all readable {@link FieldDef}initions
	 */
	public DataContainer(final FieldProcessor fieldProcessor) {
		this.data = new Data();
		this.fieldProcessor = fieldProcessor;
	}

	/**
	 * Returns the single item
	 * 
	 * @return the item
	 */
	public Object getItem() {
		return item;
	}

	/**
	 * Sets the single item
	 * 
	 * @param item
	 *             the item
	 */
	public void setItem(Object item) {
		this.item = item;
		setSingleResult(true);
		initItem();
	}

	/**
	 * Returns whether this {@link DataContainer} contains only a single item
	 * 
	 * @return {@code true} if this {@link DataContainer} only contains a single result which has been set via
	 *         {@link #setItem(Object)}
	 */
	public boolean isSingleResult() {
		return singleResult;
	}

	private void setSingleResult(boolean singleResult) {
		this.singleResult = singleResult;
	}

	/**
	 * Returns the items previously set via one of the {@code setItems(...)} -methods
	 * 
	 * @return the items
	 */
	public Collection<?> getItems() {
		return items;
	}

	/**
	 * Sets the {@link Collection} of items for this {@code DataContainer}. Note that paging and sorting is not
	 * supported when using this method.
	 * 
	 * @param items
	 *              a {@code Collection} of items
	 */
	public void setItems(Collection<?> items) {
		this.items = items;
		setSingleResult(false);
		initItems(items);
		setPageable(null);
	}

	/**
	 * Sets the {@link Page} for this {@code DataContainer}, which is being extracted from the given {@code Collection}
	 * of items, based on the given {@link Pageable}. See {@link #setPage(Collection, Pageable, boolean)} for details.
	 * 
	 * @param items
	 *                 the {@code Collection} of items the extract the {@link Page} from
	 * @param pageable
	 *                 the {@link Pageable} for the {@link Page} to extract
	 * 
	 * @see #setPage(Collection, Pageable, boolean)
	 */
	public void setPage(Collection<?> items, Pageable pageable) {
		setPage(items, pageable, false);
	}

	/**
	 * Sets the {@link Page} for this {@code DataContainer}, which is being extracted from the given {@code Collection}
	 * of items, based on the given {@link Pageable}.<br/>
	 * For example, if there are 15 items in the collection, and the {@link Pageable} requests (the 0-based) page 1 with
	 * a pagesize of 10, the resulting page will contain the (1-based) elements 11 to 15.<br/>
	 * If the {@link Pageable} has a {@link Sort} property set, the items will be sorted before extracting the page.
	 * <p>
	 * <b>Note that sorting the items is done by using Java Reflection-API, thus for performance reasons it is not
	 * recommended to apply sorting to very large collections.</b> Such collections should be pre-sorted before in a
	 * non-reflective way (then {@code skipSort} should be set to {@code true}).
	 * </p>
	 * 
	 * @param items
	 *                 the {@code Collection} of items the extract the {@link Page} from
	 * @param pageable
	 *                 the {@link Pageable} for the {@link Page} to extract
	 * @param skipSort
	 *                 if the items should not get sorted, even if the {@code pageable} has a {@code Sort} property set
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void setPage(Collection<?> items, Pageable pageable, boolean skipSort) {
		List<?> sortedItems = new ArrayList(items);
		if (!skipSort) {
			doSort(sortedItems, pageable.getSort());
		}
		int size = sortedItems.size();
		int fromIndex = pageable.getOffset() < size ? pageable.getOffset() : 0;
		int pageSize = pageable.getPageSize();
		int toIndex = fromIndex + pageSize;
		toIndex = toIndex > size ? size : toIndex;
		List subList = new ArrayList(sortedItems).subList(fromIndex, toIndex);
		int currentPage = fromIndex == 0 ? 0 : pageable.getPageNumber();
		Pageable extractedPageable = new PageRequest(currentPage, pageSize, pageable.getSort());
		Page extractedPage = new PageImpl(subList, extractedPageable, size);
		setPage(extractedPage);
		setPageable(extractedPageable);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void doSort(List items, Sort sort) {
		if (null != sort && !items.isEmpty()) {
			ComparatorChain comparatorChain = new ComparatorChain();
			Iterator<Order> iterator = sort.iterator();
			while (iterator.hasNext()) {
				final Order order = iterator.next();
				final int factor = order.isAscending() ? 1 : -1;
				comparatorChain.addComparator(new Comparator<Object>() {

					public int compare(Object o1, Object o2) {
						final String property = order.getProperty();
						final BeanWrapper bw1 = new BeanWrapperImpl(o1);
						final BeanWrapper bw2 = new BeanWrapperImpl(o2);
						final Object p1 = bw1.isReadableProperty(property) ? bw1.getPropertyValue(property) : null;
						final Object p2 = bw2.isReadableProperty(property) ? bw2.getPropertyValue(property) : null;
						if (p1 instanceof Comparable && p2 instanceof Comparable) {
							if (order.isIgnoreCase() && p1 instanceof String && p2 instanceof String) {
								return ((String) p1).compareToIgnoreCase((String) p2);
							}
							return ((Comparable) p1).compareTo(p2) * factor;
						}
						if (p1 == null && p2 != null) {
							return -1 * factor;
						}
						if (p1 != null && p2 == null) {
							return 1 * factor;
						}
						return 0;
					}
				});
			}
			Collections.sort(items, comparatorChain);
		}
	}

	/**
	 * Returns the {@link Pageable} (may be {@code null}) for this {@code DataContainer}
	 * 
	 * @return the {@link Pageable}, if any
	 * 
	 * @see #setPage(Page)
	 * @see #setPage(Collection, Pageable)
	 */
	public Pageable getPageable() {
		return pageable;
	}

	private void setPageable(Pageable pageable) {
		this.pageable = pageable;
		if (null == pageable) {
			getWrappedData().setPaginate(false);
		}
	}

	/**
	 * Returns the {@link Page} (may be {@code null}) for this {@code DataContainer}
	 * 
	 * @return the {@link Page}, if any
	 * 
	 * @see #setPage(Page)
	 * @see #setPage(Collection, Pageable)
	 */
	public Page<?> getPage() {
		return page;
	}

	/**
	 * Sets the {@link Page} for this {@code DataContainer}
	 * 
	 * @param page
	 *             the {@link Page} to set
	 */
	public void setPage(Page<?> page) {
		this.page = page;
		setSingleResult(false);
		initItems(page);
	}

	/**
	 * Convenience method to access the {@link Selection}s of the wrapped {@link Data}.
	 * 
	 * @return a list of {@link Selection}s
	 * 
	 * @see #getWrappedData()
	 */
	public List<Selection> getSelections() {
		return data.getSelections();
	}

	/**
	 * Convenience method to access the {@link SelectionGroup}s of the wrapped {@link Data}.
	 * 
	 * @return a list of {@link SelectionGroup}s
	 * 
	 * @see #getWrappedData()
	 */
	public List<SelectionGroup> getSelectionGroups() {
		return data.getSelectionGroups();
	}

	/**
	 * Returns the {@link Data} wrapped within this {@code DataContainer}
	 * 
	 * @return the {@link Data}
	 */
	public Data getWrappedData() {
		return data;
	}

	private void initItems(Iterable<?> items) {
		for (Object item : items) {
			initItem(item);
		}
	}

	private void initItem() {
		initItem(item);
	}

	private void initItem(Object item) {
		if (null != fieldProcessor) {
			List<FieldDef> fields = fieldProcessor.getFields();
			initFields(null, fields, new BeanWrapperImpl(item));
		}
	}

	/**
	 * This is needed because a LazyInitializationException occurs otherwise when accessing uninitialized fields during
	 * result-transformation
	 * 
	 * @param parentName
	 * @param fields
	 * @param wrapper
	 */
	private void initFields(String parentName, List<FieldDef> fields, BeanWrapper wrapper) {
		if (null != fields) {
			for (FieldDef fieldDef : fields) {
				if (!FieldType.LINKPANEL.equals(fieldDef.getType())) {
					String name = fieldDef.getName();
					if (StringUtils.isNotEmpty(parentName)) {
						name = parentName + "." + name;
					}
					boolean readable = wrapper.isReadableProperty(name);
					if (readable) {
						Object propertyValue = wrapper.getPropertyValue(name);
						if (propertyValue instanceof Collection<?>) {
							((Collection<?>) propertyValue).size();
						}
					}
					initFields(name, fieldDef.getFields(), wrapper);
				}
			}
		}
	}

	/**
	 * Returns the {@link FieldProcessor} this {@link DataContainer} was created with
	 * 
	 * @return the {@link FieldProcessor}
	 */
	public FieldProcessor getFieldProcessor() {
		return fieldProcessor;
	}

}
