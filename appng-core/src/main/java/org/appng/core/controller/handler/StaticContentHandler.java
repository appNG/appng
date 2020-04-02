/*
 * Copyright 2011-2020 the original author or authors.
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
package org.appng.core.controller.handler;

import static org.appng.api.Scope.REQUEST;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.appng.api.Environment;
import org.appng.api.PathInfo;
import org.appng.api.Platform;
import org.appng.api.Scope;
import org.appng.api.SiteProperties;
import org.appng.api.model.Nameable;
import org.appng.api.model.Properties;
import org.appng.api.model.ResourceType;
import org.appng.api.model.Site;
import org.appng.api.support.environment.EnvironmentKeys;
import org.appng.core.Redirect;
import org.appng.core.controller.Controller;
import org.appng.core.model.CacheProvider;

import lombok.extern.slf4j.Slf4j;

/**
 * A {@link RequestHandler} responsible for serving static resources.<br/>
 * Those resources may belong to a template or reside inside a document-folder of a {@link Site} (see
 * {@link SiteProperties#DOCUMENT_DIR} and {@link org.appng.api.Path#isDocument()}).
 * 
 * @author Matthias Müller
 */
@Slf4j
public class StaticContentHandler implements RequestHandler {

	private static final List<String> TEMPLATE_FOLDERS = Arrays.asList("assets", "resources");

	private static final String SLASH = "/";

	private Controller controller;

	public StaticContentHandler(Controller controller) {
		this.controller = controller;
	}

	public void handle(HttpServletRequest servletRequest, HttpServletResponse servletResponse, Environment environment,
			Site site, PathInfo pathInfo) throws ServletException, IOException {
		Properties platformProperties = environment.getAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG);
		String servletPath = pathInfo.getServletPath();
		String repoPath = platformProperties.getString(Platform.Property.REPOSITORY_PATH);
		String templatePrefix = platformProperties.getString(Platform.Property.TEMPLATE_PREFIX);
		String templateFolder = platformProperties.getString(Platform.Property.TEMPLATE_FOLDER);
		String defaultTemplate = platformProperties.getString(Platform.Property.DEFAULT_TEMPLATE);

		String activeTemplate = site.getProperties().getString(SiteProperties.TEMPLATE, defaultTemplate);
		String wwwDir = site.getProperties().getString(SiteProperties.WWW_DIR);
		String defaultPage = site.getProperties().getString(SiteProperties.DEFAULT_PAGE);
		String wwwRootPath = SLASH + repoPath + SLASH + site.getName() + wwwDir;
		String resourcePath = wwwRootPath + servletPath;

		if (pathInfo.isStaticContent()) {
			serveStatic(servletRequest, servletResponse, resourcePath);
		} else if (servletPath.startsWith(templatePrefix)) {
			CacheProvider cacheProvider = new CacheProvider(platformProperties);
			serveTemplateResource(servletRequest, servletResponse, site, templatePrefix, templateFolder, activeTemplate,
					defaultTemplate, repoPath, cacheProvider);
		} else if (pathInfo.isDocument()) {
			if (pathInfo.isRootIgnoreTrailingSlash()) {
				String target = pathInfo.getRootPath() + SLASH + defaultPage;
				Redirect.to(servletResponse, HttpServletResponse.SC_MOVED_PERMANENTLY, servletPath, target);
			} else {
				int idx = servletPath.indexOf('.');
				if (idx < 0 || pathInfo.isJsp()) {
					serveJsp(servletRequest, servletResponse, environment, site, pathInfo, wwwRootPath);
				} else {
					serveStatic(servletRequest, servletResponse, resourcePath);
				}
			}
		}
	}

	private void serveJsp(HttpServletRequest servletRequest, HttpServletResponse servletResponse,
			Environment environment, Site site, PathInfo pathInfo, String wwwRootPath)
			throws ServletException, IOException {
		File wwwRootFile = new File(servletRequest.getServletContext().getRealPath(wwwRootPath));
		final String forwardPath = pathInfo.getForwardPath(wwwRootPath, wwwRootFile);
		List<String> urlParameters = pathInfo.getJspUrlParameters();
		environment.setAttribute(REQUEST, EnvironmentKeys.JSP_URL_PARAMETERS, urlParameters);

		HttpServletRequestWrapper requestWrapper = new HttpServletRequestWrapper(servletRequest) {
			@Override
			public String getServletPath() {
				return forwardPath;
			}
		};
		controller.getJspHandler().handle(requestWrapper, servletResponse, environment, site, pathInfo);
	}

	private int serveStatic(HttpServletRequest servletRequest, HttpServletResponse response, final String forwardPath)
			throws IOException, ServletException {
		HttpServletRequestWrapper requestWrapper = new HttpServletRequestWrapper(servletRequest) {
			@Override
			public String getServletPath() {
				return forwardPath;
			}
		};
		controller.serveResource(requestWrapper, response);
		int status = response.getStatus();
		LOGGER.trace("returned {} for static resource {}", status, forwardPath);
		return status;
	}

	private int serveTemplateResource(HttpServletRequest servletRequest, HttpServletResponse response, Site site,
			String templatePrefix, String templateFolder, String templateName, String defaultTemplate, String repoPath,
			CacheProvider cacheProvider) throws IOException, ServletException {
		String servletPath = servletRequest.getServletPath();
		String[] splitted = servletPath.split(SLASH);
		String[] splittedPrefix = splitted[1].split("_");
		boolean isApplicationResource = splittedPrefix.length > 1;
		String folder = splitted[2];
		if (!isApplicationResource && TEMPLATE_FOLDERS.contains(folder)) {
			String wwwDir = site.getProperties().getString(SiteProperties.WWW_DIR);
			String forwardPath = SLASH + repoPath + SLASH + site.getName() + wwwDir + servletPath;
			return serveStatic(servletRequest, response, forwardPath);
		}
		String path;
		if (isApplicationResource) {
			templatePrefix = SLASH + splitted[1];
			final String applicationName = splittedPrefix[1];
			Nameable application = new Nameable() {

				public String getName() {
					return applicationName;
				}

				public String getDescription() {
					return null;
				}
			};
			String relativePlatformCache = cacheProvider.getRelativePlatformCache(site, application).replaceAll("\\\\",
					SLASH);
			String resourcePath = relativePlatformCache + SLASH + ResourceType.RESOURCE.getFolder();
			path = servletPath.replaceFirst(templatePrefix, resourcePath);
		} else {
			path = servletPath.replaceFirst(templatePrefix, templateFolder + SLASH + templateName);
		}
		return serveStatic(servletRequest, response, path);
	}
}
