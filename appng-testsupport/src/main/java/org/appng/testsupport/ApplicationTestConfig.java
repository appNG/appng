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
package org.appng.testsupport;

import javax.servlet.ServletContext;
import javax.xml.bind.JAXBException;

import org.appng.api.ApplicationConfig;
import org.appng.xml.MarshallService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.CustomScopeConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.context.support.SimpleThreadScope;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;

/**
 * A {@link Configuration} extending {@link ApplicationConfig} and adding capabilities to work with unit-tests.
 * 
 * @author Matthias Müller
 */
@Configuration
public class ApplicationTestConfig extends ApplicationConfig {

	@Bean
	public CustomScopeConfigurer scopeConfigurer() {
		CustomScopeConfigurer customScopeConfigurer = new CustomScopeConfigurer();
		customScopeConfigurer.addScope("request", new SimpleThreadScope());
		customScopeConfigurer.addScope("session", new SimpleThreadScope());
		return customScopeConfigurer;
	}

	@Bean
	public ResourceLoader resourceLoader() {
		return new FileSystemResourceLoader();
	}

	@Bean
	public ServletContext servletContext(ResourceLoader resourceLoader) {
		return new MockServletContext(resourceLoader);
	}

	@Bean
	public MarshallService marshallService() throws JAXBException {
		return MarshallService.getMarshallService();
	}

	@Bean
	@Qualifier("applicationMarshallService")
	public MarshallService applicationMarshallService() throws JAXBException {
		return MarshallService.getApplicationMarshallService();
	}

	@Bean
	@Scope(value = "prototype", proxyMode = ScopedProxyMode.NO)
	public MockHttpServletRequest httpServletRequest(ServletContext context) {
		return new MockHttpServletRequest(context);
	}

}
