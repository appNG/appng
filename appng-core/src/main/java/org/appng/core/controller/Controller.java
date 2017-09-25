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
package org.appng.core.controller;

import static org.appng.api.Scope.REQUEST;
import static org.appng.api.Scope.SESSION;

import java.io.IOException;
import java.util.List;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.ContainerServlet;
import org.apache.catalina.Context;
import org.apache.catalina.Manager;
import org.apache.catalina.Wrapper;
import org.apache.catalina.servlets.DefaultServlet;
import org.apache.catalina.util.ServerInfo;
import org.appng.api.Environment;
import org.appng.api.Path;
import org.appng.api.PathInfo;
import org.appng.api.Platform;
import org.appng.api.RequestUtil;
import org.appng.api.Scope;
import org.appng.api.SiteProperties;
import org.appng.api.model.Properties;
import org.appng.api.model.Site;
import org.appng.api.model.Site.SiteState;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.api.support.environment.EnvironmentKeys;
import org.appng.core.Redirect;
import org.appng.core.controller.handler.ErrorPageHandler;
import org.appng.core.controller.handler.GuiHandler;
import org.appng.core.controller.handler.JspHandler;
import org.appng.core.controller.handler.ProxyHandler;
import org.appng.core.controller.handler.RequestHandler;
import org.appng.core.controller.handler.ServiceRequestHandler;
import org.appng.core.controller.handler.StaticContentHandler;
import org.appng.core.domain.SiteImpl;
import org.appng.core.model.PlatformTransformer;
import org.appng.xml.MarshallService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;

/**
 * The controller {@link Servlet} of appNG, delegating {@link HttpServletRequest}s to an appropriate
 * {@link RequestHandler}.
 * 
 * @author Matthias Müller
 */
@WebServlet(name = "controller", urlPatterns = { "/", "*.jsp" }, loadOnStartup = 1)
public class Controller extends DefaultServlet implements ContainerServlet {

	private static final Logger LOGGER = LoggerFactory.getLogger(Controller.class);

	private static final String ALLOW_PLAIN_REQUESTS = "allowPlainRequests";
	private static final String ERRORPAGE = "/errorpage";
	private static final String SLASH = "/";

	private static final String SCHEME_HTTPS = "https://";
	private static final String SCHEME_HTTP = "http://";

	/**
	 * a boolean flag to be set in the {@link Environment} with {@link Scope#SESSION}, indicating that
	 * {@link #expireSessions(HttpServletRequest, Environment, Site)} should be called
	 */
	public static final String EXPIRE_SESSIONS = "expireSessions";

	protected JspHandler jspHandler;

	private Manager manager;

	private Support support;

	public Controller() {
		LOGGER.info("Controller created");
	}

	/** a SPI to support different versions of Tomcat */
	interface Support {
		void serveResource(HttpServletRequest request, HttpServletResponse response, boolean content, String encoding) throws IOException,ServletException;

		HttpServletResponse wrapResponseForHeadRequest(HttpServletResponse response);

		void init(ServletConfig config) throws ServletException;
	}

	protected void setSupport(Support support) {
		this.support = support;
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		try {
			char tomcatMajor = ServerInfo.getServerNumber().charAt(0);
			Class<?> supportClassName = Class.forName(String.format("org.appng.core.controller.Tomcat%sSupport", tomcatMajor));
			setSupport( (Support) supportClassName.newInstance());
			LOGGER.debug("created {}", support.getClass().getName());
		} catch (ReflectiveOperationException o) {
			throw new ServletException("error while creating Controller.Support", o);
		}
		support.init(config);
	}

	// for Tomcat 7 compatibility
	protected void serveResource(HttpServletRequest request, HttpServletResponse response, boolean content) throws IOException ,ServletException {
		support.serveResource(request, response, content, null);
	}

	@Override
	protected void serveResource(HttpServletRequest request, HttpServletResponse response, boolean content,
			String encoding) throws IOException, ServletException {
		support.serveResource(request, response, content, encoding);
	}

	public void serveResource(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		serveResource(request, response, true, null);
	}

	protected void doHead(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		doGet(request, support.wrapResponseForHeadRequest(response));
	}

	protected Environment getEnvironment(HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
		return DefaultEnvironment.get(getServletContext(), servletRequest, servletResponse);
	}

