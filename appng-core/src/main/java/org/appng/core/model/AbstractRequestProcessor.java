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

import static org.appng.api.Scope.REQUEST;

import java.io.File;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.Environment;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.Path;
import org.appng.api.PathInfo;
import org.appng.api.PermissionOwner;
import org.appng.api.PermissionProcessor;
import org.appng.api.Scope;
import org.appng.api.Session;
import org.appng.api.SiteProperties;
import org.appng.api.model.Application;
import org.appng.api.model.Properties;
import org.appng.api.model.Site;
import org.appng.api.support.ApplicationRequest;
import org.appng.api.support.DollarParameterSupport;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.api.support.environment.EnvironmentKeys;
import org.appng.core.domain.SiteImpl;
import org.appng.core.service.TemplateService;
import org.appng.xml.MarshallService;
import org.appng.xml.platform.ApplicationConfig;
import org.appng.xml.platform.ApplicationReference;
import org.appng.xml.platform.Authentication;
import org.appng.xml.platform.Authentications;
import org.appng.xml.platform.Content;
import org.appng.xml.platform.Localization;
import org.appng.xml.platform.Output;
import org.appng.xml.platform.OutputFormat;
import org.appng.xml.platform.OutputType;
import org.appng.xml.platform.Platform;
import org.appng.xml.platform.PlatformConfig;
import org.appng.xml.platform.SessionInfo;
import org.appng.xml.platform.Subject;
import org.appng.xml.platform.Template;
import org.slf4j.Logger;

public abstract class AbstractRequestProcessor implements RequestProcessor {

	protected PathInfo pathInfo;
	protected HttpServletRequest servletRequest;
	protected HttpServletResponse servletResponse;
	protected MarshallService marshallService;
	protected DefaultEnvironment env;
	protected boolean redirect;
	protected String contentType;
	protected int contentLength;
	protected NavigationBuilder navigationBuilder;
	protected String templatePath;
	protected OutputFormat outputFormat;
	protected OutputType outputType;
	private static final String NAV_INDEX = "index";
	private static final String SLASH = "/";

	public static void initPlatform(org.appng.xml.platform.Platform platform, Environment env, Path path) {
		PlatformConfig config = platform.getConfig();
		SessionInfo sessionInfo = new SessionInfo();
		sessionInfo.setId(env.getAttributeAsString(Scope.SESSION, Session.Environment.SID));
		sessionInfo.setTimeout((Integer) env.getAttribute(Scope.SESSION, Session.Environment.TIMEOUT));
		sessionInfo.setStarttime((Long) env.getAttribute(Scope.SESSION, Session.Environment.STARTTIME));
		config.setSession(sessionInfo);
		config.setPlatformUrl(path.getPlatformUrl());
		config.setBaseUrl(path.getRootPath());
		config.setCurrentUrl(path.getCurrentPath());

		org.appng.api.model.Subject currentSubject = env.getSubject();
		Subject subject = new Subject();
		boolean isLoggedIn = null != currentSubject && currentSubject.isAuthenticated();
		if (isLoggedIn) {
			subject.setUsername(currentSubject.getRealname());
			subject.setName(currentSubject.getName());
		}
		localize(subject, env);
		platform.setSubject(subject);
	}

	private static void localize(Subject subject, Environment env) {
		Localization localization = new Localization();
		localization.setLanguage(env.getLocale().toString());
		DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols(env.getLocale());
		localization.setGroupingSeparator(String.valueOf(decimalFormatSymbols.getGroupingSeparator()));
		localization.setDecimalSeparator(String.valueOf(decimalFormatSymbols.getDecimalSeparator()));
		subject.setLocalization(localization);
	}

