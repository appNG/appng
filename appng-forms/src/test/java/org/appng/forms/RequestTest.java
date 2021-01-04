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
package org.appng.forms;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.appng.forms.impl.RequestBean;
import org.junit.Assert;
import org.junit.Test;

public class RequestTest {

	Request request = new RequestBean();

	@Test
	public void testAddParameter() {
		request.addParameter("foo", "bar");
		Assert.assertEquals("bar", request.getParameter("foo"));
		Assert.assertEquals(1, request.getParameterNames().size());
		Assert.assertEquals("foo", request.getParameterNames().iterator().next());
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testAddParameters() {
		Map<String, String> parameters = new HashMap<>();
		parameters.put("foo", "bar");
		parameters.put("john", "doe");
		request.addParameters(parameters);
		Assert.assertEquals("bar", request.getParameter("foo"));
		Assert.assertEquals("doe", request.getParameter("john"));

		Map<String, List<String>> parametersList = request.getParametersList();

		Collection<String> parameterNames = request.getParameterNames();
		Assert.assertEquals(2, parameterNames.size());
		Assert.assertTrue(parameterNames.contains("foo"));
		Assert.assertTrue(parameterNames.contains("john"));
		Assert.assertTrue(!parameterNames.contains("bar"));
		Assert.assertTrue(!parameterNames.contains("doe"));

		List<String> actual = parametersList.get("foo");
		Assert.assertEquals(Arrays.asList("bar"), actual);
		actual.add("test");
	}
}
