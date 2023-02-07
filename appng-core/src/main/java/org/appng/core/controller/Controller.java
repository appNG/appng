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
package org.appng.core.controller;

import static org.appng.api.Scope.REQUEST;
import static org.appng.api.Scope.SESSION;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.catalina.ContainerServlet;
import org.apache.catalina.Context;
import org.apache.catalina.Manager;
import org.apache.catalina.Wrapper;
import org.apache.catalina.servlets.DefaultServlet;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.appng.api.Environment;
import org.appng.api.Path;
import org.appng.api.PathInfo;
import org.appng.api.Platform;
import org.appng.api.RequestUtil;
import org.appng.api.Scope;
import org.appng.api.SiteProperties;
import org.appng.api.messaging.Messaging;
import org.appng.api.model.Properties;
import org.appng.api.model.Site;
import org.appng.api.model.Site.SiteState;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.api.support.environment.EnvironmentKeys;
import org.appng.core.Redirect;
import org.appng.core.controller.filter.EnvironmentFilter;
import org.appng.core.controller.handler.ErrorPageHandler;
import org.appng.core.controller.handler.GuiHandler;
import org.appng.core.controller.handler.JspHandler;
import org.appng.core.controller.handler.MonitoringHandler;
import org.appng.core.controller.handler.RequestHandler;
import org.appng.core.controller.handler.ServiceRequestHandler;
import org.appng.core.controller.handler.StaticContentHandler;
import org.appng.core.domain.SiteImpl;
import org.appng.xml.MarshallService;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import lombok.extern.slf4j.Slf4j;

/**
 * The controller {@link Servlet} of appNG, delegating {@link HttpServletRequest}s to an appropriate
 * {@link RequestHandler}.
 * 
 * @author Matthias Müller
 */
@Slf4j
@WebServlet(name = "controller", urlPatterns = { "/", "*.jsp" }, loadOnStartup = 1)
public class Controller extends DefaultServlet implements ContainerServlet {

	protected static final String HEADER_VERSION = "X-appNG-Version";
	protected static final String HEADER_SITE = "X-appNG-Site";
	protected static final String HEADER_NODE = "X-appNG-Node";

	private static final String SLASH = "/";

	private static final String SCHEME_HTTPS = "https://";
	private static final String SCHEME_HTTP = "http://";

	protected JspHandler jspHandler;
	protected MonitoringHandler monitoringHandler;
	private GuiHandler guiHandler;
	private ErrorPageHandler errorHandler;
	private StaticContentHandler staticContentHandler;
	private ServiceRequestHandler serviceRequestHandler;

	private Manager manager;
	protected final byte[] loadingScreen;

