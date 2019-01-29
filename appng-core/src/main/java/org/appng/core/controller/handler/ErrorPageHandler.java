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
package org.appng.core.controller.handler;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.appng.api.Environment;
import org.appng.api.Path;
import org.appng.api.PathInfo;
import org.appng.api.Platform;
import org.appng.api.RequestUtil;
import org.appng.api.Scope;
import org.appng.api.SiteProperties;
import org.appng.api.model.Properties;
import org.appng.api.model.Site;
import org.appng.core.controller.HttpHeaders;
import org.appng.core.service.TemplateService;

import lombok.extern.slf4j.Slf4j;

/**
 * A {@link RequestHandler} responsible for providing 404 error-pages.
 * 
 * @author Matthias Müller
 */
@Slf4j
public class ErrorPageHandler implements RequestHandler {

	private static final String SLASH = "/";

	public void handle(HttpServletRequest servletRequest, HttpServletResponse servletResponse, Environment env,
			Site site, PathInfo pathInfo) throws ServletException, IOException {

		if (!pathInfo.getDocumentDirectories().isEmpty()) {
			String extension = pathInfo.getExtension();
			String uri = (String) servletRequest.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
			Properties siteProperties = site.getProperties();
			String errorPageName = siteProperties.getString(SiteProperties.ERROR_PAGE);
			String forwardPath = pathInfo.getDocumentDirectories().get(0) + SLASH + errorPageName + "." + extension;

			Path errorPath = RequestUtil.getPathInfo(env, site, uri);
			if (errorPath.isDocument()) {
				String rootPath = errorPath.getRootPath();
				String[] errorPages = StringUtils.split(siteProperties.getString(SiteProperties.ERROR_PAGES), "|");

				for (String page : errorPages) {
					String[] entry = StringUtils.split(page, "=");
					if (rootPath.equals(entry[0])) {
						forwardPath = entry[0] + SLASH + entry[1] + "." + extension;
						break;
					}
				}
			}

			String wwwDir = siteProperties.getString(SiteProperties.WWW_DIR);
			String wwwRootPath = siteProperties.getString(SiteProperties.SITE_ROOT_DIR) + wwwDir;
			File wwwFile = new File(wwwRootPath, forwardPath);

			if (!wwwFile.canRead()) {
				try (Writer writer = servletResponse.getWriter()) {
					HttpHeaders.setNoCache((HttpServletResponse) servletResponse);

					org.appng.api.model.Properties platformConfig = env.getAttribute(Scope.PLATFORM,
							Platform.Environment.PLATFORM_CONFIG);

					File error404 = new File(TemplateService.getTemplateRepoFolder(platformConfig, siteProperties),
							"/resources/error404.html");
					String errorPage;
					if (error404.canRead()) {
						errorPage = FileUtils.readFileToString(error404, Charset.defaultCharset());
						servletResponse.setContentType(HttpHeaders.CONTENT_TYPE_TEXT_HTML);
					} else {
						String template = siteProperties.getString(SiteProperties.TEMPLATE);
						errorPage = "404 Page Not Found.";
						servletResponse.setContentType(HttpHeaders.CONTENT_TYPE_TEXT_PLAIN);
						LOGGER.error("The template \"{}\" contains no 404 error page. Expected path: {}", template,
								error404);
					}
					writer.write(errorPage);
				}
			} else {
				RequestDispatcher dispatcher = servletRequest.getRequestDispatcher(forwardPath);
				servletRequest.setAttribute(FORWARDED, Boolean.TRUE);
				dispatcher.forward(servletRequest, servletResponse);
			}
		} else {
			LOGGER.warn(SiteProperties.DOCUMENT_DIR + " is empty for site " + site.getName()
					+ ", can not process request!");
		}

	}
}
