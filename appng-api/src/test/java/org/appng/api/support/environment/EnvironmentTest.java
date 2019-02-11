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
package org.appng.api.support.environment;

import static org.appng.api.Scope.SESSION;

import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.AbstractTest;
import org.appng.api.Environment;
import org.appng.api.Platform;
import org.appng.api.Scope;
import org.appng.api.VHostMode;
import org.appng.api.model.Properties;
import org.appng.api.model.Subject;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;

/**
 * Test for {@link DefaultEnvironment}.
 * 
 * @author Matthias Müller
 */
public class EnvironmentTest extends AbstractTest {

	@Test
	public void testPlatformEnvironment() {
		Environment env = DefaultEnvironment.get(ctx, httpServletRequest);
		env.setAttribute(Scope.PLATFORM, "app-attribute", "app-value");
		Assert.assertEquals("app-value", env.getAttributeAsString(Scope.PLATFORM, "app-attribute"));

		env.setAttribute(Scope.SESSION, "session-attribute", "session-value");
		Assert.assertEquals("session-value", env.getAttributeAsString(Scope.SESSION, "session-attribute"));

		env.setAttribute(Scope.REQUEST, "request-attribute", "request-value");
		Assert.assertEquals("request-value", env.getAttributeAsString(Scope.REQUEST, "request-attribute"));

	}

	@Test
	public void testAttributeMethods() {
		Environment env = DefaultEnvironment.get(ctx, httpServletRequest);
		env.setAttribute(Scope.PLATFORM, "app-attribute", "10");
		Assert.assertEquals("10", env.getAttribute(Scope.PLATFORM, "app-attribute"));

		env.setAttribute(Scope.PLATFORM, "app-attribute", "10");
		env.removeAttribute(Scope.PLATFORM, "app-attribute");
		Assert.assertEquals((Object) null, env.getAttribute(Scope.PLATFORM, "app-attribute"));
	}

	@Test
	public void testSetSubject() {
		Properties platformProps = Mockito.mock(Properties.class);
		Mockito.when(platformProps.getString(Platform.Property.VHOST_MODE)).thenReturn(VHostMode.NAME_BASED.name());
		MockServletContext mockCtx = new MockServletContext();
		Environment initialEnv = DefaultEnvironment.get(mockCtx);
		initialEnv.setAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG, platformProps);
		initialEnv.setAttribute(Scope.PLATFORM, Platform.Environment.SITES, new HashMap<>());

		MockHttpServletRequest mockRequest = new MockHttpServletRequest(mockCtx);
		String oldId = mockRequest.getSession().getId();
		mockRequest.setSession(new MockHttpSession(mockCtx));
		initialEnv.setAttribute(SESSION, org.appng.api.Session.Environment.SID, oldId);
		initialEnv.setAttribute(SESSION, org.appng.api.Session.Environment.TIMEOUT, 30000);
		initialEnv.setAttribute(SESSION, org.appng.api.Session.Environment.STARTTIME, System.currentTimeMillis());

		Subject subject = Mockito.mock(Subject.class);
		Mockito.when(subject.isAuthenticated()).thenReturn(true);

		TimeZone london = TimeZone.getTimeZone("Europe/London");
		Locale enGB = Locale.forLanguageTag("en-GB");
		Mockito.when(subject.getTimeZone()).thenReturn(london.getID());
		Mockito.when(subject.getLanguage()).thenReturn(enGB.toLanguageTag());

		DefaultEnvironment env = DefaultEnvironment.get(mockCtx, mockRequest);
		Assert.assertEquals(TimeZone.getDefault(), env.getTimeZone());
		Assert.assertEquals(Locale.getDefault(), env.getLocale());

		env.setSubject(subject);
		Assert.assertEquals(london, env.getTimeZone());
		Assert.assertEquals(enGB, env.getLocale());

		DefaultEnvironment newEnv = DefaultEnvironment.get(mockCtx, mockRequest);
		Assert.assertEquals(london, newEnv.getTimeZone());
		Assert.assertEquals(enGB, newEnv.getLocale());

		String newId = mockRequest.getSession().getId();
		Assert.assertNotNull(newId);
		Assert.assertNotEquals(oldId, newId);
		Assert.assertNull(env.getAttribute(SESSION, org.appng.api.Session.Environment.SID));
		Assert.assertNull(env.getAttribute(SESSION, org.appng.api.Session.Environment.TIMEOUT));
		Assert.assertNull(env.getAttribute(SESSION, org.appng.api.Session.Environment.STARTTIME));
	}

	@Test
	public void testGetSubject() {
		DefaultEnvironment env = DefaultEnvironment.get(ctx, httpServletRequest);

		Subject subject = Mockito.mock(Subject.class);
		Mockito.when(subject.getName()).thenReturn("admin");
		Mockito.when(subject.isAuthenticated()).thenReturn(true);

		env.setAttribute(Scope.SESSION, SESSION.name() + ".currentSubject", subject);
		Assert.assertEquals(true, env.isSubjectAuthenticated());

		Assert.assertEquals(subject, env.getAttribute(Scope.SESSION, SESSION.name() + ".currentSubject"));
		Assert.assertEquals(subject, env.getSubject());
	}

	@Test
	public void testToggleScope() {
		DefaultEnvironment env = DefaultEnvironment.get(ctx, httpServletRequest);
		toggleScope(env, Scope.PLATFORM);
		toggleScope(env, Scope.SESSION);
		toggleScope(env, Scope.SITE);
		toggleScope(env, Scope.REQUEST);
	}

	private void toggleScope(DefaultEnvironment env, Scope scope) {
		env.disable(scope);
		Assert.assertNull(env.keySet(scope));
		env.enable(scope);
		Assert.assertNotNull(env.keySet(scope));
	}

	@Test
	public void testGetServletRequest() {
		DefaultEnvironment env = DefaultEnvironment.get(ctx, httpServletRequest);
		Assert.assertEquals(httpServletRequest, env.getServletRequest());
	}

	@Test
	public void testGetServletResponse() {
		DefaultEnvironment env = DefaultEnvironment.get(httpServletRequest, httpServletResponse);
		Assert.assertEquals(httpServletResponse, env.getServletResponse());
	}

	@Test
	public void testSiteEnv() {
		Mockito.when(site.getHost()).thenReturn("localhost");
		MockServletContext mockedCtx = new MockServletContext();
		SiteEnvironment siteEnv = new SiteEnvironment(mockedCtx, site.getHost());
		Assert.assertEquals(site.getHost(), siteEnv.getAttribute("host"));
		Assert.assertEquals(Scope.SITE, siteEnv.getScope());
		DefaultEnvironment.get(mockedCtx).clearSiteScope(site);
		Assert.assertNull(siteEnv.getAttribute("host"));
	}

	@Test
	public void testSessionEnvironment() {
		SessionEnvironment sessionEnv = new SessionEnvironment(httpSession, StringUtils.EMPTY);
		String attributeName = "localhost";
		Object attribute = sessionEnv.getAttribute(attributeName);
		Assert.assertEquals(null, attribute);
		sessionEnv.setAttribute(attributeName, "foo");
		Assert.assertEquals("foo", attribute = sessionEnv.removeAttribute(attributeName));
		Assert.assertEquals((Object) null, sessionEnv.getAttribute(attributeName));

		Assert.assertEquals(httpSession, sessionEnv.getHttpSession());
		Assert.assertTrue(sessionEnv.isValid());
		sessionEnv.logout();
		Assert.assertFalse(sessionEnv.isValid());
	}
}
