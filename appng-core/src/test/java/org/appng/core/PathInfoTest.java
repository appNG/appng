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
package org.appng.core;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.appng.api.Path;
import org.appng.api.PathInfo;
import org.junit.Assert;
import org.junit.Test;

public class PathInfoTest {

	public static final List<String> ASSETS_DIRS = Arrays.asList("/assets");
	public static final List<String> DOCUMENT_DIRS = Arrays.asList("/de");
	public static final String CURRENT_SITE = "manager";
	public static final String SERVICE_PATH = "/services";
	public static final String MANAGER_PATH = "/manager";
	public static final String HOST = "s1.example.com";
	public static final String DOMAIN = "http://s1.example.com:8080";
	public static final String REPOSITORY = "repository";
	public static final String JSP = "jsp";

	@Test
	public void testGuiComplete() {
		Path pathInfo = new PathInfo(HOST, DOMAIN, CURRENT_SITE, "/manager/admin/admin/page1/action/2", MANAGER_PATH,
				SERVICE_PATH, ASSETS_DIRS, DOCUMENT_DIRS, REPOSITORY, JSP);
		Assert.assertEquals(HOST, pathInfo.getHost());
		Assert.assertEquals("admin", pathInfo.getApplicationName());
		Assert.assertEquals("admin", pathInfo.getSiteName());
		Assert.assertEquals("page1", pathInfo.getPage());
		Assert.assertTrue(pathInfo.isGui());
		Assert.assertEquals(Arrays.asList("action", "2"), pathInfo.getApplicationUrlParameters());
		Assert.assertFalse(pathInfo.isDocument());
		Assert.assertFalse(pathInfo.isStaticContent());
		Assert.assertFalse(pathInfo.isService());
	}

	@Test
	public void testInvalidGui() {
		Path pathInfo = new PathInfo(HOST, DOMAIN, CURRENT_SITE, "/managers/admin/admin/page1/action/2", MANAGER_PATH,
				SERVICE_PATH, ASSETS_DIRS, DOCUMENT_DIRS, REPOSITORY, JSP);
		Assert.assertFalse(pathInfo.isGui());
		Assert.assertFalse(pathInfo.isDocument());
		Assert.assertFalse(pathInfo.isStaticContent());
		Assert.assertFalse(pathInfo.isService());
	}

	@Test
	public void testRepository() {
		Path pathInfo = new PathInfo(HOST, DOMAIN, CURRENT_SITE, "/repository", MANAGER_PATH, SERVICE_PATH,
				ASSETS_DIRS, DOCUMENT_DIRS, REPOSITORY, JSP);
		Assert.assertTrue(pathInfo.isRepository());
	}

	@Test
	public void testShortUrl() {
		PathInfo pathInfo = new PathInfo(HOST, DOMAIN, CURRENT_SITE, "/manager/page1/action/2", MANAGER_PATH,
				SERVICE_PATH, ASSETS_DIRS, DOCUMENT_DIRS, REPOSITORY, JSP);
		pathInfo.setApplicationName("admin");
		pathInfo.setPage("page1");
		Assert.assertEquals(HOST, pathInfo.getHost());
		Assert.assertEquals("admin", pathInfo.getApplicationName());
		// Assert.assertEquals("admin", pathInfo.getSiteName());
		Assert.assertEquals("page1", pathInfo.getPage());
		Assert.assertTrue(pathInfo.isGui());
		Assert.assertEquals(Arrays.asList("action", "2"), pathInfo.getApplicationUrlParameters());
		Assert.assertFalse(pathInfo.isDocument());
		Assert.assertFalse(pathInfo.isStaticContent());
		Assert.assertFalse(pathInfo.isService());
	}

	@Test
	public void testServletPath() {
		PathInfo pathInfo = new PathInfo(HOST, DOMAIN, CURRENT_SITE, MANAGER_PATH, MANAGER_PATH, SERVICE_PATH,
				ASSETS_DIRS, DOCUMENT_DIRS, REPOSITORY, JSP);
		String application = "foobar";
		pathInfo.setApplicationName(application);
		Assert.assertEquals(HOST, pathInfo.getHost());
		Assert.assertEquals("/manager/manager/foobar", pathInfo.getCurrentPath());
		Assert.assertEquals(MANAGER_PATH, pathInfo.getServletPath());
		Assert.assertEquals(application, pathInfo.getApplicationName());
		Assert.assertNull(pathInfo.getPage());
		Assert.assertEquals(CURRENT_SITE, pathInfo.getSiteName());
		Assert.assertTrue(pathInfo.isGui());
		Assert.assertEquals(new ArrayList<>(), pathInfo.getApplicationUrlParameters());
		Assert.assertFalse(pathInfo.isDocument());
		Assert.assertFalse(pathInfo.isStaticContent());
		Assert.assertFalse(pathInfo.isService());
	}

