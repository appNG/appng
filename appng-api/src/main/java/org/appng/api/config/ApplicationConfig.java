/*
 * Copyright 2011-2023 the original author or authors.
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
package org.appng.api.config;

import static org.appng.api.Scope.SESSION;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.appng.api.Environment;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.api.support.RequestFactoryBean;
import org.appng.api.support.ResourceBundleMessageSource;
import org.appng.api.support.SelectionFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.core.convert.ConversionService;
import org.springframework.web.context.annotation.RequestScope;

import lombok.extern.slf4j.Slf4j;

/**
 * Basic {@link Configuration} for all {@link Application}s.
 * 
 * @author Matthias Müller
 * 
 * @since 1.23
 */
@Slf4j
@Configuration
public class ApplicationConfig {

	@Bean
	public ConversionServiceFactoryBean conversionService() {
		return new ConversionServiceFactoryBean();
	}

	@Bean
	public ResourceBundleMessageSource messageSource() {
		ResourceBundleMessageSource rbms = new ResourceBundleMessageSource();
		rbms.setAlwaysUseMessageFormat(true);
		rbms.setDefaultEncoding(StandardCharsets.UTF_8.name());
		rbms.setFallbackToSystemLocale(false);
		return rbms;
	}

	@Bean
	public SelectionFactory selectionFactory() {
		return new SelectionFactory();
	}

	@Bean
	@RequestScope(proxyMode = ScopedProxyMode.NO)
	public Environment environment(HttpServletRequest request, HttpServletResponse response, Site site) {
		Environment environment = (Environment) request.getAttribute(Environment.class.getName());
		for (Application app : site.getApplications()) {
			String sessionParamName = app.getSessionParamKey(site);
			if (null == environment.getAttribute(SESSION, sessionParamName)) {
				environment.setAttribute(SESSION, sessionParamName, new HashMap<>());
			}
		}
		return environment;
	}

	@Bean
	@RequestScope(proxyMode = ScopedProxyMode.NO)
	public RequestFactoryBean request(Environment env, Site site, Application application, HttpServletRequest request,
			ConversionService conversionService, MessageSource messageSource) {
		return new RequestFactoryBean(request, env, site, application, conversionService, messageSource);
	}

}
