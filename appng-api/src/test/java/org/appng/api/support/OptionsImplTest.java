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

import java.util.Set;

import org.appng.api.Option;
import org.appng.xml.platform.FieldType;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test for {@link OptionsImpl}.
 * 
 * @author Gajanan Nilwarn
 * 
 */
public class OptionsImplTest {

	private OptionsImpl options = new OptionsImpl();

	private Option getOption() {
		return new OptionImpl("testOption");
	}

	@Test
	public void testAddOption() {
		options.addOption(getOption());
		Assert.assertEquals(getOption().getName(), options.getOption("testOption").getName());
	}

	@Test
	public void testGetOptionValue() {
		options.optionsMap.putAll(OptionData.getOptionsMap());
		String optionValue = options.getOptionValue("options-4", "attribute-4");
		Assert.assertEquals("value-4", optionValue);
	}

	@Test
	public void testGetOptionValueNull() {
		options.optionsMap.putAll(OptionData.getOptionsMap());
		String optionValue = options.getOptionValue("not-exists", "attribute-4");
		Assert.assertEquals(null, optionValue);
	}

	@Test
	public void testGetInteger() {
		options.optionsMap.putAll(OptionData.getOptionsMap());
		Assert.assertEquals(Integer.valueOf(6), options.getInteger("options-6", "integer-6"));
		Assert.assertEquals(Integer.valueOf(-7), options.getInteger("options-7", "integer-7"));
		Assert.assertNull(options.getInteger("options-4", "attribute-4"));
		Assert.assertNull(options.getInteger("options-7","not-exists"));
	}

	@Test
	public void testGetBoolean() {
		options.optionsMap.putAll(OptionData.getOptionsMap());
		Assert.assertEquals(Boolean.TRUE, options.getBoolean("options-9", "bool-9"));
		Assert.assertEquals(Boolean.FALSE, options.getBoolean("options-7", "integer-7"));
	}

	@Test
	public void testGetEnum() {
		options.optionsMap.putAll(OptionData.getOptionsMap());
		Assert.assertEquals(FieldType.TEXT, options.getEnum("options-8", "enum-8", FieldType.class));
		Assert.assertNull(options.getEnum("options-7", "integer-7", FieldType.class));
	}

	@Test
	public void testGetOptionNames() {
		options.optionsMap.putAll(OptionData.getOptionsMap());
		Set<String> optionsKeys = options.getOptionNames();
		Assert.assertEquals(9, optionsKeys.size());
		Assert.assertEquals(OptionData.getOptionsMap().keySet(), optionsKeys);
	}

	@Test
	public void testToString() {
		options.optionsMap.clear();
		options.optionsMap.put("key", new OptionImpl("value").addAttribute("attr", "attr-value"));
		String actual = options.toString();
		Assert.assertEquals("[value [ attr=\"attr-value\" ]]", actual);
	}
}
