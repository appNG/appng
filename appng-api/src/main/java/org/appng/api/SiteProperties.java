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
package org.appng.api;

import org.appng.api.model.Application;
import org.appng.api.model.Properties;
import org.appng.api.model.Site;

/**
 * 
 * Utility-class providing constants for the names of a {@link Site}'s {@link Properties}.
 * 
 * @author Matthias Müller
 * 
 * @see Site#getProperties()
 */
public class SiteProperties {

	/**
	 * If set to true, the name of the currently selected tab is being appended to the URL as a get-parameter. Addresses
	 * the issue that IE loses the anchor on a redirect.
	 */
	public static final String APPEND_TAB_ID = "appendTabId";
	/**
	 * A semicolon-separated list of folder-names (relative to {@link #WWW_DIR}) containing static resources such as
	 * images or pdfs
	 */
	public static final String ASSETS_DIR = "assetsDir";
	/** The name of the {@link Application} which is responsible for the authentication */
	public static final String AUTH_APPLICATION = "authApplication";
	/**
	 * The names of the login-pages (comma-separated) within the application defined in {@link #AUTH_APPLICATION}. The
	 * number of comma-separated pages must be the same as in {@link #AUTH_LOGIN_REF}, because {@value #AUTH_LOGIN_REF}
	 * [n] refers to {@value #AUTH_LOGIN_PAGE}[n]!
	 */
	public static final String AUTH_LOGIN_PAGE = "authLoginPage";
	/**
	 * The action names (comma-separated) for the pages defined in {@link #AUTH_LOGIN_PAGE}. The number of
	 * comma-separated names must be the same as in {@link #AUTH_LOGIN_PAGE}, because {@value #AUTH_LOGIN_REF} [n]
	 * refers to {@value #AUTH_LOGIN_PAGE}[n]!
	 */
	public static final String AUTH_LOGIN_REF = "authLoginRef";
	/** The name for the parameter defining the action on the logout-page */
	public static final String AUTH_LOGOUT_ACTION_NAME = "authLogoutActionName";
	/** The value for the parameter defining the action on the logout-page */
	public static final String AUTH_LOGOUT_ACTION_VALUE = "authLogoutActionValue";
	/** The name of the logout-page within the application defined in {@link #AUTH_APPLICATION} */
	public static final String AUTH_LOGOUT_PAGE = "authLogoutPage";
	/** The reference-path for the logout-action */
	public static final String AUTH_LOGOUT_REF = "authLogoutRef";
	/** Should the session be renewed after a successful login? */
	public static final String RENEW_SESSION_AFTER_LOGIN = "renewSessionAfterLogin";
	/** Set to {@code true} to enable CSRF-protection for this site */
	public static final String CSRF_PROTECTION_ENABLED = "csrfProtectionEnabled";
	/** a comma-separated list of HTTP-methods to enable CSRF protection for */
	public static final String CSRF_PROTECTED_METHODS = "csrfProtectedMethods";
	/** a comma-separated list of path-prefixes to enable CSRF protection for */
	public static final String CSRF_PROTECTED_PATHS = "csrfProtectedPaths";
	/**
	 * The fully qualified name of a class implementing {@code org.appng.core.repository.config.DatasourceConfigurer},
	 * which is responsible for JDBC connection-pooling. Supported are
	 * {@code org.appng.core.repository.config.HikariCPConfigurer} and
	 * {@code org.appng.core.repository.config.TomcatJdbcConfigurer}
	 */
	public static final String DATASOURCE_CONFIGURER = "DatasourceConfigurer";
	/**
	 * The name of the default-page (without extension) relative to one of the directories defined in
	 * {@link #DOCUMENT_DIR}
	 */
	public static final String DEFAULT_PAGE = "defaultPage";
	/** The default page size (items per page) */
	public static final String DEFAULT_PAGE_SIZE = "defaultPageSize";
	/** The {@link Application} to be called after a successful login */
	public static final String DEFAULT_APPLICATION = "defaultApplication";
	/**
	 * A semicolon-separated list of folder-names (relative to {@link #WWW_DIR}) containing JSP-files and also static
	 * resources like CSS or JavaScript files
	 */
	public static final String DOCUMENT_DIR = "documentDir";
	/** Set to true to enable Ehcache for this site */
	public static final String EHCACHE_ENABLED = "ehcacheEnabled";
	/** URL path prefixes which are never cached. Contains one prefix per line (CLOB value). */
	public static final String EHCACHE_EXCEPTIONS = "ehcacheExceptions";
	/**
	 * The time, in milliseconds, to wait for the filter before a
	 * {@code net.sf.ehcache.constructs.blocking.LockTimeoutException} is thrown
	 */
	public static final String EHCACHE_BLOCKING_TIMEOUT = "ehcacheBlockingTimeout";
	/** Set to true to enable Ehcache statistics */
	public static final String EHCACHE_STATISTICS = "ehcacheStatistics";
	/**
	 * The suffix to be removed from a <rule><from> element when parsing the rules from urlrewrite.xml for the
	 * repository watchers
	 */
	public static final String EHCACHE_WATCHER_RULE_SOURCE_SUFFIX = "ehcacheWatcherRuleSourceSuffix";
	/** Whether or not to watch the repository folder for changes and invalidate cache elements, if necessary */
	public static final String EHCACHE_WATCH_REPOSITORY = "ehcacheWatchRepository";
	/** Whether or not the Ehcache is cleared on a site shutdown/reload */
	public static final String EHCACHE_CLEAR_ON_SHUTDOWN = "ehcacheClearOnShutdown";
	/** Set to true to enforce the protocol used by the site (http or https) */
	public static final String ENFORCE_PRIMARY_DOMAIN = "enforcePrimaryDomain";
	/** The name of the default error-page (without extension) relative to {@link #WWW_DIR} */
	public static final String ERROR_PAGE = "errorPage";
	/**
	 * The name of the error-page per document-directory (see {@link #DOCUMENT_DIR}), multiple entries separated by a
	 * pipe (|)
	 */
	public static final String ERROR_PAGES = "errorPages";
	/** */
	public static final String FEATURE_IMAGE_PROCESSING = "imageProcessing";
	/** */
	public static final String FEATURE_INDEXING = "indexing";
	/** The host of the site. For convenience only, do not change! */
	public static final String HOST = "host";
	/**
	 * For each directory defined in {@link #DOCUMENT_DIR}, there can be defined which locale and which Lucene-analyzer
	 * to use for indexing.
	 */
	public static final String INDEX_CONFIG = "indexConfig";
	/** The folder containing the Lucene-Index, relative to {@link #WWW_DIR} */
	public static final String INDEX_DIR = "indexDir";
	/** Set to {@code true} to enable JDBC Performance Logger */
	public static final String LOG_JDBC_PERFORMANCE="logJdbcPerformance";
	/** The timeout in milliseconds for indexing */
	public static final String INDEX_TIMEOUT = "indexTimeout";
	/** the queue size used per directory when indexing the file system **/
	public static final String INDEX_FILE_SYSTEM_QUEUE_SIZE = "indexFileSystemQueueSize";
	/** A list of comma-separated file-extensions (without leading dot) which are being indexed */
	public static final String INDEX_FILETYPES = "indexFileTypes";
	/** The queue size used for document indexing */
	public static final String INDEX_QUEUE_SIZE = "indexQueueSize";
	/** The name of the site. For convenience only, do not change! */
	public static final String NAME = "name";
	/**
	 * the location of the rewrite rules for <a href="http://tuckey.org/urlrewrite/">UrlRewriteFilter</a>, relative to
	 * {@link #SITE_ROOT_DIR}
	 */
	public static final String REWRITE_CONFIG = "rewriteConfig";
	/** A comma-separated list of the languages supported by the {@link Site}. */
	public static final String SUPPORTED_LANGUAGES = "supportedLanguages";
	/** The chunksize (items per page) for the search-tag */
	public static final String SEARCH_CHUNK_SIZE = "searchChunkSize";
	/** The maximum number of hits for the search-tag */
	public static final String SEARCH_MAX_HITS = "searchMaxHits";
	/** The output format to be used when actions/datasources are being called through service URLs */
	public static final String SERVICE_OUTPUT_FORMAT = "serviceOutputFormat";
	/** The output type to be used when actions/datasources are being called through service URLs */
	public static final String SERVICE_OUTPUT_TYPE = "serviceOutputType";
	/**
	 * The path-suffix for the services offered by appNG (such as {@link Webservice}s, {@link SoapService}s,
	 * {@link ActionProvider}s and {@link DataProvider}s)
	 */
	public static final String SERVICE_PATH = "service-path";
	/** The absolute path to the sites root-directory */
	public static final String SITE_ROOT_DIR = "siteRootDir";
	/** If {@code true}, a site reload is performed when a file named {@code .reload} is created in the site's root directory */
	public static final String SUPPORT_RELOAD_FILE = "supportReloadFile";
	/** The prefix used for the appNG JSP-tags. */
	public static final String TAG_PREFIX = "tagPrefix";
	/** The name of the template to use */
	public static final String TEMPLATE = "template";
	/** The path-suffix for the appNG-Webapplication */
	public static final String MANAGER_PATH = "manager-path";
	/**
	 * The name of the folder containing the web-contents, relative to {@link Platform.Property#REPOSITORY_PATH}
	 * configured in the appNG base configuration
	 */
	public static final String WWW_DIR = "wwwDir";

	/**
	 * URL path prefixes where XSS protection is omitted. Contains one prefix per line (CLOB value). Supports blank
	 * lines and comments (#).
	 */
	public static final String XSS_EXCEPTIONS = "xssExceptions";

	private SiteProperties() {

	}

}
