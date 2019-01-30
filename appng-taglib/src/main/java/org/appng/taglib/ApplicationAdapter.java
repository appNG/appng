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
package org.appng.taglib;

import static org.appng.api.Scope.REQUEST;
import static org.appng.api.Scope.SESSION;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.Environment;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.PathInfo;
import org.appng.api.Platform;
import org.appng.api.Scope;
import org.appng.api.SiteProperties;
import org.appng.api.model.Application;
import org.appng.api.model.Properties;
import org.appng.api.model.Site;
import org.appng.api.model.Subject;
import org.appng.api.support.ApplicationRequest;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.api.support.environment.EnvironmentKeys;
import org.appng.core.controller.HttpHeaders;
import org.appng.core.domain.SiteImpl;
import org.appng.core.domain.SubjectImpl;
import org.appng.core.model.ApplicationProvider;
import org.appng.core.model.RequestProcessor;
import org.appng.xml.MarshallService;
import org.appng.xml.platform.ItemType;
import org.appng.xml.platform.NavigationItem;
import org.springframework.xml.transform.StringSource;

import lombok.extern.slf4j.Slf4j;

/**
 * Used to embed an appNG {@link Application} inside a JSP page. This is achieved by calling the given
 * {@link Application} and then transforming the returned {@link org.appng.xml.platform.Platform} using an XSLT
 * stylesheet.
 * <p>
 * <b>Attributes:</b>
 * <ul>
 * <li>application - the name of the {@link Application} that should be embedded.</li>
 * </ul>
 * </p>
 * <p>
 * <b>Parameters:</b> ({@code <appNG:param>})
 * <ul>
 * <li>defaultBaseUrl - the url of the page where the application is embedded</li>
 * <li>defaultPage - The default page of the application, used when the url parameters do not contain a page name</li>
 * <li>xslStyleSheet - The path to the XSLT stylesheet used for transformation, relative to the {@link Site}'s
 * repository folder. If omitted, the plain XML is written as an HTML comment</li>
 * <li>requestAttribute - The name of an {@link Environment}-attribute with the scope {@link Scope#REQUEST} where the
 * transformation result should be stored in. If this parameter is not set, the result is directly written to the JSP's
 * {@link Writer}.</li>
 * <li>locale - The locale to use, for example en_US or es_MX</li>
 * </ul>
 * </p>
 * <p>
 * <b>GET-Parameters:</b>
 * <ul>
 * <li>xsl - if {@code false}, the plain XML is written as an HTML comment</li>
 * </ul>
 * </p>
 * <p>
 * <b>Usage:</b><br/>
 * The following example assumes you want to embed the application {@code acme-app} into the page {@code /en/acme}.<br/>
 * Every path segment after {@code /en/acme} is passed as an url-parameter to the application.
 * <p/>
 * 
 * <pre>
 * &lt;appNG:application name="acme-app">
 *   &lt;appNG:param name="defaultBaseUrl">/en/acme&lt;/appNG:param>
 *   &lt;appNG:param name="defaultPage">/index/welcome&lt;/appNG:param>
 *   &lt;appNG:param name="xslStyleSheet">/meta/xsl/acme/platform.xsl&lt;/appNG:param>
 *   &lt;appNG:param name="requestAttribute">acmeResult&lt;/appNG:param>
 * &lt;/appNG:application>
 * &lt;!-- later in JSP -->
 * &lt;appNG:attribute mode="read" name="acmeResult" scope="REQUEST" />
 * </pre>
 * 
 * @author Matthias Müller
 */
@Slf4j
public final class ApplicationAdapter extends BodyTagSupport implements ParameterOwner {

	private static final String TAGL_DEFAULT_BASE_URL = "defaultBaseUrl";
	private static final String TAGL_DEFAULT_PAGE = "defaultPage";
	private static final String TAGL_XSL_STYLE_SHEET = "xslStyleSheet";
	private static final String TAGL_REQUEST_ATTRIBUTE = "requestAttribute";
	private static final String TAGL_LOCALE = "locale";
	private static final ConcurrentMap<String, TemplateWrapper> TEMPLATE_CACHE = new ConcurrentHashMap<String, TemplateWrapper>();
	private static final TransformerFactory transformerFactory = TransformerFactory.newInstance();
	private String application;
	private Map<String, String> tagletAttributes;

	public void setName(String application) {
		this.application = application;
	}

	@Override
	public int doStartTag() throws JspException {
		tagletAttributes = new HashMap<>();
		return super.doStartTag();
	}

