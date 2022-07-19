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
package org.appng.api;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.appng.api.model.Properties;
import org.appng.api.model.Site;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class RequestMappingTest {

	@Mock
	private ServletContext ctx;

	@Mock
	private Environment env;
	
	@Mock
	Properties platformProperties;


	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		Mockito.when(env.getAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG)).thenReturn(platformProperties);

		Map<String, Site> siteMap = new HashMap<>();
		Site site1 = Mockito.mock(Site.class);
		Mockito.when(site1.getName()).thenReturn("site1");
		Mockito.when(site1.getHost()).thenReturn("site1.loc");
		Mockito.when(site1.getHostAliases()).thenReturn(new HashSet<String>(Arrays.asList("alias1-1.loc", "alias1-2.loc", "alias1-2.loc")));
		siteMap.put(site1.getName(), site1);

		Site site2 = Mockito.mock(Site.class);
		Mockito.when(site2.getName()).thenReturn("site2");
		Mockito.when(site2.getHost()).thenReturn("127.0.47.11");
		Mockito.when(site2.getHostAliases()).thenReturn(new HashSet<String>(Arrays.asList("127.0.47.12")));
		siteMap.put(site2.getName(), site2);

		Site site3 = Mockito.mock(Site.class);
		Mockito.when(site3.getName()).thenReturn("site3");
		Mockito.when(site3.getHost()).thenReturn("site3.loc");
		Mockito.when(site3.getHostAliases()).thenReturn(new HashSet<String>());
		siteMap.put(site3.getName(), site3);

		Mockito.when(env.getAttribute(Scope.PLATFORM, Platform.Environment.SITES)).thenReturn(siteMap);
	}
	
	@Test
	public void testSiteMappingIPBasedMatchHost() throws Exception {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		Mockito.when(request.getLocalAddr()).thenReturn("127.0.47.11");
		Mockito.when(platformProperties.getString(Platform.Property.VHOST_MODE)).thenReturn(VHostMode.IP_BASED.toString());
	
		Site site = RequestUtil.getSite(env, request);
		Assert.assertEquals(site.getName(), "site2");
	}

	@Test
	public void testSiteMappingIPBasedMatchHostAlias() throws Exception {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		Mockito.when(request.getLocalAddr()).thenReturn("127.0.47.12");
		Mockito.when(platformProperties.getString(Platform.Property.VHOST_MODE)).thenReturn(VHostMode.IP_BASED.toString());
	
		Site site = RequestUtil.getSite(env, request);
		Assert.assertEquals(site.getName(), "site2");
	}

	@Test
	public void testSiteMappingIPBasedMiss() throws Exception {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		Mockito.when(request.getLocalAddr()).thenReturn("1.2.3.4");
		Mockito.when(platformProperties.getString(Platform.Property.VHOST_MODE)).thenReturn(VHostMode.IP_BASED.toString());
	
		Site site = RequestUtil.getSite(env, request);
		Assert.assertNull(site);
	}

	@Test
	public void testSiteMappingNameBasedMatchHost() throws Exception {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		Mockito.when(request.getServerName()).thenReturn("site3.loc");
		Mockito.when(platformProperties.getString(Platform.Property.VHOST_MODE)).thenReturn(VHostMode.NAME_BASED.toString());
	
		Site site = RequestUtil.getSite(env, request);
		Assert.assertEquals(site.getName(), "site3");
	}

	@Test
	public void testSiteMappingNameBasedMatchHostAlias() throws Exception {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		Mockito.when(request.getServerName()).thenReturn("alias1-2.loc");
		Mockito.when(platformProperties.getString(Platform.Property.VHOST_MODE)).thenReturn(VHostMode.NAME_BASED.toString());
	
		Site site = RequestUtil.getSite(env, request);
		Assert.assertEquals(site.getName(), "site1");
	}

	@Test
	public void testSiteMappingDirectMatchSERVER_LOCAL_NAME() throws Exception {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		Mockito.when(request.getAttribute(RequestUtil.SERVER_LOCAL_NAME)).thenReturn("site1");
		Mockito.when(platformProperties.getString(Platform.Property.VHOST_MODE)).thenReturn(VHostMode.NAME_BASED.toString());
	
		Site site = RequestUtil.getSite(env, request);
		Assert.assertEquals(site.getName(), "site1");
	}

	@Test
	public void testSiteMappingDirectMatchX_APPNG_SITE() throws Exception {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		Mockito.when(request.getHeader(RequestUtil.X_APPNG_SITE)).thenReturn("site2");
		Mockito.when(platformProperties.getString(Platform.Property.VHOST_MODE)).thenReturn(VHostMode.NAME_BASED.toString());
	
		Site site = RequestUtil.getSite(env, request);
		Assert.assertEquals(site.getName(), "site2");
	}

	@Test
	public void testSiteMappingDirectMatchPriority() throws Exception {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		Mockito.when(request.getAttribute(RequestUtil.X_APPNG_SITE)).thenReturn("site3");
		Mockito.when(request.getAttribute(RequestUtil.SERVER_LOCAL_NAME)).thenReturn("site1");
		Mockito.when(platformProperties.getString(Platform.Property.VHOST_MODE)).thenReturn(VHostMode.NAME_BASED.toString());
	
		Site site = RequestUtil.getSite(env, request);
		Assert.assertEquals(site.getName(), "site1");
	}

}