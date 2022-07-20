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
package org.appng.api.support;

import java.util.Arrays;
import java.util.List;

import org.appng.api.Person;
import org.appng.api.support.OptionOwner.HitCounter;
import org.appng.api.support.OptionOwner.Selector;
import org.appng.xml.platform.FieldType;
import org.appng.xml.platform.Option;
import org.appng.xml.platform.Selection;
import org.appng.xml.platform.SelectionType;
import org.junit.Assert;
import org.junit.Test;

public class SelectionBuilderTest {

	@Test
	public void testBuilder() {
		Person a = new Person();
		a.setId(1);
		a.setName("a");
		Person b = new Person();
		b.setId(2);
		b.setName("b");
		Person c = new Person();
		c.setId(3);
		c.setName("c");
		SelectionBuilder<Person> builder = new SelectionBuilder<Person>("persons");
		Selector selector = o -> {
			if (o.getValue().equals(c.getId().toString())) {
				o.setSelected(true);
			}
		};
		HitCounter<Person> counter = p -> p.getName().equals(a.getName()) ? 1 : 3;
		Selection selection = builder.title("label").tooltipId("tooltip").options(Arrays.asList(a, b, c)).select(b)
				.selector(selector).disable(c).hitCounter(counter).type(SelectionType.SELECT_MULTIPLE)
				.defaultOption("-please select-", "").build();

		Assert.assertEquals("persons", selection.getId());
		Assert.assertEquals(SelectionType.SELECT_MULTIPLE, selection.getType());
		Assert.assertEquals("label", selection.getTitle().getValue());
		Assert.assertEquals("tooltip", selection.getTooltip().getId());
		List<Option> options = selection.getOptions();
		Assert.assertEquals(4, options.size());

		Assert.assertEquals("-please select-", options.get(0).getName());
		Assert.assertEquals("", options.get(0).getValue());

		Assert.assertEquals(a.getId().toString(), options.get(1).getValue());
		Assert.assertEquals(b.getId().toString(), options.get(2).getValue());
		Assert.assertEquals(c.getId().toString(), options.get(3).getValue());

		Assert.assertEquals(a.getName(), options.get(1).getName());
		Assert.assertEquals(b.getName(), options.get(2).getName());
		Assert.assertEquals(c.getName(), options.get(3).getName());

		Assert.assertEquals(Boolean.FALSE, options.get(1).isSelected());
		Assert.assertEquals(Boolean.TRUE, options.get(2).isSelected());
		Assert.assertEquals(Boolean.TRUE, options.get(3).isSelected());

		Assert.assertEquals(null, options.get(1).isDisabled());
		Assert.assertEquals(null, options.get(2).isDisabled());
		Assert.assertEquals(Boolean.TRUE, options.get(3).isDisabled());

		Assert.assertEquals(Integer.valueOf(1), options.get(1).getHits());
		Assert.assertEquals(Integer.valueOf(3), options.get(2).getHits());
		Assert.assertEquals(Integer.valueOf(3), options.get(3).getHits());

	}

	@Test
	public void testBuilderWithEnum() {
		Selection selection = new SelectionBuilder<FieldType>("type").options(Arrays.asList(FieldType.values()))
				.select(FieldType.TEXT).disable(FieldType.TEXT).name(t -> t.name().toLowerCase()).build();
		Assert.assertEquals(FieldType.TEXT.name().toLowerCase(), selection.getOptions().get(0).getName());
		Assert.assertEquals(FieldType.TEXT.name(), selection.getOptions().get(0).getValue());
		Assert.assertTrue(selection.getOptions().get(0).isSelected());
		Assert.assertTrue(selection.getOptions().get(0).isDisabled());
	}

}
