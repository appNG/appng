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
package org.appng.taglib;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.Tag;

import org.appng.api.Environment;
import org.appng.api.model.Properties;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.core.domain.SiteImpl;
import org.appng.core.domain.SubjectImpl;
import org.appng.core.model.ApplicationProvider;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class PermissionTest {

	@Test
	public void test() throws JspException {
		PageContext pageContext = AttributeTest.setupTagletTest();
		DefaultEnvironment environment = DefaultEnvironment.get(pageContext);
		environment.setSubject(new SubjectImpl());

		final MultiSiteSupport mock = Mockito.mock(MultiSiteSupport.class);
		ApplicationProvider app = Mockito.mock(ApplicationProvider.class);
		Mockito.when(app.getProperties()).thenReturn(Mockito.mock(Properties.class));
		Mockito.when(app.getProperties().getBoolean("permissionsEnabled", true)).thenReturn(false);
		Mockito.when(mock.getApplicationProvider()).thenReturn(app);
		Mockito.when(mock.getExecutingSite()).thenReturn(Mockito.mock(SiteImpl.class));
		Permission permission = new Permission() {
			@Override
			protected MultiSiteSupport processMultiSiteSupport(Environment env, HttpServletRequest servletRequest)
					throws JspException {
				return mock;
			}
		};
		permission.setPageContext(pageContext);
		Assert.assertEquals(Tag.EVAL_BODY_INCLUDE, permission.doStartTag());
	}

}
