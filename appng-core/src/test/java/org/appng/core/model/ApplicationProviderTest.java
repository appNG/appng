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
package org.appng.core.model;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.appng.api.Path;
import org.appng.api.PathInfo;
import org.appng.api.Scope;
import org.appng.api.model.FeatureProvider;
import org.appng.api.model.Permission;
import org.appng.api.model.Properties;
import org.appng.api.model.Resources;
import org.appng.api.model.Role;
import org.appng.api.support.environment.EnvironmentKeys;
import org.appng.testsupport.TestBase;
import org.appng.testsupport.validation.WritingXmlValidator;
import org.appng.testsupport.validation.XPathDifferenceHandler;
import org.appng.xml.application.ApplicationInfo;
import org.appng.xml.platform.Action;
import org.appng.xml.platform.ApplicationReference;
import org.appng.xml.platform.Datasource;
import org.appng.xml.platform.PlatformConfig;
import org.custommonkey.xmlunit.DifferenceListener;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(locations = { "classpath:applications/application1/beans.xml", TestBase.TESTCONTEXT,
		TestBase.TESTCONTEXT_JPA }, inheritLocations = false, initializers = ApplicationProviderTest.class)
public class ApplicationProviderTest extends TestBase {

	private static final String PAGE_ID = "entity";
	private static final String TESTAPPLICATION = "demo-application";
	private PlatformConfig platformConfig;
	private ApplicationProvider applicationProvider;
	private ApplicationProvider monitoredApplicationProvider;

	public ApplicationProviderTest() {
		super(TESTAPPLICATION, "src/test/resources/applications/application1");
		setEntityPackage("org.appng.persistence.model");
		setRepositoryBase("");
	}

	class SimpleAccessibleApplication extends SimpleApplication implements AccessibleApplication {

		SimpleAccessibleApplication(String name, ConfigurableApplicationContext context) {
			super(name, context);
		}

		public void setId(Integer id) {
		}

		public void setName(String name) {
		}

		public void setDescription(String description) {
		}

		public void setFileBased(boolean fileBased) {
		}

		public void setVersion(Date version) {
		}

		public void setPermissions(Set<Permission> permissions) {
		}

		public void setRoles(Set<Role> roles) {
		}

		public void setResources(Resources applicationResourceHolder) {
		}

		public void setProperties(Properties properties) {
		}

		public void setContext(ConfigurableApplicationContext applicationContext) {
		}

		public ConfigurableApplicationContext getContext() {
			return context;
		}

		public void setPrivileged(boolean isPrivileged) {

		}

		public void setFeatureProvider(FeatureProvider featureProvider) {
		}

		public void closeContext() {

		}

		public void init(ApplicationInfo applicationInfo){
			super.init(new java.util.Properties(), applicationInfo);
		}

	}

	@Override
	@Before
	public void setup() throws Exception {
		super.setup();
		environment.setAttribute(Scope.REQUEST, EnvironmentKeys.PATH_INFO, Mockito.mock(Path.class));
		SimpleAccessibleApplication app = new SimpleAccessibleApplication(TESTAPPLICATION, context);
		app.init(request.getApplicationConfig().getApplicationInfo());
		applicationProvider = new ApplicationProvider(site, app);
		monitoredApplicationProvider = new ApplicationProvider(site, app, true);
		URI masterFile = getClass().getClassLoader().getResource("applications/application1/conf/master.xml").toURI();
		this.platformConfig = marshallService.unmarshall(new File(masterFile), PlatformConfig.class);
		applicationProvider.setApplicationConfig(request.getApplicationConfig().cloneConfig(marshallService));
		monitoredApplicationProvider.setApplicationConfig(request.getApplicationConfig().cloneConfig(marshallService));
	}

	@Test
	public void testEntities() throws Exception {
		addParameter("sortEntities", "name:asc;doubleValue:desc;pageSize:2");
		initParameters();
		runMonitoredTest(getPathInfo(TESTAPPLICATION + "/" + PAGE_ID));
	}

	@Test
	public void testUpdateInclude() throws JAXBException, IOException {
		addParameter("action", "update");
		addParameter("entityId", "1");
		initParameters();
		runTest(getPathInfo(TESTAPPLICATION + "/" + PAGE_ID + "/update/1"));
	}

	@Test
	public void testUpdateExecute() throws JAXBException, IOException {
		addParameter("action", "update");
		addParameter("entityId", "1");
		addParameter("form_action", "update");
		addParameter("name", "new name");
		initParameters(true);
		runMonitoredTest(getPathInfo(TESTAPPLICATION + "/" + PAGE_ID + "/update/1"));
	}

	@Test
	public void testUpdateExceptionDataSource() throws JAXBException, IOException {
		addParameter("action", "update");
		addParameter("entityId", "2");
		addParameter("form_action", "update");
		addParameter("name", "exception");
		initParameters(true);
		runTest(getPathInfo(TESTAPPLICATION + "/" + PAGE_ID + "/update/1"), getMessageTextDifferenceListener());
	}