	@Test
	public void testGuiNoPath() {
		Path pathInfo = new PathInfo(HOST, DOMAIN, CURRENT_SITE, MANAGER_PATH, MANAGER_PATH, SERVICE_PATH, ASSETS_DIRS,
				DOCUMENT_DIRS, REPOSITORY, JSP);
		Assert.assertEquals(HOST, pathInfo.getHost());
		Assert.assertNull(pathInfo.getApplicationName());
		Assert.assertNull(pathInfo.getPage());
		Assert.assertEquals(CURRENT_SITE, pathInfo.getSiteName());
		Assert.assertTrue(pathInfo.isGui());
		Assert.assertEquals(new ArrayList<>(), pathInfo.getApplicationUrlParameters());
		Assert.assertFalse(pathInfo.isDocument());
		Assert.assertFalse(pathInfo.isStaticContent());
		Assert.assertFalse(pathInfo.isService());
	}

	@Test
	public void testGui() {
		Path pathInfo = new PathInfo(HOST, DOMAIN, CURRENT_SITE, "/manager/admin", MANAGER_PATH, SERVICE_PATH,
				ASSETS_DIRS, DOCUMENT_DIRS, REPOSITORY, JSP);
		Assert.assertEquals(HOST, pathInfo.getHost());
		Assert.assertNull(pathInfo.getApplicationName());
		Assert.assertNull(pathInfo.getPage());
		Assert.assertEquals("admin", pathInfo.getSiteName());
		Assert.assertTrue(pathInfo.isGui());
		Assert.assertEquals(new ArrayList<>(), pathInfo.getApplicationUrlParameters());
		Assert.assertFalse(pathInfo.isDocument());
		Assert.assertFalse(pathInfo.isStaticContent());
		Assert.assertFalse(pathInfo.isService());
	}

	@Test
	public void testAction() {
		Path pathInfo = new PathInfo(HOST, DOMAIN, CURRENT_SITE,
				"/services/manager/someapplication/action/siteEvent/create", MANAGER_PATH, SERVICE_PATH, ASSETS_DIRS,
				DOCUMENT_DIRS, REPOSITORY, JSP);
		Assert.assertEquals(HOST, pathInfo.getHost());
		Assert.assertEquals("someapplication", pathInfo.getApplicationName());
		Assert.assertEquals(CURRENT_SITE, pathInfo.getSiteName());
		Assert.assertEquals("manager", pathInfo.getSiteName());
		Assert.assertEquals("siteEvent", pathInfo.getService());
		Assert.assertEquals(Arrays.asList("create"), pathInfo.getApplicationUrlParameters());
		Assert.assertFalse(pathInfo.isDocument());
		Assert.assertFalse(pathInfo.isStaticContent());
		Assert.assertFalse(pathInfo.isGui());
		Assert.assertTrue(pathInfo.isService());
	}

	@Test
	public void testDatasource() {
		Path pathInfo = new PathInfo(HOST, DOMAIN, CURRENT_SITE, "/services/manager/someapplication/datasource/sites",
				MANAGER_PATH, SERVICE_PATH, ASSETS_DIRS, DOCUMENT_DIRS, REPOSITORY, JSP);
		Assert.assertEquals(HOST, pathInfo.getHost());
		Assert.assertEquals("someapplication", pathInfo.getApplicationName());
		Assert.assertEquals(CURRENT_SITE, pathInfo.getSiteName());
		Assert.assertEquals("manager", pathInfo.getSiteName());
		Assert.assertEquals("sites", pathInfo.getService());
		Assert.assertEquals(new ArrayList<>(), pathInfo.getApplicationUrlParameters());
		Assert.assertFalse(pathInfo.isDocument());
		Assert.assertFalse(pathInfo.isStaticContent());
		Assert.assertFalse(pathInfo.isGui());
		Assert.assertTrue(pathInfo.isService());
	}

