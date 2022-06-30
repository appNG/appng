/*
 * Copyright 2011-2022 the original author or authors.
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
package org.appng.core.controller.rest.openapi;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.appng.api.Path;
import org.appng.api.Scope;
import org.appng.api.model.FeatureProvider;
import org.appng.api.model.Permission;
import org.appng.api.model.Properties;
import org.appng.api.model.Resources;
import org.appng.api.model.Role;
import org.appng.api.support.ApplicationRequest;
import org.appng.api.support.DummyPermissionProcessor;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.api.support.environment.EnvironmentKeys;
import org.appng.core.domain.SubjectImpl;
import org.appng.core.model.AccessibleApplication;
import org.appng.core.model.ApplicationProvider;
import org.appng.openapi.model.Action;
import org.appng.openapi.model.Datasource;
import org.appng.openapi.model.PageDefinition;
import org.appng.testsupport.TestBase;
import org.appng.testsupport.validation.WritingJsonValidator;
import org.appng.xml.application.ApplicationInfo;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(locations = { "classpath:applications/application1/beans.xml", TestBase.TESTCONTEXT,
		TestBase.TESTCONTEXT_JPA }, inheritLocations = false, initializers = OpenApiTest.class)
public class OpenApiTest extends TestBase {

	private static final String PAGE_ID = "entity";
	private static final String TESTAPPLICATION = "demo-application";
	private ApplicationProvider applicationProvider;

	static {
		WritingJsonValidator.writeJson = false;
	}

	public OpenApiTest() {
		super(TESTAPPLICATION, "src/test/resources/applications/application1");
		setEntityPackage("org.appng.persistence.model");
		setRepositoryBase("");
	}

	@Test
	public void testPage() throws Exception {
		OpenApiPage openApiPage = new OpenApiPage(site, applicationProvider, request, messageSource, true) {
		};
		ResponseEntity<PageDefinition> page = openApiPage.getPage(PAGE_ID, null, environment, servletRequest,
				servletResponse);
		WritingJsonValidator.validate(page, "rest/openapi/page.json");
	}

	@Test
	public void testAction() throws Exception {
		OpenApiAction openApiAction = new OpenApiAction(site, applicationProvider, request, messageSource, true) {
		};
		Map<String, String> pathVariables = new HashMap<>();
		pathVariables.put("form_action", "create");
		pathVariables.put("action", "create");
		ResponseEntity<Action> action = openApiAction.getAction("events", "create", pathVariables, environment,
				servletRequest, servletResponse);
		WritingJsonValidator.validate(action, "rest/openapi/action.json");

		servletRequest.addParameter("form_action", "create");
		servletRequest.addParameter("action", "create");
		ResponseEntity<Action> validated = openApiAction.performActionMultiPart("events", "create", environment,
				servletRequest, servletResponse);
		WritingJsonValidator.validate(validated, "rest/openapi/action-validate.json");

		servletRequest.addParameter("name", "foobar");
		ResponseEntity<Action> performed = openApiAction.performActionMultiPart("events", "create", environment,
				servletRequest, servletResponse);
		WritingJsonValidator.validate(performed, "rest/openapi/action-performed.json");

	}

	@Test
	public void testDataSource() throws Exception {
		OpenApiDataSource openApiDatasource = new OpenApiDataSource(site, applicationProvider, request, messageSource,
				true) {
		};
		ResponseEntity<Datasource> datasource = openApiDatasource.getDataSource("entities", null, environment,
				servletRequest, servletResponse);
		WritingJsonValidator.validate(datasource, "rest/openapi/datasource.json");
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

		public void init(ApplicationInfo applicationInfo) {
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
		applicationProvider.setApplicationConfig(request.getApplicationConfig().cloneConfig(marshallService));
		ApplicationRequest applicationRequest = applicationProvider.getApplicationRequest(servletRequest,
				servletResponse);
		applicationRequest.setPermissionProcessor(new DummyPermissionProcessor(subject, site, app));
		((DefaultEnvironment) environment).setSubject(new SubjectImpl());
		subjectWithRole("Administrator");
		((DefaultEnvironment) environment).setSubject(subject);
	}

}
