package org.appng.core.service;

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
import org.appng.persistence.dialect.HSQLDialect;
import org.appng.persistence.repository.SearchRepositoryImpl;
import org.appng.testsupport.persistence.TestDataProvider;
import org.appng.xml.MarshallService;
import org.appng.xml.MarshallService.AppNGSchema;
import org.appng.xml.transformation.StyleSheetProvider;
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
@ComponentScan(excludeFilters = @Filter(type = FilterType.REGEX, pattern = "org\\.appng\\.core\\.controller\\.rest\\.*"))
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
	public DataSource dataSource() {
		return new DriverManagerDataSource("jdbc:hsqldb:mem:hsql-testdb");
	}

	@Bean
	public LocalEntityManagerFactoryBean entityManagerFactory() {
		LocalEntityManagerFactoryBean lemfb = new LocalEntityManagerFactoryBean();
		lemfb.setPersistenceProviderClass(HibernatePersistenceProvider.class);
		lemfb.setPersistenceUnitName("hsql-testdb");
		Properties jpaProperties = new Properties();
		jpaProperties.put("hibernate.dialect", HSQLDialect.class.getName());
		jpaProperties.put("hibernate.connection.driver_class", JDBCDriver.class.getName());
		jpaProperties.put("hibernate.connection.url", "jdbc:hsqldb:mem:hsql-testdb");
		jpaProperties.put("hibernate.connection.username", "sa");
		jpaProperties.put("hibernate.connection.password", "");
		jpaProperties.put("hibernate.hbm2ddl.auto", "create");
		jpaProperties.put("hibernate.id.new_generator_mappings", false);
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
