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

import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.appng.api.model.Application;
import org.appng.api.model.Properties;
import org.appng.api.model.Site;
import org.springframework.context.ApplicationContext;

/**
 * 
 * Utility class providing platform-wide used constants.
 * 
 * @author Matthias Müller
 * 
 */
public final class Platform {

	private Platform() {

	}

	/**
	 * constant for services of type 'rest'
	 * 
	 * @see Path#getService()
	 * @see Path#isService()
	 */

	public static final String SERVICE_TYPE_REST = "rest";
	/**
	 * constant for services of type 'soap'
	 * 
	 * @see Path#getService()
	 * @see Path#isService()
	 */
	public static final String SERVICE_TYPE_SOAP = "soap";

	/**
	 * constant for services of type 'webservice'
	 * 
	 * @see Path#getService()
	 * @see Path#isService()
	 */
	public static final String SERVICE_TYPE_WEBSERVICE = "webservice";

	/**
	 * constant for services of type 'datasource'
	 * 
	 * @see Path#getService()
	 * @see Path#isService()
	 */
	public static final String SERVICE_TYPE_DATASOURCE = "datasource";

	/**
	 * constant for services of type 'action'
	 * 
	 * @see Path#getService()
	 * @see Path#isService()
	 */
	public static final String SERVICE_TYPE_ACTION = "action";

