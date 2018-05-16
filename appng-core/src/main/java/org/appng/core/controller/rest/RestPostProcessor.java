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

import org.appng.api.Request;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.RequestScope;

public class RestPostProcessor implements BeanDefinitionRegistryPostProcessor, Ordered {

	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		StandardAnnotationMetadata restActionMetaData = new StandardAnnotationMetadata(RestAction.class);
		AnnotatedGenericBeanDefinition restAction = new AnnotatedGenericBeanDefinition(restActionMetaData);
		restAction.setScope("request");
		registry.registerBeanDefinition("restAction", restAction);
		StandardAnnotationMetadata restDataSourcesMetaData = new StandardAnnotationMetadata(RestDataSource.class);
		AnnotatedGenericBeanDefinition restDataSource = new AnnotatedGenericBeanDefinition(restDataSourcesMetaData);
		restDataSource.setScope("request");
		registry.registerBeanDefinition("restDataSource", restDataSource);
	}

	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

	}

	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

	@RestController
	@RequestScope
	static class RestAction extends RestActionBase {
		@Autowired
		public RestAction(Site site, Application application, Request request,
				@Value("${useRestPathParameters:true}") boolean supportPathParameters, MessageSource messageSource) {
			super(site, application, request, supportPathParameters, messageSource);
		}

	}

	@RestController
	@RequestScope
	static class RestDataSource extends RestDataSourceBase {
		@Autowired
		public RestDataSource(Site site, Application application, Request request,
				@Value("${useRestPathParameters:true}") boolean supportPathParameters) {
			super(site, application, request, supportPathParameters);
		}

	}
}
