/*
 * Copyright 2011-2020 the original author or authors.
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.NoResultException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.xml.bind.JAXBException;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.appng.api.ApplicationController;
import org.appng.api.BusinessException;
import org.appng.api.Environment;
import org.appng.api.FieldProcessor;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.Path;
import org.appng.api.Platform;
import org.appng.api.Request;
import org.appng.api.Scope;
import org.appng.api.SiteProperties;
import org.appng.api.auth.PasswordPolicy;
import org.appng.api.model.Application;
import org.appng.api.model.ApplicationSubject;
import org.appng.api.model.AuthSubject;
import org.appng.api.model.Group;
import org.appng.api.model.Permission;
import org.appng.api.model.Properties;
import org.appng.api.model.Property;
import org.appng.api.model.Resource;
import org.appng.api.model.ResourceType;
import org.appng.api.model.Resources;
import org.appng.api.model.Role;
import org.appng.api.model.Site;
import org.appng.api.model.Site.SiteState;
import org.appng.api.model.Subject;
import org.appng.api.model.UserType;
import org.appng.api.support.ApplicationResourceHolder;
import org.appng.api.support.PropertyHolder;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.api.support.environment.EnvironmentKeys;
import org.appng.core.controller.CachedResponse;
import org.appng.core.controller.handler.SoapService;
import org.appng.core.controller.messaging.SiteDeletedEvent;
import org.appng.core.domain.ApplicationImpl;
import org.appng.core.domain.DatabaseConnection;
import org.appng.core.domain.GroupImpl;
import org.appng.core.domain.PermissionImpl;
import org.appng.core.domain.PersistentPropertyHolder;
import org.appng.core.domain.PlatformEvent.Type;
import org.appng.core.domain.PlatformEventListener;
import org.appng.core.domain.PropertyImpl;
import org.appng.core.domain.RepositoryImpl;
import org.appng.core.domain.ResourceImpl;
import org.appng.core.domain.RoleImpl;
import org.appng.core.domain.SiteApplication;
import org.appng.core.domain.SiteApplicationPK;
import org.appng.core.domain.SiteImpl;
import org.appng.core.domain.SubjectImpl;
import org.appng.core.domain.Template;
import org.appng.core.model.AccessibleApplication;
import org.appng.core.model.ApplicationSubjectImpl;
import org.appng.core.model.CacheProvider;
import org.appng.core.model.PackageArchive;
import org.appng.core.model.ZipFileProcessor;
import org.appng.core.repository.ApplicationRepository;
import org.appng.core.repository.DatabaseConnectionRepository;
import org.appng.core.repository.GroupRepository;
import org.appng.core.repository.PermissionRepository;
import org.appng.core.repository.PropertyRepository;
import org.appng.core.repository.RepoRepository;
import org.appng.core.repository.ResourceRepository;
import org.appng.core.repository.RoleRepository;
import org.appng.core.repository.SiteApplicationRepository;
import org.appng.core.repository.SiteRepository;
import org.appng.core.repository.SubjectRepository;
import org.appng.core.security.BCryptPasswordHandler;
import org.appng.core.security.DigestValidator;
import org.appng.core.security.PasswordHandler;
import org.appng.core.security.Sha1PasswordHandler;
import org.appng.core.service.MigrationService.MigrationStatus;
import org.appng.persistence.repository.SearchQuery;
import org.appng.xml.MarshallService;
import org.appng.xml.application.ApplicationInfo;
import org.appng.xml.application.PackageInfo;
import org.appng.xml.application.PermissionRef;
import org.appng.xml.application.Permissions;
import org.appng.xml.application.PropertyType;
import org.appng.xml.application.Roles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import lombok.extern.slf4j.Slf4j;

/**
 * A service implementing the core business logic for creation/retrieval/removal of business-objects.
 * 
 * @author Matthias Müller
 * @author Matthias Herlitzius
 */
@Slf4j
@Transactional(rollbackFor = BusinessException.class)
public class CoreService {

	@Autowired
	protected DatabaseConnectionRepository databaseConnectionRepository;

	@Autowired
	protected SiteApplicationRepository siteApplicationRepository;

	@Autowired
	protected GroupRepository groupRepository;

	@Autowired
	protected SiteRepository siteRepository;

	@Autowired
	protected ApplicationRepository applicationRepository;

	@Autowired
	protected SubjectRepository subjectRepository;

	@Autowired
	protected PropertyRepository propertyRepository;

	@Autowired
	protected PermissionRepository permissionRepository;

	@Autowired
	protected RoleRepository roleRepository;

	@Autowired
	protected RepoRepository repoRepository;

	@Autowired
	protected ResourceRepository resourceRepository;

	@Autowired
	protected DatabaseService databaseService;

	@Autowired
	protected LdapService ldapService;

	@Autowired
	protected TemplateService templateService;

	@Autowired
	protected PlatformEventListener auditableListener;

	public Subject createSubject(SubjectImpl subject) {
		boolean changePasswordAllowed = UserType.LOCAL_USER.equals(subject.getUserType());
		subject.setChangePasswordAllowed(changePasswordAllowed);
		if(changePasswordAllowed) {
			subject.setPasswordLastChanged(new Date());
		}
		return subjectRepository.save(subject);
	}

	public PropertyHolder getPlatformProperties() {
		return getPlatform(true, false);
	}

	protected PropertyHolder getPlatform(boolean finalize, boolean detached) {
		Iterable<PropertyImpl> properties = getPlatformPropertiesList(null);
		if (detached) {
			properties.forEach(p -> propertyRepository.detach(p));
		}
		PropertyHolder propertyHolder = new PersistentPropertyHolder(PropertySupport.PREFIX_PLATFORM, properties);
		if (finalize) {
			propertyHolder.setFinal();
		}
		return propertyHolder;
	}

	public PlatformProperties initPlatformConfig(java.util.Properties defaultOverrides, String rootPath,
			Boolean devMode, boolean persist, boolean tempOverrides) {
		PropertyHolder platformConfig = getPlatform(false, !persist && tempOverrides);
		new PropertySupport(platformConfig).initPlatformConfig(rootPath, devMode, defaultOverrides, false);
		if (persist && !tempOverrides) {
			saveProperties(platformConfig);
		}
		addPropertyIfExists(platformConfig, defaultOverrides, InitializerService.APPNG_USER);
		addPropertyIfExists(platformConfig, defaultOverrides, InitializerService.APPNG_GROUP);
		platformConfig.setFinal();
		return PlatformProperties.get(platformConfig);
	}

	private void addPropertyIfExists(PropertyHolder platformConfig, java.util.Properties defaultOverrides,
			String name) {
		if (defaultOverrides.containsKey(name)) {
			platformConfig.addProperty(name, defaultOverrides.getProperty(name), null,
					org.appng.api.model.Property.Type.TEXT);
		}
	}

	private Page<PropertyImpl> getPlatformPropertiesList(Pageable pageable) {
		return getProperties(PropertySupport.PREFIX_PLATFORM, pageable);
	}

	private Page<PropertyImpl> getSiteProperties(Integer siteId, Pageable pageable) {
		SiteImpl site = siteRepository.findOne(siteId);
		return getProperties(PropertySupport.getSitePrefix(site), pageable);
	}

	private PropertyHolder getSiteProperties(Site site) {
		String prefix = PropertySupport.getSitePrefix(site);
		Iterable<PropertyImpl> properties = getProperties(prefix);
		return new PersistentPropertyHolder(prefix, properties);
	}

	private PropertyHolder getApplicationProperties(Site site, Application application) {
		String prefix = PropertySupport.getPropertyPrefix(site, application);
		Iterable<PropertyImpl> properties = getProperties(prefix);
		return new PersistentPropertyHolder(prefix, properties);
	}

	private Page<PropertyImpl> getProperties(String prefix, Pageable pageable) {
		SearchQuery<PropertyImpl> query = new SearchQuery<PropertyImpl>(PropertyImpl.class);
		query.like("name", prefix + "%");
		query.notLike("name", prefix + "%.%");
		Page<PropertyImpl> page = propertyRepository.search(query, pageable);
		return page;
	}

	private Page<PropertyImpl> getProperties(String prefix) {
		return getProperties(prefix, (Pageable) null);
	}

	private Page<PropertyImpl> getApplicationPropertiesList(Integer siteId, Integer applicationId, Pageable pageable) {
		String prefix = getPropertyPrefix(siteId, applicationId);
		return getProperties(prefix, pageable);
	}

	private Page<PropertyImpl> getApplicationPropertiesList(Integer siteId, Integer applicationId) {
		String prefix = getPropertyPrefix(siteId, applicationId);
		return getProperties(prefix);
	}

	private String getPropertyPrefix(Integer siteId, Integer applicationId) {
		Site site = null == siteId ? null : siteRepository.findOne(siteId);
		Application application = null == applicationId ? null : applicationRepository.findOne(applicationId);
		String prefix = PropertySupport.getPropertyPrefix(site, application);
		return prefix;
	}

	public Page<PropertyImpl> getProperties(String siteName, String applicationName) {
		return getProperties(siteName, applicationName, null);
	}

	public Page<PropertyImpl> getProperties(String siteName, String applicationName, Pageable pageable) {
		Integer siteId = null;
		Integer applicationId = null;
		if (null != siteName) {
			Site site = siteRepository.findByName(siteName);
			if (null == site) {
				throw new IllegalArgumentException("No such site: '" + siteName + "'");
			}
			siteId = site.getId();
		}
		if (null != applicationName) {
			Application application = applicationRepository.findByName(applicationName);
			if (null == application) {
				throw new IllegalArgumentException("No such application: '" + applicationName + "'");
			}
			applicationId = application.getId();
		}
		return getProperties(siteId, applicationId, pageable);
	}

	protected Page<PropertyImpl> getProperties(Integer siteId, Integer applicationId) {
		return getProperties(siteId, applicationId, null);
	}

