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
package org.appng.core.repository.config;

import java.util.Collection;
import java.util.Map;

import org.appng.api.model.Application;
import org.appng.api.support.MessageSourceChain;
import org.appng.core.domain.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.Ordered;

/**
 * A {@link BeanFactoryPostProcessor} that configures the {@code datasource} bean which is of type
 * {@link javax.sql.DataSource}, but only if the {@link Application} requires a database.
 * 
 * @author Matthias Müller
 * 
 */
public class ApplicationPostProcessor implements BeanFactoryPostProcessor, Ordered {

	private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationPostProcessor.class);
	private static final String DATASOURCE_BEAN_NAME = "datasource";
	private static final String MESSAGES_CORE = "messages-core";
	private final DatabaseConnection connection;
	private Collection<String> dictionaryNames;

	/**
	 * Creates a new {@code ApplicationPostProcessor} using the given {@link DatabaseConnection}.
	 * 
	 * @param databaseConnection
	 *            a {@link DatabaseConnection}, may be {@code null}.
	 * @param dictionaryNames
	 *            the name of the dictionary files that the {@link Application} uses (see
	 *            {@link ResourceBundleMessageSource#setBasenames(String...)})
	 */
	public ApplicationPostProcessor(DatabaseConnection databaseConnection, Collection<String> dictionaryNames) {
		this.connection = databaseConnection;
		this.dictionaryNames = dictionaryNames;
	}

	/**
	 * If this {@code ApplicationPostProcessor} was created with a non-{@code null} {@link DatabaseConnection}, a
	 * {@link DatasourceConfigurer} is configured using that {@link DatabaseConnection}. Otherwise, the
	 * {@code datasource} bean gets destroyed.
	 * 
	 * @see DatasourceConfigurer
	 */
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		try {
			DatasourceConfigurer datasourceConfigurer = beanFactory.getBean(DatasourceConfigurer.class);
			if (null != connection) {
				try {
					LOGGER.debug("configuring {}", connection);
					datasourceConfigurer.configure(connection);
				} catch (Exception e) {
					throw new BeanCreationException("error while creating DatasourceConfigurer", e);
				}
			} else {
				LOGGER.debug("no connection given, destroying bean '{}'", DATASOURCE_BEAN_NAME);
				beanFactory.destroyBean(DATASOURCE_BEAN_NAME, datasourceConfigurer);
			}
		} catch (NoSuchBeanDefinitionException e) {
			LOGGER.debug("no DatasourceConfigurer found in bean-factory");
		}

		dictionaryNames.add(MESSAGES_CORE);

		// an application can define several message sources. These message sources will be set as parent message source
		// to the main, global message source in a message source chain.
		Map<String, MessageSource> messageSources = beanFactory.getBeansOfType(MessageSource.class);
		MessageSource messageSource = messageSources.remove("messageSource");
		ResourceBundleMessageSource globalMessageSource = (ResourceBundleMessageSource) messageSource;
		globalMessageSource.setBasenames(dictionaryNames.toArray(new String[dictionaryNames.size()]));
		if (messageSources.size() > 0) {
			MessageSource[] msArray = messageSources.values().toArray(new MessageSource[messageSources.size()]);
			globalMessageSource.setParentMessageSource(new MessageSourceChain(msArray));
		}
	}

	public int getOrder() {
		return Integer.MAX_VALUE;
	}

}
