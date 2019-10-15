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
package org.appng.core.controller;

import static org.appng.api.Scope.REQUEST;
import static org.appng.api.Scope.SESSION;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
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
import org.apache.commons.io.output.NullOutputStream;
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
import org.appng.core.controller.handler.RequestHandler;
import org.appng.core.controller.handler.ServiceRequestHandler;
import org.appng.core.controller.handler.StaticContentHandler;
import org.appng.core.domain.SiteImpl;
import org.appng.core.model.PlatformTransformer;
import org.appng.xml.MarshallService;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;

import lombok.extern.slf4j.Slf4j;

/**
 * The controller {@link Servlet} of appNG, delegating
 * {@link HttpServletRequest}s to an appropriate {@link RequestHandler}.
 * 
 * @author Matthias Müller
 */
@Slf4j
@WebServlet(name = "controller", urlPatterns = { "/", "*.jsp" }, loadOnStartup = 1)
public class Controller extends DefaultServlet implements ContainerServlet {

	private static final String ALLOW_PLAIN_REQUESTS = "allowPlainRequests";
	private static final String ERRORPAGE = "/errorpage";
	private static final String SLASH = "/";

	private static final String SCHEME_HTTPS = "https://";
	private static final String SCHEME_HTTP = "http://";

	protected static final String SESSION_MANAGER = "sessionManager";

	protected JspHandler jspHandler;

	private Manager manager;

	public Controller() {
		LOGGER.info("Controller created");
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
				return new PrintWriter(new NullOutputStream());
			}
		};
	}

	public void serveResource(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		serveResource(request, response, true, null);
	}

	protected Environment getEnvironment(HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
		return DefaultEnvironment.get(getServletContext(), servletRequest, servletResponse);
	}

	@Override
	protected void doPut(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
			throws ServletException, IOException {
		if (isServiceRequest(servletRequest, servletResponse)) {
			doGet(servletRequest, servletResponse);
		} else {
			LOGGER.debug("PUT not allowed for {}", servletRequest.getServletPath());
			servletResponse.sendError(HttpStatus.FORBIDDEN.value());
		}
	}

	@Override
	protected void doDelete(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
			throws ServletException, IOException {
		if (isServiceRequest(servletRequest, servletResponse)) {
			doGet(servletRequest, servletResponse);
		} else {
			LOGGER.debug("DELETE not allowed for {}", servletRequest.getServletPath());
			servletResponse.sendError(HttpStatus.FORBIDDEN.value());
		}
	}

	private boolean isServiceRequest(HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
		Environment env = DefaultEnvironment.get(getServletContext());
		Site site = RequestUtil.getSiteByHost(env, RequestUtil.getHostIdentifier(servletRequest, env));
		return RequestUtil.getPathInfo(env, site, servletRequest.getServletPath()).isService();
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

		expireSessions(env, site);

		if (site != null) {
			try {
				int requests = ((SiteImpl) site).addRequest();
				LOGGER.debug("site {} currently handles {} requests", site, requests);
				site = RequestUtil.waitForSite(env, site.getName());

				if (site.hasState(SiteState.STARTED)) {
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
					String appngData = platformProperties.getString(org.appng.api.Platform.Property.APPNG_DATA);
					File debugFolder = new File(appngData, "debug").getAbsoluteFile();
					if (!(debugFolder.exists() || debugFolder.mkdirs())) {
						LOGGER.warn("Failed to create {}", debugFolder.getPath());
					}

					if (("/".equals(servletPath)) || ("".equals(servletPath)) || (null == servletPath)) {
						if (!pathInfo.getDocumentDirectories().isEmpty()) {
							String defaultPage = site.getProperties().getString(SiteProperties.DEFAULT_PAGE);
							String target = pathInfo.getDocumentDirectories().get(0) + SLASH + defaultPage;
							Redirect.to(servletResponse, HttpServletResponse.SC_MOVED_PERMANENTLY, target);
						} else {
							LOGGER.warn("{} is empty for site {}, can not process request!",
									SiteProperties.DOCUMENT_DIR, site.getName());
						}
					} else if (pathInfo.isStaticContent() || pathInfo.getServletPath().startsWith(templatePrefix)
							|| pathInfo.isDocument()) {
						requestHandler = new StaticContentHandler(this);
					} else if (pathInfo.isGui()) {
						requestHandler = new GuiHandler(debugFolder);
					} else if (pathInfo.isService()) {
						ApplicationContext ctx = env.getAttribute(Scope.PLATFORM,
								Platform.Environment.CORE_PLATFORM_CONTEXT);
						MarshallService marshallService = ctx.getBean(MarshallService.class);
						PlatformTransformer platformTransformer = ctx.getBean(PlatformTransformer.class);
						requestHandler = new ServiceRequestHandler(marshallService, platformTransformer, debugFolder);
					} else if (pathInfo.isJsp()) {
						requestHandler = jspHandler;
					} else if (ERRORPAGE.equals(servletPath)) {
						requestHandler = new ErrorPageHandler();
					} else {
						if (allowPlainRequests && !pathInfo.isRepository()) {
							super.doGet(servletRequest, servletResponse);
							int status = servletResponse.getStatus();
							LOGGER.debug("returned {} for request {}", status, servletPath);
						} else {
							servletResponse.setStatus(HttpServletResponse.SC_NOT_FOUND);
							LOGGER.debug("was not an internal request, rejecting {}", servletPath);
						}
					}
					if (null != requestHandler) {
						if (site.hasState(SiteState.STARTED)) {
							requestHandler.handle(servletRequest, servletResponse, env, site, pathInfo);
							if (pathInfo.isGui() && servletRequest.isRequestedSessionIdValid()) {
								getEnvironment(servletRequest, servletResponse).setAttribute(SESSION,
										EnvironmentKeys.PREVIOUS_PATH, servletPath);
							}
						} else {
							LOGGER.error("site {} should be STARTED.", site);
							servletResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
						}
					}

				} else {
					LOGGER.error("timeout while waiting for site {}", site);
					servletResponse.setStatus(HttpStatus.NOT_FOUND.value());
				}
			} finally {
				if (null != site) {
					int requests = ((SiteImpl) site).removeRequest();
					LOGGER.debug("site {} currently handles {} requests", site, requests);
				}
			}

		} else if (allowPlainRequests) {
			LOGGER.debug("no site found for request '{}'", servletPath);
			super.doGet(servletRequest, servletResponse);
			int status = servletResponse.getStatus();
			LOGGER.debug("returned {} for request '{}'", status, servletPath);
		} else {
			LOGGER.debug("access to '{}' not allowed", servletPath);
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
		Boolean render = !Boolean.FALSE.toString().equalsIgnoreCase(request.getParameter("render"));
		env.setAttribute(REQUEST, EnvironmentKeys.RENDER, render);
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
		Context context = ((Context) wrapper.getParent());
		this.manager = context.getManager();
		context.getServletContext().setAttribute(SESSION_MANAGER, manager);
		Environment env = DefaultEnvironment.get(context.getServletContext());
		Properties platformConfig = env.getAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG);
		Integer sessionTimeout = platformConfig.getInteger(Platform.Property.SESSION_TIMEOUT);
		context.setSessionTimeout((int) TimeUnit.SECONDS.toMinutes(sessionTimeout));
	}

	protected void expireSessions(Environment env, Site site) {
		SessionListener.expire(manager, env, site);
	}

	public JspHandler getJspHandler() {
		return jspHandler;
	}

}
