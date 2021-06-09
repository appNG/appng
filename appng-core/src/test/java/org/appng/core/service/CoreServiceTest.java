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
package org.appng.core.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

import javax.persistence.EntityManager;

import org.apache.commons.lang3.time.DateUtils;
import org.appng.api.BusinessException;
import org.appng.api.FieldProcessor;
import org.appng.api.Platform;
import org.appng.api.Scope;
import org.appng.api.SiteProperties;
import org.appng.api.auth.PasswordPolicy;
import org.appng.api.auth.PasswordPolicy.ValidationResult;
import org.appng.api.messaging.Sender;
import org.appng.api.messaging.TestReceiver;
import org.appng.api.messaging.TestReceiver.TestSerializer;
import org.appng.api.model.Application;
import org.appng.api.model.AuthSubject;
import org.appng.api.model.Group;
import org.appng.api.model.Permission;
import org.appng.api.model.Properties;
import org.appng.api.model.Property;
import org.appng.api.model.Role;
import org.appng.api.model.Site;
import org.appng.api.model.Site.SiteState;
import org.appng.api.model.Subject;
import org.appng.api.model.UserType;
import org.appng.api.support.FieldProcessorImpl;
import org.appng.api.support.PropertyHolder;
import org.appng.api.support.SiteClassLoader;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.core.controller.messaging.NodeEvent;
import org.appng.core.controller.messaging.NodeEvent.NodeState;
import org.appng.core.controller.messaging.SiteStateEvent;
import org.appng.core.domain.ApplicationImpl;
import org.appng.core.domain.DatabaseConnection;
import org.appng.core.domain.DatabaseConnection.DatabaseType;
import org.appng.core.domain.GroupImpl;
import org.appng.core.domain.PlatformEvent.Type;
import org.appng.core.domain.PlatformEventListener;
import org.appng.core.domain.PropertyImpl;
import org.appng.core.domain.RepositoryImpl;
import org.appng.core.domain.RoleImpl;
import org.appng.core.domain.SiteApplication;
import org.appng.core.domain.SiteImpl;
import org.appng.core.domain.SubjectImpl;
import org.appng.core.model.AccessibleApplication;
import org.appng.core.model.Repository;
import org.appng.core.model.RepositoryCacheFactory;
import org.appng.core.model.RepositoryMode;
import org.appng.core.model.RepositoryType;
import org.appng.core.repository.DatabaseConnectionRepository;
import org.appng.core.security.BCryptPasswordHandler;
import org.appng.core.security.ConfigurablePasswordPolicy;
import org.appng.core.security.DefaultPasswordPolicy;
import org.appng.core.security.DigestUtil;
import org.appng.core.security.PasswordHandler;
import org.appng.core.security.SaltedDigest;
import org.appng.core.security.SaltedDigestSha1;
import org.appng.core.security.Sha1PasswordHandler;
import org.appng.core.service.MigrationService.MigrationStatus;
import org.appng.testsupport.persistence.TestDataProvider;
import org.appng.xml.platform.Messages;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Transactional(rollbackFor = BusinessException.class)
@Rollback(false)
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = PlatformTestConfig.class, initializers = TestInitializer.class)
@DirtiesContext
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CoreServiceTest {

	@Autowired
	protected ApplicationContext context;

	@Autowired
	private CoreService coreService;

	@Mock
	private DefaultEnvironment environment;

	private MockHttpServletRequest servletRequest = new MockHttpServletRequest();

	@Autowired
	private EntityManager entityManager;

	private static boolean init = true;
	private String rootPath = "target/ROOT";
	private Properties platformConfig;

	private Subject envSubject;

	static {
		RepositoryCacheFactory.init(null, null, null, null, false);
	}

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		if (init) {
			context.getBean(TestDataProvider.class).writeTestData(entityManager);
			init = false;
		}
		java.util.Properties defaultOverrides = new java.util.Properties();
		defaultOverrides.put(PropertySupport.PREFIX_PLATFORM + Platform.Property.INACTIVE_LOCK_PERIOD, "90");
		defaultOverrides.put(PropertySupport.PREFIX_PLATFORM + Platform.Property.MAX_LOGIN_ATTEMPTS, "3");
		platformConfig = coreService.initPlatformConfig(defaultOverrides, rootPath, false, true, false);
		Mockito.when(environment.getAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG))
				.thenReturn(platformConfig);
		Mockito.doAnswer(i -> {
			envSubject = i.getArgumentAt(0, Subject.class);
			return null;
		}).when(environment).setSubject(Mockito.any());
		Mockito.when(environment.getSubject()).thenReturn(envSubject);
		Map<String, Site> siteMap = new HashMap<>();
		for (Integer siteId : coreService.getSiteIds()) {
			SiteImpl site = coreService.getSite(siteId);
			site.setSiteClassLoader(new SiteClassLoader(new URL[0], getClass().getClassLoader(), site.getName()));
			siteMap.put(site.getName(), site);
		}
		Mockito.when(environment.getAttribute(Scope.PLATFORM, Platform.Environment.SITES)).thenReturn(siteMap);
	}

	@Test
	public void testAddGroupsToSubject() throws BusinessException {
		List<String> groupNames = new ArrayList<>(Arrays.asList("group-1", "group-2", "group-3"));
		coreService.addGroupsToSubject("subject-1", groupNames, true);
		SubjectImpl subject = coreService.getSubjectByName("subject-1", true);
		for (Group group : subject.getGroups()) {
			groupNames.remove(group.getName());
		}
		assertTrue(groupNames.isEmpty());
	}

	@Test
	public void testAddApplicationRolesToGroup() {
		coreService.addApplicationRolesToGroup("group-3", "foobar", Arrays.asList("debugger"), true);
		Group group = coreService.getGroupByName("group-3");
		Set<Role> applicationRoles = group.getRoles();
		assertEquals(1, applicationRoles.size());
		assertEquals("debugger", applicationRoles.iterator().next().getName());
	}

	@Test
	public void testAssignGroupsToSubject() {
		List<Integer> groupIds = new ArrayList<>(Arrays.asList(1, 2, 3));
		coreService.assignGroupsToSubject(1, groupIds, true);
		Subject subject = coreService.getSubjectById(1, true);
		for (Group group : subject.getGroups()) {
			groupIds.remove(group.getId());
		}
		assertTrue(groupIds.isEmpty());
	}

	@Test
	public void testAssignApplicationRolesToGroup() {
		int groupId = 1;
		int roleId = 1;
		SiteImpl site = coreService.getSite(1);
		Group group = coreService.getGroupByName("group-1");
		coreService.assignRolesToGroup(group, site, Arrays.asList(roleId));
		assertEquals(Integer.valueOf(groupId), group.getId());
		boolean roleFound = false;
		for (Role applicationRole : group.getRoles()) {
			roleFound = applicationRole.getId().equals(roleId);
			if (roleFound)
				break;
		}
		assertTrue(roleFound);
	}

	@Test
	public void testAssignApplicationToSite() {
		SiteImpl site = coreService.getSite(1);
		Application application = coreService.findApplicationByName("foobar");
		MigrationStatus state = coreService.assignApplicationToSite(site, application, true);
		assertEquals(MigrationStatus.NO_DB_SUPPORTED, state);
		Iterable<PropertyImpl> properties = coreService.getProperties(1, application.getId());
		String prefix = "platform.site." + site.getName() + ".application." + application.getName() + ".";
		PropertyHolder propertyHolder = new PropertyHolder(prefix, properties);
		assertEquals("foobaz", propertyHolder.getString("foobar"));
		Property prop = propertyHolder.getProperty("foobar");
		assertEquals(Property.Type.TEXT, prop.getType());
		assertEquals(prefix + "foobar", prop.getName());
	}

	@Test
	public void testAssignApplicationToSiteErroneous() {
		SiteImpl site = coreService.getSite(1);
		Application application = coreService.findApplicationByName("foobar");
		DatabaseConnection dbc = new DatabaseConnection();
		CoreService mockedCoreService = new CoreService() {
			protected MigrationStatus createDatabaseConnection(SiteApplication siteApplication) {
				siteApplication.setDatabaseConnection(dbc);
				return MigrationStatus.ERROR;
			}
		};
		mockedCoreService.databaseConnectionRepository = Mockito.mock(DatabaseConnectionRepository.class);
		mockedCoreService.auditableListener = Mockito.mock(PlatformEventListener.class);
		mockedCoreService.databaseService = Mockito.mock(DatabaseService.class);
		MigrationStatus state = mockedCoreService.assignApplicationToSite(site, application, true);
		assertEquals(MigrationStatus.ERROR, state);
		Mockito.verify(mockedCoreService.databaseService).dropDataBaseAndUser(dbc);
		Mockito.verify(mockedCoreService.databaseConnectionRepository).delete(dbc);
		Mockito.verify(mockedCoreService.auditableListener).createEvent(Mockito.eq(Type.ERROR), Mockito.anyString());
	}

	@Test
	public void testCreateApplicationProperty() {
		PropertyImpl property = coreService.createProperty(null, null, new PropertyImpl("foobaz", "foobar"));
		assertTrue(coreService.checkPropertyExists(null, null, new PropertyImpl("foobaz", "foobar")));
		assertEquals(Property.Type.TEXT, property.getType());
	}

	@Test
	public void testCreateDatabaseConnection() {
		DatabaseConnection databaseConnection = new DatabaseConnection(DatabaseType.HSQL, "testdb", "sa",
				"".getBytes());
		databaseConnection.setName("testconection");
		DatabaseConnection savedConnection = coreService.createDatabaseConnection(databaseConnection, true);

		assertNotNull(savedConnection.getId());
		assertNotNull(savedConnection.getVersion());
		assertEquals(databaseConnection.getName(), savedConnection.getName());
		assertEquals(databaseConnection.getType(), savedConnection.getType());
		assertEquals(databaseConnection.getJdbcUrl(), savedConnection.getJdbcUrl());
		assertTrue(savedConnection.isManaged());
	}

	@Test
	public void testCreateGroup() {
		GroupImpl group = new GroupImpl();
		group.setName("groupy-group");
		coreService.createGroup(group);
		assertNotNull(group.getId());
		assertNotNull(group.getVersion());
	}

	@Test
	public void testCreateApplicationRepository() throws URISyntaxException {
		RepositoryImpl repository = new RepositoryImpl();
		repository.setName("name");
		repository.setRepositoryMode(RepositoryMode.ALL);
		repository.setRepositoryType(RepositoryType.LOCAL);
		repository.setActive(true);
		repository.setUri(new URI("file:/tmp/repo"));
		RepositoryImpl savedRepo = coreService.createRepository(repository);
		assertNotNull(savedRepo.getId());
		assertNotNull(savedRepo.getVersion());
	}

	@Test
	public void testCreatePropertyForApplication() {
		coreService.createProperty(null, 1, new PropertyImpl("foo", "bar"));
		assertTrue(coreService.checkPropertyExists(null, 1, new PropertyImpl("foo", "bar")));
	}

	@Test
	public void testCreatePropertyForSite() {
		PropertyImpl property = coreService.createProperty(1, null, new PropertyImpl("foo", "bar"));
		assertTrue(coreService.checkPropertyExists(1, null, new PropertyImpl("foo", "bar")));
		assertEquals(Property.Type.TEXT, property.getType());
	}

	@Test
	public void testCreatePropertyForSiteAndApplication() {
		coreService.createProperty(1, 1, new PropertyImpl("foo", "bar"));
		assertTrue(coreService.checkPropertyExists(1, 1, new PropertyImpl("foo", "bar")));
	}

	@Test
	public void testCreateSite() {
		SiteImpl site = new SiteImpl();
		site.setName("example");
		site.setHost("example.com");
		site.setDomain("example.com");
		site.setActive(true);
		coreService.createSite(site);
		validateSite(site);
	}

	@Test
	public void testCreateSubject() {
		SubjectImpl subject = new SubjectImpl();
		subject.setRealname("John Doe");
		subject.setLanguage("de");
		subject.setName("john");
		subject.setUserType(UserType.LOCAL_USER);
		Subject savedSubject = coreService.createSubject(subject);
		assertNotNull(savedSubject);
		assertNotNull(savedSubject.getId());
	}

	@Test
	public void testDeleteApplicationRepository() {
		coreService.deleteApplicationRepository(coreService.getApplicationRepositoryByName("name"));
	}

	@Test
	@Ignore
	public void testDeleteApplicationResource() {
	}

	@Test
	public void testDeleteApplicationRole() throws BusinessException {
		coreService.deleteRole(1, "applicationroleDeleteError", "applicationroleErrorInvalid");
	}

	@Test(expected = BusinessException.class)
	public void testDeleteApplicationRoleInvalid() throws BusinessException {
		try {
			coreService.deleteRole(7, "applicationroleDeleteError", "applicationroleErrorInvalid");
		} catch (BusinessException e) {
			assertEquals("applicationroleErrorInvalid", e.getMessageKey());
			throw e;
		}
	}

	@Rollback
	@Test(expected = BusinessException.class)
	public void testDeleteApplicationSiteActive() throws BusinessException {
		coreService.deleteApplication("bugtracker", new FieldProcessorImpl("delete"));
	}

	@Test
	@Ignore
	public void testDeleteApplicationSiteInactive() throws BusinessException {
	}

	@Test
	@Ignore
	public void testDeleteApplicationVersion() {
	}

	@Test
	@Ignore
	public void testDeleteApplicationWithEnvironment() throws BusinessException {
	}

	@Test
	@Ignore
	public void testDeleteProperty() {
	}

	@Test
	public void testDeleteSite() throws BusinessException {
		FieldProcessorImpl fp = new FieldProcessorImpl("delete");
		coreService.deleteSite("site-3", fp);
		assertFalse(fp.hasErrors());
	}

	@Test
	public void testDeleteSiteWithEnvironment() throws BusinessException, IOException, InterruptedException {
		SiteImpl site = coreService.getSite(2);
		Map<String, Site> siteMap = environment.getAttribute(Scope.PLATFORM, Platform.Environment.SITES);
		TestReceiver receiver = new TestReceiver();
		receiver.runWith(Executors.newSingleThreadExecutor());
		String nodeId = "test";
		receiver.configure(new TestSerializer(environment, nodeId));
		Sender sender = receiver.createSender();
		Mockito.when(environment.getAttribute(Scope.PLATFORM, Platform.Environment.MESSAGE_SENDER)).thenReturn(sender);
		Mockito.when(environment.getAttribute(Scope.PLATFORM, Platform.Environment.CORE_PLATFORM_CONTEXT))
				.thenReturn(context);
		Map<String, NodeState> nodeStates = new ConcurrentHashMap<>();
		Map<String, SiteState> stateMap = new ConcurrentHashMap<>();
		Mockito.when(environment.getAttribute(Scope.PLATFORM, NodeEvent.NODE_STATE)).thenReturn(nodeStates);
		Mockito.when(environment.getAttribute(Scope.PLATFORM, SiteStateEvent.SITE_STATE)).thenReturn(stateMap);
		SiteImpl realSite = (SiteImpl) siteMap.get(site.getName());
		realSite.setSender(sender);
		realSite.setState(SiteState.STARTING);
		realSite.setState(SiteState.STARTED);
		while (null == nodeStates.get(nodeId)) {
			Thread.sleep(100);
		}
		CacheService.createCacheManager(HazelcastConfigurer.getInstance(null), false);

		coreService.deleteSite(environment, site);
		// 5x SiteStateEvent(STARTING, STARTED, STOPPING, STOPPED, DELETED)
		// 5x NodeEvent
		// 1x SiteDeletedEvent
		while (11 != receiver.getProcessed().size()) {
			Thread.sleep(100);
		}
		LOGGER.info("Processed {} events", receiver.getProcessed().size());
		Thread.sleep(1000);
		Assert.assertNull(stateMap.get(realSite.getName()));
		receiver.close();
	}

	@Test
	public void testDeleteSubject() {
		Subject subject = coreService.getSubjectByName("john", false);
		coreService.deleteSubject(subject);
		assertNull(coreService.getSubjectById(subject.getId(), false));
	}

	@Test
	public void testFindApplicationByName() {
		Application application = coreService.findApplicationByName("manager");
		assertEquals("manager", application.getName());
	}

	@Test
	public void testGetApplicationProperties() {
		coreService.saveProperty(new PropertyImpl(PropertySupport.PREFIX_PLATFORM + "foo", "bar"));
		PropertyHolder platformProperties = coreService.getPlatformProperties();
		assertEquals("bar", platformProperties.getString("foo"));
	}

	@Test
	@Ignore
	public void testGetDatabaseConnection() {
	}

	@Test
	@Ignore
	public void testGetDatabaseConnections() {
	}

	@Test
	public void testGetGroupByName() {
		Group group = coreService.getGroupByName("group-1");
		assertEquals(Integer.valueOf(1), group.getId());
	}

	@Test
	public void testGetGroups() {
		List<? extends Group> groups = coreService.getGroups();
		assertEquals(4, groups.size());
		assertEquals("group-1", groups.get(0).getName());
		assertEquals("group-2", groups.get(1).getName());
		assertEquals("group-3", groups.get(2).getName());
		assertEquals("groupy-group", groups.get(3).getName());
	}

	@Test
	public void testGetApplicationFolder() {
		assertEquals(new File(rootPath + "/applications/manager"),
				coreService.getApplicationFolder(environment, "manager"));
	}

	@Test
	public void testGetApplicationRepositories() {
		List<RepositoryImpl> applicationRepositories = coreService.getApplicationRepositories();
		assertEquals(1, applicationRepositories.size());
		assertEquals("repo", applicationRepositories.get(0).getName());
	}

	@Test
	@Ignore
	public void testGetApplicationRepositoryByName() {
	}

	@Test
	@Ignore
	public void testGetApplicationResourceHolder() {
	}

	@Test
	public void testGetApplicationRolesForApplication() {
		List<RoleImpl> applicationRoles = coreService.getApplicationRolesForApplication(2);
		assertEquals(1, applicationRoles.size());
		assertEquals("user", applicationRoles.get(0).getName());
	}

	@Test
	public void testGetApplicationRootFolder() {
		assertEquals(new File(rootPath + "/applications"), coreService.getApplicationRootFolder(environment));
	}

	@Test
	@Ignore
	public void testGetApplications() {
	}

	@Test
	@Ignore
	public void testGetApplicationSubjects() {
	}

	@Test
	public void testGetPropertiesIntegerInteger() {
		Iterable<PropertyImpl> properties = coreService.getProperties(1, 1);
		Iterator<PropertyImpl> iterator = properties.iterator();
		PropertyImpl prop = iterator.next();
		assertEquals("platform.site.site-1.application.manager.foo", prop.getName());
		assertEquals("bar", prop.getString());
		iterator.next();
		assertFalse(iterator.hasNext());
	}

	@Test
	public void testGetPropertiesStringString() {
		Iterable<PropertyImpl> properties = coreService.getProperties("site-1", "manager");
		Iterator<PropertyImpl> iterator = properties.iterator();
		PropertyImpl prop = iterator.next();
		assertEquals("platform.site.site-1.application.manager.foo", prop.getName());
		assertEquals("bar", prop.getString());
		iterator.next();
		assertFalse(iterator.hasNext());
	}

	@Test
	@Ignore
	public void testGetProperty() {
	}

	@Test
	public void testGetSite() {
		SiteImpl site = coreService.getSite(1);
		assertNotNull(site);
		assertEquals(Integer.valueOf(1), site.getId());
	}

	@Test
	public void testGetSiteByName() {
		SiteImpl site = coreService.getSiteByName("site-1");
		assertNotNull(site);
		assertEquals(Integer.valueOf(1), site.getId());
		assertEquals("site-1", site.getName());
	}

	@Test
	public void testGetSiteIds() {
		List<Integer> siteIds = coreService.getSiteIds();
		assertEquals(2, siteIds.size());
		assertEquals(Integer.valueOf(1), siteIds.get(0));
		assertEquals(Integer.valueOf(4), siteIds.get(1));
	}

	@Test
	public void testGetSites() {
		List<SiteImpl> sites = coreService.getSites();
		assertEquals(2, sites.size());
		assertEquals("site-1", sites.get(0).getName());
		assertEquals("example", sites.get(1).getName());
	}

	@Test
	public void testGetSubjectByEmail() {
		Subject subject = coreService.getSubjectByEmail("subject1@aiticon.de");
		assertEquals(Integer.valueOf(1), subject.getId());
	}

	@Test
	public void testGetSubjectById() {
		Subject subject = coreService.getSubjectById(1, false);
		assertEquals(Integer.valueOf(1), subject.getId());
	}

	@Test
	public void testGetSubjectByName() {
		Subject subject = coreService.getSubjectByName("subject-1", false);
		assertEquals(Integer.valueOf(1), subject.getId());
	}

	@Test
	public void testGetSubjects() {
		List<SubjectImpl> subjects = coreService.getSubjects();
		assertEquals(3, subjects.size());
		assertEquals("subject-1", subjects.get(0).getName());
		assertEquals("subject-2", subjects.get(1).getName());
		assertEquals("subject-3", subjects.get(2).getName());
	}

	@Test
	@Ignore
	public void testInitApplicationPropertiesApplicationProvider() {
	}

	@Test
	@Ignore
	public void testInitApplicationPropertiesSiteApplicationProvider() {
	}

	@Test
	public void testInitSitePropertiesSiteImpl() {
		SiteImpl site = coreService.getSite(1);
		coreService.initSiteProperties(site);
		validateSite(site);
	}

	@Test
	public void testLoginPrincipal() {
		Subject subject = coreService.getSubjectById(2, true);
		Mockito.when(environment.getServletRequest()).thenReturn(servletRequest);
		Principal principal = Mockito.mock(Principal.class);
		Mockito.when(principal.getName()).thenReturn("subject-2");
		boolean success = coreService.login(environment, principal);
		assertTrue(success);
		Mockito.verify(environment).setSubject(subject);
		logout();
	}

	private void logout() {
		Mockito.when(environment.getSubject()).thenReturn(new SubjectImpl());
		coreService.logoutSubject(environment);
		Mockito.verify(environment).logoutSubject();
	}

	@Test
	public void testLoginPrincipalWithGroup() {
		servletRequest.addUserRole("subject-1");
		Mockito.when(environment.getServletRequest()).thenReturn(servletRequest);
		Principal principal = Mockito.mock(Principal.class);
		boolean success = coreService.login(environment, principal);
		assertTrue(success);
		Mockito.verify(environment).setSubject(Mockito.any(SubjectImpl.class));
		logout();
	}

	@Test
	public void testLoginDigest() {
		Subject subject = coreService.getSubjectById(3, true);
		String sharedSecret = platformConfig.getString(Platform.Property.SHARED_SECRET);
		boolean success = coreService.login(environment, DigestUtil.getDigest(subject.getName(), sharedSecret), 3);
		assertTrue(success);
		Mockito.verify(environment).setSubject(subject);
		logout();
	}

	@Test
	public void testLoginGroup() {
		SubjectImpl authSubject = new SubjectImpl();
		authSubject.setDigest(AppNGTestDataProvider.DIGEST);
		authSubject.setSalt(AppNGTestDataProvider.SALT);
		authSubject.setEmail("john@doe.org");
		authSubject.setTimeZone(TimeZone.getDefault().getDisplayName());
		authSubject.setLanguage(Locale.ENGLISH.getLanguage());
		authSubject.setName("johndoe");
		authSubject.setRealname("John Doe");
		boolean success = coreService.loginGroup(environment, authSubject, "test", 1);
		assertTrue(success);
		ArgumentCaptor<Subject> subjectCaptor = ArgumentCaptor.forClass(Subject.class);
		Mockito.verify(environment).setSubject(subjectCaptor.capture());
		Subject subject = subjectCaptor.getValue();
		Assert.assertEquals(authSubject.getEmail(), subject.getEmail());
		Assert.assertEquals(authSubject.getLanguage(), subject.getLanguage());
		Assert.assertEquals(authSubject.getTimeZone(), subject.getTimeZone());
		Assert.assertEquals(authSubject.getAuthName(), subject.getName());
		Assert.assertEquals(authSubject.getRealname(), subject.getRealname());
		logout();
	}

	@Test
	public void testLogin() {
		boolean success = coreService.login(null, environment, "subject-3", "test");
		assertTrue(success);
		Mockito.verify(environment).setSubject(Mockito.any(SubjectImpl.class));

		SubjectImpl subject = coreService.getSubjectByName("subject-3", true);
		assertNotNull(subject.getLastLogin());
		assertFalse(subject.isLocked());
		assertNotNull(envSubject);
		assertFalse(envSubject.isLocked());
		logout();
	}

	@Test
	public void testLoginFailedAttempts() {
		for (int i = 1; i <= 3; i++) {
			assertFalse(coreService.login(null, environment, "subject-3", "wrong"));
			SubjectImpl subject = coreService.getSubjectByName("subject-3", true);
			if (i < 3) {
				assertFalse(subject.isLocked());
			}
			assertEquals(Integer.valueOf(i), subject.getFailedLoginAttempts());
		}
		Mockito.verify(environment, Mockito.times(2)).setAttribute(Scope.REQUEST, "subject.locked", false);
		Mockito.verify(environment, Mockito.times(1)).setAttribute(Scope.REQUEST, "subject.locked", true);

		SubjectImpl subject = coreService.getSubjectByName("subject-3", false);
		assertFalse(subject.isExpired(new Date()));
		assertTrue(subject.isLocked());
		Mockito.verify(environment, Mockito.never()).setSubject(Mockito.any());
		resetSubject("subject-3");
	}

	@Test
	public void testLoginSubjectIsExpired() {
		SubjectImpl subject = coreService.getSubjectByName("subject-3", false);
		subject.setExpiryDate(new Date());
		coreService.updateSubject(subject);
		assertFalse(coreService.login(null, environment, "subject-3", "test"));

		Mockito.verify(environment).setAttribute(Scope.REQUEST, "subject.locked", true);

		subject = coreService.getSubjectByName("subject-3", false);
		assertTrue(subject.isExpired(new Date()));
		assertTrue(subject.isLocked());

		Mockito.verify(environment, Mockito.never()).setSubject(Mockito.any());
		resetSubject("subject-3");
	}

	@Test
	public void testLoginSubjectIsLocked() {
		SubjectImpl subject = coreService.getSubjectByName("subject-3", false);
		subject.setLocked(true);
		coreService.updateSubject(subject);
		assertFalse(coreService.login(null, environment, "subject-3", "test"));

		Mockito.verify(environment).setAttribute(Scope.REQUEST, "subject.locked", true);
		Mockito.verify(environment, Mockito.never()).setSubject(Mockito.any());
		resetSubject("subject-3");
	}

	@Test
	public void testLoginSubjectIsInactive() {
		SubjectImpl subject = coreService.getSubjectByName("subject-3", false);
		subject.setLastLogin(DateUtils.addDays(new Date(), -91));
		coreService.updateSubject(subject);
		assertFalse(coreService.login(null, environment, "subject-3", "test"));

		Mockito.verify(environment).setAttribute(Scope.REQUEST, "subject.locked", true);
		subject = coreService.getSubjectByName("subject-3", false);
		assertTrue(subject.isLocked());
		assertFalse(subject.isExpired(new Date()));
		assertTrue(subject.getFailedLoginAttempts() == 0);
		Mockito.verify(environment, Mockito.never()).setSubject(Mockito.any());
		resetSubject("subject-3");
	}

	private void resetSubject(String name) {
		SubjectImpl subject = coreService.getSubjectByName(name, false);
		subject.setLocked(false);
		subject.setExpiryDate(null);
		subject.setFailedLoginAttempts(0);
		coreService.updateSubject(subject);
	}

	@Test
	public void testLdapLogin() {
		SiteImpl site = coreService.getSite(1);
		boolean success = coreService.login(site, environment, "subject-2", "tester");
		assertFalse(success);
	}

	@Test
	public void testLdapLoginGroup() {
		SiteImpl site = coreService.getSite(1);
		boolean success = coreService.login(site, environment, "subject-1", "tester");
		assertFalse(success);
	}

	@Test
	public void testProvideApplication() throws URISyntaxException, BusinessException {
		RepositoryImpl repository = new RepositoryImpl();
		repository.setActive(true);
		repository.setName("testrepo");
		repository.setRepositoryMode(RepositoryMode.STABLE);
		repository.setRepositoryType(RepositoryType.LOCAL);
		URI uri = getClass().getClassLoader().getResource("zip").toURI();
		repository.setUri(uri);
		coreService.createRepository(repository);
		String applicationName = "demo-application";
		String applicationVersion = "1.5.2";
		String applicationTimestamp = "2012-11-27-1305";
		String appngVersion = "1.0.0-M1";

		coreService.installPackage(repository.getId(), applicationName, applicationVersion, applicationTimestamp, true,
				false, true);

		Application application = coreService.findApplicationByName(applicationName);
		((ApplicationImpl) application).setDisplayName("a cool display name");

		validateApplication(appngVersion, applicationName, applicationVersion, applicationTimestamp, application);

		assertEquals("bar", application.getProperties().getString("foo"));
		Property property = ((PropertyHolder) application.getProperties()).getProperty("foo");
		assertEquals("a foo property", property.getDescription());
		assertEquals(Property.Type.TEXT, property.getType());
		Property multiline = ((PropertyHolder) application.getProperties()).getProperty("clobValue");
		assertEquals(Property.Type.MULTILINE, multiline.getType());
		assertEquals("a\nb\nc", application.getProperties().getClob("clobValue"));
	}

	@Test
	public void testProvideApplicationWithAdminRole() throws URISyntaxException, BusinessException {
		RepositoryImpl repository = new RepositoryImpl();
		repository.setActive(true);
		repository.setName("testrepo");
		repository.setRepositoryMode(RepositoryMode.STABLE);
		repository.setRepositoryType(RepositoryType.LOCAL);
		URI uri = getClass().getClassLoader().getResource("zip").toURI();
		repository.setUri(uri);
		coreService.createRepository(repository);
		String applicationName = "demo-application";
		String applicationVersion = "1.5.4";
		String applicationTimestamp = "2017-04-10-1046";
		GroupImpl adminGroup = new GroupImpl();
		adminGroup.setName("Administrators");
		adminGroup.setDescription("Admin Group");
		adminGroup.setDefaultAdmin(true);
		coreService.createGroup(adminGroup);

		FieldProcessor fp = new FieldProcessorImpl("REF_TEST");
		coreService.installPackage(repository.getId(), applicationName, applicationVersion, applicationTimestamp, true,
				false, true, fp, false);

		Messages messages = fp.getMessages();
		assertEquals(1, messages.getMessageList().size());
		assertEquals("Add admin role(s) Administrator to admin group(s) Administrators ",
				messages.getMessageList().get(0).getContent());

		GroupImpl groupByName = coreService.getGroupByName("Administrators");
		assertEquals(1, groupByName.getRoles().size());
		Role role = groupByName.getRoles().iterator().next();
		assertEquals("Administrator", role.getName());
		assertEquals("demo-application", role.getApplication().getName());

	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void testProvideApplicationUpdate() throws URISyntaxException, BusinessException {
		Repository repository = coreService.getApplicationRepositoryByName("testrepo");
		String applicationName = "demo-application";
		String applicationVersion = "1.5.3";
		String applicationTimestamp = "2013-01-13-1303";
		String appngVersion = "1.0.0-M3";

		SiteImpl site = coreService.getSite(1);
		AccessibleApplication currentApplication = coreService.findApplicationByName("demo-application");
		coreService.assignApplicationToSite(site, currentApplication, true);

		coreService.installPackage(repository.getId(), applicationName, applicationVersion, applicationTimestamp, true,
				false, true);
		Application application = coreService.findApplicationByName(applicationName);
		validateApplication(appngVersion, applicationName, applicationVersion, applicationTimestamp, application);

		validatePermissionsPresent(application, new ArrayList(Arrays.asList("testPermission")));
		validateRolesPresent(application, new ArrayList(Arrays.asList("Tester")));

		// the clob property has been updated in the platform, therefore it is the new
		// clob value
		// of the latest package
		validateProperties((PropertyHolder) application.getProperties(), "d\ne\nf");

		// the clob property, configured in the site was not updated by updating the
		// package. It is still
		// the old one which has been defined initially when the application has been
		// assigned to the site
		coreService.initApplicationProperties(site, currentApplication);
		validateProperties((PropertyHolder) currentApplication.getProperties(), "a\nb\nc");

		coreService.deleteApplicationRepository(repository);
	}

	private void validateProperties(PropertyHolder propertyHolder, String ecpectedClobValue) {
		assertEquals("foobar", propertyHolder.getString("foo"));
		Property foo = propertyHolder.getProperty("foo");
		assertEquals(Property.Type.TEXT, foo.getType());
		assertEquals("a foo property [UPDATED]", foo.getDescription());

		assertEquals("foobaz", propertyHolder.getString("bar"));
		assertEquals("a new property", propertyHolder.getProperty("bar").getDescription());
		assertEquals(ecpectedClobValue, propertyHolder.getClob("clobValue"));
		Property clobValue = propertyHolder.getProperty("clobValue");
		assertEquals(Property.Type.MULTILINE, clobValue.getType());
	}

	@Test
	public void testProvideApplicationUpdateDelete() throws BusinessException {
		String applicationName = "demo-application";
		coreService.unlinkApplicationFromSite(1, coreService.findApplicationByName(applicationName).getId());
		coreService.deleteApplication(applicationName, new FieldProcessorImpl("deleteApplication"));
		assertNull(coreService.findApplicationByName("demo-application"));
	}

	@Test(expected = BusinessException.class)
	public void testReloadRepository() throws BusinessException {
		coreService.reloadRepository(1);
	}

	@Test
	@Ignore
	public void testResetConnection() {
	}

	@Test
	public void testResetPassword() {
		AuthSubject subject = coreService.getSubjectByName("subject-3", true);
		String currentDigest = subject.getDigest();
		String currentSalt = subject.getSalt();
		SaltedDigest saltedDigest = new SaltedDigestSha1();
		if (null == currentSalt) {
			currentSalt = saltedDigest.getSalt();
			subject.setSalt(currentSalt);
		}
		String hash = saltedDigest.getDigest(subject.getEmail(), currentSalt);
		coreService.resetPassword(subject, new DefaultPasswordPolicy(), subject.getEmail(), hash);
		assertNull(subject.getSalt());
		assertNotEquals(subject.getDigest(), currentDigest);
		assertTrue(subject.getDigest().startsWith(BCryptPasswordHandler.getPrefix()));
	}

	@Test
	public void testRestoreSubject() {
		Subject subject = coreService.restoreSubject("subject-1");
		assertTrue(subject.isAuthenticated());
		assertNull(subject.getDigest());
		assertNull(subject.getSalt());
	}

	@Test
	public void testSaveProperties() {
		List<Property> props = new ArrayList<>();
		PropertyImpl a = new PropertyImpl("foobaz.a", "a");
		PropertyImpl b = new PropertyImpl("foobaz.b", "b");
		props.add(a);
		props.add(b);
		coreService.saveProperties(new PropertyHolder("foobaz.", props));
		assertEquals(a, coreService.getProperty("foobaz.a"));
		assertEquals(b, coreService.getProperty("foobaz.b"));
	}

	@Test
	public void testSaveProperty() {
		PropertyImpl property = coreService.saveProperty(new PropertyImpl("prop1", "value"));
		assertNotNull(property);
		assertNotNull(property.getVersion());
		assertEquals("prop1", property.getId());
		assertEquals("value", property.getString());
	}

	@Test
	@Ignore
	public void testSetSiteStartUpTime() {
	}

	@Test
	@Ignore
	public void testShutdownSite() {
	}

	@Test
	@Ignore
	public void testSynchronizeApplicationResources() {
	}

	@Test
	public void testUnlinkApplicationFromSiteIntegerString() {
		MigrationStatus migrationStatus = coreService.unlinkApplicationFromSite(1, 1);
		assertEquals(MigrationStatus.NO_DB_SUPPORTED, migrationStatus);
	}

	@Test
	@Ignore
	public void testUnsetReloadRequired() {
	}

	@Test
	public void testUpdatePassword() throws BusinessException {
		SubjectImpl subject = coreService.getSubjectByName("subject-3", false);
		PasswordPolicy dummyPolicy = new ConfigurablePasswordPolicy() {

			public ValidationResult validatePassword(String username, char[] currentPassword, char[] password) {
				return new ValidationResult(true, null);
			}
		};
		ValidationResult updatePassword = coreService.updatePassword(dummyPolicy, "test".toCharArray(),
				"foobar".toCharArray(), subject);
		assertTrue(updatePassword.isValid());
	}

	@Test
	public void testUpdatePasswordFail() throws BusinessException {
		SubjectImpl subject = coreService.getSubjectByName("subject-3", false);
		PasswordPolicy policy = new ConfigurablePasswordPolicy();
		policy.configure(null);
		ValidationResult updatePassword = coreService.updatePassword(policy, "test".toCharArray(),
				"foobar".toCharArray(), subject);
		assertFalse(updatePassword.isValid());
	}

	@Test
	@Ignore
	public void testUpdateSubject() {
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void validateApplication(String appngVersion, String applicationName, String applicationVersion,
			String applicationTimestamp, Application application) {
		assertEquals(applicationName, application.getName());
		assertEquals("a cool display name", application.getDisplayName());
		assertEquals("A demo Application", application.getDescription());
		assertEquals("This is an amazing demo application", application.getLongDescription());
		assertEquals(appngVersion, application.getAppNGVersion());
		assertEquals(applicationVersion, application.getPackageVersion());
		assertEquals(applicationTimestamp, application.getTimestamp());
		assertTrue(application.isPrivileged());
		assertTrue(application.isFileBased());
		assertFalse(application.isHidden());
		assertTrue(application.isInstalled());
		assertFalse(application.isSnapshot());

		List<String> permissions = new ArrayList(Arrays.asList("debug", "output-format.html", "output-type.webgui"));
		validatePermissionsPresent(application, permissions);

		List<String> roles = new ArrayList(Arrays.asList("Administrator", "Debugger"));
		validateRolesPresent(application, roles);

	}

	private void validateRolesPresent(Application application, List<String> roles) {
		for (Role role : application.getRoles()) {
			roles.remove(role.getName());
		}
		assertTrue(roles.isEmpty());
	}

	private void validatePermissionsPresent(Application application, List<String> permissions) {
		for (Permission perm : application.getPermissions()) {
			permissions.remove(perm.getName());
		}
		assertTrue(permissions.isEmpty());
	}

	private void validateSite(SiteImpl site) {
		Properties properties = site.getProperties();
		assertEquals("appng-manager", properties.getString(SiteProperties.DEFAULT_APPLICATION));
		assertEquals(rootPath + "/repository/" + site.getName(), properties.getString(SiteProperties.SITE_ROOT_DIR));
		assertNotNull(site.getPasswordPolicy());
	}

	@Test
	public void testMigratePassword() {
		SubjectImpl subject = new SubjectImpl();
		subject.setRealname("Deve Loper");
		subject.setLanguage("en");
		subject.setName("deve");
		subject.setUserType(UserType.LOCAL_USER);
		subject.setDigest(AppNGTestDataProvider.DIGEST);
		subject.setSalt(AppNGTestDataProvider.SALT);
		Subject persistedSubject = coreService.createSubject(subject);
		assertFalse(subject.getDigest().startsWith(BCryptPasswordHandler.getPrefix()));
		Date changedOnCreation = subject.getPasswordLastChanged();
		assertNotNull(changedOnCreation);

		PasswordHandler handler = new Sha1PasswordHandler(persistedSubject);
		handler.migrate(coreService, "veryStrongPassword");

		assertNull(subject.getSalt());
		assertTrue(subject.getDigest().startsWith(BCryptPasswordHandler.getPrefix()));
		assertNotNull(subject.getPasswordLastChanged());
		assertNotEquals(changedOnCreation, subject.getPasswordLastChanged());
		assertTrue(changedOnCreation.before(subject.getPasswordLastChanged()));

		coreService.deleteSubject(persistedSubject);
	}

}
