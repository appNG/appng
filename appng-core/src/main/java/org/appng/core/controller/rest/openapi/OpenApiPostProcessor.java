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

import javax.xml.bind.JAXBException;

import org.appng.api.Request;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.core.controller.rest.openapi.OpenApiOperation.RestErrorHandler;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.MessageSource;
import org.springframework.core.Ordered;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.web.context.WebApplicationContext;

public class OpenApiPostProcessor implements BeanDefinitionRegistryPostProcessor, Ordered {

	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		registerRequestScoped(registry, OpenApiActionImpl.class);
		registerRequestScoped(registry, OpenApiDataSourceImpl.class);
		registerRequestScoped(registry, OpenApiPageImpl.class);
		registry.registerBeanDefinition(SwaggerUI.class.getSimpleName(), getBeanDefinition(SwaggerUI.class));
		registry.registerBeanDefinition(RestErrorHandler.class.getSimpleName(),
				getBeanDefinition(RestErrorHandler.class));
	}

	private void registerRequestScoped(BeanDefinitionRegistry registry, Class<?> beanClass) {
		BeanDefinition bean = getBeanDefinition(beanClass);
		bean.setScope(WebApplicationContext.SCOPE_REQUEST);
		registry.registerBeanDefinition(beanClass.getSimpleName(), bean);
	}

	protected BeanDefinition getBeanDefinition(Class<?> beanClass) {
		return new AnnotatedGenericBeanDefinition(new StandardAnnotationMetadata(beanClass));
	}

	static class OpenApiActionImpl extends OpenApiAction {
		@Autowired
		public OpenApiActionImpl(Site site, Application application, Request request, MessageSource messageSource,
				@Value("${restUsePathParameters:true}") boolean supportPathParameters) throws JAXBException {
			super(site, application, request, messageSource, supportPathParameters);
		}
	}

	static class OpenApiDataSourceImpl extends OpenApiDataSource {
		@Autowired
		public OpenApiDataSourceImpl(Site site, Application application, Request request, MessageSource messageSource,
				@Value("${restUsePathParameters:true}") boolean supportPathParameters) throws JAXBException {
			super(site, application, request, messageSource, supportPathParameters);
		}
	}

	static class OpenApiPageImpl extends OpenApiPage {
		@Autowired
		public OpenApiPageImpl(Site site, Application application, Request request, MessageSource messageSource,
				@Value("${restUsePathParameters:true}") boolean supportPathParameters) throws JAXBException {
			super(site, application, request, messageSource, supportPathParameters);
		}
	}

	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		// nothing to do
	}

}
