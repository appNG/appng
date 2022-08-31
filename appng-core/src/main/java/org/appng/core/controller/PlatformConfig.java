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
package org.appng.core.controller;

import java.util.Arrays;
import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;

import org.appng.api.Request;
import org.appng.core.domain.DatabaseConnection;
import org.appng.core.domain.DatabaseConnection.DatabaseType;
import org.appng.core.domain.PlatformEventListener;
import org.appng.core.model.PlatformProcessor;
import org.appng.core.model.PlatformTransformer;
import org.appng.core.model.RequestProcessor;
import org.appng.core.model.ThymeleafProcessor;
import org.appng.core.repository.config.DataSourceFactory;
import org.appng.core.repository.config.HikariCPConfigurer;
import org.appng.core.service.CoreService;
import org.appng.core.service.DatabaseService;
import org.appng.core.service.HazelcastConfigurer;
import org.appng.core.service.InitializerService;
import org.appng.core.service.LdapService;
import org.appng.core.service.TemplateService;
import org.appng.persistence.repository.SearchRepositoryImpl;
import org.appng.xml.BuilderFactory;
import org.appng.xml.MarshallService;
import org.appng.xml.MarshallService.AppNGSchema;
import org.appng.xml.transformation.StyleSheetProvider;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.support.SharedEntityManagerBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.context.annotation.RequestScope;

import com.hazelcast.spring.cache.HazelcastCacheManager;

/**
 * Central {@link Configuration} for appNG's platform context.
 * 
 * @author Matthias Müller
 */
@Configuration
@EnableTransactionManagement
// @formatter:off
@ComponentScan(
	basePackages = { "org.appng.core", "org.appng.taglib.config" },
	excludeFilters = @Filter(type = FilterType.REGEX, pattern = "org\\.appng\\.core\\.controller\\.rest\\.*")
)
@EnableJpaRepositories(
	repositoryBaseClass = SearchRepositoryImpl.class,
	basePackages = "org.appng.core.repository",
	entityManagerFactoryRef = "entityManagerFactory",
	transactionManagerRef = "coreTxManager"
)
// @formatter:on
public class PlatformConfig {

	@Bean
	public PlatformEventListener platformEventListener() {
		PlatformEventListener pel = new PlatformEventListener();
		pel.setAuditUser("appNG platform");
		return pel;
	}

	@Bean
	public DataSourceFactory dataSource(
	// @formatter:off
			@Value("${hibernate.connection.url}") String jdbcUrl,
			@Value("${hibernate.connection.username}") String userName,
			@Value("${hibernate.connection.password}") String password,
			@Value("${hibernate.connection.driver_class}") String driverClass,
			@Value("${database.type}") String type,
			@Value("${database.minConnections:3}") Integer minConnections,
			@Value("${database.maxConnections:10}") Integer maxConnections,
			@Value("${database.maxLifetime:90000}") Integer maxLifetime,
			@Value("${database.validationQuery:}") String validationQuery,
			@Value("${database.validationPeriod:}") Integer validationPeriod,
			@Value("${database.validationTimeout:5000}") Integer validationTimeout,
			@Value("${database.connectionTimeout:5000}") Integer connectionTimeout,
			@Value("${database.logPerformance:false}") boolean logPerformance,
			@Value("${database.autoCommit:true}") boolean autoCommit
	// @formatter:on
	) {
		DatabaseConnection connection = new DatabaseConnection(DatabaseType.valueOf(type.toUpperCase()), jdbcUrl,
				driverClass, userName, password.getBytes(), validationQuery);
		connection.setMinConnections(minConnections);
		connection.setMaxConnections(maxConnections);
		connection.setValidationPeriod(validationPeriod);
		connection.setName("appNG ROOT connection");
		HikariCPConfigurer configurer = new HikariCPConfigurer(connection, logPerformance);
		configurer.setMaxLifetime(maxLifetime);
		configurer.setValidationTimeout(validationTimeout);
		configurer.setConnectionTimeout(connectionTimeout);
		configurer.setAutoCommit(autoCommit);
		return new DataSourceFactory(configurer);
	}

	@Bean
	public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource,
			@Value("${hibernate.dialect}") String dialect) {
		LocalContainerEntityManagerFactoryBean lcemfb = new LocalContainerEntityManagerFactoryBean();
		lcemfb.setPersistenceProviderClass(HibernatePersistenceProvider.class);
		lcemfb.setPersistenceUnitName("appNG");
		lcemfb.setDataSource(dataSource);
		Properties jpaProperties = new Properties();
		jpaProperties.put(AvailableSettings.DIALECT, dialect);
		lcemfb.setJpaProperties(jpaProperties);
		lcemfb.setPackagesToScan("org.appng.core.domain");
		return lcemfb;
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
		DocumentBuilderFactory dbf = BuilderFactory.documentBuilderFactory();
		dbf.setNamespaceAware(true);
		return dbf;
	}

	@Bean
	public TransformerFactory transformerFactory() throws TransformerConfigurationException {
		return BuilderFactory.transformerFactory();
	}

	@Bean(initMethod = "init")
	@RequestScope(proxyMode = ScopedProxyMode.NO)
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
		marshallService.setCdataElements(Arrays.asList("title", "description", "label", "tooltip", "value", "message"));
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
	@RequestScope(proxyMode = ScopedProxyMode.NO)
	public ThymeleafProcessor thymeleafProcessor(DocumentBuilderFactory dbf, MarshallService marshallService) {
		ThymeleafProcessor thymeleafProcessor = new ThymeleafProcessor(dbf);
		thymeleafProcessor.setMarshallService(marshallService);
		return thymeleafProcessor;
	}

	@Bean
	@Lazy
	@RequestScope(proxyMode = ScopedProxyMode.NO)
	public PlatformTransformer platformTransformer(StyleSheetProvider styleSheetProvider) {
		PlatformTransformer platformTransformer = new PlatformTransformer();
		platformTransformer.setStyleSheetProvider(styleSheetProvider);
		return platformTransformer;
	}

	@Bean
	@RequestScope(proxyMode = ScopedProxyMode.NO)
	public RequestProcessor requestProcessor(MarshallService marshallService, PlatformTransformer platformTransformer) {
		PlatformProcessor platformProcessor = new PlatformProcessor();
		platformProcessor.setMarshallService(marshallService);
		platformProcessor.setPlatformTransformer(platformTransformer);
		return platformProcessor;
	}

	@Bean
	@Lazy
	public CacheManager platformCacheManager() {
		return new HazelcastCacheManager(HazelcastConfigurer.getInstance(null));
	}

}
