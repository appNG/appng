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
package org.appng.persistence;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.appng.api.AppNgApplication;
import org.appng.api.ApplicationConfig;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.tool.hbm2ddl.SchemaExport.Action;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.support.SharedEntityManagerBean;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * A {@link Configuration} adding JPA capabilities to {@link ApplicationConfig}.
 * <p>
 * Usage:
 * 
 * <pre>
 * &#64;Configuration
 * &#64;ComponentScan("com.acme.business")
 * &#64;AppNgApplication(entityPackage = "com.acme.domain")
 * &#64;EnableJpaRepositories(basePackages = "com.acme.repository", repositoryBaseClass = SearchRepositoryImpl.class)
 * public class AcmeConfig extends ApplicationConfigJPA {
 * 
 * }
 * </pre>
 * </p>
 * 
 * @author Matthias Müller
 * @see AppNgApplication
 */
@Configuration
@EnableTransactionManagement
public class ApplicationConfigJPA extends ApplicationConfig {

	@Bean
	public FactoryBean<EntityManager> entityManager(EntityManagerFactory emf) {
		SharedEntityManagerBean em = new SharedEntityManagerBean();
		em.setEntityManagerFactory(emf);
		return em;
	}

	@Bean
	public FactoryBean<EntityManagerFactory> entityManagerFactory(DataSource dataSource,
			@Value("${entityPackage}") String entityPackage,
			@Value("${persistenceUnitName}") String persistenceUnitName,
			@Value("${createDatabase:false}") boolean createDatabase, @Value("${formatSql:true}") boolean formatSql) {
		LocalContainerEntityManagerFactoryBean emfb = new LocalContainerEntityManagerFactoryBean();
		emfb.setJpaDialect(new HibernateJpaDialect());
		emfb.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
		emfb.setPersistenceProvider(new HibernatePersistenceProvider());
		emfb.setPackagesToScan(entityPackage);
		emfb.setPersistenceUnitName(persistenceUnitName);
		emfb.setDataSource(dataSource);
		Map<String, Object> jpaProperties = new HashMap<>();
		if (createDatabase) {
			jpaProperties.put(AvailableSettings.HBM2DDL_AUTO, Action.CREATE.name().toLowerCase());
		}
		jpaProperties.put(AvailableSettings.FORMAT_SQL, formatSql);
		emfb.setJpaPropertyMap(jpaProperties);
		return emfb;
	}

	@Bean
	public PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
		return new JpaTransactionManager(emf);
	}
}
