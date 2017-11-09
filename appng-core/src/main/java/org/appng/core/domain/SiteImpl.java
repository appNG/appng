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
package org.appng.core.domain;

import static org.appng.api.Scope.REQUEST;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.appng.api.Environment;
import org.appng.api.Path;
import org.appng.api.Scope;
import org.appng.api.SiteProperties;
import org.appng.api.ValidationMessages;
import org.appng.api.auth.PasswordPolicy;
import org.appng.api.messaging.Event;
import org.appng.api.messaging.Sender;
import org.appng.api.model.Application;
import org.appng.api.model.Named;
import org.appng.api.model.Properties;
import org.appng.api.model.Site;
import org.appng.api.support.SiteClassLoader;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.api.support.environment.EnvironmentKeys;
import org.appng.core.Redirect;
import org.appng.core.controller.HttpHeaders;
import org.appng.core.controller.messaging.SiteStateEvent;
import org.appng.core.model.AccessibleApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Default {@link Site}-implementation
 * 
 * @author Matthias Müller
 * 
 */
@Entity
@Table(name = "site")
public class SiteImpl implements Site {

	private static final Logger log = LoggerFactory.getLogger(SiteImpl.class);
	private static final String SLASH = "/";
	private static final String TAB_PARAM_NAME = "tab";
	private static final String AGENT_MSIE = "MSIE";
	private static final char EQ = '=';
	private static final char QMARK = '?';
	private static final char AMPERSAND = '&';
	private static final char ANCHOR = '#';
	private Integer id;
	private String name;
	private String description;
	private Date version;
	private String host;
	private String domain;
	private Set<SiteApplication> applications = new HashSet<SiteApplication>();
	private boolean active;
	private boolean createRepository = false;
	private SiteClassLoader siteClassLoader;
	private Properties properties;
	private Set<Named<Integer>> groups = new HashSet<Named<Integer>>();
	private File siteRootDirectory;
	private PasswordPolicy policy;
	private Date startupTime;
	private boolean isRunning;
	private Sender sender;
	private AtomicReference<SiteState> state = new AtomicReference<Site.SiteState>(SiteState.STOPPED);
	private AtomicInteger requests = new AtomicInteger(0);

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@NotNull(message = ValidationMessages.VALIDATION_NOT_NULL)
	@Pattern(regexp = ValidationPatterns.NAME_STRICT_PATTERN, message = ValidationPatterns.NAME_STRICT_MSSG)
	@Size(max = ValidationPatterns.LENGTH_64, message = ValidationMessages.VALIDATION_STRING_MAX)
	@Column(unique = true)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Size(max = ValidationPatterns.LENGTH_8192, message = ValidationMessages.VALIDATION_STRING_MAX)
	@Column(length = ValidationPatterns.LENGTH_8192)
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Version
	public Date getVersion() {
		return version;
	}

	public void setVersion(Date version) {
		this.version = version;
	}

	@OneToMany(targetEntity = org.appng.core.domain.SiteApplication.class, fetch = FetchType.LAZY, mappedBy = "site")
	public Set<SiteApplication> getSiteApplications() {
		return applications;
	}

	public void setSiteApplications(Set<SiteApplication> applications) {
		this.applications = applications;
	}

	@Transient
	public Set<Application> getApplications() {
		Set<Application> applicationList = new HashSet<Application>();
		for (SiteApplication application : applications) {
			applicationList.add(application.getApplication());
		}
		return applicationList;
	}

	@NotNull(message = ValidationMessages.VALIDATION_NOT_NULL)
	@Pattern(regexp = ValidationPatterns.HOST_PATTERN, message = ValidationPatterns.HOST_MSSG)
	@Column(unique = true)
	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	@NotNull(message = ValidationMessages.VALIDATION_NOT_NULL)
	@Pattern(regexp = ValidationPatterns.DOMAIN_PATTERN, message = ValidationPatterns.DOMAIN_MSSG)
	@Column(unique = true)
	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	@Column(name = "create_repository")
	public boolean isCreateRepository() {
		return createRepository;
	}

	public void setCreateRepository(boolean createRepository) {
		this.createRepository = createRepository;
	}

	@Transient
	public Map<String, Application> getApplicationMap() {
		Map<String, Application> map = new HashMap<String, Application>();
		for (SiteApplication p : applications) {
			map.put(p.getApplication().getName(), p.getApplication());
		}
		return Collections.unmodifiableMap(map);
	}

	@Transient
	public Application getApplication(String name) {
		if (null == name) {
			return null;
		}
		return getApplicationMap().get(name);
	}

	@Transient
	public boolean hasApplication(String name) {
		return getApplication(name) != null;
	}

	@Override
	public int hashCode() {
		return ObjectUtils.hashCode(this);
	}

	@Override
	public boolean equals(Object o) {
		return ObjectUtils.equals(this, o);
	}