	@Override
	public final int doEndTag() {
		int state = EVAL_PAGE;
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		try {
			DefaultEnvironment env = DefaultEnvironment.get(pageContext);
			HttpServletRequest servletRequest = env.getServletRequest();
			HttpServletResponse servletResponse = env.getServletResponse();

			MultiSiteSupport multiSiteSupport = new MultiSiteSupport();
			multiSiteSupport.process(env, application, servletRequest);
			SiteImpl executingSite = multiSiteSupport.getExecutingSite();
			SiteImpl callingSite = multiSiteSupport.getCallingSite();
			ApplicationProvider executingApplication = multiSiteSupport.getApplicationProvider();

			ApplicationRequest request = executingApplication.getApplicationRequest(servletRequest, servletResponse,
					true);

			String styleSheetName = tagletAttributes.remove(TAGL_XSL_STYLE_SHEET);
			String defaultPage = tagletAttributes.remove(TAGL_DEFAULT_PAGE);
			String defaultBaseUrl = tagletAttributes.remove(TAGL_DEFAULT_BASE_URL);
			Subject subject = env.getSubject();
			List<String> urlParameters;
			Properties platformProperties = env.getAttribute(org.appng.api.Scope.PLATFORM,
					Platform.Environment.PLATFORM_CONFIG);
			String jspExtension = platformProperties.getString(Platform.Property.JSP_FILE_TYPE);
			String repositoryPath = platformProperties.getString(Platform.Property.REPOSITORY_PATH);
			Properties properties = executingSite.getProperties();
			boolean logoutRequested = false;
			String loginPage = properties.getString(SiteProperties.AUTH_LOGIN_PAGE);
			String loginAction = properties.getString(SiteProperties.AUTH_LOGIN_REF);
			String logoutPage = properties.getString(SiteProperties.AUTH_LOGOUT_PAGE);
			String logoutAction = properties.getString(SiteProperties.AUTH_LOGOUT_ACTION_VALUE);
			List<String> originalUrlParameters = request.getUrlParameters();
			if (null != originalUrlParameters) {
				int logoutPageIdx = originalUrlParameters.indexOf(logoutPage);
				int logoutActionIdx = originalUrlParameters.indexOf(logoutAction);
				logoutRequested = logoutPageIdx >= 0 && logoutActionIdx == logoutPageIdx + 1;
			}

			PathInfo pathInfo;
			if (env.isSubjectAuthenticated() && logoutRequested) {
				urlParameters = Arrays.asList(logoutPage, logoutAction);
				String managerPath = properties.getString(SiteProperties.MANAGER_PATH);
				String servletPath = managerPath + "/" + logoutPage + "/" + logoutAction;
				pathInfo = getPathInfo(executingSite, executingApplication, urlParameters, platformProperties,
						servletPath);
			} else if (!env.isSubjectAuthenticated()) {
				urlParameters = Arrays.asList(loginPage, loginAction);
				pathInfo = getPathInfo(executingSite, executingApplication, urlParameters, platformProperties, null);
				boolean hasSubject = null != subject;
				if (!hasSubject) {
					subject = new SubjectImpl();
				}
				String originalServletPath = env.getAttributeAsString(REQUEST, EnvironmentKeys.SERVLETPATH);
				if (null != originalServletPath) {
					env.setAttribute(SESSION, "preLoginPath", originalServletPath);
				}
				String languageTag = tagletAttributes.get(TAGL_LOCALE);
				if (StringUtils.isBlank(languageTag)) {
					languageTag = callingSite.getProperties().getString(Platform.Property.LOCALE);
				}
				if (!(hasSubject && env.getLocale().toLanguageTag().equals(languageTag))) {
					((SubjectImpl) subject).setLanguage(languageTag);
					((DefaultEnvironment) env).setSubject(subject);
					LOGGER.info("setting language: {}", languageTag);
				}
			} else {
				urlParameters = originalUrlParameters;
				if (null == urlParameters || urlParameters.isEmpty()) {
					LOGGER.info("no URL parameters. Set page=defaultPage={}", defaultPage);
					urlParameters = Arrays.asList(defaultPage);
				}
				pathInfo = getPathInfo(executingSite, executingApplication, urlParameters, platformProperties, null);
			}

			LOGGER.info("pathInfo.getApplicationName(): {}", pathInfo.getApplicationName());
			LOGGER.info("pathInfo.getCurrentPath(): {}", pathInfo.getCurrentPath());

			Thread.currentThread().setContextClassLoader(executingSite.getSiteClassLoader());

			RequestProcessor processor = getProcessor(executingSite, env, platformProperties, executingApplication,
					pathInfo);
			org.appng.xml.platform.Platform platform = processor.processPlatform(executingSite);

			String requestURI = servletRequest.getRequestURI();
			String baseUrl = getBaseUrl(executingSite, defaultBaseUrl, urlParameters, jspExtension, repositoryPath,
					requestURI);

			String location = servletResponse.getHeader(HttpHeaders.LOCATION);
			if (processor.isRedirect()) {
				ApplicationRequest applicationRequest = executingApplication.getApplicationRequest(servletRequest,
						servletResponse, false);
				String redirectTarget = applicationRequest.getRedirectTarget();
				if (null != redirectTarget) {
					redirectTarget = redirectTarget.substring(redirectTarget.indexOf("/"), redirectTarget.length());
					if (redirectTarget.equals("/" + loginPage + "/" + loginAction)) {
						redirectTarget = "";
					}
					state = doRedirect(servletResponse, requestURI, baseUrl + redirectTarget);
				}

			} else if (null != location) {
				String redirectTarget = normalizeUrl(executingSite, jspExtension, location, baseUrl);
				state = doRedirect(servletResponse, location, redirectTarget);
			} else {
				String result = "";
				if (!StringUtils.equalsIgnoreCase(Boolean.FALSE.toString(), request.getParameters().get("replace"))) {
					platform.getConfig().setBaseUrl(baseUrl);
					adjustNavigationItems(platform.getNavigation().getItem(), "", "");
				}
				MarshallService marshallService = executingApplication.getBean(MarshallService.class);
				String plainXML = marshallService.marshal(platform);
				if (Boolean.FALSE.toString().equalsIgnoreCase(request.getParameters().get("transform"))
						|| StringUtils.isEmpty(styleSheetName)) {
					result = "<!-- " + plainXML + " -->";
				} else {
					File styleSheetFile = callingSite.readFile(styleSheetName);
					boolean doXsl = !"false".equalsIgnoreCase(servletRequest.getParameter("xsl"));
					Integer maxTemplateAge = executingApplication.getProperties().getInteger("maxTemplateAge", 60000);
					result = transform(styleSheetFile, plainXML, maxTemplateAge.longValue(), doXsl);
				}
				String resultParam = tagletAttributes.get(TAGL_REQUEST_ATTRIBUTE);
				if (StringUtils.isNotBlank(resultParam)) {
					env.setAttribute(org.appng.api.Scope.REQUEST, resultParam, result);
				} else {
					getBodyContent().getEnclosingWriter().write(result);
				}
			}
		} catch (Exception e) {
			LOGGER.error("error while processing", e);
		} finally {
			Thread.currentThread().setContextClassLoader(contextClassLoader);
		}
		return state;
	}