	public org.appng.xml.platform.Platform processPlatform(Site applicationSite) throws InvalidConfigurationException {
		org.appng.xml.platform.Platform platform = getPlatform(marshallService, pathInfo);

		initPlatform(platform, env, pathInfo);

		org.appng.api.model.Subject currentSubject = env.getSubject();
		navigationBuilder = new NavigationBuilder(pathInfo, env);

		boolean isLoggedIn = null != currentSubject && currentSubject.isAuthenticated();
		// APPNG-601
		PlatformConfig config = platform.getConfig();
		Authentications authentications = config.getAuthentications();
		if (null == authentications) {
			authentications = new Authentications();
			config.setAuthentications(authentications);
		}

		Properties siteProperties = applicationSite.getProperties();
		Map<?, ?> plainSiteProperties = siteProperties.getPlainProperties();
		Map<String, String> sitePropertyMap = new HashMap<>();
		for (Object object : plainSiteProperties.keySet()) {
			sitePropertyMap.put("site." + object.toString(), plainSiteProperties.get(object).toString());
		}
		DollarParameterSupport parameterSupport = new DollarParameterSupport(sitePropertyMap);
		parameterSupport.allowDotInName();
		Authentication authentication = determineActiveAuthentication(applicationSite, authentications,
				parameterSupport);

		if (isLoggedIn) {
			String siteName = applicationSite.getName();
			String sitePath = config.getBaseUrl() + SLASH + siteName + SLASH;
			String loginPage = sitePath + authentication.getApplication() + SLASH + authentication.getPage();
			String currentUrl = config.getCurrentUrl();
			if (currentUrl.startsWith(loginPage)) {
				String defaultApplication = siteProperties.getString(SiteProperties.DEFAULT_APPLICATION);
				String path = sitePath + defaultApplication;
				applicationSite.sendRedirect(env, path);
				setRedirect(true);
				return null;
			}
		} else {
			processAuthentication(authentication);
			navigationBuilder.selectNavigationItem(authentication);
		}

		navigationBuilder.processNavigation(platform.getNavigation(), parameterSupport);

		ApplicationReference applicationReference = processApplication(applicationSite, config);
		Content content = new Content();
		content.setApplication(applicationReference);
		platform.setContent(content);

		return platform;
	}

	protected Authentication determineActiveAuthentication(Site site, Authentications authentications,
			DollarParameterSupport parameterSupport) {
		String applicationName = pathInfo.getApplicationName();
		String page = pathInfo.getPage();
		Authentication defaultAuthentication = null;
		Properties siteProperties = site.getProperties();
		List<Authentication> authenticationList = authentications.getAuthentication();
		authenticationList.clear();
		String authApplication = siteProperties.getString(SiteProperties.AUTH_APPLICATION);
		List<String> loginPages = siteProperties.getList(SiteProperties.AUTH_LOGIN_PAGE, ",");
		List<String> loginReferences = siteProperties.getList(SiteProperties.AUTH_LOGIN_REF, ",");
		for (int i = 0; i < loginPages.size(); i++) {
			String loginPage = loginPages.get(i);
			String loginRef = loginReferences.get(i);
			Authentication authentication = new Authentication();
			authentication.setApplication(authApplication);
			authentication.setIndex(NAV_INDEX);
			authentication.setPage(loginPage);
			authentication.setRef(loginRef);
			authentication.setSite(site.getName());
			authenticationList.add(authentication);
			if (StringUtils.equals(applicationName, authApplication) && StringUtils.equals(page, loginPage)) {
				defaultAuthentication = authentication;
			}
		}

		if (null == defaultAuthentication) {
			for (Authentication authentication : authenticationList) {
				if (Boolean.TRUE.equals(authentication.isSelected())) {
					defaultAuthentication = authentication;
				}
			}
			if (null == defaultAuthentication) {
				defaultAuthentication = authenticationList.get(0);
			}
		}
		return defaultAuthentication;
	}

