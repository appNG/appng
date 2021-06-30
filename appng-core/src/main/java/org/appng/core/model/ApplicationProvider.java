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
package org.appng.core.model;

import static org.appng.api.Scope.PLATFORM;
import static org.appng.api.Scope.SESSION;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.appng.api.ApplicationConfigProvider;
import org.appng.api.Environment;
import org.appng.api.FieldProcessor;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.MessageParam;
import org.appng.api.ParameterSupport;
import org.appng.api.Path;
import org.appng.api.PermissionOwner;
import org.appng.api.PermissionProcessor;
import org.appng.api.ProcessingException;
import org.appng.api.Scope;
import org.appng.api.model.Application;
import org.appng.api.model.ApplicationSubject;
import org.appng.api.model.FeatureProvider;
import org.appng.api.model.Permission;
import org.appng.api.model.Properties;
import org.appng.api.model.Resource;
import org.appng.api.model.ResourceType;
import org.appng.api.model.Resources;
import org.appng.api.model.Role;
import org.appng.api.model.Site;
import org.appng.api.model.Subject;
import org.appng.api.support.ApplicationRequest;
import org.appng.api.support.CallableAction;
import org.appng.api.support.CallableDataSource;
import org.appng.api.support.DefaultPermissionProcessor;
import org.appng.api.support.DummyPermissionProcessor;
import org.appng.api.support.ElementHelper;
import org.appng.api.support.RequestFactoryBean;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.core.controller.filter.CsrfSetupFilter;
import org.appng.core.domain.DatabaseConnection;
import org.appng.core.domain.SiteApplication;
import org.appng.core.model.JarInfo.JarInfoBuilder;
import org.appng.xml.MarshallService;
import org.appng.xml.platform.Action;
import org.appng.xml.platform.ActionRef;
import org.appng.xml.platform.ApplicationConfig;
import org.appng.xml.platform.ApplicationReference;
import org.appng.xml.platform.ApplicationRootConfig;
import org.appng.xml.platform.Config;
import org.appng.xml.platform.Content;
import org.appng.xml.platform.DataConfig;
import org.appng.xml.platform.Datasource;
import org.appng.xml.platform.DatasourceRef;
import org.appng.xml.platform.Label;
import org.appng.xml.platform.Message;
import org.appng.xml.platform.MessageType;
import org.appng.xml.platform.Messages;
import org.appng.xml.platform.PageConfig;
import org.appng.xml.platform.PageDefinition;
import org.appng.xml.platform.PageReference;
import org.appng.xml.platform.PagesReference;
import org.appng.xml.platform.Param;
import org.appng.xml.platform.Params;
import org.appng.xml.platform.Permissions;
import org.appng.xml.platform.Platform;
import org.appng.xml.platform.PlatformConfig;
import org.appng.xml.platform.Section;
import org.appng.xml.platform.SectionDef;
import org.appng.xml.platform.Sectionelement;
import org.appng.xml.platform.SectionelementDef;
import org.appng.xml.platform.Session;
import org.appng.xml.platform.Structure;
import org.appng.xml.platform.Template;
import org.appng.xml.platform.UrlParams;
import org.appng.xml.platform.UrlSchema;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.util.StopWatch;

import lombok.extern.slf4j.Slf4j;

/**
 * An {@link ApplicationProvider} actually processes the {@link ApplicationRequest} by building and executing
 * {@link CallableAction}s and {@link CallableDataSource}s.
 * 
 * @see #process(ApplicationRequest, MarshallService, Path, PlatformConfig)
 * @see CallableAction#perform()
 * @see CallableDataSource#perform(String, boolean, boolean)
 * 
 * @author Matthias Müller
 */
@Slf4j
public class ApplicationProvider extends SiteApplication implements AccessibleApplication {

	private Site site;
	private AccessibleApplication application;
	private ApplicationConfigProvider applicationConfig;
	private List<JarInfo> jarInfos = new ArrayList<>();
	private ElementHelper elementHelper;

	private DatabaseConnection databaseConnection;

	private ApplicationRequest applicationRequest;

	private boolean monitorPerformance;

	public ApplicationProvider(Site site, Application application, boolean monitorPerformance) {
		this.application = (AccessibleApplication) application;
		this.site = site;
		this.elementHelper = new ElementHelper(site, application);
		this.monitorPerformance = monitorPerformance;
	}

	public ApplicationProvider(Site site, Application application) {
		this(site, application, false);
	}

	private String getPrefix() {
		return " [" + site.getName() + ":" + application.getName() + "] ";
	}