	@Transient
	public Properties getProperties() {
		return properties;
	}

	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	@Transient
	public SiteClassLoader getSiteClassLoader() {
		return siteClassLoader;
	}

	public void setSiteClassLoader(SiteClassLoader siteClassLoader) {
		this.siteClassLoader = siteClassLoader;
	}

	public void setSender(Sender sender) {
		this.sender = sender;
	}

	public boolean sendEvent(Event event) {
		if (null == sender) {
			log.debug("messaging is disabled, not sending event {}", event);
			return false;
		}
		return sender.send(event);
	}

	/**
	 * @see SiteImpl#sendRedirect(Environment, String)
	 */
	public void sendRedirect(Environment env, String target) {
		sendRedirect(env, target, HttpServletResponse.SC_MOVED_PERMANENTLY);
	}

	public void sendRedirect(Environment env, String target, Integer statusCode) {
		sendRedirect(env, target, statusCode, false);
	}

	/**
	 * @see SiteImpl#sendRedirect(Environment, String, Integer)
	 */
	public void sendRedirect(Environment env, String target, Integer statusCode, boolean keepOrigin) {
		DefaultEnvironment defEnv = (DefaultEnvironment) env;
		String completePath = target;
		if (!target.startsWith(SLASH)) {
			Path path = env.getAttribute(Scope.REQUEST, EnvironmentKeys.PATH_INFO);
			completePath = path.getGuiPath() + SLASH + getName() + SLASH + target;
		}

		int tabIdx = completePath.indexOf(ANCHOR);
		Boolean appendTabId = getProperties().getBoolean(SiteProperties.APPEND_TAB_ID, false);
		if (appendTabId && tabIdx > 0) {
			String userAgent = env.getAttribute(REQUEST, HttpHeaders.USER_AGENT);
			if (userAgent.indexOf(AGENT_MSIE) >= 0) {
				int qmIdx = completePath.indexOf(QMARK);
				String tabId = completePath.substring(tabIdx + 1);
				String pathExtension = (qmIdx < 0 ? QMARK : AMPERSAND) + TAB_PARAM_NAME + EQ + tabId + ANCHOR + tabId;
				completePath = completePath.substring(0, tabIdx) + pathExtension;
			}
		}
		String origin = defEnv.getServletRequest().getServletPath();
		if (keepOrigin) {
			env.setAttribute(Scope.SESSION, "REDIRECT_ORIGIN", origin);
		}
		Redirect.to(defEnv.getServletResponse(), statusCode, origin, completePath);
	}

	public File readFile(String relativePath) {
		return new File(siteRootDirectory, relativePath);
	}

	// @Column(name = "startup_time")
	@Transient
	public Date getStartupTime() {
		return startupTime;
	}

	public void setStartupTime(Date startupTime) {
		this.startupTime = startupTime;
	}

	@Override
	public String toString() {
		return getName() + " [" + getState() + "] (#" + System.identityHashCode(this) + ")";
	}

	public void closeSiteContext() {
		log.info("closing context for site {}", this);
		for (SiteApplication p : getSiteApplications()) {
			((AccessibleApplication) p.getApplication()).closeContext();
		}
		closeClassloader();
	}

	private void closeClassloader() {
		try {
			siteClassLoader.close();
		} catch (IOException e) {
			log.error("error while closing classloader", e);
		}
		siteClassLoader = null;
	}

	@Transient
	public Set<Named<Integer>> getGroups() {
		return Collections.unmodifiableSet(groups);
	}

	public void setGroups(Set<Named<Integer>> groups) {
		this.groups = groups;
	}

	public void setRootDirectory(File siteRootDirectory) {
		this.siteRootDirectory = siteRootDirectory;
	}

	public SiteApplication getSiteApplication(String name) {
		for (SiteApplication application : applications) {
			if (application.getApplication().getName().equals(name)) {
				return application;
			}
		}
		return null;
	}

	@Transient
	public PasswordPolicy getPasswordPolicy() {
		return policy;
	}

	public void setPasswordPolicy(PasswordPolicy policy) {
		this.policy = policy;
	}

	@Transient
	public boolean isRunning() {
		return isRunning;
	}

	public void setRunning(boolean isRunning) {
		this.isRunning = isRunning;
	}

	@Transient
	public SiteState getState() {
		return state.get();
	}

	public void setState(SiteState state) {
		SiteState oldState = getState();
		this.state.set(state);
		log.debug("set state for site {} (was: {})", toString(), oldState);
		sendEvent(new SiteStateEvent(getName(), state));
	}

	public boolean hasState(SiteState... states) {
		if (null == states) {
			return null == state.get();
		}
		for (SiteState siteState : states) {
			if (siteState.equals(state.get())) {
				return true;
			}
		}
		return false;
	}

	public int addRequest() {
		return requests.incrementAndGet();
	}

	public int removeRequest() {
		return requests.decrementAndGet();
	}

	@Transient
	public int getRequests() {
		return requests.get();
	}
}
