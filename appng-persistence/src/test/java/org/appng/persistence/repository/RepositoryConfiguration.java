/*
 * Copyright 2011-2018 the original author or authors.
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
package org.appng.persistence.repository;

import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.appng.persistence.model.QTestEntity;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.support.SharedEntityManagerBean;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableJpaRepositories(basePackages = {
		"org.appng.persistence.repository" }, repositoryBaseClass = QueryDslSearchRepositoryImpl.class, repositoryFactoryBeanClass = SearchRepositoryFactoryBean.class, excludeFilters = {
				@Filter(type = FilterType.REGEX, pattern = { ".*TestEntityEnversRepo" }) })
public class RepositoryConfiguration {

	public static void main(String[] args) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(RepositoryConfiguration.class);
		ctx.refresh();
		TestEntityRepo bean = ctx.getBean(TestEntityRepo.class);
		long count = bean.count(QTestEntity.testEntity.name.like("%foo%"));
		ctx.close();
	}

	@Bean
	public PlatformTransactionManager transactionManager() {
		JpaTransactionManager transactionManager = new JpaTransactionManager(entityManagerFactory());
		transactionManager.afterPropertiesSet();
		return transactionManager;
	}

	@Bean
	public EntityManagerFactory entityManagerFactory() {
		LocalContainerEntityManagerFactoryBean emfb = new LocalContainerEntityManagerFactoryBean();
		emfb.setPersistenceUnitName("hsql-testdb");
		emfb.setPackagesToScan("org.appng.persistence.model");
		Properties props = new Properties();
		props.put("hibernate.show_sql", "false");
		emfb.setJpaProperties(props);
		emfb.setPersistenceProviderClass(HibernatePersistenceProvider.class);
		emfb.afterPropertiesSet();
		return emfb.getObject();
	}

	@Bean
	public EntityManager entityManager() {
		SharedEntityManagerBean semb = new SharedEntityManagerBean();
		semb.setEntityManagerFactory(entityManagerFactory());
		semb.afterPropertiesSet();
		return semb.getObject();
	}

}
