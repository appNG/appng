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
package org.appng.api.support;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.appng.api.ParameterSupport;
import org.appng.forms.RequestContainer;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class ParameterSupportTest {

	List<String> PARAMETER_NAMES = Arrays.asList("foo", "foobar", "empty");

	private static final String INPUT_1 = "/abc/de/${foo}/${foobar}/${empty}";
	private static final String INPUT_2 = "/abc/de/${foobar}/${foo}/${empty}";

	private static final String INPUT_3 = "/abc/de/#{foo}/#{foobar}/#{empty}";
	private static final String INPUT_4 = "/abc/de/#{foobar}/#{foo}/#{empty}";

	private static final String OUTPUT_1 = "/abc/de/jin/fizz/";
	private static final String OUTPUT_2 = "/abc/de/fizz/jin/";

	@Test
	public void testMap() {
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put("foo", "jin");
		parameters.put("foobar", "fizz");
		ParameterSupport dollarSupport = new DollarParameterSupport(parameters);
		ParameterSupport hashSupport = new HashParameterSupport(parameters);

		String result = dollarSupport.replaceParameters(INPUT_1);
		String result2 = hashSupport.replaceParameters(INPUT_3);
		Assert.assertEquals(OUTPUT_1, result);
		Assert.assertEquals(OUTPUT_1, result2);

		result = dollarSupport.replaceParameters(INPUT_2);
		result2 = hashSupport.replaceParameters(INPUT_4);
		Assert.assertEquals(OUTPUT_2, result);
		Assert.assertEquals(OUTPUT_2, result2);

		Assert.assertEquals(PARAMETER_NAMES, dollarSupport.getParameters(INPUT_1));
		Assert.assertEquals(PARAMETER_NAMES, hashSupport.getParameters(INPUT_3));
	}

	@Test
	public void testEscapeDollar() {
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put("foo", "$jin");
		parameters.put("foobar", "jin$fizz$");
		ParameterSupport dollarSupport = new DollarParameterSupport(parameters);
		Assert.assertEquals("jin$fizz$", dollarSupport.replaceParameters("${foobar}"));
		Assert.assertEquals("$jin", dollarSupport.replaceParameters("${foo}"));
	}

	@Test
	public void test() {
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put("foo", "jin");
		parameters.put("foobar", "fizz");

		RequestContainer container = Mockito.mock(RequestContainer.class);
		Mockito.when(container.getParameters()).thenReturn(parameters);

		ParameterSupport dollarSupport = new DollarParameterSupport(container.getParameters());
		ParameterSupport hashSupport = new HashParameterSupport(container.getParameters());

		String result = dollarSupport.replaceParameters(INPUT_1);
		String result2 = hashSupport.replaceParameters(INPUT_3);

		Assert.assertEquals(OUTPUT_1, result);
		Assert.assertEquals(OUTPUT_1, result2);
		result = dollarSupport.replaceParameters(INPUT_2);
		result2 = hashSupport.replaceParameters(INPUT_4);
		Assert.assertEquals(OUTPUT_2, result);
		Assert.assertEquals(OUTPUT_2, result2);
	}

	@Test
	public void testCustom() {
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put("foo", "jin");
		parameters.put("foobar", "fizz");
		ParameterSupport parameterSupport = new ParameterSupportBase("\\[\\[", "\\]\\]", parameters);
		String replaced = parameterSupport.replaceParameters("[[foo]][[][[foobar]]");
		Assert.assertEquals("jin[[]fizz", replaced);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidPrefix() {
		new ParameterSupportBase("", "\\}", new HashMap<String, String>());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidSuffix() {
		new ParameterSupportBase("\\$", "", new HashMap<String, String>());
	}

}