	/**
	 * Processes the {@link ApplicationRequest} by building and executing {@link CallableAction}s and
	 * {@link CallableDataSource}s.
	 * 
	 * @param applicationRequest
	 *                           the {@link ApplicationRequest} to process
	 * @param marshallService
	 *                           a {@link MarshallService}
	 * @param pathInfo
	 *                           the current {@link Path}
	 * @param platformConfig
	 *                           the current {@link PlatformConfig}
	 * 
	 * @return the {@link ApplicationReference} to be used for the {@link Content} of the {@link Platform}
	 */
	public ApplicationReference process(ApplicationRequest applicationRequest, MarshallService marshallService,
			Path pathInfo, PlatformConfig platformConfig) {
		PermissionProcessor permissionProcessor = applicationRequest.getPermissionProcessor();

		ApplicationConfigProvider applicationConfigProvider = null;
		try {
			applicationConfigProvider = applicationConfig.cloneConfig(marshallService);
			applicationRequest.setApplicationConfig(applicationConfigProvider);
		} catch (InvalidConfigurationException e) {
			error("error while (re)loading configuration!", e);
		}

		List<String> applicationUrlParameters = pathInfo.getApplicationUrlParameters();
		trace("Processing application \"" + pathInfo.getApplicationName() + "\" with url-parameters \""
				+ applicationUrlParameters + "\"");

		ApplicationReference applicationReference = new ApplicationReference();
		applicationReference.setVersion(getPackageVersion());
		ApplicationRootConfig applicationRootConfig = applicationConfigProvider.getApplicationRootConfig();
		ApplicationConfig applicationConfig = applicationRootConfig.getConfig();
		permissionProcessor.hasPermissions(new PermissionOwner(applicationConfig));
		applicationReference.setConfig(applicationConfig);
		applicationReference.setId(getName());

		StopWatch performPage = new StopWatch();
		if (monitorPerformance) {
			performPage.start();
		}

		PageReference pageReference = new PageReference();
		applicationReference.setPages(new PagesReference());
		applicationReference.getPages().getPage().add(pageReference);

		String pageId = pathInfo.getPage();
		boolean isDefault = pageId == null;
		String defaultPageName = applicationConfigProvider.getDefaultPage();
		if (isDefault) {
			pageId = defaultPageName;
		}

		PageDefinition defaultPage = applicationConfigProvider.getPage(defaultPageName);
		PageDefinition page = applicationConfigProvider.getPage(pageId);
		if (null == page) {
			if (!isDefault) {
				warn("could not find requested page with id '" + pageId + "', returning defaultpage '" + defaultPageName
						+ "' instead");
				page = defaultPage;
				pageId = defaultPageName;
				isDefault = true;
			}
			if (null == page) {
				warn("no page found");
				return applicationReference;
			}
		} else {
			trace("found requested page with id '" + pageId + "'");
		}

		PageConfig pageConfig = page.getConfig();
		addTemplate(applicationConfig, pageConfig.getTemplates());

		pageReference.setConfig(pageConfig);
		pageReference.setType(page.getType());
		pageReference.setId(page.getId());

		if (!permissionProcessor.hasPermissions(new PermissionOwner(page))) {
			info("no permissions to display page '" + page.getId() + "', returning default page '" + defaultPageName
					+ "'");
			page = defaultPage;
		}
		Environment env = applicationRequest.getEnvironment();

		UrlSchema urlSchema = pageConfig.getUrlSchema();
		Set<String> sessionParamNames = new HashSet<>();
		for (Param sessionParam : applicationConfig.getSession().getSessionParams().getSessionParam()) {
			sessionParamNames.add(sessionParam.getName());
		}

		PageParameterProcessor pageParameterProcessor = new PageParameterProcessor(getSessionParamKey(site),
				sessionParamNames, env, applicationRequest);
		boolean urlParamsAdded = pageParameterProcessor.processPageParams(applicationUrlParameters, urlSchema);
		Map<String, String> pageParams = pageParameterProcessor.getParameters();
		initSession(applicationConfig, env, getSessionParamKey(site));
		if (urlParamsAdded || isDefault) {
			String redirectPath = getRedirectPath(pageId, urlSchema);
			site.sendRedirect(env, redirectPath);
			applicationRequest.setRedirectTarget(redirectPath);
			return applicationReference;
		}
		if (pathInfo.hasAction()) {
			// if action has been explicitly set at item element, override
			// an existing entry in params
			pageParams.put(pathInfo.getActionName(), pathInfo.getActionValue());
		}

		applicationRequest.addParameters(pageParams);
		applicationRequest.setLabels(platformConfig.getLabels());
		applicationRequest.setLabels(applicationConfig);
		applicationRequest.setLabels(page.getConfig());
		if (null == applicationConfig.getTitle()) {
			Label title = new Label();
			title.setValue(getDisplayName());
			applicationConfig.setTitle(title);
		}

		elementHelper.initNavigation(applicationRequest, pathInfo, pageConfig);
		Structure structure = null;
		boolean debugPermission = permissionProcessor.hasPermission("debug");
		try {
			structure = buildStructure(applicationRequest, applicationConfig, pageReference, page, true, null);
		} catch (ProcessingException e) {
			FieldProcessor fieldProcessor = e.getFieldProcessor();
			structure = handleException(applicationRequest, pageReference, e, fieldProcessor, debugPermission);
		} catch (Exception e) {
			handleException(applicationRequest, pageReference, e, null, debugPermission);
		}

		pageReference.setStructure(structure);
		if (monitorPerformance) {
			performPage.stop();
			pageReference.setExecutionTime(performPage.getTotalTimeMillis());
		}
		return applicationReference;
	}

