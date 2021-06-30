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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.ext.ParamConverter.Lazy;
import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.Environment;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.Path;
import org.appng.api.ProcessingException;
import org.appng.api.Request;
import org.appng.api.Scope;
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
import org.appng.xml.platform.Param;
import org.appng.xml.platform.SectionConfig;
import org.appng.xml.platform.UrlSchema;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.RequestScope;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Lazy
@RestController
@RequestScope(proxyMode = ScopedProxyMode.NO)
public class OpenApiPage extends OpenApiOperation {

	private OpenApiDataSource openApiDataSource;
	private OpenApiAction openApiAction;

	@Autowired
	public OpenApiPage(Site site, Application application, Request request, MessageSource messageSource,
			@Value("${restUsePathParameters:true}") boolean supportPathParameters, OpenApiDataSource openApiDataSource,
			OpenApiAction openApiAction) throws JAXBException {
		super(site, application, request, messageSource, supportPathParameters);
		this.openApiDataSource = openApiDataSource;
		this.openApiAction = openApiAction;
	}

	@GetMapping(path = "/openapi/page/{id}")
	public ResponseEntity<PageDefinition> getPage(
	// @formatter:off
		@PathVariable(name = "id") String pageId,
		@RequestParam(required = false, name = "_sect") String[] sections,
		@RequestParam(required = false, name = "_urlPath") String path,
		Environment env,
		HttpServletRequest servletReq,
		HttpServletResponse servletResp
	// @formatter:on
	) throws JAXBException, InvalidConfigurationException, ProcessingException {
		ApplicationProvider applicationProvider = (ApplicationProvider) application;

		PageDefinition pageDefinition = new PageDefinition();
		User user = getUser(env);
		pageDefinition.setUser(user);

		org.appng.xml.platform.PageDefinition originalPage = applicationProvider.getApplicationConfig().getPage(pageId);

		if (null == originalPage) {
			LOGGER.debug("Page {} not found on application {} of site {}", pageId, application.getName(),
					site.getName());
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		MarshallService marshallService = MarshallService.getMarshallService();
		Path pathInfo = env.getAttribute(Scope.REQUEST, EnvironmentKeys.PATH_INFO);

		List<Parameter> currentUrlParams = new ArrayList<>();
		if (StringUtils.isNotBlank(path)) {
			String[] pathSegments = path.substring(1).split("/");
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

		org.appng.xml.platform.PageReference pageReference = applicationProvider.processPage(marshallService, pathInfo,
				pageId, null == sections ? Collections.emptyList() : Arrays.asList(sections));

		pageDefinition.setId(pageId);
		pageDefinition.setSelf(getSelf("/page/" + pageId).toString());
		pageDefinition.setUserUrl("/manager/" + site.getName() + "/" + application.getName() + "/" + pageId);

		StringBuilder templatePath = getSelf("/page/" + pageId);

		UrlSchema urlSchema = pageReference.getConfig().getUrlSchema();
		urlSchema.getUrlParams().getParamList().forEach(p -> {
			templatePath.append("/{" + p.getName() + "}");
		});

		pageDefinition.setUrlTemplate(templatePath.toString());
		pageDefinition.setUrlPath(path);
		pageDefinition.setUrlParameters(currentUrlParams);

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

			pageDefinition.addSectionsItem(section);

			s.getElement().forEach(e -> {

				SectionElement element = new SectionElement();
				element.setCollapsed(Boolean.valueOf(e.getFolded()));

				org.appng.xml.platform.Action a = e.getAction();
				if (null != a) {
					Action action = openApiAction.getAction(request, a, env, null);
					action.setUser(null);
					element.setAction(action);
				}
				org.appng.xml.platform.Datasource d = e.getDatasource();
				if (null != d) {
					Datasource datasource = openApiDataSource.transformDataSource(env, servletResp, applicationProvider,
							d);
					datasource.setUser(null);
					element.setDatasource(datasource);

				}
				section.getElements().add(element);
			});
		});

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Processed page: {}", marshallService.marshallNonRoot(pageReference));
		}

		if (servletResp.getStatus() != HttpStatus.OK.value()) {
			LOGGER.debug("Page {} on application {} of site {} returned status {}", pageId, application.getName(),
					site.getName(), servletResp.getStatus());
			return new ResponseEntity<>(HttpStatus.valueOf(servletResp.getStatus()));
		}

		return new ResponseEntity<PageDefinition>(pageDefinition, hasErrors() ? HttpStatus.BAD_REQUEST : HttpStatus.OK);
	}

	Logger getLogger() {
		return LOGGER;
	}
}
