/*
 * Copyright 2011-2017 the original author or authors.
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
package org.appng.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.appng.api.model.Properties;
import org.appng.api.model.Property;
import org.appng.api.model.SimpleProperty;
import org.appng.api.model.Site;
import org.appng.api.support.PropertyHolder;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;

public abstract class AbstractTest {

	private static final String PLATFORM = "platform.";

	@Mock
	public Site site;
	@Mock
	public ServletContext ctx;

	@Mock
	protected HttpSession httpSession;
	@Mock
	protected HttpServletResponse httpServletResponse;
	@Mock
	protected HttpServletRequest httpServletRequest;
	@Mock
	protected ServletConfig servletConfig;

	@Mock
	protected FilterChain chain;
	@Mock
	protected FilterConfig filterConfig;
	@Mock
	protected Properties properties;

	@Mock
	protected ApplicationContext platformContext;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		Mockito.when(httpServletRequest.getSession()).thenReturn(httpSession);
		Mockito.when(httpServletRequest.getServletContext()).thenReturn(ctx);
		Mockito.when(httpServletRequest.getServerName()).thenReturn("localhost");

		ConcurrentMap<String, Object> platformContainer = new ConcurrentHashMap<String, Object>();
		Properties platformProperties = new PropertyHolder(PLATFORM, getPlatformProperties());
		platformContainer.put(Platform.Environment.SITES, new HashMap<String, Site>());
		platformContainer.put(Platform.Environment.PLATFORM_CONFIG, platformProperties);

		ConcurrentMap<String, Object> siteContainer = new ConcurrentHashMap<String, Object>();
		ConcurrentMap<String, Object> sessionContainer = new ConcurrentHashMap<String, Object>();
		ConcurrentMap<String, Object> requestContainer = new ConcurrentHashMap<String, Object>();

		Mockito.when(ctx.getAttribute(Scope.PLATFORM.name())).thenReturn(platformContainer);
		Mockito.when(httpSession.getAttribute(Scope.SESSION.name())).thenReturn(sessionContainer);
		Mockito.when(httpServletRequest.getAttribute(Scope.REQUEST.name())).thenReturn(requestContainer);
		Mockito.when(ctx.getAttribute(Scope.SITE.forSite("localhost"))).thenReturn(siteContainer);
	}

	public static MessageSource getMessageSource() {
		ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
		messageSource.setBasename("testmessages");
		return messageSource;
	}

	public static List<Property> getPlatformProperties() {
		List<Property> platformProperties = new ArrayList<Property>();
		addProperty(platformProperties, PLATFORM + "repositoryPath", "repository");
		addProperty(platformProperties, PLATFORM + Platform.Property.DEV_MODE, "true");
		addProperty(platformProperties, PLATFORM + Platform.Property.FORMAT_OUTPUT, "true");
		addProperty(platformProperties, PLATFORM + Platform.Property.APPLICATION_DIR, "applications");
		addProperty(platformProperties, PLATFORM + Platform.Property.CACHE_FOLDER, "cache");
		addProperty(platformProperties, PLATFORM + Platform.Property.PLATFORM_CACHE_FOLDER, "platform");
		addProperty(platformProperties, PLATFORM + Platform.Property.APPLICATION_CACHE_FOLDER, "application");
		addProperty(platformProperties, PLATFORM + Platform.Property.IMAGE_CACHE_FOLDER, "image");
		addProperty(platformProperties, PLATFORM + Platform.Property.TEMPLATE_PREFIX, "/template");
		addProperty(platformProperties, PLATFORM + Platform.Property.TEMPLATE_FOLDER, "/templates");
		addProperty(platformProperties, PLATFORM + Platform.Property.JSP_FILE_TYPE, "jsp");
		addProperty(platformProperties, PLATFORM + Platform.Property.DEFAULT_TEMPLATE, "default");
		addProperty(platformProperties, PLATFORM + Platform.Property.MAX_UPLOAD_SIZE, "50000");
		addProperty(platformProperties, PLATFORM + Platform.Property.IMAGEMAGICK_PATH, "");
		addProperty(platformProperties, PLATFORM + Platform.Property.VHOST_MODE, "NAME_BASED");
		addProperty(platformProperties, PLATFORM + Platform.Property.PLATFORM_ROOT_PATH, "/src/test/resources/");
		return platformProperties;
	}

	public static void addProperty(List<Property> properties, String name, String value) {
		SimpleProperty p = new SimpleProperty(name, value);
		properties.add(p);
	}

}
