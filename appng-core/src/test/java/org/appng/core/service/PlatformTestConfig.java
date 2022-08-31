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
package org.appng.core.service;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;

import org.appng.api.support.environment.EnvironmentFactoryBean;
import org.appng.core.domain.PlatformEventListener;
import org.appng.core.model.PlatformProcessor;
import org.appng.core.model.PlatformTransformer;
import org.appng.core.model.RequestProcessor;
import org.appng.core.model.ThymeleafProcessor;
import org.appng.persistence.hibernate.dialect.HSQLDialect;
import org.appng.persistence.repository.SearchRepositoryImpl;
import org.appng.testsupport.persistence.TestDataProvider;
import org.appng.xml.MarshallService;
import org.appng.xml.MarshallService.AppNGSchema;
import org.appng.xml.transformation.StyleSheetProvider;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hsqldb.jdbc.JDBCDriver;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalEntityManagerFactoryBean;
import org.springframework.orm.jpa.support.SharedEntityManagerBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@ComponentScan(basePackages = "org.appng.core", excludeFilters = @Filter(type = FilterType.REGEX, pattern = "org\\.appng\\.core\\.controller\\.rest\\.*"))
@EnableTransactionManagement
@EnableJpaRepositories(repositoryBaseClass = SearchRepositoryImpl.class, basePackages = "org.appng.core.repository", entityManagerFactoryRef = "entityManagerFactory", transactionManagerRef = "coreTxManager")
public class PlatformTestConfig {

	@Bean
	public TestDataProvider testDataProvider() {
		AppNGTestDataProvider appNGTestDataProvider = new AppNGTestDataProvider();
		return appNGTestDataProvider;
	}

	@Bean
	public PlatformEventListener platformEventListener() {
		PlatformEventListener pel = new PlatformEventListener();
		pel.setAuditUser("appNG platform");
		pel.setPersist(false);
		return pel;
	}

	@Bean
	public DataSource dataSource() throws SQLException {
		DriverManagerDataSource ds = new DriverManagerDataSource("jdbc:hsqldb:mem:hsql-testdb");
		try (
				Connection conn = ds.getConnection();
				// http://www.hsqldb.org/doc/2.0/guide/sessions-chapt.html#snc_tx_mvcc
				CallableStatement stmt = conn.prepareCall("SET DATABASE TRANSACTION CONTROL MVCC")) {
			stmt.execute();
		}
		return ds;
	}

	@Bean
	public LocalEntityManagerFactoryBean entityManagerFactory() {
		LocalEntityManagerFactoryBean lemfb = new LocalEntityManagerFactoryBean();
		lemfb.setPersistenceProviderClass(HibernatePersistenceProvider.class);
		lemfb.setPersistenceUnitName("hsql-testdb");
		Properties jpaProperties = new Properties();
		jpaProperties.put(AvailableSettings.DIALECT, HSQLDialect.class.getName());
		jpaProperties.put(AvailableSettings.DRIVER, JDBCDriver.class.getName());
		jpaProperties.put(AvailableSettings.URL, "jdbc:hsqldb:mem:hsql-testdb");
		jpaProperties.put(AvailableSettings.USER, "sa");
		jpaProperties.put(AvailableSettings.PASS, "");
		jpaProperties.put(AvailableSettings.HBM2DDL_AUTO, "create");
		jpaProperties.put(AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS, false);
		lemfb.setJpaProperties(jpaProperties);
		return lemfb;
	}

	@Bean
	@Qualifier("coreTxManager")
	public JpaTransactionManager coreTxManager(EntityManagerFactory emf) {
		return new JpaTransactionManager(emf);
	}

	@Bean
	public SharedEntityManagerBean entityManager(EntityManagerFactory emf) {
		SharedEntityManagerBean semb = new SharedEntityManagerBean();
		semb.setEntityManagerFactory(emf);
		return semb;
	}

	@Bean
	public DocumentBuilderFactory documentBuilderFactory() {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		return dbf;
	}

	@Bean
	public TransformerFactory transformerFactory() {
		return TransformerFactory.newInstance();
	}

	@Bean(initMethod = "init")
	public StyleSheetProvider styleSheetProvider(DocumentBuilderFactory dbf, TransformerFactory tf) {
		StyleSheetProvider styleSheetProvider = new StyleSheetProvider();
		styleSheetProvider.setDocumentBuilderFactory(dbf);
		styleSheetProvider.setTransformerFactory(tf);
		return styleSheetProvider;
	}

	@Bean(initMethod = "init")
	@Scope("prototype")
	public MarshallService marshallService(DocumentBuilderFactory dbf, TransformerFactory tf) {
		MarshallService marshallService = new MarshallService();
		marshallService.setDocumentBuilderFactory(dbf);
		marshallService.setTransformerFactory(tf);
		marshallService.setSchema(AppNGSchema.PLATFORM);
		marshallService.setUseSchema(false);
		marshallService.setPrettyPrint(true);
		marshallService.setSchemaLocation("http://www.appng.org/schema/platform/appng-platform.xsd");
		marshallService.setCdataElements(Arrays.asList("title", "description", "label", "value", "message"));
		return marshallService;
	}

	@Bean
	@Lazy
	public CoreService coreService() {
		return new CoreService();
	}

	@Bean
	@Lazy
	public DatabaseService databaseService() {
		return new DatabaseService();
	}

	@Bean
	@Lazy
	public TemplateService templateService() {
		return new TemplateService();
	}

	@Bean
	@Lazy
	public InitializerService initializerService() {
		return new InitializerService();
	}

	@Bean
	@Lazy
	public LdapService ldapService() {
		return new LdapService();
	}

	@Bean
	public ThymeleafProcessor thymeleafProcessor(DocumentBuilderFactory dbf, MarshallService marshallService) {
		ThymeleafProcessor thymeleafProcessor = new ThymeleafProcessor(dbf);
		thymeleafProcessor.setMarshallService(marshallService);
		return thymeleafProcessor;
	}

	@Bean
	public EnvironmentFactoryBean environment() {
		return new EnvironmentFactoryBean();
	}

	@Bean
	@Lazy
	public PlatformTransformer platformTransformer(StyleSheetProvider styleSheetProvider) {
		PlatformTransformer platformTransformer = new PlatformTransformer();
		platformTransformer.setStyleSheetProvider(styleSheetProvider);
		return platformTransformer;
	}

	@Bean
	public RequestProcessor requestProcessor(MarshallService marshallService, PlatformTransformer platformTransformer) {
		PlatformProcessor platformProcessor = new PlatformProcessor();
		platformProcessor.setMarshallService(marshallService);
		platformProcessor.setPlatformTransformer(platformTransformer);
		return platformProcessor;
	}

}
