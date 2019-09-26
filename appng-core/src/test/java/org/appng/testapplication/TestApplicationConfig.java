package org.appng.testapplication;

import org.appng.persistence.ApplicationConfigJPA;
import org.appng.persistence.repository.SearchRepositoryImpl;
import org.appng.testsupport.ApplicationTestConfig;
import org.appng.testsupport.persistence.ApplicationConfigDataSource;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@Import({ ApplicationConfigJPA.class, ApplicationTestConfig.class, ApplicationConfigDataSource.class })
@ImportResource("classpath:applications/application1/beans.xml")
@ComponentScan("org.appng.testapplication")
@EnableJpaRepositories(basePackages = "org.appng.testapplication", repositoryBaseClass = SearchRepositoryImpl.class)
public class TestApplicationConfig {

}
