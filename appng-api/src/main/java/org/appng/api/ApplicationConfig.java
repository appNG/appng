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
package org.appng.api;

import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletRequest;

import org.appng.api.model.Application;
import org.appng.api.support.RequestFactoryBean;
import org.appng.api.support.ResourceBundleMessageSource;
import org.appng.api.support.SelectionFactory;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.api.support.environment.EnvironmentFactoryBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.core.convert.ConversionService;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Base {@link Configuration} for all {@link Application}s.
 * 
 * @author Matthias Müller
 */
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
	@Scope(value = "request", proxyMode = ScopedProxyMode.NO)
	public Environment environment() throws Exception {
		ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder
				.getRequestAttributes();
		if (null == requestAttributes) {
			return new EnvironmentFactoryBean().getObject();
		}
		return DefaultEnvironment.get(requestAttributes.getRequest(), requestAttributes.getResponse());
	}

	@Bean
	@Lazy
	@RequestScope(proxyMode = ScopedProxyMode.NO)
	public RequestFactoryBean request(HttpServletRequest request, Environment environment, MessageSource messageSource,
			ConversionService conversionService) {
		return new RequestFactoryBean(request, environment, conversionService, messageSource);
	}

}