	@Override
	protected void doGet(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
			throws ServletException, IOException {
		String servletPath = servletRequest.getServletPath();
		String serverName = servletRequest.getServerName();

		Environment env = getEnvironment(servletRequest, servletResponse);

		String hostIdentifier = RequestUtil.getHostIdentifier(servletRequest, env);
		Site site = RequestUtil.getSiteByHost(env, hostIdentifier);
		Properties platformProperties = env.getAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG);
		Boolean allowPlainRequests = platformProperties.getBoolean(ALLOW_PLAIN_REQUESTS, true);

		expireSessions(servletRequest, env, site);

		if (site != null) {
			try {
				int requests = ((SiteImpl) site).addRequest();
				LOGGER.debug("site {} currently handles {} requests", site, requests);
				long waited = 0;
				int waitTime = platformProperties.getInteger(Platform.Property.WAIT_TIME, 1000);
				int maxWaitTime = platformProperties.getInteger(Platform.Property.MAX_WAIT_TIME, 30000);

				while (waited < maxWaitTime && (site = RequestUtil.getSiteByName(env, site.getName()))
						.hasState(SiteState.STOPPING, SiteState.STOPPED)) {
					try {
						Thread.sleep(waitTime);
						waited += waitTime;
					} catch (InterruptedException e) {
						LOGGER.error("error while waiting for site to be started", e);
					}
					LOGGER.info("site '{}' is currently beeing stopped, waited {}ms", site, waited);
				}

				while (waited < maxWaitTime
						&& (site = RequestUtil.getSiteByName(env, site.getName())).hasState(SiteState.STARTING)) {
					try {
						Thread.sleep(waitTime);
						waited += waitTime;
					} catch (InterruptedException e) {
						LOGGER.error("error while waiting for site to be started", e);
					}
					LOGGER.info("site '{}' is currently being started, waited {}ms", site, waited);
				}

				if ((site = RequestUtil.getSiteByName(env, site.getName())).hasState(SiteState.STARTED)) {
					boolean enforcePrimaryDomain = site.getProperties()
							.getBoolean(SiteProperties.ENFORCE_PRIMARY_DOMAIN, false);
					if (enforcePrimaryDomain) {
						String primaryDomain = site.getDomain();
						if (!(primaryDomain.startsWith(SCHEME_HTTP + serverName))
								|| (primaryDomain.startsWith(SCHEME_HTTPS + serverName))) {
							Redirect.to(servletResponse, HttpServletResponse.SC_MOVED_PERMANENTLY, primaryDomain);
						}
					}

					PathInfo pathInfo = RequestUtil.getPathInfo(env, site, servletPath);
					setRequestAttributes(servletRequest, env, pathInfo);
					String templatePrefix = platformProperties.getString(Platform.Property.TEMPLATE_PREFIX);

					RequestHandler requestHandler = null;

					Boolean isProxyEnabled = site.getProperties().getBoolean("proxyEnabled", Boolean.valueOf(false));
					if (isProxyEnabled.booleanValue()) {
						requestHandler = new ProxyHandler();
					} else if (("/".equals(servletPath)) || ("".equals(servletPath)) || (null == servletPath)) {
						if (!pathInfo.getDocumentDirectories().isEmpty()) {
							String defaultPage = site.getProperties().getString(SiteProperties.DEFAULT_PAGE);
							String target = pathInfo.getDocumentDirectories().get(0) + SLASH + defaultPage;
							Redirect.to(servletResponse, HttpServletResponse.SC_MOVED_PERMANENTLY, target);
						} else {
							LOGGER.warn(SiteProperties.DOCUMENT_DIR + " is empty for site " + site.getName()
									+ ", can not process request!");
						}
					} else if (pathInfo.isStaticContent() || pathInfo.getServletPath().startsWith(templatePrefix)
							|| pathInfo.isDocument()) {
						requestHandler = new StaticContentHandler(this);
					} else if (pathInfo.isGui()) {
						requestHandler = new GuiHandler();
					} else if (pathInfo.isService()) {
						ApplicationContext ctx = env.getAttribute(Scope.PLATFORM,
								Platform.Environment.CORE_PLATFORM_CONTEXT);
						MarshallService marshallService = ctx.getBean(MarshallService.class);
						PlatformTransformer platformTransformer = ctx.getBean(PlatformTransformer.class);
						requestHandler = new ServiceRequestHandler(marshallService, platformTransformer);
					} else if (pathInfo.isJsp()) {
						requestHandler = jspHandler;
					} else if (ERRORPAGE.equals(servletPath)) {
						requestHandler = new ErrorPageHandler();
					} else {
						if (allowPlainRequests && !pathInfo.isRepository()) {
							super.doGet(servletRequest, servletResponse);
							int status = servletResponse.getStatus();
							LOGGER.debug("returned " + status + " for request " + servletPath);
						} else {
							servletResponse.setStatus(HttpServletResponse.SC_NOT_FOUND);
							LOGGER.debug("was not an internal request, rejecting " + servletPath);
						}
					}
					if (null != requestHandler) {
						if (site.hasState(SiteState.STARTED)) {
							requestHandler.handle(servletRequest, servletResponse, env, site, pathInfo);
							if (pathInfo.isGui()) {
								getEnvironment(servletRequest, servletResponse).setAttribute(SESSION,
										EnvironmentKeys.PREVIOUS_PATH, servletPath);
							}
						} else {
							LOGGER.error("site {} should be STARTED.", site);
							servletResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
						}
					}

				} else {
					LOGGER.error("timeout while waiting for site {}, waited {}ms", site, waited);
					servletResponse.setStatus(HttpStatus.NOT_FOUND.value());
				}
			} finally {
				if (null != site) {
					int requests = ((SiteImpl) site).removeRequest();
					LOGGER.debug("site {} currently handles {} requests", site, requests);
				}
			}

		} else if (allowPlainRequests) {
			LOGGER.debug("no site found for request '" + servletPath + "'");
			super.doGet(servletRequest, servletResponse);
			int status = servletResponse.getStatus();
			LOGGER.debug("returned " + status + " for request " + servletPath);
		} else {
			LOGGER.debug("access to '" + servletPath + "' not allowed");
		}

	}

