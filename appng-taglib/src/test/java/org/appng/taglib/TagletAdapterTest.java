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
package org.appng.taglib;

import static org.appng.api.Scope.PLATFORM;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;

import org.appng.api.Environment;
import org.appng.api.Platform;
import org.appng.api.support.ApplicationRequest;
import org.appng.api.support.SiteClassLoader;
import org.appng.core.domain.SiteImpl;
import org.appng.core.model.ApplicationProvider;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockBodyContent;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockJspWriter;
import org.springframework.mock.web.MockPageContext;
import org.springframework.mock.web.MockServletContext;

public class TagletAdapterTest {

	@Test
	public void test() throws Exception {
		final Environment environment = Mockito.mock(Environment.class);
		final SiteImpl site = Mockito.mock(SiteImpl.class);
		SiteClassLoader siteClassloader = new SiteClassLoader(new URL[0], getClass().getClassLoader(), "localhost");
		Mockito.when(site.getSiteClassLoader()).thenReturn(siteClassloader);
		final ApplicationProvider applicationProvider = Mockito.mock(ApplicationProvider.class);
		final TagletProcessor tagletProcessor = Mockito.mock(TagletProcessor.class);
		ApplicationContext appContext = Mockito.mock(ApplicationContext.class);

		TagletAdapter tagletAdapter = getTagletAdapter(environment, site, applicationProvider);

		Mockito.when(appContext.containsBean("tagletProcessor")).thenReturn(true);
		Mockito.when(appContext.getBean("tagletProcessor", TagletProcessor.class)).thenReturn(tagletProcessor);

		Answer<Boolean> tagletProcessorAnswer = new Answer<Boolean>() {
			public Boolean answer(InvocationOnMock invocation) throws Throwable {
				Writer writer = invocation.getArgumentAt(7, Writer.class);
				writer.write("TagletProcessor.perform()");
				writer.flush();
				return true;
			}
		};
		Map<String, String> expectedParams = new HashMap<>();
		expectedParams.put("foo", "bar");
		Mockito.when(tagletProcessor.perform(Mockito.eq(site), Mockito.eq(site), Mockito.eq(applicationProvider),
				Mockito.eq(expectedParams), Mockito.any(ApplicationRequest.class), Mockito.eq("method"),
				Mockito.eq("xml"), Mockito.any(Writer.class))).thenAnswer(tagletProcessorAnswer);

		Mockito.when(environment.getAttribute(PLATFORM, Platform.Environment.CORE_PLATFORM_CONTEXT))
				.thenReturn(appContext);

		tagletAdapter.setApplication("application");
		tagletAdapter.setMethod("method");
		tagletAdapter.setType("xml");

		MockServletContext context = new MockServletContext();
		final MockHttpServletResponse response = new MockHttpServletResponse();
		final PrintWriter responseWriter = response.getWriter();
		final MockHttpServletRequest request = new MockHttpServletRequest(context);
		MockPageContext pageContext = new MockPageContext() {
			@Override
			public JspWriter getOut() {
				return new MockJspWriter(response, responseWriter);
			}

			@Override
			public ServletRequest getRequest() {
				return request;
			}

			@Override
			public ServletResponse getResponse() {
				return response;
			}
		};
		StringWriter bodyWriter = new StringWriter();
		MockBodyContent bodyContent = new MockBodyContent("content", response, bodyWriter);

		tagletAdapter.setBodyContent(bodyContent);
		tagletAdapter.setPageContext(pageContext);
		tagletAdapter.doStartTag();
		tagletAdapter.addParameter("foo", "bar");
		tagletAdapter.doEndTag();

		Assert.assertEquals("Do not write to body content!", "", bodyWriter.toString());
		Assert.assertEquals("Write to pageContext.getOut()", "TagletProcessor.perform()",
				response.getContentAsString());
	}

	protected TagletAdapter getTagletAdapter(final Environment environment, final SiteImpl site,
			final ApplicationProvider applicationProvider) {
		TagletAdapter tagletAdapter = new TagletAdapter() {
			@Override
			protected Environment getEnvironment() {
				return environment;
			}

			@Override
			protected MultiSiteSupport getMultiSiteSupport(HttpServletRequest servletRequest, Environment environment)
					throws JspException {
				return new MultiSiteSupport() {
					@Override
					public SiteImpl getCallingSite() {
						return site;
					}

					@Override
					public SiteImpl getExecutingSite() {
						return site;
					}

					@Override
					public ApplicationProvider getApplicationProvider() {
						return applicationProvider;
					}
				};

			}
		};
		return tagletAdapter;
	}

}
