/*
 * Copyright 2011-2020 the original author or authors.
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
package org.appng.core.service;

import org.springframework.beans.factory.config.PropertyResourceConfigurer;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

public class TestInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

	protected boolean showSql = false;

	public void initialize(ConfigurableApplicationContext applicationContext) {
		java.util.Properties properties = getProperties();
		PropertyResourceConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
		configurer.setProperties(properties);
		applicationContext.addBeanFactoryPostProcessor(configurer);
	}

	protected java.util.Properties getProperties() {
		java.util.Properties properties = new java.util.Properties();
		properties.put("database", getClass().getSimpleName());
		properties.put("hibernate.show_sql", showSql);
		return properties;
	}

}
