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

import org.junit.Assert;
import org.junit.Test;

/**
 * Test for {@link OptionImpl}.
 * 
 * @author Gajanan Nilwarn
 * 
 */
public class OptionImplTest {

	private OptionImpl option = new OptionImpl("option", OptionData.getAttributesMap());

	@Test
	public void testGetName() {
		String name = option.getName();
		Assert.assertEquals("option", name);
	}

	@Test
	public void testAddMap() {
		option.getAttributeMap().clear();
		option.addMap(OptionData.getAttributesMap());
		Assert.assertEquals(OptionData.getAttributesMap().size(), option.getAttributeMap().size());
	}

	@Test
	public void testAddAttribute() {
		option.addAttribute("key", "value");
		Assert.assertEquals("value", option.getString("key"));
	}

	@Test
	public void testGetInteger() {
		option.addAttribute("key", "value");
		option.addAttribute("int", "1");
		Assert.assertEquals(Integer.valueOf(1), option.getInteger("int"));
		Assert.assertNull(option.getInteger("key"));
		Assert.assertNull(option.getInteger("not-exists"));
	}

	@Test
	public void testToString() {
		option = new OptionImpl("option");
		option.addAttribute("key", "value");
		Assert.assertEquals("option [ key=\"value\" ]", option.toString());
	}

	@Test
	public void testGetAttributeAsInteger() {
		option = new OptionImpl("option");
		option.addAttribute("key", "1");
		Assert.assertEquals(1, option.getAttributeAsInteger("key"));
	}

	@Test
	public void testContainsAttribute() {
		option.getAttributeMap().clear();
		option.addMap(OptionData.getAttributesMap());
		Assert.assertEquals(true, option.containsAttribute("attribute-1"));
	}

}
