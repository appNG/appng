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
package org.appng.core.repository.config;

import java.util.ArrayList;

import org.appng.api.model.Application;
import org.appng.api.model.Properties;
import org.appng.api.model.Site;
import org.appng.api.support.MessageSourceChain;
import org.appng.core.domain.DatabaseConnection;
import org.appng.core.domain.SiteImpl;
import org.appng.core.model.ApplicationCacheManager;
import org.appng.core.service.ApplicationProperties;
import org.appng.core.service.HazelcastConfigurer;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.cache.Cache;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.context.support.StaticMessageSource;

import com.hazelcast.config.MapConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spring.cache.HazelcastCacheManager;

public class ApplicationPostProcessorTest {

	@Test
	public void testPostProcessBeanFactory() {
		SiteImpl site = Mockito.mock(SiteImpl.class);
		Mockito.when(site.getReloadCount()).thenReturn(42);
		Mockito.when(site.getName()).thenReturn("localhost");
		Application application = Mockito.mock(Application.class);
		Mockito.when(application.getName()).thenReturn("app");
		Properties props = Mockito.mock(Properties.class);
		Mockito.when(application.getProperties()).thenReturn(props);
		java.util.Properties cacheProps = new java.util.Properties();
		cacheProps.put("mycache.ttl", 3600);
		cacheProps.put("mycache.maxIdle", 1800);
		Mockito.when(props.getProperties(ApplicationProperties.PROP_CACHE_CONFIG)).thenReturn(cacheProps);
		DatasourceConfigurer datasourceConfigurer = Mockito.mock(DatasourceConfigurer.class);
		DatabaseConnection databaseConnection = new DatabaseConnection();

		ArrayList<String> dictionaryNames = new ArrayList<>();
		HazelcastCacheManager platformCacheManager = new HazelcastCacheManager();
		HazelcastInstance hazelcastInstance = HazelcastConfigurer.getInstance(null);
		platformCacheManager.setHazelcastInstance(hazelcastInstance);

		ApplicationPostProcessor applicationPostProcessor = new ApplicationPostProcessor(site, application,
				databaseConnection, platformCacheManager, dictionaryNames);
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("mockConfigurer", datasourceConfigurer);
		ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
		beanFactory.registerSingleton("messageSource", messageSource);
		beanFactory.registerSingleton("additionalMessageSource", new StaticMessageSource());

		applicationPostProcessor.postProcessBeanFactory(beanFactory);
		ApplicationCacheManager cacheManager = beanFactory.getBean("cacheManager", ApplicationCacheManager.class);

		Cache cache = cacheManager.getCache("mycache");
		Assert.assertNotNull(cache);
		cache.put("foo", "bar");
		Assert.assertEquals(cache, platformCacheManager.getCache(cache.getName()));
		Assert.assertEquals("bar", cache.get("foo", String.class));
		MapConfig mapConfig = hazelcastInstance.getConfig().getMapConfig(cache.getName());
		Assert.assertEquals(1800, mapConfig.getMaxIdleSeconds());
		Assert.assertEquals(3600, mapConfig.getTimeToLiveSeconds());

		Mockito.verify(datasourceConfigurer).configure(databaseConnection);
		Assert.assertEquals(site, beanFactory.getBean("site"));
		Assert.assertEquals(application, beanFactory.getBean("application"));
		Assert.assertEquals(site, beanFactory.getBean(Site.class));
		Assert.assertEquals(application, beanFactory.getBean(Application.class));
		Assert.assertTrue(messageSource.getParentMessageSource() instanceof MessageSourceChain);
		Assert.assertTrue(dictionaryNames.contains("messages-core"));
	}

}