	private void processAuthentication(Authentication authentication) {
		String defaultPath = null;
		String executePath = "";
		String currentNavPath = pathInfo.getRootPath() + SLASH + authentication.getRef();

		if (pathInfo.getServletPath().startsWith(currentNavPath)) {
			executePath = currentNavPath;
		}

		if (NAV_INDEX.equals(authentication.getIndex())) {
			defaultPath = currentNavPath;
		}

		env.setAttribute(REQUEST, EnvironmentKeys.EXECUTE_PATH, executePath);
		env.setAttribute(REQUEST, EnvironmentKeys.DEFAULT_PATH, defaultPath);

	}

	public void init(HttpServletRequest servletRequest, HttpServletResponse servletResponse, PathInfo pathInfo,
			String templateDir) {
		this.pathInfo = pathInfo;
		this.servletRequest = servletRequest;
		this.servletResponse = servletResponse;
		this.templatePath = templateDir;
		this.env = DefaultEnvironment.get(servletRequest, servletResponse);
	}

	public boolean isRedirect() {
		return redirect;
	}

	protected void setRedirect(boolean redirect) {
		this.redirect = redirect;
	}

	private ApplicationReference processApplication(Site applicationSite, PlatformConfig platformConfig)
			throws InvalidConfigurationException {

		ApplicationProvider applicationProvider = getApplicationProvider(applicationSite);
		ApplicationRequest applicationRequest = applicationProvider.getApplicationRequest(servletRequest,
				servletResponse);

		boolean hasOutputFormat = false;
		boolean hasOutputType = false;
		PermissionProcessor permissionProcessor = applicationRequest.getPermissionProcessor();
		hasOutputFormat = permissionProcessor.hasPermissions(new PermissionOwner(outputFormat));
		org.appng.api.model.Subject currentSubject = env.getSubject();
		if (null != currentSubject && currentSubject.isAuthenticated()) {
			hasOutputType = permissionProcessor.hasPermissions(new PermissionOwner(outputType));
		} else {
			hasOutputFormat = true;
			hasOutputType = true;
		}

		if (hasOutputFormat && hasOutputType) {
			applicationProvider.setPlatformScope();
			long start = System.currentTimeMillis();
			ApplicationReference applicationReference = applicationProvider.process(applicationRequest, marshallService,
					pathInfo, platformConfig);
			long end = System.currentTimeMillis() - start;
			logger().debug("succesfully called application \"" + pathInfo.getApplicationName() + "\" in site \""
					+ applicationSite.getName() + "\" in " + end + " ms");
			ApplicationConfig config = applicationReference.getConfig();
			if (null != config) {
				addTemplates(config.getTemplates());
			}
			setRedirect(applicationRequest.isRedirect());
			return applicationReference;
		} else {
			logger().info("no application called, missing outputformat '" + outputFormat.getId()
					+ "' and/or outputType '" + outputType.getId() + "'");
			// forward to first non-hidden application the user has access to
			for (Application p : applicationSite.getApplications()) {
				if (env.getSubject().hasApplication(p) && !p.isHidden()) {
					String path = applicationSite.getProperties().getString(SiteProperties.MANAGER_PATH) + SLASH
							+ applicationSite.getName() + SLASH + p.getName();
					applicationSite.sendRedirect(env, path);
					setRedirect(true);
					break;
				}
			}
		}
		return null;

	}

	protected abstract void addTemplates(List<Template> templates);

	abstract Logger logger();

	protected ApplicationProvider getApplicationProvider(Site site) throws InvalidConfigurationException {
		ApplicationProvider applicationProvider = (ApplicationProvider) ((SiteImpl) site)
				.getSiteApplication(pathInfo.getApplicationName());
		if (null == applicationProvider) {
			throw new InvalidConfigurationException(site, pathInfo.getApplicationName(),
					"application '" + pathInfo.getApplicationName() + "' not found for site '" + site.getName() + "'");
		}
		return applicationProvider;
	}

	public Integer getContentLength() {
		return contentLength;
	}

	public String getContentType() {
		return contentType;
	}

	public MarshallService getMarshallService() {
		return marshallService;
	}

	public void setMarshallService(MarshallService marshallService) {
		this.marshallService = marshallService;
	}