	/**
	 * 
	 * Constants used for the global platform-configuration. Those get wrapped into a {@link Properties}-object and can
	 * be accessed via the {@link org.appng.api.Environment} as follows:
	 * 
	 * <pre>
	 * org.appng.api.model.Properties platformConfig = environment.getAttribute(Scope.PLATFORM,
	 * 		Platform.Environment.PLATFORM_CONFIG);
	 * String platformRootPath = platformConfig.getString(Platform.Property.PLATFORM_ROOT_PATH);
	 * </pre>
	 * 
	 * <b>Note: A {@link Application} can only access the {@link Scope#PLATFORM} if it's a privileged application, i.e.
	 * {@link Application#isPrivileged()} returns {@code true}.</b>
	 * 
	 * @author Matthias Müller
	 * 
	 */
	public final class Property {
		/** The absolute root-path of the platform */
		public static final String PLATFORM_ROOT_PATH = "platformRootPath";
		/**
		 * The folder for the application-cache, relative to {@link #CACHE_FOLDER}. Applications might use this folder
		 * to cache temporary data.
		 */
		public static final String APPLICATION_CACHE_FOLDER = "cacheApplicationFolder";
		/**
		 * The cache folder, relative to WEB-INF. Contains the {@link #PLATFORM_CACHE_FOLDER} and the
		 * {@link #APPLICATION_CACHE_FOLDER}.
		 */
		public static final String CACHE_FOLDER = "cacheFolder";
		/**
		 * Whether validation constraints should be added as a {@link org.appng.xml.platform.Rule} to the
		 * {@link org.appng.xml.platform.FieldDef}s {@link org.appng.xml.platform.Validation}
		 */
		public static final String CONSTRAINTS_AS_RULE = "constraintsAsRule";
		/**
		 * Set to {@code true} to enable a filter preventing CSRF-attacks
		 */
		public static final String CSRF_FILTER_ENABLED = "csrfFilterEnabled";
		/**
		 * The prefix to use when generating database names
		 */
		public static final String DATABASE_PREFIX = "databasePrefix";
		/**
		 * The idle database connection test period in minutes. If a database connection remains idle for the specified
		 * time, the validation query defined in the database connection will be sent to prevent a database connection
		 * timeout.
		 */
		public static final String DATABASE_VALIDATION_PERIOD = "databaseValidationPeriod";
		/** The name of the default template to use (must be a folder located under {@link #TEMPLATE_FOLDER}) */
		public static final String DEFAULT_TEMPLATE = "defaultTemplate";
		/**
		 * Disable for production use. If enabled, XML and XSL resources will be written to the cache directory. If
		 * disabled, XML and XSL resources will be cached in memory.
		 */
		public static final String DEV_MODE = "devMode";
		/** The charset/encoding used for http-responses. */
		public static final String ENCODING = "encoding";
		/**
		 * The global page cache configuration using the Ehcache XML configuration format. This cache is used to cache
		 * HTTP responses.
		 */
		public static final String EHCACHE_CONFIG = "ehcacheConfig";
		/** Set to 'true' if applications should be deployed to the local filesystem, 'false' otherwise. */
		public static final String FILEBASED_DEPLOYMENT = "filebasedDeployment";
		/** Disable for production use. If enabled, debugging is easier, but Textarea values are formatted wrong. */
		public static final String FORMAT_OUTPUT = "formatOutput";
		/** The folder used for caching images, within the {@link #APPLICATION_CACHE_FOLDER} */
		public static final String IMAGE_CACHE_FOLDER = "cacheImageFolder";
		/** The path to the ImageMagick executables */
		public static final String IMAGEMAGICK_PATH = "imageMagickPath";
		/** The file-extension for JSP-files. */
		public static final String JSP_FILE_TYPE = "jspFileType";
		/** The default {@link Locale}. Use one of {@link java.util.Locale#getAvailableLocales()} */
		public static final String LOCALE = "locale";
		/** The name of the logfile generated by appNG */
		public static final String LOGFILE = "logfile";
		/** Set to 'true' to disable mailing and log the e-mails instead. */
		public static final String MAIL_DISABLED = "mailDisabled";
		/** The mail-host to use */
		public static final String MAIL_HOST = "mailHost";
		/** The mail-port to use */
		public static final String MAIL_PORT = "mailPort";
		/**
		 * If set to 'true', appNG will manage the databases (create schemas and users) required by the
		 * {@link Application}s.
		 */
		public static final String MANAGE_DATABASES = "manageDatabases";
		/** set to {@code true} to enable support for Mapped Diagnostic Context (MDC) Logging. */
		public static final String MDC_ENABLED = "mdcEnabled";
		/** Set to true to enable cluster messaging */
		public static final String MESSAGING_ENABLED = "messagingEnabled";
		/** Class name of the desired messaging Receiver implementation. Default is multicast **/
		public static final String MESSAGING_RECEIVER = "messagingReceiver";
		/** The multicast address used for messaging */
		public static final String MESSAGING_GROUP_ADDRESS = "messagingGroupAddress";
		/** The port used for multicast messaging */
		public static final String MESSAGING_GROUP_PORT = "messagingGroupPort";
		/** The maximum size for file uploads in bytes */
		public static final String MAX_UPLOAD_SIZE = "maxUploadSize";
		/** Set to true to enable performance monitoring for the target XML */
		public static final String MONITOR_PERFORMANCE = "monitorPerformance";
		/**
		 * The resource-bundle key (for messages-core) for the message which is being displayed when the password does
		 * not match the policy.
		 */
		public static final String PASSWORD_POLICY_ERROR_MSSG_KEY = "passwordPolicyErrorMessageKey";
		/** A regular expression describing the password-policy */
		public static final String PASSWORD_POLICY_REGEX = "passwordPolicyRegEx";
		/**
		 * The folder for the platform-cache, relative to {@link #CACHE_FOLDER}. The platform cache is used by appNG to
		 * cache application resources.
		 */
		public static final String PLATFORM_CACHE_FOLDER = "cachePlatformFolder";
		/** The folder used for installing file-based-applications, relative to the webapp-root */
		public static final String APPLICATION_DIR = "applicationDir";
		/** The folder used for the repositories of the site, relative to the webapp-root */
		public static final String REPOSITORY_PATH = "repositoryPath";
		/** The timeout for a user session in seconds */
		public static final String SESSION_TIMEOUT = "sessionTimeout";
		/** The shared secret used for digest authentication */
		public static final String SHARED_SECRET = "sharedSecret";
		/** The folder used for templates, relative to the webapp-root */
		public static final String TEMPLATE_FOLDER = "templateFolder";
		/** The path under which the resources of the active template are beeing served. */
		public static final String TEMPLATE_PREFIX = "templatePrefix";
		/** The default {@link TimeZone}. Use one of {@link java.util.TimeZone#getAvailableIDs()} */
		public static final String TIME_ZONE = "timeZone";
		/** The folder for saving uploads, relative to the webapp-root */
		public static final String UPLOAD_DIR = "uploadDir";
		/** Defines whether the server is identified by its IP ('IP_BASED') or by its name ('NAME_BASED') */
		public static final String VHOST_MODE = "vHostMode";
		/**
		 * When set to {@code true}, the XML, XSLT and potential Exceptions occurring on a request to the appNG manager
		 * GUI are written to {@code <platformRootPath>/debug}
		 */
		public static final String WRITE_DEBUG_FILES = "writeDebugFiles";
		/** The default digest for a published local application repository */
		public static final String REPOSITORY_DEFAULT_DIGEST = "repositoryDefaultDigest";
		/** The certificate to use when verifying a signed remote repository (PEM format) */
		public static final String REPOSITORY_CERT = "repositoryCert";
		/** The private key to use when signing a local published repository (PEM format) */
		public static final String REPOSITORY_SIGNATURE = "repositorySignature";
		/**
		 * The truststore used when verifying a signed remote repository, using {@code file://}-protocol. If empty, the
		 * default {@code $java.home/lib/security/cacerts} is being used.
		 */
		public static final String REPOSITORY_TRUSTSTORE = "repositoryTrustStore";
		/** The truststore's password */
		public static final String REPOSITORY_TRUST_STORE_PASSWORD = "repositoryTrustStorePassword";
		/**
		 * When set to {@code true}, signed remote repositories are validated against the configured (or default)
		 * truststore.
		 */
		public static final String REPOSITORY_VERIFY_SIGNATURE = "repositoryVerifySignature";

