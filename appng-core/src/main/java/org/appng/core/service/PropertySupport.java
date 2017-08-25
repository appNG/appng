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
package org.appng.core.service;

import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.Platform;
import org.appng.api.SiteProperties;
import org.appng.api.VHostMode;
import org.appng.api.auth.AuthTools;
import org.appng.api.model.Application;
import org.appng.api.model.Properties;
import org.appng.api.model.Property;
import org.appng.api.model.Site;
import org.appng.api.support.PropertyHolder;
import org.appng.core.controller.HttpHeaders;
import org.appng.core.controller.messaging.MulticastReceiver;
import org.appng.core.domain.SiteImpl;
import org.appng.core.repository.config.HikariCPConfigurer;
import org.appng.core.security.DefaultPasswordPolicy;

/**
 * A service offering methods for initializing and retrieving the configuration {@link Properties} of the platform, a
 * {@link Site} or an {@link Application}.
 * 
 * @author Matthias Müller
 * 
 * @see Properties
 * @see PropertyHolder
 */
public class PropertySupport {

	private static final String PREFIX_EMPTY = "";
	public static final String PREFIX_PLATFORM = "platform.";
	private static final String PREFIX_SITE = "site.";
	private static final String PREFIX_APPLICATION = "application.";
	private static final String DOT = ".";

	public static final String PROP_PATTERN = "[a-zA-Z0-9\\-_]+";

	private PropertyHolder propertyHolder;
	private ResourceBundle bundle;

	/**
	 * Creates a new {@link PropertySupport} using the given {@link PropertyHolder}.
	 * 
	 * @param propertyHolder
	 *            the {@link PropertyHolder} to use
	 */
	public PropertySupport(PropertyHolder propertyHolder) {
		this.propertyHolder = propertyHolder;
	}

	/**
	 * Aggregates the {@link Properties} of the platform, the given {@link Site} and given {@link Application} to a
	 * single {@link java.util.Properties} object, using a prefix for determining the origin of a certain property.The
	 * prefix for a site-property is {@code site.}, for a platform-property it's {@value #PREFIX_PLATFORM}. For an
	 * {@link Application} property no prefix is used.
	 * 
	 * @param platFormConfig
	 *            the platform configuration, only needed if {@code addPlatformScope} is {@code true}.
	 * @param site
	 *            the {@link Site} to retrieve {@link Properties} from (may be null)
	 * @param application
	 *            the {@link Application} to retrieve {@link Properties} from (may be null)
	 * @param addPlatformScope
	 *            set to {@code true} to add the platform properties
	 * @return the aggregated {@link java.util.Properties} with prefixed entries
	 * @see Properties#getPlainProperties()
	 */
	public static java.util.Properties getProperties(Properties platFormConfig, Site site, Application application,
			boolean addPlatformScope) {
		java.util.Properties props = new java.util.Properties();
		if (null != application) {
			addProperties(props, PREFIX_EMPTY, application.getProperties().getPlainProperties());
		}
		if (null != site) {
			addProperties(props, PREFIX_SITE, site.getProperties().getPlainProperties());
		}
		if (addPlatformScope) {
			addProperties(props, PREFIX_PLATFORM, platFormConfig.getPlainProperties());
		}
		return props;
	}

	/**
	 * Returns the dot-separated full name for a given property, depending on whether a {@link Site} and/or an
	 * {@link Application} are given.
	 * 
	 * @param site
	 *            the {@link Site}, may be {@code null}
	 * @param application
	 *            the {@link Application}, may be {@code null}
	 * @param name
	 *            the raw name of the property, without dot-notation
	 * @return the full name of the property.
	 */
	public static String getPropertyName(Site site, Application application, String name) {
		return getPropertyPrefix(site, application) + name;
	}

	/**
	 * Returns the dot-separated property-prefix, depending on whether a {@link Site} and/or an {@link Application} are
	 * given.
	 * 
	 * @param site
	 *            the {@link Site}, may be {@code null}
	 * @param application
	 *            the {@link Application}, may be {@code null}
	 */
	public static String getPropertyPrefix(Site site, Application application) {
		String prefix = PREFIX_PLATFORM;
		if (null != site) {
			prefix += PREFIX_SITE + site.getName() + DOT;
		}
		if (null != application) {
			prefix += PREFIX_APPLICATION + application.getName() + DOT;
		}
		return prefix;
	}

