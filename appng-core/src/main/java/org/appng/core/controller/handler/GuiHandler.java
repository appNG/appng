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

import static org.appng.api.Scope.PLATFORM;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

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
import org.appng.api.model.Subject;
import org.appng.api.support.ElementHelper;
import org.appng.core.Redirect;
import org.appng.core.controller.HttpHeaders;
import org.appng.core.domain.GroupImpl;
import org.appng.core.model.RequestProcessor;
import org.appng.core.service.TemplateService;
import org.appng.xml.application.Template;
import org.appng.xml.platform.Messages;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
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

	public static final String PLATFORM_MESSAGES = "platformMessages";
	private final File debugFolder;

	public GuiHandler(File debugFolder) {
		this.debugFolder = debugFolder;
	}

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
		Subject subject = env.getSubject();
		if (env.isSubjectAuthenticated()) {

			if (!(pathInfo.hasSite() || pathInfo.hasApplication())) {
				// no site and/or application has been set
				String managerPath = site.getProperties().getString(SiteProperties.MANAGER_PATH);
				String target = getForwardTargetForSite(managerPath, site, subject);
				site.sendRedirect(env, target);
				return;
			}
			boolean isAdmin = env.getSubject().getGroups().stream().filter(g -> ((GroupImpl) g).isDefaultAdmin())
					.findAny().isPresent();

			Messages platformMessages = env.removeAttribute(PLATFORM, PLATFORM_MESSAGES);
			if (isAdmin && null != platformMessages) {
				ElementHelper.addMessages(env, platformMessages);
			}
		}
		StopWatch sw = new StopWatch("process GUI");
		sw.start();

		String siteName = pathInfo.getSiteName();
		Site applicationSite = RequestUtil.getSiteByName(env, siteName);

		// "siteName" might be a reference to a certain application page
		boolean isShortPath = null == applicationSite;
		if (isShortPath) {
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

		// check if user has access to the requested site and application
		Application targetApp = applicationSite.getApplication(applicationName);
		if (!isShortPath && env.isSubjectAuthenticated() && !subject.hasApplication(targetApp)) {
			LOGGER.info("Subject '{}' does not have access to application '{}' on site '{}', trying to redirect.",
					subject.getName(), applicationName, applicationSite.getName());
			String managerPath = applicationSite.getProperties().getString(SiteProperties.MANAGER_PATH);
			// try to find application on same site
			String target = getForwardTargetForSite(managerPath, applicationSite, subject);

			if (managerPath.equals(target)) {
				// try to find application on any other site
				Set<String> remainingSites = new HashSet<>(RequestUtil.getSiteNames(env));
				remainingSites.remove(applicationSite.getName());
				for (String otherSite : remainingSites) {
					target = getForwardTargetForSite(managerPath, RequestUtil.getSiteByName(env, otherSite), subject);
					if (!managerPath.equals(target)) {
						break;
					}
				}
			}
			// Take it on the other side...Take it on, take it on
			site.sendRedirect(env, target, HttpStatus.FOUND.value());
			return;
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

			final String result = processor.processWithTemplate(applicationSite, debugFolder);

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

	private String getForwardTargetForSite(String managerPath, Site site, Subject subject) {
		Application targetApp = null;
		String applicationName = site.getProperties().getString(SiteProperties.DEFAULT_APPLICATION);
		Application defaultApp = site.getApplication(applicationName);
		if (null != defaultApp && !defaultApp.isHidden() && subject.hasApplication(defaultApp)) {
			targetApp = defaultApp;
		} else {
			for (Application application : site.getApplications()) {
				if (!application.isHidden() && subject.hasApplication(application)) {
					targetApp = application;
					break;
				}
			}
		}
		return null == targetApp ? managerPath
				: String.format("%s/%s/%s", managerPath, site.getName(), targetApp.getName());
	}

}