	protected Page<PropertyImpl> getProperties(Integer siteId, Integer applicationId, Pageable pageable) {
		Page<PropertyImpl> properties;
		if (null != applicationId) {
			properties = getApplicationPropertiesList(siteId, applicationId, pageable);
		} else if (null != siteId) {
			properties = getSiteProperties(siteId, pageable);
		} else {
			properties = getPlatformPropertiesList(pageable);
		}
		return properties;
	}

	public PropertyImpl createProperty(Integer siteId, Integer applicationId, PropertyImpl property) {
		Site site = null;
		Application application = null;
		if (null != siteId) {
			site = siteRepository.findOne(siteId);
		}
		if (null != applicationId) {
			application = applicationRepository.findOne(applicationId);
		}
		String propertyPrefix = PropertySupport.getPropertyPrefix(site, application);
		String currentName = property.getName();
		property.setName(propertyPrefix + currentName);
		property.determineType();
		saveProperty(property);
		String logMssg = "created property '" + property.getName();
		if (null != application) {
			logMssg += "' for application '" + application.getName() + "'";
		}
		if (null != site) {
			logMssg += " in site '" + site.getName() + "'";
		}
		LOGGER.debug(logMssg);
		return property;
	}

	protected boolean checkPropertyExists(Integer siteId, Integer applicationId, PropertyImpl property) {
		String propertyPrefix = getPropertyPrefix(siteId, applicationId);
		String propertyName = propertyPrefix + property.getName();
		Property findById = propertyRepository.findByName(propertyName);
		return findById != null;
	}

	protected List<Integer> getSiteIds() {
		return siteRepository.getSiteIds();
	}

	public SiteImpl getSite(Integer id) {
		SiteImpl site = siteRepository.findOne(id);
		initSite(site);
		return site;
	}

	public SiteImpl getSiteByName(String name) {
		SiteImpl site = siteRepository.findByName(name);
		initSite(site);
		return site;
	}

	public void saveSite(SiteImpl site) {
		siteRepository.save(site);
	}

	private void initSite(Site site) {
		if (null != site) {
			Set<Application> applications = site.getApplications();
			for (Application application : applications) {
				Set<Role> roles = application.getRoles();
				for (Role applicationRole : roles) {
					applicationRole.getPermissions().iterator().hasNext();
				}
				application.getPermissions().iterator().hasNext();
				application.getResourceSet().iterator().hasNext();
				applicationRepository.detach((ApplicationImpl) application);
			}
			siteRepository.detach((SiteImpl) site);
			initSiteProperties((SiteImpl) site, null, true);
		}
	}

	public PropertyImpl saveProperty(PropertyImpl property) {
		return propertyRepository.save(property);
	}

	private Subject loginSubject(Site site, String username, String password, Environment env) {
		char[] pwdArr = password.toCharArray();
		LOGGER.debug("user '{}' tries to login", username);
		Subject loginSubject = null;
		SubjectImpl subject = getSubjectByName(username, true);
		if (subject != null) {
			boolean precondition = false;
			switch (subject.getUserType()) {
			case LOCAL_USER:
				precondition = isValidPassword(subject, password);
				break;
			case GLOBAL_USER:
				precondition = ldapService.loginUser(site, username, pwdArr);
				break;
			default:
				break;
			}

			if (verifySubject(precondition, username, env, "User '{}' submitted wrong password.")) {
				loginSubject = subject;
			}

		} else {
			List<SubjectImpl> globalGroups = getSubjectsByType(UserType.GLOBAL_GROUP);
			if (!globalGroups.isEmpty()) {
				List<String> groupNames = new ArrayList<>();
				for (SubjectImpl subjectImpl : globalGroups) {
					groupNames.add(subjectImpl.getName());
				}
				SubjectImpl groupSubject = new SubjectImpl();
				List<String> subjectNames = ldapService.loginGroup(site, username, pwdArr, groupSubject, groupNames);
				if (!subjectNames.isEmpty()) {
					for (String subjectName : subjectNames) {
						Subject s = getSubjectByName(subjectName, true);
						List<Group> groups = s.getGroups();
						groupSubject.getGroups().addAll(groups);
					}
					loginSubject = groupSubject;
				}
			} else {
				LOGGER.warn("no such user: '{}'", username);
			}
		}
		for (int i = 0; i < pwdArr.length; i++) {
			pwdArr[i] = '#';
		}
		password = String.valueOf(pwdArr);
		return loginSubject;
	}

	public List<SubjectImpl> getSubjectsByType(UserType type) {
		return subjectRepository.findByUserType(type);
	}