	public PageReference processPage(MarshallService marshallService, Path pathInfo, String pageId,
			List<String> sectionIds) {
		PermissionProcessor permissionProcessor = applicationRequest.getPermissionProcessor();
		Environment env = applicationRequest.getEnvironment();

		ApplicationConfigProvider applicationConfigProvider = null;
		try {
			applicationConfigProvider = applicationConfig.cloneConfig(marshallService);
			applicationRequest.setApplicationConfig(applicationConfigProvider);
		} catch (InvalidConfigurationException e) {
			error("error while (re)loading configuration!", e);
		}

		List<String> applicationUrlParameters = pathInfo.getApplicationUrlParameters();
		trace("Processing application \"" + pathInfo.getApplicationName() + "\" with url-parameters \""
				+ applicationUrlParameters + "\"");

		ApplicationRootConfig applicationRootConfig = applicationConfigProvider.getApplicationRootConfig();
		ApplicationConfig applicationConfig = applicationRootConfig.getConfig();
		permissionProcessor.hasPermissions(new PermissionOwner(applicationConfig));

		StopWatch performPage = new StopWatch();
		if (monitorPerformance) {
			performPage.start();
		}

		PageReference pageReference = new PageReference();

		boolean isDefault = pageId == null;
		String defaultPageName = applicationConfigProvider.getDefaultPage();
		if (isDefault) {
			pageId = defaultPageName;
		}

		PageDefinition defaultPage = applicationConfigProvider.getPage(defaultPageName);
		PageDefinition page = applicationConfigProvider.getPage(pageId);
		if (null == page) {
			if (!isDefault) {
				warn("could not find requested page with id '" + pageId + "', returning defaultpage '" + defaultPageName
						+ "' instead");
				page = defaultPage;
				pageId = defaultPageName;
				isDefault = true;
			}
			if (null == page) {
				warn("no page found");
				return null;
			}
		} else {
			trace("found requested page with id '" + pageId + "'");
		}

		PageConfig pageConfig = page.getConfig();
		addTemplate(applicationConfig, pageConfig.getTemplates());

		pageReference.setConfig(pageConfig);
		pageReference.setType(page.getType());
		pageReference.setId(page.getId());

		if (!permissionProcessor.hasPermissions(new PermissionOwner(page))) {
			info("no permissions to display page '" + page.getId() + "', returning default page '" + defaultPageName
					+ "'");
			page = defaultPage;
		}

		Set<String> sessionParamNames = new HashSet<>();
		for (Param sessionParam : applicationConfig.getSession().getSessionParams().getSessionParam()) {
			sessionParamNames.add(sessionParam.getName());
		}

		PageParameterProcessor pageParameterProcessor = new PageParameterProcessor(getSessionParamKey(site),
				sessionParamNames, env, applicationRequest);
		Map<String, String> pageParams = pageParameterProcessor.getParameters();
		initSession(applicationConfig, env, getSessionParamKey(site));

		if (pathInfo.hasAction()) {
			// if action has been explicitly set at item element, override
			// an existing entry in params
			pageParams.put(pathInfo.getActionName(), pathInfo.getActionValue());
		}

		applicationRequest.addParameters(pageParams);
		applicationRequest.setLabels(applicationConfig);
		applicationRequest.setLabels(page.getConfig());
		if (null == applicationConfig.getTitle()) {
			Label title = new Label();
			title.setValue(getDisplayName());
			applicationConfig.setTitle(title);
		}

		elementHelper.initNavigation(applicationRequest, pathInfo, pageConfig);
		Structure structure = null;
		boolean debugPermission = permissionProcessor.hasPermission("debug");
		try {
			structure = buildStructure(applicationRequest, applicationConfig, pageReference, page, false, sectionIds);
		} catch (ProcessingException e) {
			FieldProcessor fieldProcessor = e.getFieldProcessor();
			structure = handleException(applicationRequest, pageReference, e, fieldProcessor, debugPermission);
		} catch (Exception e) {
			handleException(applicationRequest, pageReference, e, null, debugPermission);
		}

		pageReference.setStructure(structure);
		if (monitorPerformance) {
			performPage.stop();
			pageReference.setExecutionTime(performPage.getTotalTimeMillis());
		}
		return pageReference;
	}

