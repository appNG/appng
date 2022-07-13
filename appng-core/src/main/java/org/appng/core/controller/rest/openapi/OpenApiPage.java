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

import static org.appng.api.Scope.SESSION;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.Environment;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.Path;
import org.appng.api.ProcessingException;
import org.appng.api.Request;
import org.appng.api.Scope;
import org.appng.api.Session;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.api.support.environment.EnvironmentKeys;
import org.appng.core.model.ApplicationProvider;
import org.appng.openapi.model.Action;
import org.appng.openapi.model.Datasource;
import org.appng.openapi.model.Label;
import org.appng.openapi.model.PageDefinition;
import org.appng.openapi.model.Parameter;
import org.appng.openapi.model.Section;
import org.appng.openapi.model.SectionElement;
import org.appng.openapi.model.User;
import org.appng.xml.MarshallService;
import org.appng.xml.platform.Messages;
import org.appng.xml.platform.Param;
import org.appng.xml.platform.SectionConfig;
import org.appng.xml.platform.Structure;
import org.appng.xml.platform.UrlSchema;
import org.slf4j.Logger;
import org.springframework.context.MessageSource;
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

	public OpenApiPage(Site site, Application application, Request request, MessageSource messageSource)
			throws JAXBException {
		super(site, application, request, messageSource);
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
		if (null != pageUrlParams && pageUrlParams.length() > pathLength) {
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
		String pageSelf = getSelf("/page/" + pageId + (null == pageUrlParams ? "" : "/" + pageUrlParams)).toString();
		pageDefinition.setSelf(pageSelf);
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
		Structure structure = pageReference.getStructure();
		if (null != structure) {
			structure.getSection().forEach(s -> {
				Section section = new Section();
				section.setElements(new ArrayList<>());
				section.setId(s.getId());
				section.setHidden(Boolean.valueOf(s.getHidden()));
				String sectSelf = pageDefinition.getSelf() + (null == s.getId() ? "" : "?_sect=" + s.getId());
				section.setSelf(sectSelf);
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
					if (null != sectionTitle) {
						element.setTitle(sectionTitle.getValue());
					}
					if (null != a) {
						Action action = new Action();
						action.setId(a.getId());
						action.setEventId(a.getEventId());
						StringBuilder actionSelf = getSelf("/action/" + a.getEventId() + "/" + a.getId(),
								a.getConfig().getParams());

						action.setSelf(actionSelf.toString());
						element.setAction(action);
					}
					org.appng.xml.platform.Datasource d = e.getDatasource();
					if (null != d) {
						Datasource datasource = new Datasource();
						datasource.setId(d.getId());
						datasource.setSelf(getSelf("/datasource/" + d.getId(), d.getConfig().getParams()).toString());
						element.setDatasource(datasource);
					}

					section.getElements().add(element);
				});
				pageDefinition.addSectionsItem(section);
			});
		}
	}

	Logger getLogger() {
		return LOGGER;
	}
}
