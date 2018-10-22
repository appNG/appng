/*
 * Copyright 2011-2018 the original author or authors.
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
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.MessageSource;
import org.springframework.core.Ordered;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.RequestScope;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RestPostProcessor implements BeanDefinitionRegistryPostProcessor, Ordered {

	private Properties properties;

	public RestPostProcessor(Properties properties) {
		this.properties = properties;
	}

	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		StandardAnnotationMetadata restActionMetaData = new StandardAnnotationMetadata(RestAction.class);
		AnnotatedGenericBeanDefinition restAction = new AnnotatedGenericBeanDefinition(restActionMetaData);
		restAction.setScope("request");
		registry.registerBeanDefinition("restAction", restAction);

		StandardAnnotationMetadata restDataSourcesMetaData = new StandardAnnotationMetadata(RestDataSource.class);
		AnnotatedGenericBeanDefinition restDataSource = new AnnotatedGenericBeanDefinition(restDataSourcesMetaData);
		restDataSource.setScope("request");
		registry.registerBeanDefinition("restDataSource", restDataSource);

		StandardAnnotationMetadata restErrorHandlerMetaData = new StandardAnnotationMetadata(RestErrorHandler.class);
		AnnotatedGenericBeanDefinition restErrorHandler = new AnnotatedGenericBeanDefinition(restErrorHandlerMetaData);
		registry.registerBeanDefinition("restErrorHandler", restErrorHandler);
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

	@RestController
	@RequestScope
	static class RestAction extends RestActionBase {
		@Autowired
		public RestAction(Site site, Application application, Request request, MessageSource messageSource,
				@Value("${restUsePathParameters:true}") boolean supportPathParameters) throws JAXBException {
			super(site, application, request, messageSource, supportPathParameters);
		}

	}

	@RestController
	@RequestScope
	static class RestDataSource extends RestDataSourceBase {
		@Autowired
		public RestDataSource(Site site, Application application, Request request, MessageSource messageSource,
				@Value("${restUsePathParameters:true}") boolean supportPathParameters) throws JAXBException {
			super(site, application, request, messageSource, supportPathParameters);
		}

	}

}
