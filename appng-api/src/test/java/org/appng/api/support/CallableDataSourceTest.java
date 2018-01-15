/*
 * Copyright 2011-2018 the original author or authors.
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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.appng.api.ApplicationConfigProvider;
import org.appng.api.DataContainer;
import org.appng.api.DataProvider;
import org.appng.api.Environment;
import org.appng.api.FieldProcessor;
import org.appng.api.Options;
import org.appng.api.Path;
import org.appng.api.PermissionProcessor;
import org.appng.api.Person;
import org.appng.api.ProcessingException;
import org.appng.api.Request;
import org.appng.api.Scope;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.api.model.Subject;
import org.appng.api.support.environment.EnvironmentKeys;
import org.appng.xml.platform.ApplicationConfig;
import org.appng.xml.platform.ApplicationRootConfig;
import org.appng.xml.platform.Bean;
import org.appng.xml.platform.DataConfig;
import org.appng.xml.platform.Datasource;
import org.appng.xml.platform.DatasourceRef;
import org.appng.xml.platform.MetaData;
import org.appng.xml.platform.Params;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Test for {@link CallableDataSource}.
 * 
 * @author Matthias Herlitzius
 * 
 */
public class CallableDataSourceTest {

	private static final String MY_DATASOURCE = "myDatasource";
	private static final String TEST_BEAN = "testBean";

	@Mock
	private Bean bean;

	@Mock
	private DataConfig config;

	@Mock
	private DataProvider dataProvider;

	@Mock
	private Datasource datasource;

	@Mock
	private DatasourceRef datasourceRef;

	@Mock
	private PermissionProcessor permissionProcessor;

	@Mock
	private Application application;

	@Mock
	private ApplicationConfigProvider applicationConfigProvider;

	@Mock
	private ApplicationRequest applicationRequest;

	@Mock
	private Environment environment;

	@Mock
	private Path path;
	@Mock
	private Subject subject;

	@Mock
	private Site site;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		permissionProcessor = new DefaultPermissionProcessor(subject, site, application);
		Mockito.when(applicationRequest.getPermissionProcessor()).thenReturn(permissionProcessor);
		Mockito.when(applicationRequest.getApplicationConfig()).thenReturn(applicationConfigProvider);
		Mockito.when(bean.getId()).thenReturn(TEST_BEAN);
		Mockito.when(config.getParams()).thenReturn(new Params());
		Mockito.when(datasource.getConfig()).thenReturn(config);
		MetaData metaData = new MetaData();
		metaData.setBindClass("java.util.Set");
		Mockito.when(config.getMetaData()).thenReturn(metaData);
		Mockito.when(datasource.getBean()).thenReturn(bean);
		Mockito.when(datasource.getId()).thenReturn(MY_DATASOURCE);
		Mockito.when(datasourceRef.getId()).thenReturn(MY_DATASOURCE);
		Mockito.when(datasourceRef.getParams()).thenReturn(new Params());
		Mockito.when(datasourceRef.getPageSize()).thenReturn(10);
		Mockito.when(site.getSiteClassLoader()).thenReturn(new URLClassLoader(new URL[0]));
		Mockito.when(application.getBean(TEST_BEAN, DataProvider.class)).thenReturn(dataProvider);
		Mockito.when(applicationConfigProvider.getDatasource(MY_DATASOURCE)).thenReturn(datasource);

		Mockito.when(applicationRequest.getLocale()).thenReturn(Locale.US);
		MockHttpServletRequest servletRequest = new MockHttpServletRequest();
		servletRequest.setServletPath("/manager/app/page");
		Mockito.when(applicationRequest.getHttpServletRequest()).thenReturn(servletRequest);

		Mockito.when(applicationRequest.getEnvironment()).thenReturn(environment);
		Mockito.when(environment.getAttribute(Scope.REQUEST, EnvironmentKeys.PATH_INFO)).thenReturn(path);
		ApplicationRootConfig applicationRootConfig = new ApplicationRootConfig();
		applicationRootConfig.setConfig(new ApplicationConfig());
		Mockito.when(applicationConfigProvider.getApplicationRootConfig()).thenReturn(applicationRootConfig);
	}

	@Test
	public void testInvalidPage() throws Exception {
		mockPage(new PageRequest(3, 25), 5);
		getDataSource().perform("test");
		verifyRedirect(1);
	}

	@Test
	public void testEmptyPage() throws Exception {
		mockPage(new PageRequest(0, 25), 0);
		getDataSource().perform("test");
		verifyRedirect(0);
	}

	protected void verifyRedirect(int times) {
		Mockito.verify(site, Mockito.times(times)).sendRedirect(applicationRequest.getEnvironment(),
				"/manager/app/page?sortMyDatasource=page:0;pageSize:25", HttpServletResponse.SC_FOUND);
	}

	@Test
	public void testBindClassNoMatch() throws Exception {
		try {
			MetaData metaData = new MetaData();
			metaData.setBindClass(Set.class.getName());
			DataContainer dataContainer = new DataContainer(new FieldProcessorImpl("foo", metaData));
			dataContainer.setItem(new ArrayList<String>());
			mockDataProvider(dataContainer);

			getDataSource().perform("test");
			fail("must throw ProcessingException");
		} catch (ProcessingException e) {
			Throwable cause = e.getCause();
			Assert.assertEquals(IllegalArgumentException.class, cause.getClass());
			Assert.assertTrue(cause.getMessage()
					.startsWith("the object of type 'java.util.ArrayList' returned by 'org.appng.api.DataProvider"));
			String message = cause.getMessage();
			Assert.assertTrue(
					message.endsWith("' is not of the desired type 'java.util.Set' as defined in the meta-data!"));
		}
	}

	private void mockPage(Pageable pageable, int total) {
		MetaData metaData = new MetaData();
		metaData.setBindClass(Person.class.getName());
		DataContainer dataContainer = new DataContainer(new FieldProcessorImpl("foo", metaData));
		Page<Person> page = new PageImpl<Person>(Collections.<Person> emptyList(), pageable, total);
		dataContainer.setPage(page);
		mockDataProvider(dataContainer);
	}

	private void mockDataProvider(DataContainer dataContainer) {
		Mockito.when(dataProvider.getData(Mockito.any(Site.class), Mockito.any(Application.class),
				Mockito.any(Environment.class), Mockito.any(Options.class), Mockito.any(Request.class),
				Mockito.any(FieldProcessor.class))).thenReturn(dataContainer);
	}

	@Test
	public void testPerformException() {
		try {
			getDataSource().perform("test");
			fail();
		} catch (ProcessingException e) {
			String message = e.getMessage();
			assertTrue(message.matches("error retrieving datasource 'myDatasource', ID: \\d{6,12}"));
		}
	}

	protected CallableDataSource getDataSource() throws ProcessingException {
		return new CallableDataSource(site, application, applicationRequest, new DollarParameterSupport(),
				datasourceRef);
	}
}
