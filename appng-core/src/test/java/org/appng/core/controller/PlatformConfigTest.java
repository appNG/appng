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
package org.appng.core.controller;

import java.util.Properties;

import org.appng.core.domain.DatabaseConnection;
import org.appng.core.service.DatabaseService;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.config.PreferencesPlaceholderConfigurer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.ClassPathResource;

public class PlatformConfigTest {

	@Test
	public void testStartAndStop() throws Exception {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(PlatformConfig.class);

		PreferencesPlaceholderConfigurer ppc = new PreferencesPlaceholderConfigurer();
		Properties props = new Properties();
		ppc.setProperties(props);
		ClassPathResource configResource = new ClassPathResource("appNG-hsql.properties");
		props.load(configResource.getInputStream());
		ppc.afterPropertiesSet();

		ctx.addBeanFactoryPostProcessor(ppc);
		ctx.refresh();
		DatabaseConnection platformConnection = ctx.getBean(DatabaseService.class).getPlatformConnection(props);
		StringBuilder dbInfo = new StringBuilder();
		platformConnection.testConnection(dbInfo);
		Assert.assertTrue(dbInfo.toString().contains("HSQL Database Engine 2.5"));

		ctx.close();
	}

}
