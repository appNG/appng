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
package org.appng.core.repository.config;

import java.util.ArrayList;

import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.api.support.MessageSourceChain;
import org.appng.core.domain.DatabaseConnection;
import org.appng.core.model.ApplicationCacheManager;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.context.support.StaticMessageSource;

public class ApplicationPostProcessorTest {

	@Test
	public void testPostProcessBeanFactory() {
		Site site = Mockito.mock(Site.class);
		Application application = Mockito.mock(Application.class);
		DatasourceConfigurer datasourceConfigurer = Mockito.mock(DatasourceConfigurer.class);
		DatabaseConnection databaseConnection = new DatabaseConnection();

		ArrayList<String> dictionaryNames = new ArrayList<>();
		ApplicationPostProcessor applicationPostProcessor = new ApplicationPostProcessor(site, application,
				databaseConnection, null, dictionaryNames);
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("mockConfigurer", datasourceConfigurer);
		ApplicationCacheManager cacheManager = Mockito.mock(ApplicationCacheManager.class);
		beanFactory.registerSingleton("cacheManager", cacheManager);
		ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
		beanFactory.registerSingleton("messageSource", messageSource);
		beanFactory.registerSingleton("additionalMessageSource", new StaticMessageSource());

		applicationPostProcessor.postProcessBeanFactory(beanFactory);

		Mockito.verify(datasourceConfigurer).configure(databaseConnection);
		Mockito.verify(cacheManager).initialize(site, application, null);
		Assert.assertEquals(site, beanFactory.getBean("site"));
		Assert.assertEquals(application, beanFactory.getBean("application"));
		Assert.assertEquals(site, beanFactory.getBean(Site.class));
		Assert.assertEquals(application, beanFactory.getBean(Application.class));
		Assert.assertTrue(messageSource.getParentMessageSource() instanceof MessageSourceChain);
		Assert.assertTrue(dictionaryNames.contains("messages-core"));
	}

}