	public void addParameter(String name, String value) {
		tagletAttributes.put(name, value);
	}

	protected int doRedirect(HttpServletResponse servletResponse, String location, String redirectTarget)
			throws IOException {
		LOGGER.info("Redirecting {} to {}", location, redirectTarget);
		servletResponse.setStatus(HttpServletResponse.SC_FOUND);
		servletResponse.sendRedirect(redirectTarget);
		return SKIP_PAGE;
	}

	protected String getBaseUrl(SiteImpl executingSite, String defaultBaseUrl, List<String> urlParameters,
			String jspExtension, String repositoryPath, String requestURI) {
		String urlParams = StringUtils.join(urlParameters, "/");
		LOGGER.info("requestURI: {}", requestURI);
		LOGGER.info("urlParams: {}", urlParams);

		int lastIndexOf = requestURI.lastIndexOf(urlParams);
		String baseUrl = "";
		if (lastIndexOf >= 0) {
			baseUrl = requestURI.subSequence(0, lastIndexOf).toString().replaceAll("/+", "/");
		} else {
			baseUrl = defaultBaseUrl;
		}
		LOGGER.info("baseURL: {} ", baseUrl);
		return baseUrl;
	}

	private PathInfo getPathInfo(Site executingSite, Application application, List<String> urlParameters,
			Properties platformProperties, String servletPath) {
		Properties siteProperties = executingSite.getProperties();
		String guiPath = siteProperties.getString(SiteProperties.MANAGER_PATH);
		String servicePath = siteProperties.getString(SiteProperties.SERVICE_PATH);
		String repoPath = platformProperties.getString(Platform.Property.REPOSITORY_PATH);
		String extension = platformProperties.getString(Platform.Property.JSP_FILE_TYPE);
		List<String> blobDirectories = siteProperties.getList(SiteProperties.ASSETS_DIR, ";");
		List<String> documentDirectories = siteProperties.getList(SiteProperties.DOCUMENT_DIR, ";");
		if (null == servletPath) {
			StringBuilder servletPathBuilder = new StringBuilder();
			servletPathBuilder.append(guiPath + "/" + executingSite.getName() + "/");
			servletPathBuilder.append(application.getName());
			if (null != urlParameters) {
				servletPathBuilder.append("/");
				servletPathBuilder.append(StringUtils.join(urlParameters, "/"));
			}
			servletPath = servletPathBuilder.toString();
		}
		servletPath = servletPath.replaceAll("/+", "/");
		LOGGER.info("servletPath: {}", servletPath);
		return new PathInfo(executingSite.getHost(), executingSite.getDomain(), executingSite.getName(), servletPath,
				guiPath, servicePath, blobDirectories, documentDirectories, repoPath, extension);
	}

