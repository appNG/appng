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
package org.appng.core.controller.rest.openapi;

import static org.appng.api.Scope.PLATFORM;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.ApplicationConfigProvider;
import org.appng.api.Environment;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.Platform;
import org.appng.api.ProcessingException;
import org.appng.api.Request;
import org.appng.api.SiteProperties;
import org.appng.api.model.Application;
import org.appng.api.model.Properties;
import org.appng.api.model.Site;
import org.appng.api.support.DollarParameterSupport;
import org.appng.api.support.ResourceBundleMessageSource;
import org.appng.core.model.ApplicationProvider;
import org.appng.openapi.model.Navigation;
import org.appng.openapi.model.NavigationItem;
import org.appng.xml.platform.Linkpanel;
import org.slf4j.Logger;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
abstract class OpenApiNavigation extends OpenApiOperation {

	public OpenApiNavigation(Site site, Application application, Request request, MessageSource messageSource,
			boolean supportPathParameters) throws JAXBException {
		super(site, application, request, messageSource, supportPathParameters);
	}

	@GetMapping(path = "/openapi/navigation")
	public ResponseEntity<Navigation> getNavigation(HttpServletRequest servletReq, HttpServletResponse servletResp)
			throws JAXBException, InvalidConfigurationException, ProcessingException {
		Navigation navigation = new Navigation();
		Environment env = request.getEnvironment();
		org.appng.api.model.Subject subject = env.getSubject();

		Map<String, Site> sites = env.getAttribute(PLATFORM, Platform.Environment.SITES);
		Properties properties = site.getProperties();
		Application authApp = site.getApplication(properties.getString(SiteProperties.AUTH_APPLICATION));
		String managerPrefix = properties.getString(SiteProperties.MANAGER_PATH) + "/";
		String authAppPath = managerPrefix + site.getName() + "/" + authApp.getName() + "/";

		if (null != subject && subject.isAuthenticated()) {
			for (String siteName : sites.keySet()) {
				Site navSite = sites.get(siteName);
				NavigationItem siteNavigation = new NavigationItem();
				siteNavigation.setType(NavigationItem.TypeEnum.SITE);
				siteNavigation.setName(navSite.getName());
				for (Application app : navSite.getApplications()) {
					if (!app.isHidden() && subject.hasApplication(app)) {
						LOGGER.debug("{} on site {}", app, navSite);
						ApplicationProvider applicationProvider = (ApplicationProvider) app;
						ApplicationConfigProvider applicationConfig = applicationProvider.getApplicationConfig();
						String defaultPage = applicationConfig.getDefaultPage();

						NavigationItem appItem = new NavigationItem();
						appItem.setType(NavigationItem.TypeEnum.APP);
						appItem.setName(app.getDisplayName());
						appItem.setSelf(getSelf(applicationProvider.getName(), "/page") + defaultPage);
						appItem.setPath(managerPrefix + navSite.getName() + "/" + app.getName());
						siteNavigation.addItemsItem(appItem);

						if (site.getName().equals(navSite.getName()) && app.getName().equals(application.getName())) {
							appItem.setActive(true);
							siteNavigation.setActive(true);

							Linkpanel topNav = applicationConfig.getApplicationRootConfig().getNavigation();
							for (org.appng.xml.platform.Link link : topNav.getLinks()) {
								NavigationItem pageItem = new NavigationItem();
								pageItem.setType(NavigationItem.TypeEnum.PAGE);
								ResourceBundleMessageSource messages = application
										.getBean(ResourceBundleMessageSource.class);
								pageItem.setName(getLabelMessage(link.getLabel(), messages, env.getLocale(),
										new DollarParameterSupport()));
								pageItem.setPath(appItem.getPath() + link.getTarget());
								pageItem.setSelf(getSelf("/page" + link.getTarget()).toString());
								String pageName = link.getTarget().substring(1);
								int pathSeparator = pageName.indexOf('/');
								if (pathSeparator > 0) {
									pageName = pageName.substring(0, pathSeparator);
								}
								pageItem.setDefault(StringUtils.equals(defaultPage, pageName));
								appItem.addItemsItem(pageItem);
							}
						}

					}
					sortItems(siteNavigation.getItems());
				}
				if (null != siteNavigation.getItems()) {
					navigation.addItemsItem(siteNavigation);
					sortItems(navigation.getItems());
				}
			}
			String logoutPage = properties.getString(SiteProperties.AUTH_LOGOUT_PAGE);
			String logoutRef = properties.getString(SiteProperties.AUTH_LOGOUT_REF);
			NavigationItem logoutItem = new NavigationItem();
			logoutItem.setName("Logout");
			logoutItem.setType(NavigationItem.TypeEnum.PAGE);
			logoutItem.setPath(managerPrefix + logoutRef);
			logoutItem.setSelf(getSelf(authApp.getName(), "/page/" + logoutPage).toString());
			navigation.addItemsItem(logoutItem);

		} else {
			String loginPage = properties.getList(SiteProperties.AUTH_LOGIN_PAGE, ",").get(0);
			String loginRef = properties.getString(SiteProperties.AUTH_LOGIN_REF);
			NavigationItem loginItem = new NavigationItem();
			loginItem.setName("Login");
			loginItem.setType(NavigationItem.TypeEnum.PAGE);
			loginItem.setPath(authAppPath + loginPage + "/" + loginRef);
			loginItem.setSelf(getSelf(authApp.getName(), "/page/" + loginPage).toString());
			navigation.addItemsItem(loginItem);
		}
		navigation.setUser(getUser(env));
		return new ResponseEntity<Navigation>(navigation, HttpStatus.OK);
	}

	private void sortItems(List<NavigationItem> items) {
		if (null != items) {
			Collections.sort(items, (i1, i2) -> i1.getName().compareTo(i2.getName()));
		}
	}

	Logger getLogger() {
		return LOGGER;
	}
}
