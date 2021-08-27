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
package org.appng.appngizer;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import javax.xml.bind.JAXBException;

import org.appng.appngizer.controller.AppNGizerConfigurer;
import org.appng.appngizer.controller.Jaxb2Marshaller;
import org.appng.appngizer.controller.SessionInterceptor;
import org.appng.core.domain.DatabaseConnection;
import org.appng.core.domain.DatabaseConnection.DatabaseType;
import org.appng.core.domain.PlatformEventListener;
import org.appng.core.repository.config.DataSourceFactory;
import org.appng.core.repository.config.HikariCPConfigurer;
import org.appng.core.service.DatabaseService;
import org.appng.core.service.LdapService;
import org.appng.core.service.TemplateService;
import org.appng.persistence.repository.SearchRepositoryImpl;
import org.appng.xml.MarshallService;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.xml.MarshallingHttpMessageConverter;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.support.SharedEntityManagerBean;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "org.appng.core.repository", repositoryBaseClass = SearchRepositoryImpl.class)
public class AppNGizer extends WebMvcConfigurationSupport {

	@Override
	public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
		super.configureMessageConverters(converters);
		Jaxb2Marshaller jaxb2Marshaller = jaxb2Marshaller();
		converters.add(new MarshallingHttpMessageConverter(jaxb2Marshaller, jaxb2Marshaller));
		converters.add(new StringHttpMessageConverter(StandardCharsets.UTF_8));
	}

	@Bean
	public Jaxb2Marshaller jaxb2Marshaller() {
		Jaxb2Marshaller jaxb2Marshaller = new Jaxb2Marshaller();
		jaxb2Marshaller.setCheckForXmlRootElement(false);
		jaxb2Marshaller.setSupportJaxbElementClass(true);
		jaxb2Marshaller.setPackagesToScan("org.appng.appngizer.model.xml");
		jaxb2Marshaller.setSchema(new ClassPathResource("appngizer.xsd"));
		return jaxb2Marshaller;
	}

	@Override
	protected void addInterceptors(InterceptorRegistry registry) {
		registry.addWebRequestInterceptor(new SessionInterceptor());
	}

	@Bean
	public static AppNGizerConfigurer configurer(ResourceLoader loader) {
		AppNGizerConfigurer appNGizerConfigurer = new AppNGizerConfigurer();
		appNGizerConfigurer.setLocation(loader.getResource("/WEB-INF/conf/appNG.properties"));
		return appNGizerConfigurer;
	}

	@Bean
	public PlatformEventListener platformEventListener() {
		PlatformEventListener pel = new PlatformEventListener();
		pel.setAuditUser("appNGizer");
		pel.setAuditApplication("appNGizer");
		return pel;
	}

	@Bean
	public JpaTransactionManager transactionManager(EntityManagerFactory emf) {
		return new JpaTransactionManager(emf);
	}

	@Bean
	public SharedEntityManagerBean entityManager(EntityManagerFactory emf) {
		SharedEntityManagerBean em = new SharedEntityManagerBean();
		em.setEntityManagerFactory(emf);
		return em;
	}

	@Bean
	public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource,
			@Value("${hibernate.dialect}") String hibernateDialect) {
		LocalContainerEntityManagerFactoryBean emfb = new LocalContainerEntityManagerFactoryBean();
		emfb.setPersistenceUnitName("appNGizer");
		emfb.setPersistenceProviderClass(HibernatePersistenceProvider.class);
		emfb.setDataSource(dataSource);
		emfb.setJpaDialect(new HibernateJpaDialect());
		Properties jpaProperties = new Properties();
		jpaProperties.put(org.hibernate.cfg.AvailableSettings.DIALECT, hibernateDialect);
		emfb.setJpaProperties(jpaProperties);
		emfb.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
		emfb.setPackagesToScan("org.appng.core.domain");
		return emfb;
	}

	@Bean
	public DataSourceFactory datasource(AppNGizerConfigurer configurer) {
		Properties props = configurer.getProps();
		DatabaseType dbType = DatabaseType.valueOf(props.getProperty("database.type"));
		DatabaseConnection connection = new DatabaseConnection(dbType, props.getProperty("hibernate.connection.url"),
				props.getProperty("hibernate.connection.driver_class"),
				props.getProperty("hibernate.connection.username"),
				props.getProperty("hibernate.connection.password").getBytes(), null);
		connection.setName("appNGizer Root Connection");
		connection.setMinConnections(Integer.valueOf(props.getProperty("database.minConnections", "3")));
		connection.setMaxConnections(Integer.valueOf(props.getProperty("database.maxConnections", "10")));
		return new DataSourceFactory(new HikariCPConfigurer(connection));
	}

	@Bean
	public MessageSource messageSource() {
		ResourceBundleMessageSource rbms = new ResourceBundleMessageSource();
		rbms.setBasename("messages-core");
		rbms.setFallbackToSystemLocale(false);
		return rbms;
	}

	@Bean
	public MarshallService marshallService() throws JAXBException {
		return MarshallService.getMarshallService();
	}

	@Bean
	public ConversionServiceFactoryBean conversionService() {
		return new ConversionServiceFactoryBean();
	}

	@Bean
	public DatabaseService databaseService() {
		return new DatabaseService();
	}

	@Bean
	public LdapService ldapService() {
		return new LdapService();
	}

	@Bean
	public org.appng.core.service.CoreService coreService() {
		return new org.appng.core.service.CoreService();
	}

	@Bean
	public TemplateService templateService() {
		return new TemplateService();
	}

}