	protected String normalizeUrl(Site executingSite, String jspExtension, String location, String baseUrl) {
		String managerPath = executingSite.getProperties().getString(SiteProperties.MANAGER_PATH);
		String siteName = executingSite.getName();
		String managerUrl = managerPath + "/" + siteName + "/" + application;
		return location.replace(managerUrl, baseUrl);
	}

	private RequestProcessor getProcessor(Site executingSite, DefaultEnvironment env, Properties platformProperties,
			Application application, PathInfo pathInfo) throws InvalidConfigurationException {
		String siteRoot = executingSite.getProperties().getString(SiteProperties.SITE_ROOT_DIR);
		String siteWwwDir = executingSite.getProperties().getString(SiteProperties.WWW_DIR);
		String templatePrefix = platformProperties.getString(Platform.Property.TEMPLATE_PREFIX);
		String templateDir = siteRoot + siteWwwDir + templatePrefix;
		RequestProcessor processor = application.getBean("requestProcessor", RequestProcessor.class);
		processor.init(env.getServletRequest(), env.getServletResponse(), pathInfo, templateDir);
		return processor;
	}

	private void adjustNavigationItems(List<NavigationItem> navItems, String site, String app) {
		if (null != navItems && navItems.size() > 0) {
			for (NavigationItem navItem : navItems) {
				if (null != navItem) {
					if (null != navItem.isSelected() && navItem.isSelected()) {
						if (ItemType.SITE.equals(navItem.getType())) {
							navItem.setRef(site);
						}
						if (ItemType.APPLICATION.equals(navItem.getType())) {
							navItem.setRef(app);
						}
					}
					adjustNavigationItems(navItem.getItem(), site, app);
				}
			}
		}
	}

	public String transform(File xslFile, String content, long templateMaxAge, boolean doTransform)
			throws TransformerConfigurationException, TransformerException {
		if (doTransform) {
			return transform(xslFile, content, templateMaxAge);
		} else {
			return "<!-- " + content + " -->";
		}
	}

	public String transform(File xslFile, String content, long templateMaxAge)
			throws TransformerConfigurationException, TransformerException {
		if (xslFile.exists() && !xslFile.isDirectory()) {
			StringWriter output = new StringWriter();
			Transformer transformer = getTemplate(xslFile, templateMaxAge).newTransformer();
			transformer.setErrorListener(new ErrorListener() {
				public void warning(TransformerException exception) throws TransformerException {
					LOGGER.error(exception.getMessage(), exception);
				}

				public void fatalError(TransformerException exception) throws TransformerException {
					LOGGER.error(exception.getMessage(), exception);
				}

				public void error(TransformerException exception) throws TransformerException {
					LOGGER.error(exception.getMessage(), exception);
				}
			});
			transformer.transform(new StringSource(content), new StreamResult(output));
			return output.toString();
		} else {
			LOGGER.warn("{} does not exist or is a directory!", xslFile.getAbsolutePath());
			return "";
		}
	}

	private Templates getTemplate(File xslFile, long templateMaxAge) throws TransformerConfigurationException {
		String xslPath = xslFile.getAbsolutePath();
		TemplateWrapper templates = TEMPLATE_CACHE.get(xslPath);
		if (null == templates || xslFile.lastModified() > templates.lastModified
				|| templates.getAge() > templateMaxAge) {
			templates = new TemplateWrapper(transformerFactory.newTemplates(new StreamSource(xslFile)));
			TEMPLATE_CACHE.put(xslPath, templates);
			LOGGER.debug("updating cache: {}, last modified {}", xslPath, new Date(xslFile.lastModified()));
		} else {
			LOGGER.debug("retrieved from cache: {}, last modified {}", xslPath, new Date(templates.lastModified));
		}
		return templates;
	}

	class TemplateWrapper implements Templates {
		final Templates templates;
		final long lastModified;

		TemplateWrapper(Templates templates) {
			this.templates = templates;
			this.lastModified = System.currentTimeMillis();
		}

		public long getAge() {
			return System.currentTimeMillis() - lastModified;
		}

		public java.util.Properties getOutputProperties() {
			return templates.getOutputProperties();
		}

		public Transformer newTransformer() throws TransformerConfigurationException {
			return templates.newTransformer();
		}

	}

}
