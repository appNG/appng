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
import java.util.Set;

import org.appng.api.Path;
import org.appng.api.Platform;
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
import org.appng.testsupport.TestBase;
import org.appng.testsupport.validation.WritingJsonValidator;
import org.appng.xml.application.ApplicationInfo;
import org.junit.Before;
import org.junit.Ignore;
import org.mockito.Mockito;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;

@Ignore
@ContextConfiguration(locations = { "classpath:applications/application1/beans.xml", TestBase.TESTCONTEXT,
		TestBase.TESTCONTEXT_JPA }, inheritLocations = false, initializers = OpenApiActionTest.class)
class OpenApiTest extends TestBase {

	protected static final String PAGE_ID = "entity";
	protected static final String TESTAPPLICATION = "demo-application";
	protected ApplicationProvider applicationProvider;

	static {
		WritingJsonValidator.writeJson = false;
	}

	public OpenApiTest() {
		super(TESTAPPLICATION, "src/test/resources/applications/application1");
		setEntityPackage("org.appng.persistence.model");
		setRepositoryBase("");
	}

	@Override
	@Before
	public void setup() throws Exception {
		super.setup();
		environment.setAttribute(Scope.REQUEST, EnvironmentKeys.PATH_INFO, Mockito.mock(Path.class));
		environment.setAttribute(Scope.PLATFORM, Platform.Environment.APPNG_VERSION, "1.25.x");
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

}
