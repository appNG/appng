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
package org.appng.api;

import java.util.ArrayList;
import java.util.List;

import org.appng.api.support.FieldProcessorImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;

public class DataContainerTest {

	private DataContainer dataContainer;
	private FieldProcessor fieldProcessor;
	private List<Person> persons = new ArrayList<>();
	private Pageable pageable = new PageRequest(0, 10);
	private Page<Person> page = new PageImpl<Person>(persons, pageable, 2);
	private Person luke = new Person(1, "Luke", "Skywalker");
	private Person obiWan = new Person(2, "Obi Wan", "Kenobi");

	@Before
	public void setup() {
		fieldProcessor = new FieldProcessorImpl("test", MetaDataProvider.getMetaData());
		dataContainer = new DataContainer(fieldProcessor);
		persons.add(luke);
		persons.add(obiWan);
	}

	@Test
	public void testSetItem() {
		dataContainer.setItem(persons.get(0));
		Assert.assertEquals(fieldProcessor, dataContainer.getFieldProcessor());
		Assert.assertEquals(persons.get(0), dataContainer.getItem());
		Assert.assertNull(dataContainer.getPageable());
		Assert.assertNull(dataContainer.getPage());
		Assert.assertTrue(dataContainer.isSingleResult());
		Assert.assertNull(dataContainer.getItems());
		Assert.assertNull(dataContainer.getWrappedData().isPaginate());
	}

	@Test
	public void testSetPage() {
		dataContainer.setPage(page);
		Assert.assertNull(dataContainer.getPageable());
		Assert.assertEquals(page, dataContainer.getPage());
		Assert.assertFalse(dataContainer.isSingleResult());
		Assert.assertNull(dataContainer.getItems());
		Assert.assertNull(dataContainer.getWrappedData().isPaginate());
	}

	@Test
	public void testSetItems() {
		dataContainer.setItems(persons);
		Assert.assertNull(dataContainer.getPageable());
		Assert.assertNull(dataContainer.getPage());
		Assert.assertFalse(dataContainer.isSingleResult());
		Assert.assertEquals(persons, dataContainer.getItems());
		Assert.assertFalse(dataContainer.getWrappedData().isPaginate());
	}

	@Test
	public void testSetPageFromCollection() {
		PageRequest newPageable = new PageRequest(1, 1, new Sort(Direction.DESC, "firstName"));
		dataContainer.setPage(persons, newPageable);
		Assert.assertEquals(newPageable, dataContainer.getPageable());
		Assert.assertNotNull(dataContainer.getPage());
		Assert.assertEquals(obiWan, dataContainer.getPage().iterator().next());
		Assert.assertFalse(dataContainer.isSingleResult());
		Assert.assertNull(dataContainer.getItems());
		Assert.assertNull(dataContainer.getWrappedData().isPaginate());
	}

	@Test
	public void testSetPageSort() {
		PageRequest newPageable = new PageRequest(5, 10, new Sort(new Order(Direction.ASC, "name"), new Order(
				Direction.DESC, "firstName")));
		List<Person> personList = new ArrayList<>(persons);
		Person anakin = new Person(3, "Anakin", "Skywalker");
		Person unknown = new Person(4, null, "Skywalker");
		Person c3p0 = new Person(5, "C3P0", null);
		personList.add(anakin);
		personList.add(c3p0);
		personList.add(unknown);
		dataContainer.setPage(personList, newPageable);
		Page<?> newPage = dataContainer.getPage();
		List<?> list = newPage.getContent();
		Assert.assertEquals(c3p0, list.get(0));
		Assert.assertEquals(obiWan, list.get(1));
		Assert.assertEquals(luke, list.get(2));
		Assert.assertEquals(anakin, list.get(3));
		Assert.assertEquals(unknown, list.get(4));
	}
}
