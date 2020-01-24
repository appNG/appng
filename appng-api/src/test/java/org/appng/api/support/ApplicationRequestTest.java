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
package org.appng.api.support;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.appng.api.Environment;
import org.appng.api.Path;
import org.appng.api.PermissionProcessor;
import org.appng.api.RequestSupport;
import org.appng.api.Scope;
import org.appng.api.support.ApplicationRequest.ApplicationPath;
import org.appng.api.support.environment.EnvironmentKeys;
import org.appng.el.ExpressionEvaluator;
import org.appng.forms.Request;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class ApplicationRequestTest {

	@Test
	public void testApplicationPath() {
		Request request = Mockito.mock(Request.class);
		HashMap<String, String> params = new HashMap<>();
		params.put("entity", "item");
		params.put("action", "update");
		params.put("id", "2");

		Mockito.when(request.getParameters()).thenReturn(params);
		RequestSupport rs = Mockito.mock(RequestSupport.class);
		Environment env = Mockito.mock(Environment.class);
		Mockito.when(rs.getEnvironment()).thenReturn(env);
		Path path = Mockito.mock(Path.class);
		Mockito.when(path.getApplicationUrlParameters()).thenReturn(Arrays.asList("item", "update", "2"));
		Mockito.when(env.getAttribute(Scope.REQUEST, EnvironmentKeys.PATH_INFO)).thenReturn(path);
		ApplicationRequest ar = new ApplicationRequest(request, Mockito.mock(PermissionProcessor.class), rs);

		ApplicationPath applicationPath = ar.applicationPath();
		Map<String, Object> conditionsParams = new HashMap<>(params);
		conditionsParams.put(ApplicationPath.PATH_VAR, applicationPath);
		ExpressionEvaluator ee = new ExpressionEvaluator(conditionsParams);

		params.keySet().forEach(k -> Assert.assertTrue(applicationPath.hasParam(k)));
		Assert.assertFalse(applicationPath.hasParam("foo"));

		Assert.assertTrue(ee.evaluate("${ PATH.eq('/item', '/update/', id) }"));
		Assert.assertTrue(ee.evaluate("${ PATH.eq('/item/update/2') }"));
		Assert.assertTrue(ee.evaluate("${ PATH.starts('/item', '/update/', id) }"));
		Assert.assertTrue(ee.evaluate("${ PATH.ends('/item/update/', id) }"));
		Assert.assertTrue(ee.evaluate("${ PATH.contains('/item', '/update/', id) }"));
		Assert.assertTrue(ee.evaluate("${ PATH.matches('/item/update/','\\\\d+') }"));
	}

}
