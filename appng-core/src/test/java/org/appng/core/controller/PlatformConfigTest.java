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

import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletContext;

import org.appng.core.domain.DatabaseConnection;
import org.appng.core.service.DatabaseService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.PlaceholderConfigurerSupport;
import org.springframework.beans.factory.config.PreferencesPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@Configuration
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { PlatformConfig.class, PlatformConfigTest.class })
public class PlatformConfigTest {

	@Autowired
	DatabaseService databaseService;

	@Test
	public void testStartAndStop() throws Exception {
		DatabaseConnection platformConnection = databaseService.getPlatformConnection(properties());
		StringBuilder dbInfo = new StringBuilder();
		platformConnection.testConnection(dbInfo);
		Assert.assertTrue(dbInfo.toString().contains("HSQL Database Engine 2.5"));
	}

	@Bean
	public ServletContext servletContext() {
		return new MockServletContext();
	}

	@Bean
	public static PlaceholderConfigurerSupport platformProperties() throws IOException {
		PreferencesPlaceholderConfigurer ppc = new PreferencesPlaceholderConfigurer();
		ppc.setProperties(properties());
		return ppc;
	}

	private static Properties properties() throws IOException {
		Properties props = new Properties();
		props.load(new ClassPathResource("appNG-hsql.properties").getInputStream());
		return props;
	}

}