	private Structure handleException(ApplicationRequest applicationRequest, PageReference pageReference, Exception e,
			FieldProcessor fieldProcessor, boolean debugPermission) {

		Structure structure;
		error("error while building structure for page '" + pageReference.getId() + "'", e);
		structure = new Structure();
		Messages messages = new Messages();
		messages.setRef(pageReference.getId());

		if (null != fieldProcessor) {
			if (e instanceof MessageParam) {
				MessageParam messageParam = (MessageParam) e;
				applicationRequest.addErrorMessage(fieldProcessor, messageParam);
			} else if (e.getCause() != null && e.getCause() instanceof MessageParam) {
				MessageParam messageParam = (MessageParam) e.getCause();
				applicationRequest.addErrorMessage(fieldProcessor, messageParam);
			}
			messages.getMessageList().addAll(fieldProcessor.getMessages().getMessageList());
			pageReference.setMessages(messages);
			if (debugPermission) {
				addStackTrace(messages, e);
			}
		}
		return structure;
	}

	private void addStackTrace(Messages messages, Throwable e) {
		String content = getStackTrace(e);
		Message stackTrace = new Message();
		stackTrace.setClazz(MessageType.ERROR);
		stackTrace.setContent(content);
		messages.getMessageList().add(stackTrace);
	}

