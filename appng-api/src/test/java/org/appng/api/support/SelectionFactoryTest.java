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
package org.appng.api.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.appng.api.Person;
import org.appng.api.model.NameProvider;
import org.appng.api.support.OptionOwner.Selector;
import org.appng.xml.platform.Option;
import org.appng.xml.platform.Selection;
import org.appng.xml.platform.SelectionType;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class SelectionFactoryTest {

	SelectionFactory selectionFactory = new SelectionFactory();
	private static Collection<Person> allElements;
	private static Collection<Person> selectedElements;

	private static Person luke = new Person(0, "Luke", "Skywalker");
	private static Person han = new Person(1, "Han", "Solo");
	private static Person darklord = new Person(2, "Darth", "Vader");
	private static Force[] forces = Force.values();
	private static Collection<Force> brightSide = Arrays.asList(Force.BRIGHTSIDE);

	private static String title = "title";
	private static String id = "id";

	private static NameProvider<Person> nameProvider = new NameProvider<Person>() {
		public String getName(Person instance) {
			return instance.getName() + " " + instance.getFirstname();
		}
	};

	private static NameProvider<Force> forceNameProvider = new NameProvider<Force>() {
		public String getName(Force instance) {
			return instance.name().toLowerCase();
		}
	};

	private static Selector selector = new Selector() {
		public void select(Option o) {
			if (o.getValue().equals(luke.getId().toString())) {
				o.setSelected(true);
			}
		}
	};

	enum Force {
		BRIGHTSIDE, DARKSIDE;
	}

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		allElements = new ArrayList<Person>();
		allElements.add(darklord);
		allElements.add(luke);
		allElements.add(han);

		selectedElements = new ArrayList<Person>();
		selectedElements.add(luke);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testFromEnum() {
		Selection s1 = selectionFactory.fromEnum(id, title, forces, brightSide);
		Selection s2 = selectionFactory.fromEnum(id, title, forces, Force.BRIGHTSIDE);
		Selection s3 = selectionFactory.fromEnum(id, title, forces, brightSide, forceNameProvider);
		Selection s4 = selectionFactory.fromEnum(id, title, forces, Force.BRIGHTSIDE, forceNameProvider);

		assertSelectionEquals(s1, s2);
		assertSelectionEquals(s3, s4);
	}

	@Test
	public void testFromNamed() {
		Selection s1 = selectionFactory.fromNamed(id, title, allElements, selectedElements);
		Selection s2 = selectionFactory.fromNamed(id, title, allElements, luke);
		Selection s3 = selectionFactory.fromNamed(id, title, allElements, selectedElements, nameProvider);
		Selection s4 = selectionFactory.fromNamed(id, title, allElements, luke, nameProvider);
		Selection s5 = selectionFactory.fromNamed(id, title, allElements, selector);
		Selection s6 = selectionFactory.fromNamed(id, title, allElements, selector, nameProvider);

		assertSelectionEquals(s1, s2);
		assertSelectionEquals(s2, s3, false);
		assertSelectionEquals(s3, s4);
		assertSelectionEquals(s4, s5, false);
		assertSelectionEquals(s5, s6, false);
		assertSelectionEquals(s4, s6);
	}

	@Test
	public void testFromIdentifiable() {
		Selection s1 = selectionFactory.fromIdentifiable(id, title, allElements, selectedElements);
		Selection s2 = selectionFactory.fromIdentifiable(id, title, allElements, luke);
		Selection s3 = selectionFactory.fromIdentifiable(id, title, allElements, selectedElements, nameProvider);
		Selection s4 = selectionFactory.fromIdentifiable(id, title, allElements, luke, nameProvider);
		Selection s5 = selectionFactory.fromIdentifiable(id, title, allElements, selector);
		Selection s6 = selectionFactory.fromIdentifiable(id, title, allElements, selector, nameProvider);

		assertSelectionEquals(s1, s2);
		assertSelectionEquals(s2, s3, false);
		assertSelectionEquals(s3, s4);
		assertSelectionEquals(s4, s5, false);
		assertSelectionEquals(s5, s6, false);
		assertSelectionEquals(s4, s6);
	}

	@Test
	public void testGetDateSelection() {
		Selection selection = selectionFactory.getDateSelection("id", "title", "03.12.2015", "dd.MM.yyyy");
		Assert.assertEquals("id", selection.getId());
		Assert.assertEquals("title", selection.getTitle().getId());
		Assert.assertEquals("id", selection.getOptions().get(0).getName());
		Assert.assertEquals("03.12.2015", selection.getOptions().get(0).getValue());
		Assert.assertEquals(SelectionType.DATE, selection.getType());
		Assert.assertEquals("dd.MM.yyyy", selection.getFormat());
	}

	@Test
	public void testGetTextSelection() {
		Selection selection = selectionFactory.getTextSelection("id", "title", "abc");
		Assert.assertEquals("id", selection.getId());
		Assert.assertEquals("title", selection.getTitle().getId());
		Assert.assertEquals("id", selection.getOptions().get(0).getName());
		Assert.assertEquals("abc", selection.getOptions().get(0).getValue());
		Assert.assertEquals(SelectionType.TEXT, selection.getType());
	}

	private void assertSelectionEquals(Selection s1, Selection s2) {
		assertSelectionEquals(s1, s2, true);
	}

	private void assertSelectionEquals(Selection s1, Selection s2, boolean includeName) {
		Assert.assertEquals(s1.getId(), s2.getId());
		Assert.assertEquals(s1.getTitle().getId(), s2.getTitle().getId());
		Assert.assertNotNull(s1.getTitle().getId());
		Assert.assertNotNull(s2.getTitle().getId());
		Assert.assertEquals(s1.getTitle().getValue(), s2.getTitle().getValue());
		int size1 = s1.getOptions().size();
		Assert.assertEquals(size1, s2.getOptions().size());
		for (int i = 0; i < size1; i++) {
			Option opt1 = s1.getOptions().get(i);
			Option opt2 = s2.getOptions().get(i);
			if (includeName) {
				Assert.assertEquals(opt1.getName(), opt2.getName());
			}
			Assert.assertEquals(opt1.getValue(), opt2.getValue());
			Assert.assertEquals(opt1.isSelected(), opt2.isSelected());
		}
	}

}