	@Test
	public void testSoap() {
		Path pathInfo = new PathInfo(HOST, DOMAIN, CURRENT_SITE,
				"/services/manager/someapplication/soap/PersonService/PersonService.wsdl", MANAGER_PATH, SERVICE_PATH,
				ASSETS_DIRS, DOCUMENT_DIRS, REPOSITORY, JSP);
		Assert.assertEquals(HOST, pathInfo.getHost());
		Assert.assertEquals("someapplication", pathInfo.getApplicationName());
		Assert.assertEquals(CURRENT_SITE, pathInfo.getSiteName());
		Assert.assertEquals("manager", pathInfo.getSiteName());
		Assert.assertEquals("PersonService", pathInfo.getService());
		Assert.assertEquals(Arrays.asList("PersonService.wsdl"), pathInfo.getApplicationUrlParameters());
		Assert.assertEquals("PersonService.wsdl", pathInfo.getElementAt(pathInfo.getElementCount() - 1));
		Assert.assertFalse(pathInfo.isDocument());
		Assert.assertFalse(pathInfo.isStaticContent());
		Assert.assertFalse(pathInfo.isGui());
		Assert.assertTrue(pathInfo.isService());
	}

	@Test
	public void testInvalidService() {
		Path pathInfo = new PathInfo(HOST, DOMAIN, CURRENT_SITE,
				"/service/manager/someapplication/webservice/someservice/foo/bar", MANAGER_PATH, SERVICE_PATH,
				ASSETS_DIRS, DOCUMENT_DIRS, REPOSITORY, JSP);
		Assert.assertFalse(pathInfo.isDocument());
		Assert.assertFalse(pathInfo.isStaticContent());
		Assert.assertFalse(pathInfo.isGui());
		Assert.assertFalse(pathInfo.isService());
	}

	@Test
	public void testWebservice() {
		Path pathInfo = new PathInfo(HOST, DOMAIN, CURRENT_SITE,
				"/services/manager/someapplication/webservice/someservice/foo/bar", MANAGER_PATH, SERVICE_PATH,
				ASSETS_DIRS, DOCUMENT_DIRS, REPOSITORY, JSP);
		Assert.assertEquals(HOST, pathInfo.getHost());
		Assert.assertEquals("someapplication", pathInfo.getApplicationName());
		Assert.assertEquals(CURRENT_SITE, pathInfo.getSiteName());
		Assert.assertEquals("manager", pathInfo.getSiteName());
		Assert.assertEquals("someservice", pathInfo.getService());
		Assert.assertEquals(Arrays.asList("foo", "bar"), pathInfo.getApplicationUrlParameters());
		Assert.assertEquals("foo", pathInfo.getElementAt(pathInfo.getElementCount() - 2));
		Assert.assertEquals("bar", pathInfo.getElementAt(pathInfo.getElementCount() - 1));
		Assert.assertFalse(pathInfo.isDocument());
		Assert.assertFalse(pathInfo.isStaticContent());
		Assert.assertFalse(pathInfo.isGui());
		Assert.assertTrue(pathInfo.isService());
	}

	@Test
	public void testDoc() {
		Path pathInfo = new PathInfo(HOST, DOMAIN, CURRENT_SITE, "/de/foo", MANAGER_PATH, SERVICE_PATH, ASSETS_DIRS,
				DOCUMENT_DIRS, REPOSITORY, JSP);
		Assert.assertEquals(HOST, pathInfo.getHost());
		Assert.assertNull(pathInfo.getApplicationName());
		Assert.assertEquals(CURRENT_SITE, pathInfo.getSiteName());
		Assert.assertNull(pathInfo.getService());
		Assert.assertEquals(new ArrayList<>(), pathInfo.getApplicationUrlParameters());
		Assert.assertTrue(pathInfo.isDocument());
		Assert.assertFalse(pathInfo.isStaticContent());
		Assert.assertFalse(pathInfo.isGui());
		Assert.assertFalse(pathInfo.isService());
	}

