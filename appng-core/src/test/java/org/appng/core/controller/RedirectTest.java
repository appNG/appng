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
package org.appng.core.controller;

import javax.servlet.http.HttpServletResponse;

import org.appng.core.Redirect;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class RedirectTest {

	@Mock
	private HttpServletResponse httpServletResponse;
	private Integer mode;
	private String connectionHeader;
	private String locationHeader;

	@Test
	public void test301() {
		MockitoAnnotations.openMocks(this);
		run(HttpServletResponse.SC_MOVED_PERMANENTLY, "/targeturl");
		Assert.assertNull(connectionHeader);
	}

	@Test
	public void test301External() {
		MockitoAnnotations.openMocks(this);
		run(HttpServletResponse.SC_MOVED_PERMANENTLY, "http://www.example.com");
		Assert.assertEquals("close", connectionHeader);
	}

	@Test
	public void test302() {
		MockitoAnnotations.openMocks(this);
		run(HttpServletResponse.SC_FOUND, "targeturl");
		Assert.assertNull(connectionHeader);
	}

	protected void run(int status, String target) {
		Mockito.doAnswer(new Answer<Object>() {
			public Void answer(InvocationOnMock invocation) {
				Object args = invocation.getArguments()[0];
				if (args instanceof Integer) {
					mode = (Integer) args;
				}
				return null;
			}
		}).when(httpServletResponse).setStatus(status);

		Mockito.doAnswer(new Answer<Object>() {
			public Void answer(InvocationOnMock invocation) {
				Object args = invocation.getArguments()[1];
				if (args instanceof String) {
					connectionHeader = args.toString();
				}
				return null;
			}
		}).when(httpServletResponse).setHeader(HttpHeaders.CONNECTION, "close");

		Mockito.doAnswer(new Answer<Object>() {
			public Void answer(InvocationOnMock invocation) {
				Object args = invocation.getArguments()[1];
				if (args instanceof String) {
					locationHeader = args.toString();
				}

				return null;
			}
		}).when(httpServletResponse).setHeader(HttpHeaders.LOCATION, target);

		Redirect.to(httpServletResponse, status, target);
		Assert.assertEquals(Integer.valueOf(status), mode);
		Assert.assertEquals(target, locationHeader);
	}

}