	public Controller() {
		LOGGER.info("Controller created");
		try (InputStream is = getClass().getResourceAsStream("loading.html")) {
			loadingScreen = IOUtils.toByteArray(is);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	protected void doHead(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		doGet(request, wrapResponseForHeadRequest(response));
	}

	private HttpServletResponse wrapResponseForHeadRequest(HttpServletResponse response) {
		return new HttpServletResponseWrapper(response) {
			@Override
			public ServletOutputStream getOutputStream() throws IOException {
				return new ServletOutputStream() {

					public void write(int b) throws IOException {
					}

					public boolean isReady() {
						return false;
					}

					public void setWriteListener(WriteListener listener) {
					}
				};
			}

			@Override
			public PrintWriter getWriter() throws IOException {
				return new PrintWriter(NullOutputStream.NULL_OUTPUT_STREAM);
			}
		};
	}

	public void serveResource(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		serveResource(request, response, true, null);
	}

	@Override
	protected void doPut(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
			throws ServletException, IOException {
		if (isServiceRequest(servletRequest)) {
			doGet(servletRequest, servletResponse);
		} else {
			LOGGER.debug("PUT not allowed for {}", servletRequest.getServletPath());
			servletResponse.sendError(HttpStatus.FORBIDDEN.value());
		}
	}

	@Override
	protected void doDelete(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
			throws ServletException, IOException {
		if (isServiceRequest(servletRequest)) {
			doGet(servletRequest, servletResponse);
		} else {
			LOGGER.debug("DELETE not allowed for {}", servletRequest.getServletPath());
			servletResponse.sendError(HttpStatus.FORBIDDEN.value());
		}
	}

	private boolean isServiceRequest(HttpServletRequest servletRequest) {
		DefaultEnvironment env = EnvironmentFilter.environment();
		Site site = env.getSite();
		return RequestUtil.getPathInfo(env, site, servletRequest.getServletPath()).isService();
	}

	@Override
	protected void doGet(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
			throws ServletException, IOException {
		Environment env = getEnvironment();
		Properties platformProperties = env.getAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG);
		Site site = env.getSite();

		if (null == site) {
			servletResponse.setStatus(HttpStatus.NOT_FOUND.value());
			return;
		}

		addDebugHeaders(servletResponse, env, site);

		String servletPath = servletRequest.getServletPath();
		PathInfo pathInfo = RequestUtil.getPathInfo(env, site, servletPath);
		if (pathInfo.isMonitoring()) {
			monitoringHandler.handle(servletRequest, servletResponse, env, site, pathInfo);
			return;
		}

		if (SiteState.STARTING.equals(site.getState())) {
			servletResponse.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
			servletResponse.setContentType(MediaType.TEXT_HTML_VALUE);
			String siteLoadingScreen = site.getProperties().getClob(SiteProperties.LOADING_SCREEN);
			if (StringUtils.isNotBlank(siteLoadingScreen)) {
				servletResponse.setContentLength(siteLoadingScreen.getBytes(StandardCharsets.UTF_8).length);
				servletResponse.getWriter().write(siteLoadingScreen);
			} else {
				servletResponse.setContentLength(loadingScreen.length);
				servletResponse.getOutputStream().write(loadingScreen);
			}
			return;
		}

		if (!site.hasState(SiteState.STARTED, SiteState.STANDBY)) {
			servletResponse.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
			String maintenanceScreen = site.getProperties().getClob(Platform.Property.MAINTENANCE_SCREEN,
					platformProperties.getClob(Platform.Property.MAINTENANCE_SCREEN));
			if (StringUtils.isNotBlank(maintenanceScreen)) {
				servletResponse.setContentLength(maintenanceScreen.getBytes(StandardCharsets.UTF_8).length);
				servletResponse.getWriter().write(maintenanceScreen);
			}
			return;
		}

		boolean enforcePrimaryDomain = site.getProperties().getBoolean(SiteProperties.ENFORCE_PRIMARY_DOMAIN, false);
		if (enforcePrimaryDomain) {
			String primaryDomain = site.getDomain();
			String serverName = servletRequest.getServerName();
			if (!(primaryDomain.startsWith(SCHEME_HTTP + serverName))
					|| (primaryDomain.startsWith(SCHEME_HTTPS + serverName))) {
				Redirect.to(servletResponse, HttpServletResponse.SC_MOVED_PERMANENTLY, primaryDomain);
				return;
			}
		}

		if ((Path.SEPARATOR.equals(servletPath)) || StringUtils.isBlank(servletPath)) {
			if (!pathInfo.getDocumentDirectories().isEmpty()) {
				String defaultPage = site.getProperties().getString(SiteProperties.DEFAULT_PAGE);
				String target = pathInfo.getDocumentDirectories().get(0) + SLASH + defaultPage;
				Redirect.to(servletResponse, HttpServletResponse.SC_MOVED_PERMANENTLY, target);
			} else {
				LOGGER.warn("{} is empty for site {}, can not process request!", SiteProperties.DOCUMENT_DIR,
						site.getName());
			}
			return;
		}

		try {
			int requests = ((SiteImpl) site).addRequest();
			LOGGER.debug("site {} currently handles {} requests", site, requests);

			RequestHandler requestHandler = null;
			String templatePrefix = platformProperties.getString(Platform.Property.TEMPLATE_PREFIX);
			if (pathInfo.isDocument() || pathInfo.isStaticContent()
					|| pathInfo.getServletPath().startsWith(templatePrefix)) {
				requestHandler = staticContentHandler;
			} else if (pathInfo.isGui()) {
				requestHandler = guiHandler;
			} else if (pathInfo.isService()) {
				requestHandler = serviceRequestHandler;
			} else if (pathInfo.isJsp()) {
				requestHandler = jspHandler;
			} else {
				requestHandler = errorHandler;
			}

			if (null != requestHandler) {
				if (site.hasState(SiteState.STARTED, SiteState.STANDBY)) {
					setRequestAttributes(servletRequest, env, pathInfo);
					requestHandler.handle(servletRequest, servletResponse, env, site, pathInfo);
					if (pathInfo.isGui() && servletRequest.isRequestedSessionIdValid()) {
						env.setAttribute(SESSION, EnvironmentKeys.PREVIOUS_PATH, servletPath);
					}
				} else {
					LOGGER.error("site {} should be STARTED.", site);
					servletResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
				}
			}

		} finally {
			if (null != site) {
				int requests = ((SiteImpl) site).removeRequest();
				LOGGER.debug("site {} currently handles {} requests", site, requests);
			}
		}

	}

	private void addDebugHeaders(HttpServletResponse servletResponse, Environment env, Site site) {
		if (site.getProperties().getBoolean(SiteProperties.SET_DEBUG_HEADERS, false)) {
			servletResponse.setHeader(HEADER_NODE, Messaging.getNodeId(env));
			servletResponse.setHeader(HEADER_VERSION,
					env.getAttribute(Scope.PLATFORM, Platform.Environment.APPNG_VERSION));
			servletResponse.setHeader(HEADER_SITE, site.getName());
		}
	}

	protected Environment getEnvironment() {
		return EnvironmentFilter.environment();
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
		Boolean render = !Boolean.FALSE.toString().equalsIgnoreCase(request.getParameter("render"));
		env.setAttribute(REQUEST, EnvironmentKeys.RENDER, render);
		String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
		env.setAttribute(REQUEST, HttpHeaders.USER_AGENT, null == userAgent ? "unknown" : userAgent);
	}

	@Override
	public void init() throws ServletException {
		super.init();
		Environment globalEnv = DefaultEnvironment.getGlobal();
		Properties platformProperties = globalEnv.getAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG);
		String appngData = platformProperties.getString(org.appng.api.Platform.Property.APPNG_DATA);
		File debugFolder = new File(appngData, "debug").getAbsoluteFile();
		jspHandler = new JspHandler(getServletConfig());
		monitoringHandler = new MonitoringHandler();
		guiHandler = new GuiHandler(debugFolder);
		errorHandler = new ErrorPageHandler();
		staticContentHandler = new StaticContentHandler(this);
		ApplicationContext ctx = globalEnv.getAttribute(Scope.PLATFORM, Platform.Environment.CORE_PLATFORM_CONTEXT);
		serviceRequestHandler = new ServiceRequestHandler(ctx.getBean(MarshallService.class));
	}

	public Wrapper getWrapper() {
		return null;
	}

	public void setWrapper(Wrapper wrapper) {
		Context context = ((Context) wrapper.getParent());
		this.manager = context.getManager();
		context.getServletContext().setAttribute(SessionListener.SESSION_MANAGER, manager);
		Environment env = DefaultEnvironment.getGlobal();
		Properties platformConfig = env.getAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG);
		Integer sessionTimeout = platformConfig.getInteger(Platform.Property.SESSION_TIMEOUT);
		context.setSessionTimeout((int) TimeUnit.SECONDS.toMinutes(sessionTimeout));
	}

	public JspHandler getJspHandler() {
		return jspHandler;
	}

}