	@Test
	public void testAsset() {
		Path pathInfo = new PathInfo(HOST, DOMAIN, CURRENT_SITE, "/assets/test.css", MANAGER_PATH, SERVICE_PATH,
				ASSETS_DIRS, DOCUMENT_DIRS, REPOSITORY, JSP);
		Assert.assertEquals(HOST, pathInfo.getHost());
		Assert.assertNull(pathInfo.getApplicationName());
		Assert.assertEquals(CURRENT_SITE, pathInfo.getSiteName());
		Assert.assertNull(pathInfo.getService());
		Assert.assertEquals(new ArrayList<>(), pathInfo.getApplicationUrlParameters());
		Assert.assertFalse(pathInfo.isDocument());
		Assert.assertTrue(pathInfo.isStaticContent());
		Assert.assertFalse(pathInfo.isGui());
		Assert.assertFalse(pathInfo.isService());
	}

	@Test
	public void testOutputFormat() {
		Path pathInfo = new PathInfo(HOST, DOMAIN, CURRENT_SITE, "/manager/_format/admin/someapplication",
				MANAGER_PATH, SERVICE_PATH, ASSETS_DIRS, DOCUMENT_DIRS, REPOSITORY, JSP);
		Assert.assertEquals("format", pathInfo.getOutputFormat());
		Assert.assertEquals("/_format", pathInfo.getOutputPrefix());
		Assert.assertNull(pathInfo.getOutputType());
		Assert.assertEquals("admin", pathInfo.getSiteName());
		Assert.assertEquals("someapplication", pathInfo.getApplicationName());
	}

	@Test
	public void testOutputFormatAndType() {
		Path pathInfo = new PathInfo(HOST, DOMAIN, CURRENT_SITE, "/manager/_format/_type/admin/someapplication",
				MANAGER_PATH, SERVICE_PATH, ASSETS_DIRS, DOCUMENT_DIRS, REPOSITORY, JSP);
		Assert.assertEquals("format", pathInfo.getOutputFormat());
		Assert.assertEquals("/_format/_type", pathInfo.getOutputPrefix());
		Assert.assertEquals("type", pathInfo.getOutputType());
		Assert.assertEquals("admin", pathInfo.getSiteName());
		Assert.assertEquals("someapplication", pathInfo.getApplicationName());
	}

	@Test
	public void testGetForwardPath() throws URISyntaxException {
		PathInfo pathInfo = new PathInfo(HOST, DOMAIN, CURRENT_SITE, "/de/test/foo/bar", MANAGER_PATH, SERVICE_PATH,
				ASSETS_DIRS, DOCUMENT_DIRS, REPOSITORY, JSP);
		ClassLoader classLoader = getClass().getClassLoader();
		URI uri = classLoader.getResource("repository/manager/www").toURI();
		String forwardPath = pathInfo.getForwardPath("/repository/manager/www", new File(uri));
		Assert.assertEquals("/repository/manager/www/de/test.jsp", forwardPath);
		Assert.assertEquals(Arrays.asList("foo", "bar"), pathInfo.getJspUrlParameters());
	}

	@Test
	public void testIsPathSelected() {
		Path pathInfo = new PathInfo(HOST, DOMAIN, CURRENT_SITE, "/manager/admin/admin/page1/action/2#someAnchor",
				MANAGER_PATH, SERVICE_PATH, ASSETS_DIRS, DOCUMENT_DIRS, REPOSITORY, JSP);
		Assert.assertTrue(pathInfo.isPathSelected("/manager"));
		Assert.assertTrue(pathInfo.isPathSelected("/manager/admin"));
		Assert.assertTrue(pathInfo.isPathSelected("/manager/admin/admin"));
		Assert.assertTrue(pathInfo.isPathSelected("/manager/admin/admin/page1/action/2"));
		Assert.assertTrue(pathInfo.isPathSelected("/manager/admin/admin/page1/action/2/"));
		Assert.assertTrue(pathInfo.isPathSelected("/manager/admin/admin/page1/action/2#someAnchor"));
		Assert.assertTrue(pathInfo.isPathSelected("/manager/admin/admin/page1/action/2#anotherAnchor"));

		Assert.assertFalse(pathInfo.isPathSelected("/manager/admin/admin/page1/action/2/564"));
		Assert.assertFalse(pathInfo.isPathSelected("/manager/admin/administratorapplication"));
		Assert.assertFalse(pathInfo.isPathSelected("/manager/admin/administratorapplication/23"));
		Assert.assertFalse(pathInfo.isPathSelected("/manager/admin1"));
		Assert.assertFalse(pathInfo.isPathSelected("/manager/admin1/admin"));
	}
}
