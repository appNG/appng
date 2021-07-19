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
import static org.appng.api.Scope.SESSION;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.ApplicationConfigProvider;
import org.appng.api.Environment;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.Path;
import org.appng.api.Platform;
import org.appng.api.ProcessingException;
import org.appng.api.Request;
import org.appng.api.Scope;
import org.appng.api.Session;
import org.appng.api.SiteProperties;
import org.appng.api.model.Application;
import org.appng.api.model.Properties;
import org.appng.api.model.Site;
import org.appng.api.support.DollarParameterSupport;
import org.appng.api.support.ResourceBundleMessageSource;
import org.appng.api.support.environment.EnvironmentKeys;
import org.appng.core.model.ApplicationProvider;
import org.appng.openapi.model.Action;
import org.appng.openapi.model.Datasource;
import org.appng.openapi.model.Label;
import org.appng.openapi.model.Message;
import org.appng.openapi.model.Navigation;
import org.appng.openapi.model.NavigationItem;
import org.appng.openapi.model.PageDefinition;
import org.appng.openapi.model.Parameter;
import org.appng.openapi.model.Section;
import org.appng.openapi.model.SectionElement;
import org.appng.openapi.model.User;
import org.appng.xml.MarshallService;
import org.appng.xml.platform.Linkpanel;
import org.appng.xml.platform.Messages;
import org.appng.xml.platform.Param;
import org.appng.xml.platform.SectionConfig;
import org.appng.xml.platform.UrlSchema;
import org.slf4j.Logger;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
abstract class OpenApiPage extends OpenApiOperation {

	private OpenApiDataSource openApiDataSource;
	private OpenApiAction openApiAction;