	private String getStackTrace(Throwable e) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		e.printStackTrace(new PrintStream(out));
		return out.toString(Charset.defaultCharset());
	}

	private Structure buildStructure(ApplicationRequest applicationRequest, ApplicationConfig applicationConfig,
			final PageReference pageReference, PageDefinition page, boolean perform, List<String> sectionIds)
			throws ProcessingException {
		Structure structure = new Structure();

		List<SectionDef> sectionDefs = page.getStructure().getSection();
		boolean hasRedirect = false;
		List<DataSourceWrapper> dataSourceWrappers = new ArrayList<>();

		int sectionId = 0;
		for (SectionDef sectionDef : sectionDefs) {
			Section section = new Section();
			section.setId(sectionDef.getId());
			if (null == section.getId()) {
				section.setId("_sect" + (sectionId++));
			}

			String hidden = applicationRequest.getExpressionEvaluator().getString(sectionDef.getHidden());
			section.setHidden(hidden);
			List<SectionelementDef> elements = sectionDef.getElement();

			boolean mustPerform = perform
					|| (1 == sectionId || (null != sectionIds && sectionIds.contains(section.getId())));
			hasRedirect |= addElements(applicationRequest, applicationConfig, section, elements, pageReference,
					dataSourceWrappers, mustPerform);

			if (!section.getElement().isEmpty()) {
				section.setTitle(sectionDef.getTitle());
				applicationRequest.setLabel(section.getTitle());
				structure.getSection().add(section);
			}
		}
		if (!hasRedirect) {
			Messages messagesFromSession = elementHelper.removeMessages(applicationRequest.getEnvironment());
			if (null != messagesFromSession) {
				pageReference.setMessages(messagesFromSession);
			}
			for (final DataSourceWrapper dataSourceWrapper : dataSourceWrappers) {
				if (dataSourceWrapper.mustPerform) {
					Callback<Void> dataSourceCallback = new Callback<Void>() {

						public void perform() throws ProcessingException {
							dataSourceWrapper.perform(pageReference.getId());
						}

						public Void getResult() {
							return null;
						}
					};

					long time = doMonitored(dataSourceCallback);
					if (monitorPerformance) {
						dataSourceWrapper.setExecutionTime(time);
					}
				}
			}

			if (monitorPerformance) {
				for (Section section : structure.getSection()) {
					section.setExecutionTime(0L);
					for (Sectionelement element : section.getElement()) {
						section.setExecutionTime(section.getExecutionTime() + element.getExecutionTime());
					}
				}
			}
		}
		return structure;
	}

	protected class DataSourceWrapper extends DataSourceElement {
		protected boolean mustPerform;

		public DataSourceWrapper(Site site, AccessibleApplication application, ApplicationRequest applicationRequest,
				ParameterSupport parameterSupportDollar, DatasourceRef datasourceRef) throws ProcessingException {
			super(site, application, applicationRequest, parameterSupportDollar, datasourceRef);
		}

	}

	private boolean addElements(final ApplicationRequest applicationRequest, final ApplicationConfig applicationConfig,
			Section section, List<SectionelementDef> elements, final PageReference pageReference,
			List<DataSourceWrapper> dataSourceWrappers, boolean perform) throws ProcessingException {
		boolean hasRedirect = false;
		boolean isSectionHidden = Boolean.parseBoolean(section.getHidden());

		for (final SectionelementDef sectionelement : elements) {

			String folded = applicationRequest.getExpressionEvaluator().getString(sectionelement.getFolded());
			String passive = applicationRequest.getExpressionEvaluator().getString(sectionelement.getPassive());
			sectionelement.setFolded(folded);
			sectionelement.setPassive(passive);
			applicationRequest.setLabel(sectionelement.getTitle());

			if (null != sectionelement.getDatasource()) {
				DataSourceWrapper datasourceElement = getDataSourceSectionElement(applicationRequest, sectionelement);
				if (null != datasourceElement) {
					datasourceElement.setTitle(sectionelement.getTitle());
					datasourceElement.mustPerform = perform;
					dataSourceWrappers.add(datasourceElement);
					section.getElement().add(datasourceElement);
				}
			} else if (null != sectionelement.getAction()) {

				Callback<ActionElement> actionCallback = new Callback<ActionElement>() {

					private ActionElement result;

					public void perform() throws ProcessingException {
						this.result = getActionSectionElement(applicationRequest, applicationConfig, sectionelement,
								pageReference, isSectionHidden, perform);
					}

					public ActionElement getResult() {
						return result;
					}
				};
				long time = doMonitored(actionCallback);
				ActionElement actionElement = actionCallback.getResult();

				if (null != actionElement) {
					if (actionElement.doExecute()) {
						hasRedirect |= actionElement.getOnSuccess() != null
								&& (!actionElement.hasErrors() || sectionelement.getAction().isForceForward());
					}
					if (actionElement.doInclude()) {
						actionElement.setTitle(sectionelement.getTitle());
						section.getElement().add(actionElement);
						if (monitorPerformance) {
							actionElement.setExecutionTime(time);
						}
					}
				}
			}

		}
		return hasRedirect;
	}

	private interface Callback<T> {
		void perform() throws ProcessingException;

		T getResult();
	}

	private <T> long doMonitored(Callback<T> callback) throws ProcessingException {
		if (monitorPerformance) {
			StopWatch sw = new StopWatch();
			sw.start();
			callback.perform();
			sw.stop();
			return sw.getTotalTimeMillis();
		} else {
			callback.perform();
			return 0l;
		}
	}

	private void initSession(ApplicationConfig applicationConfig, Environment env, String sessionParamKey) {
		String sessionId = env.getAttributeAsString(SESSION, org.appng.api.Session.Environment.SID);
		Session session = applicationConfig.getSession();
		session.setId(sessionId);
		List<Param> sessionParam = session.getSessionParams().getSessionParam();

		CsrfToken csrfToken = env.getAttribute(Scope.SESSION, CsrfSetupFilter.CSRF_TOKEN);
		if (null != csrfToken) {
			Param csrf = new Param();
			csrf.setName(csrfToken.getParameterName());
			csrf.setValue(csrfToken.getToken());
			sessionParam.add(csrf);
		}
		Map<String, String> sessionParams = env.getAttribute(SESSION, sessionParamKey);
		for (Param param : sessionParam) {
			String value = sessionParams.get(param.getName());
			if (null != value) {
				param.setValue(value);
			}
		}
	}

	private String getRedirectPath(String pageId, UrlSchema urlSchema) {
		StringBuilder sb = new StringBuilder(getName() + "/" + pageId);
		UrlParams urlParams = urlSchema.getUrlParams();
		if (!(null == urlParams || null == urlParams.getParamList())) {
			for (Param param : urlParams.getParamList()) {
				if (!StringUtils.isBlank(param.getValue())) {
					sb.append("/" + param.getValue());
				} else {
					break;
				}
			}
		}
		return sb.toString();
	}

	private DataSourceWrapper getDataSourceSectionElement(ApplicationRequest applicationRequest,
			SectionelementDef sectionelement) throws ProcessingException {
		DatasourceRef datasourceRef = sectionelement.getDatasource();
		if (null != datasourceRef) {
			DataSourceWrapper wrapper = new DataSourceWrapper(site, application, applicationRequest,
					applicationRequest.getParameterSupportDollar(), datasourceRef);
			if (wrapper.doInclude()) {
				wrapper.setFolded(sectionelement.getFolded());
				wrapper.setMode(sectionelement.getMode());
				wrapper.setPassive(sectionelement.getPassive());
				return wrapper;
			}
		}
		return null;
	}

	private ActionElement getActionSectionElement(ApplicationRequest applicationRequest,
			ApplicationConfig applicationConfig, SectionelementDef sectionelement, PageReference pageReference,
			boolean isSectionHidden, boolean perform) throws ProcessingException {
		ActionRef actionRef = sectionelement.getAction();
		if (null != actionRef) {
			ActionElement actionElement = new ActionElement(site, application, applicationRequest, actionRef);
			if (perform) {
				actionElement.perform(sectionelement, isSectionHidden);
			}
			return actionElement;
		}
		return null;
	}

	private void addTemplate(ApplicationConfig applicationConfig, List<Template> templates) {
		for (Template template : templates) {
			applicationConfig.getTemplates().add(template);
		}
	}

	void readPermissions(Map<String, org.appng.xml.platform.Permission> permissionMap, Permissions permissions) {
		if (null != permissions) {
			for (org.appng.xml.platform.Permission permission : permissions.getPermissionList()) {
				permissionMap.put(permission.getRef(), permission);
			}
		}
	}

	public void setApplicationConfig(ApplicationConfigProvider applicationConfig) throws InvalidConfigurationException {
		this.applicationConfig = applicationConfig;
	}

	private void trace(String message) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace(getPrefix() + message);
		}
	}

	private void debug(String message) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(getPrefix() + message);
		}
	}

	private void info(String message) {
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info(getPrefix() + message);
		}
	}

	private void warn(String message) {
		if (LOGGER.isWarnEnabled()) {
			LOGGER.warn(getPrefix() + message);
		}
	}

	private void error(String message, Exception e) {
		if (LOGGER.isErrorEnabled()) {
			LOGGER.error(getPrefix() + message, e);
		}
	}

	public Integer getId() {
		return application.getId();
	}

	public void setId(Integer id) {
		application.setId(id);
	}

	public String getName() {
		return application.getName();
	}

	public void setName(String name) {
		application.setName(name);
	}

	public String getDescription() {
		return application.getDescription();
	}

	public void setDescription(String description) {
		application.setDescription(description);
	}

	public String getDisplayName() {
		return application.getDisplayName();
	}

	public String getPackageVersion() {
		return application.getPackageVersion();
	}

	public String getTimestamp() {
		return application.getTimestamp();
	}

	public String getLongDescription() {
		return application.getLongDescription();
	}

	public String getAppNGVersion() {
		return application.getAppNGVersion();
	}

	public boolean isInstalled() {
		return application.isInstalled();
	}

	public boolean isSnapshot() {
		return application.isSnapshot();
	}

	public boolean isFileBased() {
		return application.isFileBased();
	}

	public void setFileBased(boolean fileBased) {
		application.setFileBased(fileBased);
	}

	public Date getVersion() {
		return application.getVersion();
	}

	public void setVersion(Date version) {
		application.setVersion(version);
	}

	public Set<Permission> getPermissions() {
		return application.getPermissions();
	}

	public void setPermissions(Set<Permission> permissions) {
		application.setPermissions(permissions);
	}

	public Set<Role> getRoles() {
		return application.getRoles();
	}

	public void setRoles(Set<Role> roles) {
		application.setRoles(roles);
	}

	public Properties getProperties() {
		return application.getProperties();
	}

	public void setProperties(Properties properties) {
		application.setProperties(properties);
	}

	public void setContext(ConfigurableApplicationContext applicationContext) {
		application.setContext(applicationContext);
	}

	public <T> T getBean(String name, Class<T> clazz) {
		return application.getBean(name, clazz);
	}

	public <T> T getBean(Class<T> clazz) {
		return application.getBean(clazz);
	}

	public Object getBean(String beanName) {
		return application.getBean(beanName);
	}

	public String[] getBeanNames(Class<?> clazz) {
		return application.getBeanNames(clazz);
	}

	public boolean isPrivileged() {
		return application.isPrivileged();
	}

	@Deprecated
	public boolean isCoreApplication() {
		return isPrivileged();
	}

	public void setPrivileged(boolean isPrivileged) {
		application.setPrivileged(isPrivileged);
	}

	public String getMessage(Locale locale, String key, Object... args) {
		return getBean(MessageSource.class).getMessage(key, args, key, locale);
	}

	public List<JarInfo> getJarInfos() {
		if (jarInfos.isEmpty()) {
			for (Resource resource : application.getResources().getResources(ResourceType.JAR)) {
				JarInfo jarInfo = JarInfoBuilder.getJarInfo(resource.getCachedFile(), application.getName());
				jarInfos.add(jarInfo);
			}
		}
		Collections.sort(jarInfos);
		return jarInfos;
	}

	@Override
	public int hashCode() {
		return application.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		return application.equals(o);
	}

	@Override
	public String toString() {
		return application.toString();
	}

	public void closeContext() {
		try {
			applicationConfig.close();
		} catch (IOException e) {
			LOGGER.warn("error closing {}", applicationConfig);
		}
		applicationConfig = null;
		application.closeContext();
	}

	public void setResources(Resources applicationResourceHolder) {
		application.setResources(applicationResourceHolder);
	}

	public Resources getResources() {
		return application.getResources();
	}

	public boolean isHidden() {
		return application.isHidden();
	}

	public boolean containsBean(String beanName) {
		return application.containsBean(beanName);
	}

	public ApplicationConfigProvider getApplicationConfig() {
		return applicationConfig;
	}

	public String getSessionParamKey(Site site) {
		return application.getSessionParamKey(site);
	}

	public Map<String, String> getSessionParams(Site site, Environment environment) {
		return application.getSessionParams(site, environment);
	}

	public FeatureProvider getFeatureProvider() {
		return application.getFeatureProvider();
	}

	public void setFeatureProvider(FeatureProvider featureProvider) {
		application.setFeatureProvider(featureProvider);
	}

	public List<ApplicationSubject> getApplicationSubjects() {
		return application.getApplicationSubjects();
	}

	public ApplicationRequest getApplicationRequest(HttpServletRequest servletRequest,
			HttpServletResponse servletResponse) {
		return getApplicationRequest(servletRequest, servletResponse, false);
	}

	public ApplicationRequest getApplicationRequest(HttpServletRequest servletRequest,
			HttpServletResponse servletResponse, boolean createNew) {
		Environment env = initEnvironment(servletRequest, servletResponse);
		Subject subject = env.getSubject();
		PermissionProcessor permissionProcessor = null;
		Boolean permissionsEnabled = getProperties().getBoolean("permissionsEnabled", Boolean.TRUE);
		if (permissionsEnabled) {
			permissionProcessor = new DefaultPermissionProcessor(subject, site, this);
		} else {
			permissionProcessor = new DummyPermissionProcessor(subject, site, this);
		}
		if (createNew) {
			MessageSource messageSource = getBean(MessageSource.class);
			ConversionService conversionService = getBean("conversionService", ConversionService.class);
			RequestFactoryBean rfb = new RequestFactoryBean(servletRequest, env, conversionService, messageSource);
			rfb.afterPropertiesSet();
			this.applicationRequest = (ApplicationRequest) rfb.getObject();
		} else {
			this.applicationRequest = getBean("request", ApplicationRequest.class);
		}

		applicationRequest.setPermissionProcessor(permissionProcessor);
		return applicationRequest;
	}

	private Environment initEnvironment(HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
		Environment env = getBean("environment", Environment.class);
		for (Application application : site.getApplications()) {
			String sessionParamName = application.getSessionParamKey(site);
			if (null == env.getAttribute(SESSION, sessionParamName)) {
				env.setAttribute(SESSION, sessionParamName, new HashMap<>());
			}
		}
		return env;
	}

	public Action processAction(HttpServletResponse servletResponse, boolean applyPermissionsOnRef,
			ApplicationRequest applicationRequest, String actionId, String eventId, MarshallService marshallService)
			throws InvalidConfigurationException, ProcessingException {
		ApplicationConfigProvider applicationConfigProvider = getApplicationConfig().cloneConfig(marshallService);
		applicationRequest.setApplicationConfig(applicationConfigProvider);
		Action action = applicationConfigProvider.getAction(eventId, actionId);
		if (null == action) {
			LOGGER.debug("Action {}:{} not found on application {} of site {}", eventId, actionId,
					application.getName(), site.getName());
			servletResponse.setStatus(HttpStatus.NOT_FOUND.value());
			return null;
		}
		Environment environment = applicationRequest.getEnvironment();
		if (permissionsPresent(action.getConfig()) || environment.isSubjectAuthenticated()) {
			Params params = action.getConfig().getParams();
			ActionRef actionRef = new ActionRef();
			actionRef.setEventId(eventId);
			actionRef.setId(actionId);
			actionRef.setParams(params);
			if (applyPermissionsOnRef) {
				actionRef.setPermissions(action.getConfig().getPermissions());
			}

			setParamValues(applicationRequest, params);
			CallableAction callableAction = new CallableAction(site, application, applicationRequest, actionRef);

			if (callableAction.doInclude() || callableAction.doExecute()) {
				LOGGER.debug("Performing action {}:{} of application {} on site {}", eventId, actionId,
						application.getName(), site.getName());
				callableAction.perform(false);
				Messages messages = elementHelper.removeMessages(environment);
				if (null != messages) {
					messages.setRef(actionId);
					action.setMessages(messages);
				}
				return action;
			}
			LOGGER.debug("Include condition for action {}:{} of application {} on site {} does not match.", eventId,
					actionId, application.getName(), site.getName());
		}
		Subject subject = environment.getSubject();
		LOGGER.debug(
				"Action {}:{} of application {} on site {} neither defines permissions, nor is the subject authenticated (subject is {}). Sending 403.",
				eventId, actionId, application.getName(), site.getName(),
				subject == null ? "<unknown>" : subject.getAuthName());
		servletResponse.setStatus(HttpStatus.FORBIDDEN.value());
		return null;
	}

	protected void setParamValues(ApplicationRequest applicationRequest, Params params) {
		if (null != params) {
			List<Param> paramList = params.getParam();
			for (Param param : paramList) {
				String parameter = applicationRequest.getParameter(param.getName());
				if (null != parameter) {
					param.setValue(parameter);
				}
			}
		}
	}

	public Datasource processDataSource(HttpServletResponse servletResponse, boolean applyPermissionsOnRef,
			ApplicationRequest applicationRequest, String dataSourceId, MarshallService marshallService)
			throws InvalidConfigurationException, ProcessingException {
		ApplicationConfigProvider applicationConfigProvider = getApplicationConfig().cloneConfig(marshallService);
		applicationRequest.setApplicationConfig(applicationConfigProvider);
		Datasource dataSource = applicationConfigProvider.getDatasource(dataSourceId);
		if (null == dataSource) {
			LOGGER.debug("DataSource {} not found on application {} of site {}", dataSource, application.getName(),
					site.getName());
			servletResponse.setStatus(HttpStatus.NOT_FOUND.value());
			return null;
		}
		DataConfig config = dataSource.getConfig();
		Environment environment = applicationRequest.getEnvironment();
		if (permissionsPresent(config) || environment.isSubjectAuthenticated()) {
			Params params = config.getParams();
			DatasourceRef datasourceRef = new DatasourceRef();
			datasourceRef.setId(dataSourceId);
			if (applyPermissionsOnRef) {
				datasourceRef.setPermissions(config.getPermissions());
			}
			datasourceRef.setParams(params);
			setParamValues(applicationRequest, params);
			ParameterSupport parameterSupport = applicationRequest.getParameterSupportDollar();

			CallableDataSource callableDataSource = new CallableDataSource(site, application, applicationRequest,
					parameterSupport, datasourceRef);
			if (callableDataSource.doInclude()) {
				LOGGER.debug("Performing dataSource {} of application {} on site {}", dataSourceId,
						application.getName(), site.getName());
				callableDataSource.perform("service");
				return callableDataSource.getDatasource();
			}
			LOGGER.debug("Include condition for dataSource {} of application {} on site {} does not match.",
					dataSourceId, application.getName(), site.getName());
		}
		Subject subject = environment.getSubject();
		LOGGER.debug(
				"DataSource {} of application {} on site {} neither defines permissions, nor is the subject authenticated (subject is {}). Sending 403.",
				dataSource, application.getName(), site.getName(),
				subject == null ? "<unknown>" : subject.getAuthName());
		servletResponse.setStatus(HttpStatus.FORBIDDEN.value());
		return null;
	}

	/**
	 * For direct URL-access to an action/ a datasource, permissions need to be present. If no permissions are present,
	 * this means direct access is forbidden.
	 * 
	 * @param config
	 * 
	 * @return
	 */
	private boolean permissionsPresent(Config config) {
		Permissions permissions = config.getPermissions();
		return !(null == permissions || permissions.getPermissionList().isEmpty());
	}

	public ConfigurableApplicationContext getContext() {
		return application.getContext();
	}

	@Override
	public Application getApplication() {
		return application;
	}

	@Override
	public Site getSite() {
		return site;
	}

	public Set<Resource> getResourceSet() {
		return application.getResourceSet();
	}

	@Override
	public void setDatabaseConnection(DatabaseConnection databaseConnection) {
		this.databaseConnection = databaseConnection;
	}

	@Override
	public DatabaseConnection getDatabaseConnection() {
		return databaseConnection;
	}

	public void setPlatformScope() {
		setPlatformScope(isCoreApplication());
	}

	public void setPlatformScope(boolean enabled) {
		DefaultEnvironment defaultEnvironment = (DefaultEnvironment) applicationRequest.getEnvironment();
		if (enabled) {
			defaultEnvironment.enable(PLATFORM);
		} else {
			defaultEnvironment.disable(PLATFORM);
		}
	}

}