	private void setRequestAttributes(HttpServletRequest request, Environment env, Path pathInfo) {
		if (!pathInfo.isJsp()) {
			env.setAttribute(REQUEST, EnvironmentKeys.SERVLETPATH, pathInfo.getServletPath());
			String queryString = request.getQueryString();
			if (null != queryString) {
				env.setAttribute(REQUEST, EnvironmentKeys.QUERY_STRING, queryString);
			}
			env.setAttribute(REQUEST, EnvironmentKeys.BASE_URL, pathInfo.getRootPath());
		}
		env.setAttribute(REQUEST, EnvironmentKeys.PATH_INFO, pathInfo);
		Boolean doXsl = !Boolean.FALSE.toString().equalsIgnoreCase(request.getParameter("xsl"));
		Boolean showXsl = Boolean.TRUE.toString().equalsIgnoreCase(request.getParameter("showXsl"));
		env.setAttribute(REQUEST, EnvironmentKeys.DO_XSL, doXsl);
		env.setAttribute(REQUEST, EnvironmentKeys.SHOW_XSL, showXsl);
		String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
		env.setAttribute(REQUEST, HttpHeaders.USER_AGENT, null == userAgent ? "unknown" : userAgent);
	}

	@Override
	public void init() throws ServletException {
		super.init();
		jspHandler = new JspHandler(getServletConfig());
	}

	public Wrapper getWrapper() {
		return null;
	}

	public void setWrapper(Wrapper wrapper) {
		this.manager = ((Context) wrapper.getParent()).getManager();
	}

	protected void expireSessions(HttpServletRequest servletRequest, Environment env, Site site) {
		if (null != manager && Boolean.TRUE.equals(env.removeAttribute(SESSION, EXPIRE_SESSIONS))) {
			List<org.appng.core.controller.Session> sessions = env.getAttribute(Scope.PLATFORM, SessionListener.SESSIONS);
			for (org.appng.core.controller.Session session : sessions) {
				org.apache.catalina.Session containerSession = getContainerSession(session.getId());
				if (null != containerSession) {
					if (session.isExpired()) {
						LOGGER.info("expiring session {}", session.getId());
						containerSession.expire();
					}
				} else {
					LOGGER.debug("session to expire not found in {}: {} (created: {}, last access: {})",
							manager.getClass().getSimpleName(), session.getId(), session.getCreationTime(),
							session.getLastAccessedTime());
					SessionListener.destroySession(session.getId(), env);
				}
			}
		}
	}

	private org.apache.catalina.Session getContainerSession(String sessionId) {
		try {
			return manager.findSession(sessionId);
		} catch (IOException e) {
			LOGGER.warn("errow while retrieving session " + sessionId, e);
		}
		return null;
	}

	public JspHandler getJspHandler() {
		return jspHandler;
	}
}