		/** Set to {@code true} to enable XSS protection */
		public static final String XSS_PROTECT = "xssProtect";
		/**
		 * A list of allowed HTML Tags, separated by '|', optionally followed by a space-separated list of allowed
		 * attributes.<br/>
		 * Example:
		 * 
		 * <pre>
		 * h1|h2|a href class style|div align style
		 * </pre>
		 */
		public static final String XSS_ALLOWED_TAGS = "xssAllowedTags";

		/**
		 * The time to wait for a {@link Site} to become available/to finish its requests (single run).
		 */
		public static final String WAIT_TIME = "waitTime";

		/**
		 * The overall maximum time to wait for a {@link Site} to become available/to finish its requests.
		 */
		public static final String MAX_WAIT_TIME = "maxWaitTime";
	}

	/**
	 * 
	 * Constants used for accessing an {@link Environment}-attribute with {@link Scope#PLATFORM}.
	 * 
	 * @author Matthias Müller
	 * 
	 */
	public final class Environment {

		/**
		 * Key for the global {@link ApplicationContext}. The return type of
		 * {@link org.appng.api.Environment#getAttribute(Scope, String)} is {@link ApplicationContext}.
		 */
		public static final String CORE_PLATFORM_CONTEXT = "corePlatformContext";

		/**
		 * Key for the global platform config. The return type of
		 * {@link org.appng.api.Environment#getAttribute(Scope, String)} is {@link Properties}.
		 */
		public static final String PLATFORM_CONFIG = "platformConfig";

		/**
		 * Key for the appNG version. The return type of {@link org.appng.api.Environment#getAttribute(Scope, String)}
		 * is {@link String} .
		 */
		public static final String APPNG_VERSION = "appNGVersion";

		/**
		 * Key for the {@link Map} containing all active {@link Site}s. The return type of
		 * {@link org.appng.api.Environment#getAttribute(Scope, String)} is {@code Map
		 * <String, Site>}
		 */
		public static final String SITES = "sites";

		/**
		 * Key for the {@link org.appng.api.messaging.Sender} that the platform uses to send
		 * {@link org.appng.api.messaging.Event}s.
		 */
		public static final String MESSAGE_SENDER = "messageSender";

		/**
		 * Key for the {@link org.appng.api.messaging.Receiver}s that the platform uses to receive
		 * {@link org.appng.api.messaging.Event}s.
		 */
		public static final String MESSAGE_RECEIVER = "messageReceiver";

	}

}