	@Test
	public void testUpdateException() throws JAXBException, IOException {
		addParameter("action", "update");
		addParameter("entityId", "1");
		addParameter("form_action", "update");
		addParameter("name", "exception");
		initParameters(true);
		runTest(getPathInfo(TESTAPPLICATION + "/" + PAGE_ID + "/update/1"), getMessageTextDifferenceListener());
	}

	private DifferenceListener getMessageTextDifferenceListener() {
		XPathDifferenceHandler differenceListener = new XPathDifferenceHandler();
		differenceListener.ignoreDifference("/applicationReference/pages/page/messages/message/text()");
		return differenceListener;
	}

	@Test
	public void testUpdateValidationError() throws JAXBException, IOException {
		addParameter("action", "update");
		addParameter("entityId", "1");
		addParameter("form_action", "update");

		addParameter("integerValue", "4");
		addParameter("doubleValue", "3.9");
		addParameter("booleanValue", "true");

		initParameters(true);
		runTest(getPathInfo(TESTAPPLICATION + "/" + PAGE_ID + "/update/1"));
	}

	@Test
	public void testCreate() throws JAXBException, IOException {
		addParameter("action", "create");
		initParameters();
		runTest(getPathInfo(TESTAPPLICATION + "/" + PAGE_ID + "/create"));
	}

	@Test
	public void testCallDataSourceEntity() throws Exception {
		addParameter("entityId", "1");
		initParameters();
		Datasource datasource = applicationProvider.processDataSource(servletResponse, true, request, "entity",
				marshallService);
		String resultXML = marshallService.marshallNonRoot(datasource, Datasource.class);
		WritingXmlValidator.validateXml(resultXML,
				"xml/" + getClass().getSimpleName() + "-testCallDataSourceEntity.xml");
	}

	@Test
	public void testCallDataSourceEntities() throws Exception {
		addParameter("sortEntities", "pageSize:2");
		initParameters();
		Datasource datasource = applicationProvider.processDataSource(servletResponse, true, request, "entities",
				marshallService);
		String resultXML = marshallService.marshallNonRoot(datasource, Datasource.class);
		WritingXmlValidator.validateXml(resultXML,
				"xml/" + getClass().getSimpleName() + "-testCallDataSourceEntities.xml");
	}

	@Test
	public void testCallDataSourceNotFound() throws Exception {
		Datasource datasource = applicationProvider.processDataSource(servletResponse, true, request, "undefined",
				marshallService);
		Assert.assertNull(datasource);
		Assert.assertEquals(HttpStatus.NOT_FOUND.value(), servletResponse.getStatus());
	}

	@Test
	public void testCallAction() throws Exception {
		addParameter("action", "create");
		addParameter("form_action", "create");
		addParameter("name", "new name");
		initParameters();
		Action action = applicationProvider.processAction(servletResponse, true, request, "create", "events",
				marshallService);
		String resultXML = marshallService.marshallNonRoot(action, Action.class);
		WritingXmlValidator.validateXml(resultXML, "xml/" + getClass().getSimpleName() + "-testCallAction.xml");
	}

	@Test
	public void testCallActionNotFound() throws Exception {
		Action action = applicationProvider.processAction(servletResponse, true, request, "foo", "bar",
				marshallService);
		Assert.assertNull(action);
		Assert.assertEquals(HttpStatus.NOT_FOUND.value(), servletResponse.getStatus());
	}

	private void runTest(Path pathInfo) throws JAXBException, IOException {
		ApplicationReference pageReference = applicationProvider.process(request, marshallService, pathInfo,
				platformConfig);
		String controlFile = getControlFile();
		WritingXmlValidator.validateXml(pageReference, controlFile);
	}

	private void runMonitoredTest(Path pathInfo) throws JAXBException, IOException {
		ApplicationReference pageReference = monitoredApplicationProvider.process(request, marshallService, pathInfo,
				platformConfig);
		XPathDifferenceHandler differenceListener = new XPathDifferenceHandler();
		differenceListener.ignoreDifference("/applicationReference/pages/page/@executionTime");
		differenceListener.ignoreDifference("/applicationReference/pages/page/structure/section/@executionTime");
		differenceListener
				.ignoreDifference("/applicationReference/pages/page/structure/section/element/@executionTime");
		WritingXmlValidator.validateXml(pageReference, getControlFile(), differenceListener);
	}

	private void runTest(Path pathInfo, DifferenceListener differenceListener) throws JAXBException, IOException {
		ApplicationReference pageReference = applicationProvider.process(request, marshallService, pathInfo,
				platformConfig);
		String controlFile = getControlFile();
		WritingXmlValidator.validateXml(pageReference, controlFile, differenceListener);
	}

	private String getControlFile() {
		String controlFile = "xml/" + getClass().getSimpleName() + "-"
				+ Thread.currentThread().getStackTrace()[3].getMethodName() + ".xml";
		return controlFile;
	}

	private Path getPathInfo(String path) {
		return new PathInfo("localhost", "http://localhost:8080", "manager", "/ws/manager/" + path, "/ws", "/services",
				new ArrayList<>(), new ArrayList<>(), "repository", "jsp");
	}
}
