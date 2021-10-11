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
package org.appng.core.controller.rest;

import javax.xml.bind.JAXBException;

import org.appng.api.Request;
import org.appng.api.model.Application;
import org.appng.api.model.Properties;
import org.appng.api.model.Site;
import org.appng.core.controller.rest.RestOperation.RestErrorHandler;
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
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RestPostProcessor implements BeanDefinitionRegistryPostProcessor, Ordered {

	private Properties properties;

	public RestPostProcessor(Properties properties) {
		this.properties = properties;
	}

	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		registerRequestScoped(registry, RestAction.class);
		registerRequestScoped(registry, RestDataSource.class);

		StandardAnnotationMetadata restErrorHandlerMetaData = new StandardAnnotationMetadata(RestErrorHandler.class);
		AnnotatedGenericBeanDefinition restErrorHandler = new AnnotatedGenericBeanDefinition(restErrorHandlerMetaData);
		registry.registerBeanDefinition("restErrorHandler", restErrorHandler);
	}

	private void registerRequestScoped(BeanDefinitionRegistry registry, Class<?> beanClass) {
		BeanDefinition bean = new AnnotatedGenericBeanDefinition(new StandardAnnotationMetadata(beanClass));
		bean.setScope(WebApplicationContext.SCOPE_REQUEST);
		registry.registerBeanDefinition(beanClass.getSimpleName(), bean);
	}

	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		Boolean restRegisterJacksonMapper = properties.getBoolean("restRegisterJsonConverter", false);
		if (restRegisterJacksonMapper
				&& beanFactory.getBeansOfType(MappingJackson2HttpMessageConverter.class).isEmpty()) {
			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.setDefaultPropertyInclusion(Include.NON_ABSENT);
			MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter = new MappingJackson2HttpMessageConverter(
					objectMapper);
			mappingJackson2HttpMessageConverter.setPrettyPrint(true);
			beanFactory.registerSingleton("mappingJackson2HttpMessageConverter", mappingJackson2HttpMessageConverter);
		}
	}

	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

	static class RestAction extends RestActionBase {
		@Autowired
		public RestAction(Site site, Application application, Request request, MessageSource messageSource,
				@Value("${restUsePathParameters:true}") boolean supportPathParameters) throws JAXBException {
			super(site, application, request, messageSource, supportPathParameters);
		}

	}

	static class RestDataSource extends RestDataSourceBase {
		@Autowired
		public RestDataSource(Site site, Application application, Request request, MessageSource messageSource,
				@Value("${restUsePathParameters:true}") boolean supportPathParameters) throws JAXBException {
			super(site, application, request, messageSource, supportPathParameters);
		}

	}

}