	protected void determineFormatAndType(PlatformConfig config, Path pathInfo) {
		if (config.getOutputFormat().isEmpty()) {
			throw new IllegalArgumentException("no output-formats defined in master.xml");
		}

		String formatFromPath = pathInfo.getOutputFormat();
		String typeFromPath = pathInfo.getOutputType();
		OutputFormat defaultFormat = null;
		for (OutputFormat outputFormat : config.getOutputFormat()) {
			if (Boolean.TRUE.equals(outputFormat.isDefault())) {
				defaultFormat = outputFormat;
			}
			if (outputFormat.getId().equals(formatFromPath)) {
				this.outputFormat = outputFormat;
				env.setAttribute(REQUEST, EnvironmentKeys.EXPLICIT_FORMAT, true);
				for (OutputType outputType : outputFormat.getOutputType()) {
					if (outputType.getId().equals(typeFromPath)) {
						this.outputType = outputType;
					}
				}
			}
		}
		if (null == outputFormat) {
			outputFormat = defaultFormat;
			if (null == outputFormat) {
				outputFormat = config.getOutputFormat().get(0);
				logger().debug("no default output-format set, using first ({})", outputFormat.getId());
				if (outputFormat.getOutputType().isEmpty()) {
					throw new IllegalArgumentException(
							"no output-types defined for output-format " + outputFormat.getId());
				}
			}
		}
		if (null == outputType) {
			if (outputFormat.getOutputType().isEmpty()) {
				if (null != defaultFormat && !defaultFormat.equals(outputFormat)) {
					logger().debug("no types defined for format {}, switching to default format {}",
							outputFormat.getId(), defaultFormat.getId());
					outputFormat = defaultFormat;
				}
			}
			for (OutputType outputType : outputFormat.getOutputType()) {
				if (Boolean.TRUE.equals(outputType.isDefault())) {
					this.outputType = outputType;
					break;
				}
			}
			if (null == outputType) {
				if (outputFormat.getOutputType().isEmpty()) {
					throw new IllegalArgumentException(
							"no output-formats defined for selected format " + outputFormat.getId());
				}
				outputType = outputFormat.getOutputType().get(0);
				logger().debug("no default output-type set, using first ({})", outputType.getId());
			}
		}

		logger().debug("using format: {}, type: {}", outputFormat.getId(), outputType.getId());
		Output output = new Output();
		output.setFormat(outputFormat.getId());
		output.setType(outputType.getId());
		config.setOutput(output);
	}

	/**
	 * Returns the {@link Platform}-object unmarshalled from the template's
	 * {@value org.appng.core.service.TemplateService#PLATFORM_XML}-file. Also determines the {@link OutputType} and
	 * {@link OutputFormat} for the upcoming transformation.
	 * 
	 * @param marshallService
	 *            the {@link MarshallService} to use for unmarshalling
	 * @param path
	 *            the current {@link Path}-object
	 * @return the {@link Platform}-object
	 * @throws InvalidConfigurationException
	 *             if the {@value org.appng.core.service.TemplateService#PLATFORM_XML}-file could net be found or
	 *             unmarshalled.
	 * @see #getOutputFormat()
	 * @see #getOutputType()
	 */
	public Platform getPlatform(MarshallService marshallService, Path path) throws InvalidConfigurationException {
		File platformXML = new File(templatePath, TemplateService.PLATFORM_XML);
		try {
			Platform platform = marshallService.unmarshall(platformXML, Platform.class);
			determineFormatAndType(platform.getConfig(), path);
			return platform;
		} catch (Exception e) {
			throw new InvalidConfigurationException(path.getApplicationName(), "error while reading " + platformXML, e);
		}
	}

	public OutputFormat getOutputFormat() {
		return outputFormat;
	}

	public OutputType getOutputType() {
		return outputType;
	}

	public String getTemplatePath() {
		return templatePath;
	}

	public void setTemplatePath(String templatePath) {
		this.templatePath = templatePath;
	}

}
