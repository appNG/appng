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

import static org.appng.api.Scope.PLATFORM;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBException;

import org.appng.api.Environment;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.PathInfo;
import org.appng.api.Platform;
import org.appng.api.RequestUtil;
import org.appng.api.Scope;
import org.appng.api.SiteProperties;
import org.appng.api.model.Application;
import org.appng.api.model.Properties;
import org.appng.api.model.Site;
import org.appng.core.Redirect;
import org.appng.core.controller.HttpHeaders;
import org.appng.core.model.RequestProcessor;
import org.appng.core.service.TemplateService;
import org.appng.xml.application.Template;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StopWatch;

import lombok.extern.slf4j.Slf4j;

/**
 * A {@link RequestHandler} responsible for handling requests to the appNG GUI provided by the several
 * {@link Application}s of a {@link Site}.
 * 
 * @author Matthias Müller
 * 
 * @see RequestProcessor
 */
@Slf4j
public class GuiHandler implements RequestHandler {

	public void handle(HttpServletRequest servletRequest, HttpServletResponse servletResponse, Environment environment,
			Site site, PathInfo pathInfo) throws ServletException, IOException {
		Properties platformProperties = environment.getAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG);
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		try {
			String siteRoot = site.getProperties().getString(SiteProperties.SITE_ROOT_DIR);
			String siteWwwDir = site.getProperties().getString(SiteProperties.WWW_DIR);
			String templatePrefix = platformProperties.getString(Platform.Property.TEMPLATE_PREFIX);
			String templateDir = siteRoot + siteWwwDir + templatePrefix;

			processGui(servletRequest, servletResponse, environment, site, platformProperties, pathInfo, templateDir);
		} catch (InvalidConfigurationException e) {
			Site errorSite = e.getSite();
			if (null != errorSite && !errorSite.equals(site)) {
				String guiPath = site.getProperties().getString(SiteProperties.MANAGER_PATH);
				LOGGER.warn(String.format("application '%s' not found for site '%s', redirecting to %s",
						e.getApplicationName(), errorSite.getName(), guiPath), e);
				Redirect.to(servletResponse, HttpServletResponse.SC_MOVED_PERMANENTLY, guiPath);
			} else {
				LOGGER.error("error while processing appNG GUI", e);
			}
		}
		Thread.currentThread().setContextClassLoader(contextClassLoader);
	}

	private void processGui(HttpServletRequest servletRequest, HttpServletResponse servletResponse, Environment env,
			Site site, Properties platformProperties, PathInfo pathInfo, String templateDir)
			throws IOException, InvalidConfigurationException {
		if (env.isSubjectAuthenticated() && !(pathInfo.hasSite() || pathInfo.hasApplication())) {
			String applicationName = site.getProperties().getString(SiteProperties.DEFAULT_APPLICATION);
			if (!site.hasApplication(applicationName)) {
				Iterator<Application> iterator = site.getApplications().iterator();
				while (iterator.hasNext()) {
					Application application = iterator.next();
					if (!application.isHidden()) {
						applicationName = application.getName();
						break;
					}
				}
			}
			site.sendRedirect(env, applicationName);
			return;
		}

		StopWatch sw = new StopWatch("process GUI");
		sw.start();

		String siteName = pathInfo.getSiteName();
		Site applicationSite = RequestUtil.getSiteByName(env, siteName);

		// "siteName" might be a reference to a certain application page
		if (null == applicationSite) {
			// assume site is current site
			applicationSite = site;
		}

		ClassLoader siteClassLoader = applicationSite.getSiteClassLoader();
		Thread.currentThread().setContextClassLoader(siteClassLoader);

		String applicationName = pathInfo.getApplicationName();
		boolean hasApplication = applicationSite.hasApplication(applicationName);
		if (hasApplication) {
			LOGGER.debug("calling application {}", applicationName);
		} else {
			applicationName = applicationSite.getProperties().getString(SiteProperties.DEFAULT_APPLICATION);
			LOGGER.debug("no application set, using default '{}'", applicationName);
			pathInfo.setApplicationName(applicationName);
		}

		HttpHeaders.setNoCache(servletResponse);

		ApplicationContext ctx = env.getAttribute(PLATFORM, Platform.Environment.CORE_PLATFORM_CONTEXT);

		TemplateService templateService = ctx.getBean(TemplateService.class);
		try {
			String requestProcessorBeanName;
			Template template = templateService.getTemplate(templateDir);
			switch (template.getType()) {
			case THYMELEAF:
				requestProcessorBeanName = "thymeleafProcessor";
				break;
			default:
				requestProcessorBeanName = "requestProcessor";
			}
			RequestProcessor processor = ctx.getBean(requestProcessorBeanName, RequestProcessor.class);
			processor.init(servletRequest, servletResponse, pathInfo, templateDir);

			final String result = processor.processWithTemplate(applicationSite);

			servletResponse.setContentType(processor.getContentType());
			servletResponse.setContentLength(processor.getContentLength());
			PrintWriter out = servletResponse.getWriter();
			out.println(result);
			out.flush();
			out.close();
			sw.stop();
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(sw.prettyPrint());
			}
		} catch (JAXBException e) {
			throw new IOException(e);
		}
	}

}
