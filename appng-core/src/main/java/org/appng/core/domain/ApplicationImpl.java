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
package org.appng.core.domain;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.appng.api.Environment;
import org.appng.api.Scope;
import org.appng.api.ValidationMessages;
import org.appng.api.model.Application;
import org.appng.api.model.ApplicationSubject;
import org.appng.api.model.FeatureProvider;
import org.appng.api.model.Permission;
import org.appng.api.model.Properties;
import org.appng.api.model.Resource;
import org.appng.api.model.Resources;
import org.appng.api.model.Role;
import org.appng.api.model.Site;
import org.appng.api.support.environment.EnvironmentKeys;
import org.appng.core.model.AccessibleApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.MessageSource;

/**
 * 
 * Default {@link Application}/{@link AccessibleApplication}-implementation
 * 
 * @author Matthias Müller
 * 
 */
@Entity
@Table(name = "application")
public class ApplicationImpl implements AccessibleApplication {

	private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationImpl.class);
	private Integer id;
	private String name;
	private String description;
	private Date version;
	private String displayName;
	private String applicationVersion;
	private String timestamp;
	private String longDescription;
	private String appNGVersion;
	private boolean fileBased;
	private Set<Permission> permissions = new HashSet<Permission>();
	private Set<Role> roles = new HashSet<Role>();
	private Set<Resource> resources = new HashSet<Resource>();
	private Properties properties;
	private ConfigurableApplicationContext context;
	private boolean isPrivileged;
	private boolean hidden;
	private boolean isSnapshot;
	private FeatureProvider featureProvider;
	private Resources applicationResources;
	private List<ApplicationSubject> applicationSubjects;

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

	@Size(max = ValidationPatterns.LENGTH_64, message = ValidationMessages.VALIDATION_STRING_MAX)
	@Column(length = ValidationPatterns.LENGTH_64)
	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	@Size(max = ValidationPatterns.LENGTH_64, message = ValidationMessages.VALIDATION_STRING_MAX)
	@Column(length = ValidationPatterns.LENGTH_64, name = "application_version")
	public String getApplicationVersion() {
		return applicationVersion;
	}

	public void setApplicationVersion(String applicationVersion) {
		this.applicationVersion = applicationVersion;
	}

	@Transient
	public String getPackageVersion() {
		return getApplicationVersion();
	}

	@Size(max = ValidationPatterns.LENGTH_64, message = ValidationMessages.VALIDATION_STRING_MAX)
	@Column(name = "time_stamp", length = ValidationPatterns.LENGTH_64)
	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	@Lob
	public String getLongDescription() {
		return longDescription;
	}

	public void setLongDescription(String longDescription) {
		this.longDescription = longDescription;
	}

	@Size(max = ValidationPatterns.LENGTH_64, message = ValidationMessages.VALIDATION_STRING_MAX)
	@Column(name = "appng_version", length = ValidationPatterns.LENGTH_64)
	public String getAppNGVersion() {
		return appNGVersion;
	}

	public void setAppNGVersion(String appNGVersion) {
		this.appNGVersion = appNGVersion;
	}

	@Transient
	public boolean isInstalled() {
		return true;
	}

	public boolean isSnapshot() {
		return isSnapshot;
	}

	public void setSnapshot(boolean isSnapshot) {
		this.isSnapshot = isSnapshot;
	}

	public boolean isFileBased() {
		return fileBased;
	}

	public void setFileBased(boolean fileBased) {
		this.fileBased = fileBased;
	}

	@OneToMany(targetEntity = PermissionImpl.class, mappedBy = "application")
	public Set<Permission> getPermissions() {
		return permissions;
	}

	public void setPermissions(Set<Permission> permissions) {
		this.permissions = permissions;
	}

	@OneToMany(targetEntity = RoleImpl.class, mappedBy = "application", fetch = FetchType.LAZY)
	public Set<Role> getRoles() {
		return roles;
	}

	public void setRoles(Set<Role> roles) {
		this.roles = roles;
	}

	@OneToMany(targetEntity = ResourceImpl.class, mappedBy = "application", fetch = FetchType.LAZY)
	public Set<Resource> getResourceSet() {
		return resources;
	}

	public void setResourceSet(Set<Resource> applicationResources) {
		this.resources = applicationResources;
	}

	@Transient
	public Properties getProperties() {
		return properties;
	}

	public void setProperties(Properties properties) {
		this.properties = properties;
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
	public ConfigurableApplicationContext getContext() {
		return context;
	}

	public synchronized void setContext(ConfigurableApplicationContext applicationContext) {
		if (this.context != null) {
			closeContext();
		}
		this.context = applicationContext;
	}

	@Transient
	public <T> T getBean(String name, Class<T> clazz) {
		long startupDate = context.getStartupDate();
		if (startupDate == 0) {
			context.refresh();
		}
		T bean = null;
		try {
			bean = context.getBean(name, clazz);
		} catch (BeansException e) {
			LOGGER.warn("error while retrieving bean", e);
		}
		return bean;
	}

	@Transient
	public <T> T getBean(Class<T> clazz) {
		T bean = null;
		try {
			bean = context.getBean(clazz);
		} catch (BeansException e) {
			LOGGER.warn(e.getMessage());
		}
		return bean;
	}

	@Transient
	public Object getBean(String beanName) {
		Object bean = null;
		try {
			bean = context.getBean(beanName);
		} catch (BeansException e) {
			LOGGER.warn(e.getMessage());
		}
		return bean;
	}

	@Transient
	public boolean containsBean(String beanName) {
		return context.containsBean(beanName);
	}

	@Transient
	public String[] getBeanNames(Class<?> clazz) {
		try {
			return context.getBeanNamesForType(clazz);
		} catch (BeansException e) {
			LOGGER.warn(e.getMessage());
		}
		return new String[0];
	}

	@Column(name = "privileged")
	public boolean isPrivileged() {
		return isPrivileged;
	}

	@Transient
	@Deprecated
	public boolean isCoreApplication() {
		return isPrivileged();
	}

	public void setPrivileged(boolean isPrivileged) {
		this.isPrivileged = isPrivileged;
	}

	@Deprecated
	public void setCoreApplication(boolean isCoreApplication) {
		setPrivileged(isCoreApplication);
	}

	public void closeContext() {
		LOGGER.info("closing context for application " + getName());
		try {
			applicationResources.close();
		} catch (IOException e) {
			LOGGER.warn("error while closing {}", applicationResources);
		}
		resources.clear();
		roles.clear();
		permissions.clear();
		applicationResources = null;
		resources = null;
		roles = null;
		permissions = null;
		if (null != context) {
			context.close();
			context = null;
		}
	}

	public String getMessage(Locale locale, String key, Object... args) {
		return getBean(MessageSource.class).getMessage(key, args, locale);
	}

	@Override
	public String toString() {
		return "Application#" + getId() + "_" + getName();
	}

	public boolean isHidden() {
		return hidden;
	}

	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}

	public String getSessionParamKey(Site site) {
		return EnvironmentKeys.SESSION_PARAMS + "." + site.getName() + "." + getName();
	}

	public Map<String, String> getSessionParams(Site site, Environment environment) {
		return environment.getAttribute(Scope.SESSION, getSessionParamKey(site));
	}

	@Transient
	public FeatureProvider getFeatureProvider() {
		return featureProvider;
	}

	public void setFeatureProvider(FeatureProvider featureProvider) {
		this.featureProvider = featureProvider;
	}

	public void setResources(Resources applicationResourceHolder) {
		this.applicationResources = applicationResourceHolder;
	}

	@Transient
	public Resources getResources() {
		return applicationResources;
	}

	@Transient
	public List<ApplicationSubject> getApplicationSubjects() {
		if (applicationSubjects == null) {
			applicationSubjects = new ArrayList<ApplicationSubject>();
		}
		return this.applicationSubjects;
	}

}