	public OpenApiPage(Site site, Application application, Request request, MessageSource messageSource,
			boolean supportPathParameters, OpenApiDataSource openApiDataSource, OpenApiAction openApiAction)
			throws JAXBException {
		super(site, application, request, messageSource, supportPathParameters);
		this.openApiDataSource = openApiDataSource;
		this.openApiAction = openApiAction;
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
						NavigationItem applicationItem = new NavigationItem();
						applicationItem.setType(NavigationItem.TypeEnum.APP);
						applicationItem.setName(app.getDisplayName());
						siteNavigation.addItemsItem(applicationItem);

						applicationItem.setPath(managerPrefix + navSite.getName() + "/" + app.getName());

						if (site.getName().equals(navSite.getName()) && app.getName().equals(application.getName())) {
							applicationItem.setActive(true);
							siteNavigation.setActive(true);

							ApplicationConfigProvider applicationConfig = ((ApplicationProvider) application)
									.getApplicationConfig();
							Linkpanel topNav = applicationConfig.getApplicationRootConfig().getNavigation();
							for (org.appng.xml.platform.Link link : topNav.getLinks()) {
								NavigationItem topNavItem = new NavigationItem();
								topNavItem.setType(NavigationItem.TypeEnum.PAGE);
								ResourceBundleMessageSource messages = application
										.getBean(ResourceBundleMessageSource.class);
								topNavItem.setName(getLabelMessage(link.getLabel(), messages, env.getLocale(),
										new DollarParameterSupport()));
								topNavItem.setPath(applicationItem.getPath() + link.getTarget());
								applicationItem.addItemsItem(topNavItem);
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
			String logoutRef = properties.getString(SiteProperties.AUTH_LOGOUT_REF);
			NavigationItem logoutItem = new NavigationItem();
			logoutItem.setName("Logout");
			logoutItem.setType(NavigationItem.TypeEnum.PAGE);
			logoutItem.setPath(managerPrefix + logoutRef);
			navigation.addItemsItem(logoutItem);

		} else {
			String loginPage = properties.getList(SiteProperties.AUTH_LOGIN_PAGE, ",").get(0);
			String loginRef = properties.getString(SiteProperties.AUTH_LOGIN_REF);
			NavigationItem loginItem = new NavigationItem();
			loginItem.setName("Login");
			loginItem.setType(NavigationItem.TypeEnum.PAGE);
			loginItem.setPath(authAppPath + loginPage + "/" + loginRef);
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

	@GetMapping(path = "/openapi/page/{id}/**", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<PageDefinition> getPage(
	// @formatter:off
		@PathVariable(name = "id") String pageId,
		@RequestParam(required = false, name = "_sect") String[] sections,
		Environment env,
		HttpServletRequest servletReq,
		HttpServletResponse servletResp
	// @formatter:on
	) throws JAXBException, InvalidConfigurationException, ProcessingException {

		ApplicationProvider applicationProvider = (ApplicationProvider) application;
		org.appng.xml.platform.PageDefinition originalPage = applicationProvider.getApplicationConfig().getPage(pageId);

		if (null == originalPage) {
			LOGGER.debug("Page {} not found on application {} of site {}", pageId, application.getName(),
					site.getName());
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		String pageUrlParams = (String) ((HttpServletRequest) servletReq)
				.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
		int pathLength = ("/openapi/page/" + pageId).length();
		if (pageUrlParams.length() > pathLength) {
			pageUrlParams = pageUrlParams.substring(pathLength + 1);
		} else {
			pageUrlParams = null;
		}

		List<Parameter> currentUrlParams = new ArrayList<>();
		if (StringUtils.isNotBlank(pageUrlParams)) {
			String[] pathSegments = pageUrlParams.split("/");
			UrlSchema urlSchema = originalPage.getConfig().getUrlSchema();
			List<Param> paramList = urlSchema.getUrlParams().getParamList();
			int i = 0;
			for (String paramValue : pathSegments) {
				if (i < paramList.size()) {
					String paramName = paramList.get(i).getName();
					request.addParameter(paramName, paramValue);
					Parameter param = new Parameter();
					param.setName(paramName);
					param.setValue(paramValue);
					currentUrlParams.add(param);
					i++;
				}
			}
		}

		MarshallService marshallService = MarshallService.getMarshallService();
		Path pathInfo = env.getAttribute(Scope.REQUEST, EnvironmentKeys.PATH_INFO);
		List<String> sectionIds = null == sections ? Collections.emptyList() : Arrays.asList(sections);
		List<String> urlParameters = null == pageUrlParams ? Collections.emptyList()
				: Arrays.asList(pageUrlParams.split("/"));
		org.appng.xml.platform.PageReference pageReference = applicationProvider.processPage(marshallService, pathInfo,
				pageId, sectionIds, urlParameters);

		PageDefinition pageDefinition = new PageDefinition();
		User user = getUser(env);
		pageDefinition.setUser(user);

		pageDefinition.setId(pageId);
		pageDefinition.setSelf(getSelf("/page/" + pageId).toString());
		pageDefinition.setUserUrl("/manager/" + site.getName() + "/" + application.getName() + "/" + pageId);

		StringBuilder templatePath = getSelf("/page/" + pageId);

		UrlSchema urlSchema = pageReference.getConfig().getUrlSchema();
		urlSchema.getUrlParams().getParamList().forEach(p -> {
			templatePath.append("/{" + p.getName() + "}");
		});

		pageDefinition.setUrlTemplate(templatePath.toString());
		pageDefinition.setUrlPath(pageUrlParams);
		pageDefinition.setUrlParameters(currentUrlParams);

		processSections(env, servletResp, applicationProvider, pageReference, pageDefinition);

		Messages messages = env.removeAttribute(SESSION, Session.Environment.MESSAGES);
		if (null != messages) {
			pageDefinition.setMessages(getMessages(messages));
		} else if (null != pageReference.getMessages()) {
			pageDefinition.setMessages(getMessages(pageReference.getMessages()));
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Processed page: {}", marshallService.marshallNonRoot(pageReference));
		}

		HttpStatus httpStatus = HttpStatus.valueOf(servletResp.getStatus());
		if (httpStatus.is5xxServerError()) {
			LOGGER.debug("Page {} on application {} of site {} returned status {}", pageId, application.getName(),
					site.getName(), servletResp.getStatus());
			return new ResponseEntity<>(httpStatus);
		}

		return new ResponseEntity<PageDefinition>(pageDefinition, hasErrors() ? HttpStatus.BAD_REQUEST : HttpStatus.OK);
	}

	protected void processSections(Environment env, HttpServletResponse servletResp,
			ApplicationProvider applicationProvider, org.appng.xml.platform.PageReference pageReference,
			PageDefinition pageDefinition) {
		pageReference.getStructure().getSection().forEach(s -> {
			Section section = new Section();
			section.setElements(new ArrayList<>());
			section.setId(s.getId());
			section.setHidden(Boolean.valueOf(s.getHidden()));
			SectionConfig config = s.getConfig();

			org.appng.xml.platform.Label title = s.getTitle();
			if (null != title) {
				section.setTitle(new Label());
				section.getTitle().setValue(title.getValue());
			} else if (null != config && null != config.getTitle()) {
				section.setTitle(new Label());
				section.getTitle().setValue(config.getTitle().getValue());
			}

			s.getElement().forEach(e -> {
				SectionElement element = new SectionElement();
				element.setCollapsed(Boolean.valueOf(e.getFolded()));

				org.appng.xml.platform.Action a = e.getAction();
				org.appng.xml.platform.Label sectionTitle = e.getTitle();
				if (null != a) {
					AtomicBoolean mustExecute = new AtomicBoolean(false);
					Action action = openApiAction.getAction(request, a, env, null, true, mustExecute);

//					if (mustExecute.get()) {
//					TODO
//						try {
//							ResponseEntity<Action> performedAction = openApiAction.performAction(a.getEventId(),
//									a.getId(), null, action, env, servletReq, servletResp);
//							action = performedAction.getBody();
//						} catch (JAXBException | InvalidConfigurationException | ProcessingException e1) {
//							// TODO Auto-generated catch block
//							e1.printStackTrace();
//						}
//					}

					if (null != a.getOnSuccess()) {
						action.setOnSuccess("/manager/" + site.getName() + "/" + a.getOnSuccess());
					}
					action.setUser(null);
					if (null != sectionTitle) {
						element.setTitle(sectionTitle.getValue());
					}
					element.setAction(action);
				}
				org.appng.xml.platform.Datasource d = e.getDatasource();
				if (null != d) {
					Datasource datasource = openApiDataSource.transformDataSource(env, servletResp, applicationProvider,
							d);
					datasource.setUser(null);
					if (null != sectionTitle) {
						element.setTitle(sectionTitle.getValue());
					}
					element.setDatasource(datasource);
				}

				section.getElements().add(element);
			});
			pageDefinition.addSectionsItem(section);
		});
	}

	Logger getLogger() {
		return LOGGER;
	}
}