	/**
	 * Returns the dot-separated property-prefix for a site-property.
	 * 
	 * @param site
	 *            the {@link Site}
	 * @return the dot-separated property-prefix
	 */
	public static String getSitePrefix(Site site) {
		return getPropertyPrefix(site, null);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static void addProperties(Map props, String prefix, java.util.Properties propsToAdd) {
		for (Object property : propsToAdd.keySet()) {
			props.put(prefix + property, propsToAdd.getProperty((String) property));
		}
	}

	private void addPlatformProperty(java.util.Properties defaultOverrides, String name, Object defaultValue) {
		addPlatformProperty(defaultOverrides, name, defaultValue, false);
	}

	private void addPlatformProperty(java.util.Properties defaultOverrides, String name, Object defaultValue,
			boolean multilined) {
		defaultValue = defaultOverrides.getOrDefault(PREFIX_PLATFORM + name, defaultValue);
		addProperty(name, defaultValue, PREFIX_PLATFORM, multilined);
	}

	private String addSiteProperty(String name, Object defaultValue) {
		return addSiteProperty(name, defaultValue, false);
	}

	private String addSiteProperty(String name, Object defaultValue, boolean multilined) {
		return addProperty(name, defaultValue, PREFIX_SITE, multilined);
	}

	private String addProperty(String name, Object defaultValue, String prefix, boolean multilined) {
		String description = bundle.getString(prefix + name);
		Property added = propertyHolder.addProperty(name, defaultValue, description, multilined);
		return multilined ? added.getClob() : added.getDefaultString();
	}

	/**
	 * Initializes the {@link Site} configuration with the default values. The properties are added to the
	 * {@link PropertyHolder} this {@link PropertySupport} was created with.
	 * 
	 * @param site
	 *            the {@link Site} to initialize the {@link Properties} for
	 * @param platformConfig
	 *            the platform configuration
	 * @see #PropertySupport(PropertyHolder)
	 * @see SiteProperties
	 */
	public void initSiteProperties(SiteImpl site, Properties platformConfig) {
		bundle = ResourceBundle.getBundle("org/appng/core/site-config");
		if (null != platformConfig) {
			String rootPath = platformConfig.getString(Platform.Property.PLATFORM_ROOT_PATH);
			String repositoryPath = platformConfig.getString(Platform.Property.REPOSITORY_PATH);
			String siteRootDir = rootPath + "/" + repositoryPath + "/" + site.getName();
			addSiteProperty(SiteProperties.SITE_ROOT_DIR, siteRootDir);
			String regEx = platformConfig.getString(Platform.Property.PASSWORD_POLICY_REGEX);
			String errorMessageKey = platformConfig.getString(Platform.Property.PASSWORD_POLICY_ERROR_MSSG_KEY);
			site.setPasswordPolicy(new DefaultPasswordPolicy(regEx, errorMessageKey));
		}
		addSiteProperty(SiteProperties.NAME, site.getName());
		addSiteProperty(SiteProperties.HOST, site.getHost());
		addSiteProperty(SiteProperties.WWW_DIR, "/www");
		String managerPath = addSiteProperty(SiteProperties.MANAGER_PATH, "/manager");
		addSiteProperty(SiteProperties.SERVICE_OUTPUT_FORMAT, "html");
		addSiteProperty(SiteProperties.SERVICE_OUTPUT_TYPE, "service");
		addSiteProperty(SiteProperties.SERVICE_PATH, "/service");
		addSiteProperty(SiteProperties.SUPPORTED_LANGUAGES, "en, de");
		addSiteProperty(SiteProperties.EHCACHE_ENABLED, false);
		addSiteProperty(SiteProperties.EHCACHE_EXCEPTIONS, managerPath, true);
		addSiteProperty(SiteProperties.EHCACHE_BLOCKING_TIMEOUT, 10000);
		addSiteProperty(SiteProperties.EHCACHE_STATISTICS, false);
		addSiteProperty(SiteProperties.EHCACHE_CLEAR_ON_SHUTDOWN, true);
		addSiteProperty(SiteProperties.ERROR_PAGE, "error");
		addSiteProperty(SiteProperties.ERROR_PAGES, "/de=fehler|/en=error");
		addSiteProperty(SiteProperties.INDEX_DIR, "/index");
		addSiteProperty(SiteProperties.INDEX_TIMEOUT, 5000);
		addSiteProperty(SiteProperties.INDEX_QUEUE_SIZE, 1000);
		addSiteProperty(SiteProperties.SEARCH_CHUNK_SIZE, 20);
		addSiteProperty(SiteProperties.SEARCH_MAX_HITS, 100);
		addSiteProperty(Platform.Property.MAIL_HOST, "localhost");
		addSiteProperty(Platform.Property.MAIL_PORT, 25);
		addSiteProperty(Platform.Property.MAIL_DISABLED, true);
		addSiteProperty(SiteProperties.INDEX_CONFIG, "/de;de;GermanAnalyzer|/assets;de;GermanAnalyzer");
		addSiteProperty(SiteProperties.INDEX_FILETYPES, "jsp,pdf,doc");
		addSiteProperty(SiteProperties.INDEX_FILE_SYSTEM_QUEUE_SIZE, "2500");
		addSiteProperty(SiteProperties.DEFAULT_PAGE, "index");
		addSiteProperty(SiteProperties.DEFAULT_PAGE_SIZE, "25");
		addSiteProperty(SiteProperties.APPEND_TAB_ID, "false");
		addSiteProperty(Platform.Property.ENCODING, HttpHeaders.CHARSET_UTF8);
		addSiteProperty(SiteProperties.ASSETS_DIR, "/assets");
		addSiteProperty(SiteProperties.DOCUMENT_DIR, "/de");
		addSiteProperty(SiteProperties.ENFORCE_PRIMARY_DOMAIN, false);
		addSiteProperty(SiteProperties.DEFAULT_APPLICATION, "appng-manager");
		addSiteProperty(Platform.Property.LOCALE, Locale.getDefault().getLanguage());
		addSiteProperty(Platform.Property.TIME_ZONE, TimeZone.getDefault().getID());
		addSiteProperty(SiteProperties.TEMPLATE, "appng");
		addSiteProperty(SiteProperties.DATASOURCE_CONFIGURER, HikariCPConfigurer.class.getName());
		addSiteProperty(SiteProperties.TAG_PREFIX, "appNG");
		addSiteProperty(SiteProperties.REWRITE_CONFIG, "/meta/conf/urlrewrite.xml");

		addSiteProperty(SiteProperties.AUTH_APPLICATION, "appng-authentication");
		addSiteProperty(SiteProperties.AUTH_LOGIN_PAGE, "webform");
		addSiteProperty(SiteProperties.AUTH_LOGIN_REF, "webform");
		addSiteProperty(SiteProperties.AUTH_LOGOUT_ACTION_NAME, "action");
		addSiteProperty(SiteProperties.AUTH_LOGOUT_ACTION_VALUE, "logout");
		addSiteProperty(SiteProperties.AUTH_LOGOUT_PAGE, "webform");
		addSiteProperty(SiteProperties.AUTH_LOGOUT_REF, "webform/logout");
		addSiteProperty(SiteProperties.CSRF_PROTECTION_ENABLED, "false");
		addSiteProperty(SiteProperties.CSRF_PROTECTED_METHODS, "POST,PUT");
		addSiteProperty(SiteProperties.CSRF_PROTECTED_PATHS, "/manager");

		StringBuilder xssExceptions = new StringBuilder();
		xssExceptions.append("# template" + StringUtils.LF);
		xssExceptions.append(platformConfig.getString(Platform.Property.TEMPLATE_PREFIX) + StringUtils.LF);
		xssExceptions.append("# appng-manager" + StringUtils.LF);
		xssExceptions.append(managerPath + "/" + site.getName() + "/appng-manager" + StringUtils.LF);
		addSiteProperty(SiteProperties.XSS_EXCEPTIONS, xssExceptions.toString(), true);

		addSiteProperty(LdapService.LDAP_HOST, "ldap:<host>:<port>");
		addSiteProperty(LdapService.LDAP_USER_BASE_DN, "OU=Users,DC=example,DC=com");
		addSiteProperty(LdapService.LDAP_GROUP_BASE_DN, "OU=Groups,DC=example,DC=com");
		addSiteProperty(LdapService.LDAP_USER, "serviceUser");
		addSiteProperty(LdapService.LDAP_PASSWORD, "secret");
		addSiteProperty(LdapService.LDAP_DOMAIN, "EXAMPLE");
		addSiteProperty(LdapService.LDAP_ID_ATTRIBUTE, "sAMAccountName");

		propertyHolder.setFinal();
	}

	public void initPlatformConfig(String rootPath, Boolean devMode) {
		initPlatformConfig(rootPath, devMode, new java.util.Properties(), true);
	}

	/**
	 * Initializes the platform configuration with the default values. The properties are added to the
	 * {@link PropertyHolder} this {@link PropertySupport} was created with.
	 * 
	 * @param rootPath
	 *            the root path of the platform (see {@link org.appng.api.Platform.Property#PLATFORM_ROOT_PATH})
	 * @param devMode
	 *            value for the {@link org.appng.api.Platform.Property#DEV_MODE} property to set
	 * @param finalize
	 *            whether or not to call {@link PropertyHolder#setFinal()}
	 * @see #PropertySupport(PropertyHolder)
	 * @see org.appng.api.Platform.Property
	 */
	public void initPlatformConfig(String rootPath, Boolean devMode, java.util.Properties defaultOverrides,
			boolean finalize) {
		bundle = ResourceBundle.getBundle("org/appng/core/platform-config");
		if (null != rootPath) {
			addPlatformProperty(defaultOverrides, Platform.Property.PLATFORM_ROOT_PATH, rootPath);
		}
		addPlatformProperty(defaultOverrides, Platform.Property.APPLICATION_CACHE_FOLDER, "application");
		addPlatformProperty(defaultOverrides, Platform.Property.CACHE_FOLDER, "cache");
		addPlatformProperty(defaultOverrides, Platform.Property.CSRF_FILTER_ENABLED, "false");
		addPlatformProperty(defaultOverrides, Platform.Property.DATABASE_PREFIX, StringUtils.EMPTY);
		addPlatformProperty(defaultOverrides, Platform.Property.DATABASE_VALIDATION_PERIOD, 15);
		addPlatformProperty(defaultOverrides, Platform.Property.DEFAULT_TEMPLATE, "appng");
		addPlatformProperty(defaultOverrides, Platform.Property.DEV_MODE, devMode);
		addPlatformProperty(defaultOverrides, Platform.Property.EHCACHE_CONFIG, "WEB-INF/conf/ehcache.xml");
		addPlatformProperty(defaultOverrides, Platform.Property.ENCODING, HttpHeaders.CHARSET_UTF8);
		addPlatformProperty(defaultOverrides, Platform.Property.FILEBASED_DEPLOYMENT, Boolean.TRUE);
		addPlatformProperty(defaultOverrides, Platform.Property.FORMAT_OUTPUT, false);
		addPlatformProperty(defaultOverrides, Platform.Property.IMAGE_CACHE_FOLDER, "image");
		addPlatformProperty(defaultOverrides, Platform.Property.IMAGEMAGICK_PATH, "/usr/bin");
		addPlatformProperty(defaultOverrides, Platform.Property.JSP_FILE_TYPE, "jsp");
		addPlatformProperty(defaultOverrides, Platform.Property.LOCALE, "en");
		addPlatformProperty(defaultOverrides, Platform.Property.LOGFILE, "appNG.log");
		addPlatformProperty(defaultOverrides, Platform.Property.MAIL_DISABLED, true);
		addPlatformProperty(defaultOverrides, Platform.Property.MAIL_HOST, "localhost");
		addPlatformProperty(defaultOverrides, Platform.Property.MAIL_PORT, 25);
		addPlatformProperty(defaultOverrides, Platform.Property.MANAGE_DATABASES, Boolean.TRUE);
		addPlatformProperty(defaultOverrides, Platform.Property.MAX_UPLOAD_SIZE, 30 * 1024 * 1024);
		addPlatformProperty(defaultOverrides, Platform.Property.MDC_ENABLED, Boolean.TRUE);
		addPlatformProperty(defaultOverrides, Platform.Property.MESSAGING_ENABLED, Boolean.FALSE);
		addPlatformProperty(defaultOverrides, Platform.Property.MESSAGING_GROUP_ADDRESS, "224.2.2.4");
		addPlatformProperty(defaultOverrides, Platform.Property.MESSAGING_GROUP_PORT, 4000);
		addPlatformProperty(defaultOverrides, Platform.Property.MESSAGING_RECEIVER, MulticastReceiver.class.getName());
		addPlatformProperty(defaultOverrides, Platform.Property.MONITOR_PERFORMANCE, false);
		addPlatformProperty(defaultOverrides, Platform.Property.PASSWORD_POLICY_ERROR_MSSG_KEY,
				DefaultPasswordPolicy.ERROR_MSSG_KEY);
		addPlatformProperty(defaultOverrides, Platform.Property.PASSWORD_POLICY_REGEX, DefaultPasswordPolicy.REGEX);
		addPlatformProperty(defaultOverrides, Platform.Property.PLATFORM_CACHE_FOLDER, "platform");
		addPlatformProperty(defaultOverrides, Platform.Property.APPLICATION_DIR, "/applications");
		addPlatformProperty(defaultOverrides, Platform.Property.REPOSITORY_PATH, "repository");
		addPlatformProperty(defaultOverrides, Platform.Property.REPOSITORY_DEFAULT_DIGEST, "");
		addPlatformProperty(defaultOverrides, Platform.Property.REPOSITORY_CERT, StringUtils.EMPTY, true);
		addPlatformProperty(defaultOverrides, Platform.Property.REPOSITORY_SIGNATURE, StringUtils.EMPTY, true);
		addPlatformProperty(defaultOverrides, Platform.Property.REPOSITORY_TRUSTSTORE, StringUtils.EMPTY);
		addPlatformProperty(defaultOverrides, Platform.Property.REPOSITORY_TRUST_STORE_PASSWORD, StringUtils.EMPTY);
		addPlatformProperty(defaultOverrides, Platform.Property.REPOSITORY_VERIFY_SIGNATURE, "true");
		addPlatformProperty(defaultOverrides, Platform.Property.SESSION_TIMEOUT, 1800);

		String sharedSecretFullName = PREFIX_PLATFORM + Platform.Property.SHARED_SECRET;
		Property sharedSecret = propertyHolder.getProperty(sharedSecretFullName);
		if (null == sharedSecret || defaultOverrides.containsKey(sharedSecretFullName)) {
			String defaultSecret = AuthTools.getRandomSalt(32);
			addPlatformProperty(defaultOverrides, Platform.Property.SHARED_SECRET, defaultSecret);
		}

		addPlatformProperty(defaultOverrides, Platform.Property.TEMPLATE_FOLDER, "/templates");
		addPlatformProperty(defaultOverrides, Platform.Property.TEMPLATE_PREFIX, "/template");
		addPlatformProperty(defaultOverrides, Platform.Property.TIME_ZONE, TimeZone.getDefault().getID());
		addPlatformProperty(defaultOverrides, Platform.Property.UPLOAD_DIR, "/uploads");
		addPlatformProperty(defaultOverrides, Platform.Property.VHOST_MODE, VHostMode.NAME_BASED.name());
		addPlatformProperty(defaultOverrides, Platform.Property.WRITE_DEBUG_FILES, Boolean.FALSE);
		addPlatformProperty(defaultOverrides, Platform.Property.XSS_PROTECT, Boolean.FALSE);
		addPlatformProperty(defaultOverrides, Platform.Property.XSS_ALLOWED_TAGS, "a href class style|div align style");
		if (finalize) {
			propertyHolder.setFinal();
		}
	}
}