	public boolean isValidPassword(AuthSubject authSubject, String password) {
		PasswordHandler passwordHandler = getPasswordHandler(authSubject);
		StopWatch sw = new StopWatch("isValidPassword");
		sw.start();
		boolean validPassword = passwordHandler.isValidPassword(password);
		if (validPassword) {
			passwordHandler.migrate(this, password);
		} else {
			LOGGER.warn("wrong password for user {}", authSubject.getAuthName());
		}
		password = "";
		sw.stop();
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(sw.shortSummary());
		}
		return validPassword;
	}

	/**
	 * Returns a {@link PasswordHandler} which is able to handle the password of a given {@link AuthSubject}. This is
	 * only relevant if {@link Subject}s exist which still use passwords hashed with an older {@link PasswordHandler}.
	 * This method may be removed in the future.
	 * 
	 * @param  authSubject
	 *                     The {@link AuthSubject} which is used to initialize the {@link PasswordHandler} and to
	 *                     determine which implementation of the {@link PasswordHandler} interface will be returned.
	 * @return             the {@link PasswordHandler} for the {@link AuthSubject}
	 */
	public PasswordHandler getPasswordHandler(AuthSubject authSubject) {
		if (!authSubject.getDigest().startsWith(BCryptPasswordHandler.getPrefix())) {
			return new Sha1PasswordHandler(authSubject);
		} else {
			return getDefaultPasswordHandler(authSubject);
		}
	}

	/**
	 * Returns the default password manager which should be used to handle all passwords.
	 * 
	 * @param  authSubject
	 *                     The {@link AuthSubject} which is used for initializing the {@link PasswordHandler}.
	 * @return             the default {@link PasswordHandler} for the {@link AuthSubject}
	 */
	public PasswordHandler getDefaultPasswordHandler(AuthSubject authSubject) {
		return new BCryptPasswordHandler(authSubject);
	}

	public Subject restoreSubject(String name) {
		Subject subject = getSubjectByName(name, true);
		if (null != subject) {
			initAuthenticatedSubject(subject);
			LOGGER.trace("successfully restored subject '{}'", subject.getName());
		} else {
			LOGGER.error("can not restore subject '{}'", name);
		}
		return subject;
	}

	protected void saveProperties(Properties properties) {
		Set<String> propertyNames = properties.getPropertyNames();
		PropertyHolder propertyHolder = (PropertyHolder) properties;
		propertyNames.forEach(name -> saveProperty((PropertyImpl) propertyHolder.getProperty(name)));
	}

	public boolean login(Environment env, Principal principal) {
		Subject loginSubject = null;
		Subject subject = getSubjectByName(principal.getName(), true);
		if (null != subject) {
			if (verifySubject(UserType.GLOBAL_USER.equals(subject.getUserType()), principal.getName(),
					env, "{} must be a global user!")) {
				LOGGER.info("user {} found", principal.getName());
				loginSubject = subject;
			}
		} else {
			LOGGER.info("Subject authentication failed. Trying to authenticate based on LDAP group membership.");
			loginSubject = new SubjectImpl();
			((SubjectImpl) loginSubject).setName(principal.getName());

			boolean hasAnyRole = false;
			List<SubjectImpl> globalGroups = getSubjectsByType(UserType.GLOBAL_GROUP);
			HttpServletRequest request = ((DefaultEnvironment) env).getServletRequest();
			for (Subject globalGroup : globalGroups) {
				if (request.isUserInRole(globalGroup.getName())) {
					LOGGER.info("user '{}' belongs to group '{}'", principal.getName(), globalGroup.getName());
					hasAnyRole = true;
					List<Group> groups = globalGroup.getGroups();
					for (Group group : groups) {
						initGroup(group);
						if (!loginSubject.getGroups().contains(group)) {
							loginSubject.getGroups().add(group);
							((SubjectImpl) loginSubject).setLanguage(globalGroup.getLanguage());
							((SubjectImpl) loginSubject).setRealname(globalGroup.getName());
						}
					}
				}
			}
			if (hasAnyRole) {
				LOGGER.info("User '{}' successfully authenticated.", principal.getName());
			} else {
				loginSubject = null;
				LOGGER.error("No valid group membership found for user '{}'", principal.getName());
			}
		}
		return login(env, loginSubject);
	}

	public boolean login(Environment env, String digest, int digestMaxValidity) {
		String sharedSecret = getPlatformConfig(env).getString(Platform.Property.SHARED_SECRET);
		DigestValidator validator = new DigestValidator(digest, digestMaxValidity);
		String username = validator.getUsername();
		if (StringUtils.isNotBlank(username)) {
			SubjectImpl s = getSubjectByName(username, true);
			if (null != s) {
				boolean success = validator.validate(sharedSecret);
				sharedSecret = "";
				success = verifySubject(success, username, env, "Digest validation failed for {}");
				if (success) {
					return login(env, s);
				}
			} else {
				LOGGER.debug("user for digest login not found: {}", username);
			}
		} else {
			LOGGER.debug("empty user name for digest validation!");
		}

		return false;
	}

	private boolean verifySubject(boolean precondition, String username, Environment env, String message) {
		Properties platformConfig = getPlatformConfig(env);
		SubjectImpl subject = getSubjectByName(username, false);
		Date now = new Date();
		if (precondition) {
			Integer inactiveLockPeriod = platformConfig.getInteger(Platform.Property.INACTIVE_LOCK_PERIOD);
			if (subject.isLocked(now)) {
				createEvent(Type.INFO, "Rejected login for locked user %s (locked since %s).", username,
						subject.getLockedSince());
			} else if (subject.isInactive(now, inactiveLockPeriod)) {
				subject.setLockedSince(now);
				createEvent(Type.WARN, "User %s has been locked due to inactivity (last login was at %s).", username,
						subject.getLastLogin());
			} else {
				subject.setLastLogin(now);
				subject.setFailedLoginAttempts(0);
				return true;
			}
		} else {
			subject.setFailedLoginAttempts(subject.getFailedLoginAttempts() + 1);
			Integer maxAttempts = platformConfig.getInteger(Platform.Property.MAX_LOGIN_ATTEMPTS);
			if (maxAttempts <= subject.getFailedLoginAttempts()) {
				subject.setLockedSince(now);
				createEvent(Type.WARN, "User %s has been locked after %s failed login attempts.", username,
						subject.getFailedLoginAttempts());
			} else {
				createEvent(Type.INFO, "User %s has %s failed login attempts.", username,
						subject.getFailedLoginAttempts());
			}
			LOGGER.debug(message, username);
		}
		env.setAttribute(Scope.REQUEST, "subject.locked", null != subject.getLockedSince());
		return false;
	}

	public boolean login(Site site, Environment env, String username, String password) {
		Subject s = loginSubject(site, username, password, env);
		return login(env, s);
	}

	public boolean loginGroup(Environment env, AuthSubject authSubject, String password, Integer groupId) {
		Group group = groupRepository.getGroup(groupId);
		if (null != group) {
			if (isValidPassword(authSubject, password)) {
				SubjectImpl subject = new SubjectImpl();
				subject.getGroups().add(group);
				subject.setLanguage(authSubject.getLanguage());
				subject.setTimeZone(authSubject.getTimeZone());
				subject.setRealname(authSubject.getRealname());
				subject.setName(authSubject.getAuthName());
				subject.setEmail(authSubject.getEmail());
				return login(env, subject);
			}
		} else {
			LOGGER.warn("no such group: {}", groupId);
		}
		return false;
	}

	private boolean login(Environment env, Subject subject) {
		if (subject != null) {
			initAuthenticatedSubject(subject);
			LOGGER.info("successfully logged in user '{}'", subject.getName());
			((DefaultEnvironment) env).setSubject(subject);
			auditableListener.createEvent(Type.INFO, "logged in");
			return true;
		}
		return false;
	}

	public void logoutSubject(Environment env) {
		Subject subject = env.getSubject();
		auditableListener.createEvent(Type.INFO, "logged out");
		((DefaultEnvironment) env).logoutSubject();
		LOGGER.info("'{}' logged out", subject.getName());
	}

	public PropertyImpl getProperty(String propertyId) {
		return propertyRepository.findOne(propertyId);
	}

	protected void createSite(SiteImpl site, Environment env) {
		if (site.isCreateRepository()) {
			File repositoryRootDir = PlatformProperties.get(getPlatformConfig(env)).getRepositoryRootFolder();
			File siteRootDir = new File(repositoryRootDir, site.getName());
			if (!siteRootDir.exists()) {
				try {
					FileUtils.forceMkdir(siteRootDir);
				} catch (IOException e) {
					LOGGER.error(String.format(
							"directory cannot be created or the file named %s already exists but is not a directory",
							site.getName()), e);
				}
			}
		}

		initSiteProperties(site, env, true);
		siteRepository.save(site);
	}

	public void createSite(SiteImpl site) {
		createSite(site, null);
	}

	private void initAuthenticatedSubject(Subject subject) {
		((SubjectImpl) subject).setAuthenticated(true);
		((SubjectImpl) subject).setSalt(null);
		((SubjectImpl) subject).setDigest(null);
	}

	protected void initSiteProperties(SiteImpl site) {
		initSiteProperties(site, null);
	}

	protected void initSiteProperties(SiteImpl site, Environment environment) {
		initSiteProperties(site, environment, false);
	}

	protected void initSiteProperties(SiteImpl site, Environment environment, boolean doSave) {
		PropertyHolder siteProperties = getSiteProperties(site);
		List<String> platformProps = PropertySupport.getSiteRelevantPlatformProps();
		SearchQuery<PropertyImpl> query = propertyRepository.createSearchQuery().in("name", platformProps);
		Page<PropertyImpl> properties = propertyRepository.search(query, new PageRequest(0, platformProps.size()));
		new PropertySupport(siteProperties).initSiteProperties(site,
				new PropertyHolder(PropertySupport.PREFIX_PLATFORM, properties));
		if (doSave) {
			saveProperties(siteProperties);
		}
		site.setProperties(siteProperties);
		LOGGER.debug("initialized properties for site {}: {}", site.getName(), siteProperties.toString());
	}

	private void createApplication(ApplicationImpl application, ApplicationInfo applicationInfo, FieldProcessor fp) {
		applicationRepository.save(application);

		if (null == applicationInfo) {
			createPermission(application, "output-format.html", "HTML format");
			createPermission(application, "output-type.webgui", "Web-GUI type");
		} else {
			if (null != applicationInfo.getPermissions() && null != applicationInfo.getPermissions().getPermission()) {
				for (org.appng.xml.application.Permission permission : applicationInfo.getPermissions()
						.getPermission()) {
					createPermission(application, permission.getId(), permission.getValue());
				}
			}

			if (null != applicationInfo.getRoles()) {
				Roles roles = applicationInfo.getRoles();
				Set<Role> applicationRoles = new HashSet<>();
				Set<Role> adminRoles = new HashSet<>();

				for (org.appng.xml.application.Role role : roles.getRole()) {
					RoleImpl applicationRole = createApplicationRole(application, role);
					applicationRoles.add(applicationRole);
					if (role.isAdminRole()) {
						adminRoles.add(applicationRole);
					}
				}
				application.getRoles().addAll(applicationRoles);

				List<GroupImpl> adminGroups = getAdminGroups();
				if (!adminRoles.isEmpty() && !adminGroups.isEmpty()) {
					StringBuilder message = new StringBuilder("Add admin role(s) ");
					adminRoles.forEach(s -> message.append(s.getName() + " "));
					message.append("to admin group(s) ");
					adminGroups.forEach(s -> {
						s.getRoles().addAll(adminRoles);
						message.append(s.getName() + " ");
					});
					if (null != fp) {
						fp.addNoticeMessage(message.toString());
					}
					LOGGER.debug(message.toString());
				}
			}

			if (null != applicationInfo.getProperties() && null != applicationInfo.getProperties().getProperty()) {
				for (org.appng.xml.application.Property prop : applicationInfo.getProperties().getProperty()) {
					PropertyImpl property = new PropertyImpl(prop.getId(), null, null);
					setPropertyValue(prop, property, true);
					createProperty(null, application.getId(), property);
				}
				final Properties applicationProperties = getApplicationProperties(null, application);
				application.setProperties(applicationProperties);
			}
		}
	}

	private RoleImpl createApplicationRole(Application application, org.appng.xml.application.Role role) {
		RoleImpl applicationRole = new RoleImpl();
		applicationRole.setName(role.getName());
		applicationRole.setDescription(role.getDescription());
		applicationRole.setApplication(application);
		roleRepository.save(applicationRole);
		addPermissionsToRole(application.getId(), role, applicationRole);
		LOGGER.info("creating new role '{}' for application '{}'", role.getName(), application.getName());
		return applicationRole;
	}

	private void addPermissionsToRole(Integer applicationId, org.appng.xml.application.Role role,
			Role applicationRole) {
		for (PermissionRef permissionRef : role.getPermission()) {
			String permissionName = permissionRef.getId();
			Permission p = permissionRepository.findByNameAndApplicationId(permissionName, applicationId);
			if (null == p) {
				LOGGER.warn("the role '{}' references permisson '{}' which does not exist!", role.getName(),
						permissionName);
			} else if (applicationRole.getPermissions().add(p)) {
				LOGGER.info("added permission '{}' to role '{}'", permissionName, role.getName());
			} else {
				LOGGER.info("role '{}' already has permission '{}'", role.getName(), permissionName);
			}
		}
	}

	public MigrationStatus assignApplicationToSite(SiteImpl site, Application application, boolean createProperties) {
		SiteApplication siteApplication = new SiteApplication(site, application);
		siteApplication.setActive(true);
		siteApplication.setReloadRequired(true);
		siteApplication.setMarkedForDeletion(false);
		MigrationStatus migrationStatus = createDatabaseConnection(siteApplication);
		DatabaseConnection dbc = siteApplication.getDatabaseConnection();
		if (!migrationStatus.isErroneous()) {
			siteApplicationRepository.save(siteApplication);
			site.getSiteApplications().add(siteApplication);
			auditableListener.createEvent(Type.INFO,
					String.format("Assigned application %s to site %s", application.getName(), site.getName()));
			if (MigrationStatus.DB_MIGRATED.equals(migrationStatus)) {
				auditableListener.createEvent(Type.INFO,
						String.format("Created database %s with user %s", dbc.getJdbcUrl(), dbc.getUserName()));
			}
			if (createProperties) {
				String prefix = PropertySupport.getPropertyPrefix(site, application);
				Iterable<PropertyImpl> properties = getProperties(prefix);
				for (PropertyImpl property : properties) {
					propertyRepository.delete(property);
				}
				PropertyHolder originalProperties = getApplicationProperties(null, application);
				if (null != originalProperties) {
					for (String name : originalProperties.getPropertyNames()) {
						Property platformApplicationProperty = originalProperties.getProperty(name);
						String value = originalProperties.getString(name);
						String propName = name.substring(name.lastIndexOf(".") + 1);
						PropertyImpl property = new PropertyImpl(propName, null, value);
						property.setDescription(platformApplicationProperty.getDescription());
						property.setType(platformApplicationProperty.getType());
						if (StringUtils.isNotEmpty(platformApplicationProperty.getClob())) {
							property.setClob(platformApplicationProperty.getClob());
						} else {
							property.setDefaultString(platformApplicationProperty.getDefaultString());
						}
						createProperty(site.getId(), application.getId(), property);
					}
				}
			}
		} else if (null != dbc) {
			auditableListener.createEvent(Type.ERROR,
					String.format("Error creating database %s with user %s for application %s on site %s",
							dbc.getJdbcUrl(), dbc.getUserName(), application.getName(), site.getName()));
			MigrationStatus droppedState = databaseService.dropDataBaseAndUser(dbc);
			if (MigrationStatus.ERROR.equals(droppedState)) {
				String message = String.format(
						"Failed to delete database and user for connection %s, manual cleanup might be required!",
						dbc.getJdbcUrl());
				LOGGER.warn(message);
				auditableListener.createEvent(Type.ERROR, message);
			}
			databaseConnectionRepository.delete(dbc);
		} else {
			auditableListener.createEvent(Type.ERROR,
					String.format("Error creating database and/or user for application %s on site %s",
							application.getName(), site.getName()));
		}

		return migrationStatus;
	}

	protected MigrationStatus createDatabaseConnection(SiteApplication siteApplication) {
		Application application = siteApplication.getApplication();
		Site site = siteApplication.getSite();
		Properties platformConfig = getPlatformProperties();
		File applicationRootFolder = getApplicationRootFolder(null);
		CacheProvider cacheProvider = new CacheProvider(platformConfig);
		try {
			File platformCache = cacheProvider.getPlatformCache(site, application);
			Resources applicationResources = getResources(application, platformCache, applicationRootFolder);
			applicationResources.dumpToCache(ResourceType.APPLICATION, ResourceType.SQL);
			ApplicationInfo applicationInfo = applicationResources.getApplicationInfo();
			File sqlFolder = new File(platformCache, ResourceType.SQL.getFolder());
			String databasePrefix = platformConfig.getString(Platform.Property.DATABASE_PREFIX);
			return databaseService.manageApplicationConnection(siteApplication, applicationInfo, sqlFolder,
					databasePrefix);
		} catch (Exception e) {
			LOGGER.error(String.format("error during database setup for application %s", application.getName()), e);
		} finally {
			cacheProvider.clearCache(site, application.getName());
		}
		return MigrationStatus.ERROR;
	}

	public PermissionImpl getPermission(String application, String name) {
		return permissionRepository.findByApplicationNameAndName(application, name);
	}

	public PermissionImpl savePermission(PermissionImpl p) {
		LOGGER.info("creating new permission '{}' for application '{}'", p.getName(), p.getApplication().getName());
		return permissionRepository.save(p);
	}

	private Permission createPermission(ApplicationImpl application, String name, String description) {
		PermissionImpl p = new PermissionImpl();
		p.setName(name);
		p.setDescription(description);
		p.setApplication(application);
		application.getPermissions().add(p);
		return savePermission(p);
	}

	public RepositoryImpl createRepository(RepositoryImpl repository) {
		return repoRepository.save(repository);
	}

	public PackageInfo installPackage(final Integer repositoryId, final String name, final String version,
			String timestamp, final boolean isPrivileged, final boolean isHidden, final boolean isFileBased,
			FieldProcessor fp) throws BusinessException {
		PackageArchive applicationArchive = getArchive(repositoryId, name, version, timestamp);
		switch (applicationArchive.getType()) {
		case APPLICATION:
			provideApplication(applicationArchive, isFileBased, isPrivileged, isHidden, fp);
			break;
		case TEMPLATE:
			provideTemplate(applicationArchive);
		}
		return applicationArchive.getPackageInfo();
	}

	public PackageInfo installPackage(final Integer repositoryId, final String name, final String version,
			String timestamp, final boolean isPrivileged, final boolean isHidden, final boolean isFileBased)
			throws BusinessException {

		return installPackage(repositoryId, name, version, timestamp, isPrivileged, isHidden, isFileBased, null);
	}

	private PackageArchive getArchive(final Integer repositoryId, final String applicationName,
			final String applicationVersion, String applicationTimestamp) throws BusinessException {
		if (null != repositoryId && null != applicationName && null != applicationVersion) {
			RepositoryImpl repository = repoRepository.findOne(repositoryId);
			if (null != repository) {
				LOGGER.info("retrieving '{}-{}' from repository {}", applicationName, applicationVersion,
						repository.getUri());
				return repository.getPackageArchive(applicationName, applicationVersion, applicationTimestamp);
			} else {
				throw new BusinessException("Repository not found: " + repositoryId);
			}
		} else {
			throw new BusinessException("Invalid parameters");
		}
	}

	public Template provideTemplate(Integer repositoryId, String templateName, String templateVersion,
			String templateTimestamp) throws BusinessException {
		PackageArchive applicationArchive = getArchive(repositoryId, templateName, templateVersion, templateTimestamp);
		return provideTemplate(applicationArchive);
	}

	private Template provideTemplate(PackageArchive packageArchive) throws BusinessException {
		return templateService.installTemplate(packageArchive);
	}

	/**
	 * Deletes a {@link Template}
	 * 
	 * @param  name
	 *              the name of the template to delete
	 * @return
	 *              <ul>
	 *              <li>0 - if everything went OK
	 *              <li>-1 - if no such template exists
	 *              <li>-2 - if the template is still in use
	 *              </ul>
	 */
	public Integer deleteTemplate(String name) {
		Template template = templateService.getTemplateByName(name);
		if (null == template) {
			return -1;
		}
		Integer sitesUsingTemplate = propertyRepository.countByActualStringAndNameLike(template.getDisplayName(),
				"platform\\.site\\.%\\." + SiteProperties.TEMPLATE);
		if (0 == sitesUsingTemplate) {
			return templateService.deleteTemplate(template);
		}
		return -2;
	}

	private ApplicationImpl provideApplication(PackageArchive applicationArchive, boolean isFileBased,
			boolean isPrivileged, boolean isHidden, FieldProcessor fp) throws BusinessException {
		ApplicationInfo applicationInfo = (ApplicationInfo) applicationArchive.getPackageInfo();
		String applicationName = applicationInfo.getName();
		String applicationVersion = applicationInfo.getVersion();
		File outputDir = getApplicationFolder(null, applicationName);
		ApplicationImpl application = applicationRepository.findByName(applicationName);
		if (null == application) {
			LOGGER.info("deploying application {}-{}", applicationName, applicationVersion);
			application = RepositoryImpl.getApplication(applicationInfo);
			application.setFileBased(isFileBased);
			application.setPrivileged(isPrivileged);
			application.setHidden(isHidden);
			createApplication(application, applicationInfo, fp);
		} else {
			LOGGER.info("updating application {}-{}", applicationName, applicationVersion);
			// Update application information
			RepositoryImpl.getApplication(application, applicationInfo);
		}
		updateApplication(application, applicationArchive, outputDir);
		return application;
	}

	protected void deletePackageVersion(Integer repositoryId, String packageName, String packageVersion,
			String packageTimestamp) throws BusinessException {
		if (null != repositoryId && null != packageName && null != packageVersion) {
			RepositoryImpl repository = repoRepository.findOne(repositoryId);
			if (null != repository) {
				try {
					repository.deletePackageVersion(packageName, packageVersion, packageTimestamp);
				} catch (Exception e) {
					throw new BusinessException("Unable to delete package from repository " + repository.getUri(), e);
				}
			} else {
				throw new BusinessException("Repository with ID " + repositoryId + " not found.");
			}
		} else {
			throw new BusinessException("Invalid parameters: repositoryId=" + repositoryId + ", packageName="
					+ packageName + ", packageVersion=" + packageVersion);
		}
	}

	protected void reloadRepository(Integer repositoryId) throws BusinessException {
		if (null != repositoryId) {
			RepositoryImpl repository = repoRepository.findOne(repositoryId);
			if (null != repository) {
				try {
					repository.reload();
				} catch (Exception e) {
					throw new BusinessException("Unable to reload repository with ID " + repositoryId, e);
				}
			}
		}

	}

	private void updateApplication(ApplicationImpl application, PackageArchive applicationArchive, File outputDir)
			throws BusinessException {
		updateApplicationFromArchive(applicationArchive, application, outputDir);
		ApplicationInfo applicationInfo = (ApplicationInfo) applicationArchive.getPackageInfo();
		Permissions permissions = applicationInfo.getPermissions();
		if (null != permissions) {
			for (org.appng.xml.application.Permission permission : permissions.getPermission()) {
				String permissionId = permission.getId();
				Permission applicationPermission = permissionRepository.findByNameAndApplicationId(permissionId,
						application.getId());
				if (null == applicationPermission) {
					createPermission(application, permissionId, permission.getValue());
				}
			}
		}
		Roles roles = applicationInfo.getRoles();
		if (null != roles) {
			for (org.appng.xml.application.Role role : roles.getRole()) {
				RoleImpl applicationRole = roleRepository.findByApplicationIdAndName(application.getId(),
						role.getName());
				if (null == applicationRole) {
					applicationRole = createApplicationRole(application, role);
					application.getRoles().add(applicationRole);
				} else {
					addPermissionsToRole(application.getId(), role, applicationRole);
					applicationRole.setDescription(role.getDescription());
				}
			}
		}

		org.appng.xml.application.Properties properties = applicationInfo.getProperties();
		if (null != properties) {
			List<SiteImpl> applicationSites = siteRepository.findSitesForApplication(application.getId());
			for (org.appng.xml.application.Property prop : properties.getProperty()) {
				String propName = PropertySupport.getPropertyName(null, application, prop.getId());
				PropertyImpl property = propertyRepository.findOne(propName);
				if (null == property) {
					property = new PropertyImpl(prop.getId(), null, null);
					createProperty(null, application.getId(), property);
				}
				setPropertyValue(prop, property, true);

				for (Site site : applicationSites) {
					propName = PropertySupport.getPropertyName(site, application, prop.getId());
					PropertyImpl siteProperty = propertyRepository.findOne(propName);
					boolean forceValue = null == siteProperty;
					if (forceValue) {
						siteProperty = new PropertyImpl(prop.getId(), null, null);
						createProperty(site.getId(), application.getId(), siteProperty);
					}
					setPropertyValue(prop, siteProperty, forceValue);
				}
			}
		}
		final Properties applicationProperties = getApplicationProperties(null, application);
		application.setProperties(applicationProperties);
	}

	private void setPropertyValue(org.appng.xml.application.Property prop, PropertyImpl property,
			boolean forceMultiline) {
		property.setDescription(prop.getDescription());
		PropertyType orignalType = prop.getType();
		Property.Type type = null != orignalType ? Property.Type.valueOf(orignalType.name())
				: Property.Type.forString(prop.getValue());
		property.setType(type);
		if (Boolean.TRUE.equals(prop.isClob()) || Property.Type.MULTILINE.equals(type)) {
			if (forceMultiline || null == property.getClob()) {
				property.setClob(prop.getValue());
			}
			property.setDefaultString(null);
			property.setActualString(null);
			property.setType(Property.Type.MULTILINE);
		} else if (StringUtils.isBlank(property.getClob())) {
			property.setDefaultString(prop.getValue());
			property.setClob(null);
		}
	}

	private void updateApplicationFromArchive(PackageArchive applicationArchive, Application application,
			File outputDir) throws BusinessException {
		if (!applicationArchive.isValid()) {
			throw new BusinessException("Not a valid ApplicationArchive!");
		}
		deleteApplicationResources(application, outputDir);
		ZipFileProcessor<List<Resource>> applicationArchiveProcessor = new ApplicationArchiveProcessor(application);
		try {
			List<Resource> resources = applicationArchive.processZipFile(applicationArchiveProcessor);
			writeApplicationResources(application, application.isFileBased(), outputDir, resources);
		} catch (IOException e) {
			throw new BusinessException("unable to process application archive", e);
		}
	}

	protected void writeApplicationResources(Application application, boolean fileBased, File outputDir,
			Collection<Resource> resources) throws BusinessException {
		if (fileBased) {
			LOGGER.info("extracting filebased application {} - {} to {}", application.getName(),
					application.getPackageVersion(), outputDir);
			writeFileBasedApplicationResources(resources, outputDir);
		} else {
			LOGGER.info("creating resource(s) for database-based application {} - {}", application.getName(),
					application.getPackageVersion());
			for (Resource r : resources) {
				LOGGER.info("saving applicationresource {}", r.getName());
				ResourceImpl resource = (r instanceof ResourceImpl) ? ((ResourceImpl) r)
						: new ResourceImpl(application, r);
				resourceRepository.save(resource);
			}
		}
	}

	protected File getApplicationRootFolder(Environment environment) {
		return PlatformProperties.get(getPlatformConfig(environment)).getApplicationDir();
	}

	public File getApplicationFolder(Environment env, String applicationName) {
		return new File(getApplicationRootFolder(env), applicationName);
	}

	private File getApplicationFolder(Environment env, Application application) {
		return getApplicationFolder(env, application.getName());
	}

	protected Properties getPlatformConfig(Environment environment) {
		return null == environment ? getPlatform(false, false)
				: environment.getAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG);
	}

	protected String deleteResource(Environment env, Integer applicationId, Integer resourceId)
			throws BusinessException {
		Application application = applicationRepository.findOne(applicationId);
		try {
			Resources applicationResourceHolder = getResources(application, null, getApplicationRootFolder(env));
			Resource applicationResource = applicationResourceHolder.getResource(resourceId);
			if (application.isFileBased()) {
				File applicationFolder = getApplicationFolder(env, application);
				File file = new File(applicationFolder, applicationResource.getResourceType().getFolder()
						+ File.separator + applicationResource.getName());
				FileUtils.deleteQuietly(file);
			} else {
				application.getResourceSet().remove(applicationResource);
				resourceRepository.delete(applicationResource.getId());
			}
			return applicationResource.getName();
		} catch (Exception e) {
			throw new BusinessException("error while deleting resource", e);
		}
	}

	protected void synchronizeApplicationResources(Environment env, Application application, boolean isFileBased)
			throws BusinessException {
		String convertDirection = "";
		try {
			Application currentApplication = applicationRepository.findOne(application.getId());
			File applicationFolder = getApplicationFolder(env, currentApplication);
			if (currentApplication.isFileBased() && !isFileBased) {
				convertDirection = "filebased to database";
				LOGGER.info("application '{}' is beeing converted from {}", currentApplication.getName(),
						convertDirection);
				Resources resources = getResources(currentApplication, null, getApplicationRootFolder(env));
				writeApplicationResources(currentApplication, false, null, resources.getResources());
				deleteApplicationResources(application, applicationFolder);
			} else if (!currentApplication.isFileBased() && isFileBased) {
				convertDirection = "database to filebased";
				LOGGER.info("application '{}' is beeing converted from {}", currentApplication.getName(),
						convertDirection);
				Resources resources = getResources(currentApplication, applicationFolder, null);
				writeApplicationResources(currentApplication, true, applicationFolder, resources.getResources());
				deleteApplicationResources(currentApplication, applicationFolder);
			}
		} catch (InvalidConfigurationException e) {
			throw new BusinessException("error while transforming application '" + application.getName() + "' from "
					+ convertDirection + ", application is in an erroneous state", e);
		}
	}

	private void writeFileBasedApplicationResources(Collection<Resource> resources, File outputDir)
			throws BusinessException {
		for (Resource applicationResource : resources) {
			String outputPath = outputDir.getAbsolutePath() + File.separator
					+ applicationResource.getResourceType().getFolder() + File.separator
					+ applicationResource.getName();
			try {
				File outputFile = new File(outputPath);
				FileUtils.forceMkdir(outputFile.getParentFile());
				try (OutputStream outputStream = new FileOutputStream(outputFile);
						InputStream inputStream = new ByteArrayInputStream(applicationResource.getBytes())) {
					IOUtils.copy(inputStream, outputStream);
					LOGGER.debug("writing {}", outputPath);
				}
			} catch (IOException e) {
				throw new BusinessException("error while updating resource " + applicationResource.getName(), e);
			}
		}
	}

	private void deleteApplicationResources(Application application, File applicationFolder) throws BusinessException {
		if (application.isFileBased()) {
			FileUtils.deleteQuietly(applicationFolder);
		} else {
			for (Resource applicationResource : application.getResourceSet()) {
				deleteApplicationResource(applicationResource);
			}
		}
	}

	private void deleteApplicationResource(Resource applicationResource) throws BusinessException {
		if (null != applicationResource) {
			resourceRepository.delete((ResourceImpl) applicationResource);
		}
	}

	public byte[] resetPassword(AuthSubject authSubject, PasswordPolicy passwordPolicy, String email, String hash) {
		SubjectImpl subjectByName = getSubjectByName(authSubject.getAuthName(), false);
		if (null != subjectByName && subjectByName.isChangePasswordAllowed() && !subjectByName.isLocked(new Date())) {
			PasswordHandler passwordHandler = getPasswordHandler(authSubject);
			if (passwordHandler.isValidPasswordResetDigest(hash)) {
				LOGGER.debug("setting new password for {}", email);
				String password = passwordPolicy.generatePassword();
				PasswordHandler defaultPasswordHandler = getDefaultPasswordHandler(authSubject);
				defaultPasswordHandler.savePassword(password);
				return password.getBytes();
			} else {
				LOGGER.debug("hash did not match, not setting new password for '{}'", authSubject.getAuthName());
			}
		} else {
			LOGGER.debug("{} does not exist, is locked or may not change password!", authSubject.getAuthName());
		}
		return null;
	}

	public String forgotPassword(AuthSubject subject) throws BusinessException {
		SubjectImpl subjectByName = getSubjectByName(subject.getAuthName(), false);
		if (null != subjectByName && subjectByName.isChangePasswordAllowed() && !subjectByName.isLocked(new Date())) {
			PasswordHandler passwordHandler = getPasswordHandler(subject);
			String digest = passwordHandler.getPasswordResetDigest();
			passwordHandler.updateSubject(this);
			return digest;
		} else {
			throw new BusinessException(String.format("%s does not exist, is locked or may not change password!", subject.getAuthName()));
		}
	}

	public void updateSubject(SubjectImpl subject) {
		subjectRepository.save(subject);
	}

	public AccessibleApplication findApplicationByName(String name) {
		ApplicationImpl application = applicationRepository.findByName(name);
		if (null != application && !application.isFileBased()) {
			application.getResourceSet().size();
		}
		return application;
	}

	public void updateApplication(ApplicationImpl application, boolean isFileBased) throws BusinessException {
		synchronizeApplicationResources(null, application, isFileBased);
		application.setFileBased(isFileBased);
		applicationRepository.save(application);
	}

	public List<? extends Group> getGroups() {
		return groupRepository.findAll();
	}

	public void deleteSite(String name, FieldProcessor fp) throws BusinessException {
		SiteImpl siteByName = siteRepository.findByName(name);
		if (null == siteByName) {
			throw new BusinessException("No such site " + name);
		}
		deleteSite(null, siteByName);
	}

	public void setSiteActive(String name, boolean active) throws BusinessException {
		SiteImpl siteByName = siteRepository.findByName(name);
		if (null == siteByName) {
			throw new BusinessException("No such site " + name);
		}
		siteByName.setActive(active);
	}

	@Deprecated
	protected void deleteSite(Environment env, SiteImpl site, FieldProcessor fp, Request request,
			final String siteAliasDeleted, final String siteDeleteError) throws BusinessException {
		deleteSite(env, site);
	}

	public void deleteSite(Environment env, SiteImpl site) throws BusinessException {
		LOGGER.info("starting deletion of site {}", site.getName());
		List<SiteApplication> grantedApplications = siteApplicationRepository.findByGrantedSitesIn(site);
		for (SiteApplication siteApplication : grantedApplications) {
			siteApplication.getGrantedSites().remove(site);
		}
		detachApplications(site);

		Iterable<PropertyImpl> siteProperties = getSiteProperties(site.getId(), null);
		deleteProperties(siteProperties);

		SiteImpl shutdownSite = shutdownSite(env, site.getName(), false);
		List<DatabaseConnection> connections = databaseConnectionRepository.findBySiteId(site.getId());
		LOGGER.info("deleting {} orphaned database connections", connections.size());
		databaseConnectionRepository.delete(connections);
		siteRepository.delete(site);
		cleanupSite(env, shutdownSite, true);
		LOGGER.info("done deleting site {}", site.getName());
	}

	public void cleanupSite(Environment env, SiteImpl site, boolean sendDeletedEvent) {
		PlatformProperties platformConfig = PlatformProperties.get(getPlatformConfig(env));
		if (null != site) {
			if (site.isCreateRepository()) {
				File siteRootFolder = new File(platformConfig.getRepositoryRootFolder(), site.getName());
				try {
					FileUtils.deleteDirectory(siteRootFolder);
					LOGGER.info("deleted site repository {}", siteRootFolder.getPath());
				} catch (IOException e) {
					LOGGER.error(String.format("error while deleting site's folder %s", siteRootFolder.getName()), e);
				}
			}
			CacheProvider cacheProvider = new CacheProvider(platformConfig);
			cacheProvider.clearCache(site);
			site.setState(SiteState.DELETED);

			Map<String, Site> siteMap = env.getAttribute(Scope.PLATFORM, Platform.Environment.SITES);
			siteMap.remove(site.getName());

			if (sendDeletedEvent) {
				site.sendEvent(new SiteDeletedEvent(site.getName()));
			}
		}
	}

	private void detachApplications(SiteImpl site) throws BusinessException {
		Collection<SiteApplication> siteApplications = new HashSet<>(site.getSiteApplications());
		for (SiteApplication siteApplication : siteApplications) {
			unlinkApplicationFromSite(siteApplication);
		}
		siteApplications.clear();
	}

	private void deleteProperties(Iterable<PropertyImpl> properties) throws BusinessException {
		if (null != properties) {
			for (PropertyImpl property : properties) {
				deleteProperty(property);
			}
		}
	}

	private void deleteProperties(Integer siteId, Integer applicationId) throws BusinessException {
		Iterable<PropertyImpl> properties = getApplicationPropertiesList(siteId, applicationId);
		deleteProperties(properties);
	}

	public void deleteProperty(PropertyImpl prop) {
		propertyRepository.delete(prop);
		LOGGER.debug("deleting property '{}'", prop.getName());
	}

	protected MigrationStatus unlinkApplicationFromSite(SiteApplication siteApplication) {
		Site site = siteApplication.getSite();
		((SiteImpl) site).getSiteApplications().remove(siteApplication);
		siteApplicationRepository.delete(siteApplication);
		deleteApplicationPropertiesFromSite(site, siteApplication.getApplication());
		DatabaseConnection databaseConnection = siteApplication.getDatabaseConnection();
		MigrationStatus status = MigrationStatus.NO_DB_SUPPORTED;
		if (null != databaseConnection) {
			databaseConnectionRepository.delete(databaseConnection);
			status = databaseService.dropDataBaseAndUser(databaseConnection);
		}
		String applicationName = siteApplication.getApplication().getName();
		LOGGER.info("unlinking application {} from site {}, status of database-connection is {}", applicationName,
				site.getName(), status);
		auditableListener.createEvent(Type.INFO,
				String.format("Removed application %s from site %s", applicationName, site.getName()));
		if (MigrationStatus.DB_MIGRATED.equals(status)) {
			auditableListener.createEvent(Type.INFO, String.format("Dropped database %s and user %s",
					databaseConnection.getJdbcUrl(), databaseConnection.getUserName()));
		} else if (MigrationStatus.ERROR.equals(status)) {
			auditableListener.createEvent(Type.ERROR, String.format("Error while dropping database %s and user %s",
					databaseConnection.getJdbcUrl(), databaseConnection.getUserName()));
		}
		return status;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public MigrationStatus unlinkApplicationFromSite(Integer siteId, Integer applicationId) {
		SiteApplication siteApplication = siteApplicationRepository
				.findOne(new SiteApplicationPK(siteId, applicationId));
		return unlinkApplicationFromSite(siteApplication);
	}

	public void saveRole(RoleImpl role) {
		roleRepository.save(role);
	}

	public void deleteRole(Role role) throws BusinessException {
		deleteRole(role.getId(), null, null);
	}

	/**
	 * Deletes a ApplicationRole
	 * 
	 * @throws BusinessException
	 */
	protected void deleteRole(Integer roleId, final String roleDeleteError, final String roleErrorInvalid)
			throws BusinessException {
		RoleImpl applicationRole = roleRepository.findOne(roleId);
		if (null != applicationRole) {
			List<GroupImpl> groups = groupRepository.findGroupsForApplicationRole(roleId);
			for (Group group : groups) {
				group.getRoles().remove(applicationRole);
			}
			roleRepository.delete(applicationRole);
		} else {
			throw new BusinessException("No such ApplicationRole " + roleId, roleErrorInvalid, roleId);
		}
	}

	/**
	 * Deletes a Application via the Command Line Interface.
	 */
	public void deleteApplication(String applicationName, FieldProcessor fp) throws BusinessException {
		Application application = findApplicationByName(applicationName);
		if (null != application) {
			File applicationFolder = getApplicationFolder(null, application);
			deleteApplication(null, fp, application, applicationFolder, null, null, null, null);
		} else {
			throw new BusinessException("No such application: " + applicationName);
		}
	}

	protected void deleteApplication(Environment environment, Request request, Integer applicationId, FieldProcessor fp,
			final String applicationDeleteErrorWithCause, final String applicationRemovedFromSite,
			final String applicationErrorInvalid, final String applicationroleDeleteError,
			final String applicationroleErrorInvalid) throws BusinessException {

		try {
			Application application = applicationRepository.findOne(applicationId);
			if (null == application) {
				throw new BusinessException("no such application " + applicationId, applicationErrorInvalid,
						applicationId);
			}
			File applicationFolder = getApplicationFolder(environment, application);
			deleteApplication(request, fp, application, applicationFolder, applicationDeleteErrorWithCause,
					applicationRemovedFromSite, applicationroleDeleteError, applicationroleErrorInvalid);
		} catch (Exception e) {
			request.handleException(fp, e);
		}

	}

	private void deleteApplication(Request request, FieldProcessor fp, Application application, File applicationFolder,
			final String applicationDeleteErrorWithCause, final String applicationRemovedFromSite,
			final String applicationroleDeleteError, final String applicationroleErrorInvalid)
			throws BusinessException {

		if (null == application) {
			throw new BusinessException("Application is null!");
		}

		String applicationName = application.getName();
		Integer applicationId = application.getId();

		List<SiteImpl> activeSites = siteRepository.findSitesForApplication(applicationId, true);
		List<SiteImpl> inactiveSites = siteRepository.findSitesForApplication(applicationId, false);
		for (SiteImpl site : activeSites) {
			if (null != request) {
				fp.addErrorMessage(request.getMessage(applicationDeleteErrorWithCause, site.getName(), site.getId()));
			} else {
				fp.addErrorMessage("Can not delete application, because it is linked to the active site \""
						+ site.getName() + "\" with ID " + site.getId());
			}
		}

		if (fp.hasErrors()) {
			String errorMessage = "Cannot delete application with ID " + applicationId + ": " + application.getName();
			LOGGER.error(errorMessage);
			throw new BusinessException(errorMessage);
		}

		// remove application from inactive sites
		for (SiteImpl site : inactiveSites) {
			SiteApplication siteApplication = site.getSiteApplication(applicationName);
			deleteProperties(site.getId(), applicationId);
			site.getSiteApplications().remove(siteApplication);
			siteApplicationRepository.delete(siteApplication);
			if (null != request) {
				fp.addNoticeMessage(request.getMessage(applicationRemovedFromSite, site.getName(), site.getId()));
			} else {
				fp.addNoticeMessage(
						"Application has been removed from site " + site.getName() + " with ID " + site.getId());
			}
		}

		Set<Role> applicationRoles = application.getRoles();
		for (Role applicationRole : applicationRoles) {
			deleteRole(applicationRole.getId(), applicationroleDeleteError, applicationroleErrorInvalid);
		}
		applicationRoles.clear();

		Set<Permission> permissions = application.getPermissions();
		for (Permission permission : permissions) {
			deletePermission(permission);
		}
		permissions.clear();

		// delete application resources
		deleteApplicationResources(application, applicationFolder);

		// delete application properties
		deleteProperties(null, applicationId);
		applicationRepository.delete((ApplicationImpl) application);

	}

	public void deletePermission(Permission permission) {
		permissionRepository.delete((PermissionImpl) permission);
	}

	private void deleteApplicationPropertiesFromSite(Site site, Application application) {
		String propertyPrefix = PropertySupport.getPropertyPrefix(site, application);
		Iterable<PropertyImpl> properties = getProperties(propertyPrefix);
		try {
			deleteProperties(properties);
		} catch (BusinessException e) {
			LOGGER.error(String.format("error while deleting properties %s", properties), e);
		}
	}

	protected void assignRolesToGroup(Group group, Site site, List<Integer> applicationRoleIds) {
		Set<Role> applicationRoles = group.getRoles();
		List<Role> currentRoles = new ArrayList<>(applicationRoles);
		for (Role applicationRole : currentRoles) {
			Application applicationOfRole = applicationRole.getApplication();
			if (site.hasApplication(applicationOfRole.getName())) {
				applicationRoles.remove(applicationRole);
			}
		}
		for (Integer roleId : applicationRoleIds) {
			Role role = roleRepository.findOne(roleId);
			applicationRoles.add(role);
		}
	}

	public void addApplicationRolesToGroup(String groupName, String applicationName, List<String> applicationRoleNames,
			boolean clear) {
		Group group = groupRepository.findByName(groupName);
		if (clear) {
			group.getRoles().clear();
		}
		Application application = applicationRepository.findByName(applicationName);
		for (String applicationRoleName : applicationRoleNames) {
			Role applicationRole = roleRepository.findByApplicationIdAndName(application.getId(), applicationRoleName);
			if (null != applicationRole) {
				group.getRoles().add(applicationRole);
			} else {
				throw new NoResultException(
						"Application '" + applicationName + "' has no role '" + applicationRoleName + "'");
			}
		}
	}

	protected void assignGroupsToSubject(Integer subjectId, List<Integer> groupIds, boolean clear) {
		Subject subject = subjectRepository.findOne(subjectId);
		if (clear) {
			subject.getGroups().clear();
		}
		if (null != groupIds && !groupIds.isEmpty()) {
			Iterable<GroupImpl> groups = groupRepository.findAll(groupIds);
			for (GroupImpl group : groups) {
				if (!subject.getGroups().contains(group)) {
					subject.getGroups().add(group);
					group.getSubjects().add(subject);
				}
			}
		}
	}

	public void addGroupsToSubject(String userName, List<String> groupNames, boolean clear) throws BusinessException {
		SubjectImpl subject = subjectRepository.findByName(userName);
		List<Integer> groupIds = groupNames.isEmpty() ? null : groupRepository.getGroupIdsForNames(groupNames);
		assignGroupsToSubject(subject.getId(), groupIds, clear);
	}

	public List<DatabaseConnection> getDatabaseConnectionsForSite(Integer siteId) {
		return databaseConnectionRepository.findBySiteId(siteId);
	}

	public Page<DatabaseConnection> getDatabaseConnections(Integer siteId, FieldProcessor fp) {
		SearchQuery<DatabaseConnection> query = new SearchQuery<DatabaseConnection>(DatabaseConnection.class);
		CacheProvider cacheProvider = null;
		if (siteId == null) {
			query.isNull("site.id");
		} else {
			query.equals("site.id", siteId);
			cacheProvider = new CacheProvider(getPlatformProperties());
		}
		Page<DatabaseConnection> connections = databaseConnectionRepository.search(query, fp.getPageable());
		for (DatabaseConnection databaseConnection : connections) {
			prepareConnection(databaseConnection, true, cacheProvider);
		}
		return connections;
	}

	private void prepareConnection(DatabaseConnection databaseConnection, boolean clearPassword,
			CacheProvider cacheProvider) {
		if (databaseConnection.isActive()) {
			boolean isWorking = databaseConnection.testConnection(true);
			if (isWorking) {
				if (null == databaseConnection.getSite()) {
					databaseConnection
							.setMigrationInfoService(databaseService.statusComplete(databaseConnection, false));
				} else {
					SiteApplication siteApplication = siteApplicationRepository
							.findByDatabaseConnectionId(databaseConnection.getId());
					if (null != siteApplication) {
						File platformCache = cacheProvider.getPlatformCache(siteApplication.getSite(),
								siteApplication.getApplication());
						File sqlFolder = new File(platformCache, ResourceType.SQL.getFolder());
						databaseService.statusComplete(databaseConnection, sqlFolder);
					}
				}
			}
		}
		if (clearPassword) {
			clearConnectionPassword(databaseConnection);
		}
	}

	public DatabaseConnection getDatabaseConnection(Integer dcId, boolean clearPassword) {
		DatabaseConnection conn = databaseConnectionRepository.findOne(dcId);
		CacheProvider cacheProvider = null == conn.getSite() ? null : new CacheProvider(getPlatformProperties());
		prepareConnection(conn, clearPassword, cacheProvider);
		return conn;
	}

	private void clearConnectionPassword(DatabaseConnection conn) {
		databaseConnectionRepository.detach(conn);
		conn.setPasswordPlain("");
	}

	protected DatabaseConnection createDatabaseConnection(DatabaseConnection databaseConnection, boolean managed) {
		databaseConnection.setManaged(managed);
		return databaseConnectionRepository.save(databaseConnection);
	}

	protected Resources getResources(Application application, File applicationCacheFolder, File applicationRootFolder)
			throws InvalidConfigurationException {
		try {
			File applicationFolder = new File(applicationRootFolder, application.getName());
			return new ApplicationResourceHolder(application, MarshallService.getApplicationMarshallService(),
					applicationFolder, applicationCacheFolder);
		} catch (JAXBException e) {
			throw new InvalidConfigurationException(application.getName(), "error while obtaining MarshallService", e);
		}
	}

	protected void initApplicationProperties(Site site, AccessibleApplication application) {
		LOGGER.info("loading properties for application '{}' of site '{}'", application.getName(), site.getName());
		PropertyHolder applicationProperties = getApplicationProperties(site, application);
		setFeatures(applicationProperties);
		applicationProperties.setFinal();
		application.setProperties(applicationProperties);
		LOGGER.debug("initialized properties for application {}: {}", application.getName(),
				applicationProperties.toString());
		initApplicationProperties(application);
	}

	protected void initApplicationProperties(AccessibleApplication application) {
		LOGGER.info("loading properties for application {}", application.getName());
		PropertyHolder applicationProperties = getApplicationProperties(null, application);
		setFeatures(applicationProperties);
		applicationProperties.setFinal();
		LOGGER.debug("initialized properties for application {}: {}", application.getName(),
				applicationProperties.toString());
	}

	private void setFeatures(Properties applicationProperties) {
		for (String feature : ApplicationProperties.FEATURES) {
			applicationProperties.getBoolean(feature, false);
		}
	}

	public Boolean updatePassword(char[] password, char[] passwordConfirmation, SubjectImpl currentSubject)
			throws BusinessException {
		boolean passwordUpdated = false;
		String passwordString = new String(password);
		String passwordConfirmationString = new String(passwordConfirmation);
		if (StringUtils.isNotEmpty(passwordString) && StringUtils.isNotEmpty(passwordConfirmationString)) {
			if (passwordString.equals(passwordConfirmationString)) {
				PasswordHandler passwordHandler = getDefaultPasswordHandler(currentSubject);
				passwordHandler.savePassword(passwordString);
				passwordUpdated = true;
			} else {
				throw new BusinessException("Passwords are not equal.");
			}
		}
		return passwordUpdated;
	}

	public void resetConnection(FieldProcessor fp, Integer conId) {
		SiteApplication siteApplication = siteApplicationRepository.findByDatabaseConnectionId(conId);
		if (null != siteApplication) {
			String databasePrefix = getPlatform(true, false).getString(Platform.Property.DATABASE_PREFIX);
			databaseService.resetApplicationConnection(siteApplication, databasePrefix);
		}
	}

	public GroupImpl createGroup(GroupImpl group) {
		return groupRepository.save(group);
	}

	public void deleteGroup(GroupImpl group) {
		if (!group.isDefaultAdmin()) {
			List<SubjectImpl> subjects = subjectRepository.findAll();
			for (Subject subject : subjects) {
				if (subject.getGroups().remove(group)) {
					LOGGER.debug("removed group '{}' from subject {}", group.getName() + subject.getName());
				}
			}
			groupRepository.delete(group);
		} else {
			LOGGER.error("Someone tried to delete default admin group: {}", group);
		}
	}

	public void deleteApplicationRepository(org.appng.core.model.Repository repository) {
		repoRepository.delete(repository.getId());
	}

	public void deleteSubject(Subject subject) {
		subjectRepository.delete(subject.getId());
	}

	public SiteImpl shutdownSite(Environment env, String siteName) {
		return shutdownSite(env, siteName, false);
	}

	public SiteImpl shutdownSite(Environment env, String siteName, boolean removeFromSiteMap) {
		Properties platformConfig = getPlatformConfig(env);
		if (null != env) {
			Map<String, Site> siteMap = env.getAttribute(Scope.PLATFORM, Platform.Environment.SITES);
			if (siteMap.containsKey(siteName) && null != siteMap.get(siteName)) {
				SiteImpl shutdownSite = (SiteImpl) siteMap.get(siteName);
				int requests;
				int waited = 0;
				int waitTime = platformConfig.getInteger(Platform.Property.WAIT_TIME, 1000);
				int maxWaitTime = platformConfig.getInteger(Platform.Property.MAX_WAIT_TIME, 30000);
				shutdownSite.setState(SiteState.STOPPING);

				if (platformConfig.getBoolean(Platform.Property.WAIT_ON_SITE_SHUTDOWN, false)) {
					LOGGER.info("preparing to shutdown site {} that is currently handling {} requests", shutdownSite,
							shutdownSite.getRequests());
					Path path = env.getAttribute(Scope.REQUEST, EnvironmentKeys.PATH_INFO);
					int requestLimit = null == path ? 0 : path.getSiteName().equals(siteName) ? 1 : 0;
					while (waited < maxWaitTime && (requests = shutdownSite.getRequests()) > requestLimit) {
						try {
							Thread.sleep(waitTime);
							waited += waitTime;
						} catch (InterruptedException e) {
							LOGGER.error("error while waiting for site to finish its requests", e);
						}
						LOGGER.info("waiting for {} requests to finish before shutting down site {}", requests,
								siteName);
					}
				}

				LOGGER.info("destroying site {}", shutdownSite);
				if (SiteState.STARTED.equals(shutdownSite.getState())) {
					for (SiteApplication siteApplication : shutdownSite.getSiteApplications()) {
						shutdownApplication(siteApplication, env);
					}
					shutdownSite.closeSiteContext();
					((DefaultEnvironment) env).clearSiteScope(shutdownSite);
					LOGGER.info("destroying site {} complete", shutdownSite);
					setSiteStartUpTime(shutdownSite, null);
					SoapService.clearCache(siteName);
					if (shutdownSite.getProperties().getBoolean(SiteProperties.CACHE_CLEAR_ON_SHUTDOWN)) {
						CacheService.clearCache(shutdownSite);
					}
				}
				shutdownSite.setState(shutdownSite.isActive() ? SiteState.STOPPED : SiteState.INACTIVE);
				auditableListener.createEvent(Type.INFO, "Shut down site " + shutdownSite.getName());
				if (removeFromSiteMap) {
					siteMap.remove(siteName);
				}
				return shutdownSite;
			}
		}
		return null;
	}

	private void shutdownApplication(SiteApplication siteApplication, Environment env) {
		Site site = siteApplication.getSite();
		Application application = siteApplication.getApplication();
		ApplicationController controller = application.getBean(ApplicationController.class);
		if (null != controller) {
			if (siteApplication.isMarkedForDeletion()) {
				controller.removeSite(site, application, env);
			} else {
				controller.shutdown(site, application, env);
			}
		}
	}

	public void unsetReloadRequired(SiteApplication siteApplication) {
		siteApplication.setReloadRequired(false);
		siteApplicationRepository.findOne(siteApplication.getSiteApplicationId()).setReloadRequired(false);
	}

	public void setSiteStartUpTime(SiteImpl site, Date date) {
		site.setStartupTime(date);
	}

	public Collection<ApplicationSubject> getApplicationSubjects(Integer applicationId, Site site) {
		List<ApplicationSubject> applicationSubjects = new ArrayList<>();
		ApplicationImpl application = applicationRepository.findOne(applicationId);
		List<SubjectImpl> subjects = subjectRepository.findSubjectsForApplication(applicationId);
		String siteTimeZone = site.getProperties().getString(Platform.Property.TIME_ZONE);
		for (SubjectImpl subject : subjects) {
			initializeSubject(subject);
			subject.getApplicationroles(application);
			if (UserType.GLOBAL_GROUP.equals(subject.getUserType())) {
				List<SubjectImpl> membersOfGroup = ldapService.getMembersOfGroup(site, subject.getAuthName());
				for (SubjectImpl ldapSubject : membersOfGroup) {
					String timeZone = subject.getTimeZone();
					if (null == timeZone) {
						timeZone = siteTimeZone;
					}
					ldapSubject.setTimeZone(timeZone);
					ldapSubject.setLanguage(subject.getLanguage());
					ldapSubject.getGroups().addAll(subject.getGroups());
					ApplicationSubject ps = getApplicationSubject(application, ldapSubject);
					applicationSubjects.add(ps);
				}
			} else {
				ApplicationSubject ps = getApplicationSubject(application, subject);
				applicationSubjects.add(ps);
			}
		}
		return applicationSubjects;
	}

	private ApplicationSubject getApplicationSubject(ApplicationImpl application, SubjectImpl subject) {
		ApplicationSubject ps = new ApplicationSubjectImpl(subject.getAuthName(), subject.getRealname(),
				subject.getEmail(), subject.getLanguage(), subject.getTimeZone());
		ps.getRoles().addAll(subject.getApplicationroles(application));
		return ps;
	}

	public SubjectImpl getSubjectByName(String name, boolean initialize) {
		SubjectImpl subject = subjectRepository.findByName(name);
		if (initialize) {
			initializeSubject(subject);
		}
		return subject;
	}

	public Subject getSubjectById(Integer id, boolean initialize) {
		SubjectImpl subject = subjectRepository.findOne(id);
		if (initialize) {
			initializeSubject(subject);
		}
		return subject;
	}

	private void initializeSubject(SubjectImpl subject) {
		if (null != subject) {
			List<Group> groups = subject.getGroups();
			for (Group group : groups) {
				initGroup(group);
			}
			subjectRepository.detach(subject);
		}
	}

	private void initGroup(Group group) {
		for (Role applicationRole : group.getRoles()) {
			Set<Permission> permissions = applicationRole.getPermissions();
			permissions.iterator().hasNext();
			groupRepository.detach((GroupImpl) group);
		}
	}

	public org.appng.core.model.Repository getApplicationRepositoryByName(String repositoryName) {
		return repoRepository.findByName(repositoryName);
	}

	public void saveRepository(RepositoryImpl repo) {
		repoRepository.save(repo);
	}

	public List<ApplicationImpl> getApplications() {
		return applicationRepository.findAll();
	}

	public List<RoleImpl> getApplicationRolesForApplication(Integer applicationId) {
		return roleRepository.findByApplicationId(applicationId);
	}

	public List<RoleImpl> getApplicationRoles() {
		return roleRepository.findAll();
	}

	public List<RepositoryImpl> getApplicationRepositories() {
		return repoRepository.findAll();
	}

	public List<SiteImpl> getSites() {
		return siteRepository.findAll();
	}

	public List<SubjectImpl> getSubjects() {
		return subjectRepository.findAll();
	}

	public GroupImpl getGroupByName(String name) {
		return groupRepository.findByName(name);
	}

	private List<GroupImpl> getAdminGroups() {
		return groupRepository.findByDefaultAdmin(true);
	}

	public GroupImpl getGroupByName(String name, boolean init) {
		GroupImpl group = getGroupByName(name);
		if (init) {
			initGroup(group);
		}
		return group;
	}

	public void updateGroup(GroupImpl group) {
		groupRepository.save(group);
	}

	public Subject getSubjectByEmail(String email) {
		return subjectRepository.findByEmail(email);
	}

	public int addPermissions(String applicationName, String roleName, List<String> permissionNames)
			throws BusinessException {
		Role role = getApplicationRole(applicationName, roleName);
		Integer applicationId = role.getApplication().getId();
		int added = 0;
		for (String permissionName : permissionNames) {
			Permission permission = permissionRepository.findByNameAndApplicationId(permissionName, applicationId);
			if (null == permission) {
				throw new BusinessException("No such permission: " + permissionName);
			}
			if (!role.getPermissions().contains(permission)) {
				role.getPermissions().add(permission);
				added++;
			}
		}
		return added;
	}

	public int removePermissions(String applicationName, String roleName, List<String> permissionNames)
			throws BusinessException {
		Role role = getApplicationRole(applicationName, roleName);
		Integer applicationId = role.getApplication().getId();
		int removed = 0;
		for (String permissionName : permissionNames) {
			Permission permission = permissionRepository.findByNameAndApplicationId(permissionName, applicationId);
			if (null == permission) {
				throw new BusinessException("No such permission: " + permissionName);
			}
			if (role.getPermissions().contains(permission)) {
				role.getPermissions().remove(permission);
				removed++;
			}
		}
		return removed;
	}

	private Role getApplicationRole(String applicationName, String roleName) throws BusinessException {
		Application application = applicationRepository.findByName(applicationName);
		if (null == application) {
			throw new BusinessException("No such application : " + applicationName);
		}
		Role role = roleRepository.findByApplicationIdAndName(application.getId(), roleName);
		if (null == role) {
			throw new BusinessException("No such role :" + roleName);
		}
		return role;
	}

	public Role getApplicationRoleForApplication(Integer applicationId, String roleName) {
		RoleImpl role = roleRepository.findByApplicationIdAndName(applicationId, roleName);
		return initRole(role);
	}

	public RoleImpl getApplicationRoleForApplication(String applicationName, String roleName) {
		RoleImpl role = roleRepository.findByApplicationNameAndName(applicationName, roleName);
		return initRole(role);
	}

	public RoleImpl initRole(RoleImpl role) {
		if (null != role) {
			role.getPermissions().size();
		}
		return role;
	}

	public List<? extends Permission> getPermissionsForApplication(Integer applicationId) {
		return permissionRepository.findByApplicationId(applicationId, new Sort(Direction.ASC, "name"));
	}

	public Site getGrantingSite(String grantedSite, String grantedApplication) {
		SiteApplication siteApplication = siteApplicationRepository
				.findByApplicationNameAndGrantedSitesName(grantedApplication, grantedSite);
		return null == siteApplication ? null : siteApplication.getSite();
	}

	public Map<String, String> getCacheStatistics(Integer siteId) {
		SiteImpl site = getSite(siteId);
		return CacheService.getCacheStatistics(site);
	}

	public List<CachedResponse> getCacheEntries(Integer siteId) {
		SiteImpl site = siteRepository.findOne(siteId);
		return CacheService.getCacheEntries(site);
	}

	public void expireCacheElement(Integer siteId, String cacheElement) throws BusinessException {
		Site site = getSite(siteId);
		CacheService.expireCacheElement(site, cacheElement);
	}

	public int expireCacheElementsStartingWith(Integer siteId, String cacheElementPrefix) throws BusinessException {
		Site site = getSite(siteId);
		return CacheService.expireCacheElementsStartingWith(site, cacheElementPrefix);
	}

	public void clearCacheStatistics(Integer siteId) {
		Site site = getSite(siteId);
		CacheService.clearStatistics(site);
	}

	public void clearCache(Integer siteId) {
		Site site = getSite(siteId);
		CacheService.clearCache(site);
	}

	public Application getApplicationForConnection(DatabaseConnection dbc) {
		return siteApplicationRepository.findApplicationForConnection(dbc);
	}

	public DatabaseConnection getDatabaseConnection(SiteImpl site, ApplicationImpl application) {
		return siteApplicationRepository.getDatabaseForSiteAndApplication(site, application);
	}

	public SiteApplication getSiteApplication(String site, String application) {
		return siteApplicationRepository.findBySiteNameAndApplicationName(site, application);
	}

	public SiteApplication getSiteApplicationWithGrantedSites(String site, String application) {
		SiteApplication siteApplication = getSiteApplication(site, application);
		if (null != siteApplication) {
			siteApplication.getGrantedSites().size();
		}
		return siteApplication;
	}

	public SiteApplication grantApplicationForSites(String site, String application, List<String> siteNames) {
		SiteApplication siteApplication = getSiteApplication(site, application);
		if (CollectionUtils.isNotEmpty(siteNames)) {
			siteApplication.getGrantedSites().clear();
			List<SiteImpl> grantedSites = siteRepository.findByNameIn(siteNames);
			siteApplication.getGrantedSites().addAll(grantedSites);
		}
		return siteApplication;
	}

	public void createEvent(Type type, String message, HttpSession session) {
		auditableListener.createEvent(type, message, session);
	}

	public void createEvent(Type type, String message, Object... args) {
		auditableListener.createEvent(type,	String.format(message, args));
	}

	public void setSiteReloadCount(SiteImpl site) {
		siteRepository.findOne(site.getId()).setReloadCount(site.getReloadCount());
	}

}
