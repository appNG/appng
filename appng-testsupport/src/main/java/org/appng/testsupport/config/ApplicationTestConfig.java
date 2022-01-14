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
package org.appng.testsupport.config;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContext;
import javax.xml.bind.JAXBException;

import org.appng.api.Platform;
import org.appng.api.VHostMode;
import org.appng.api.config.ApplicationConfig;
import org.appng.api.model.Property;
import org.appng.api.model.SimpleProperty;
import org.appng.api.support.PropertyHolder;
import org.appng.xml.MarshallService;
import org.springframework.beans.factory.config.CustomScopeConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.support.SimpleThreadScope;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.WebApplicationContext;

@Configuration
public class ApplicationTestConfig extends ApplicationConfig {

	@Bean
	@Order(Ordered.HIGHEST_PRECEDENCE)
	public static CustomScopeConfigurer customScopeConfigurer() {
		CustomScopeConfigurer customScopeConfigurer = new CustomScopeConfigurer();
		customScopeConfigurer.addScope(WebApplicationContext.SCOPE_REQUEST, new SimpleThreadScope());
		customScopeConfigurer.addScope(WebApplicationContext.SCOPE_SESSION, new SimpleThreadScope());
		return customScopeConfigurer;
	}

	@Bean
	public MockServletContext servletContext() {
		MockServletContext ctx = new MockServletContext(new FileSystemResourceLoader());
		Map<String, Object> platformEnv = new ConcurrentHashMap<>();
		Property vhostMode = new SimpleProperty(Platform.Property.VHOST_MODE, VHostMode.NAME_BASED.name());
		platformEnv.put(Platform.Environment.PLATFORM_CONFIG, new PropertyHolder("", Arrays.asList(vhostMode)));
		ctx.setAttribute(org.appng.api.Scope.PLATFORM.name(), platformEnv);
		return ctx;
	}

	@Bean
	@Scope("prototype")
	public MockHttpServletRequest httpServletRequest(ServletContext context) {
		return new MockHttpServletRequest(context);
	}

	@Bean
	@Scope("prototype")
	public MockHttpServletResponse htpServletResponse() {
		return new MockHttpServletResponse();
	}

	@Bean
	public MarshallService applicationMarshallService() throws JAXBException {
		return MarshallService.getApplicationMarshallService();
	}

	@Bean
	public MarshallService marshallService() throws JAXBException {
		return MarshallService.getMarshallService();
	}

}
