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
package org.appng.core.model;

import java.util.Properties;

import javax.servlet.ServletContext;

import org.appng.api.ApplicationConfig;
import org.appng.api.SiteProperties;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.core.domain.SiteApplication;
import org.appng.core.repository.config.DataSourceFactory;
import org.appng.core.service.PropertySupport;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * An {@link AnnotationConfigApplicationContext} representing a {@link SiteApplication}.
 * 
 * @author Matthias Müller
 */
@Slf4j
public class ApplicationContext extends AnnotationConfigApplicationContext {

	private Site site;

	/**
	 * Creates a new {@link ApplicationContext}.
	 * 
	 * @param siteApplication
	 *                        the {@link SiteApplication} this {@link ApplicationContext} represents
	 * @param parent
	 *                        the parent {@link ApplicationContext}, set only if the {@link SiteApplication}'s
	 *                        {@link Application} is a core {@link Application}
	 * @param classLoader
	 *                        the {@link ClassLoader} for this context
	 * @param sc
	 *                        the {@link ServletContext} for this context
	 * @param configLocations
	 *                        the config locations for this context
	 */
	public ApplicationContext(SiteApplication siteApplication, org.springframework.context.ApplicationContext parent,
			ClassLoader classLoader, ServletContext sc, boolean needsDataSource, Class<?> appNGApplication,
			Properties props, String[] configLocations) {
		super();
		XmlBeanDefinitionReader xmlBeanDefinitionReader = new XmlBeanDefinitionReader(this);
		xmlBeanDefinitionReader.loadBeanDefinitions(configLocations);

		WebApplicationContextUtils.registerWebApplicationScopes(this.getBeanFactory(), sc);
		if (needsDataSource) {
			DataSourceFactory dataSourceFactory = new DataSourceFactory();
			dataSourceFactory.setConfigurerClass(
					props.getProperty(PropertySupport.PREFIX_SITE + SiteProperties.DATASOURCE_CONFIGURER));
			dataSourceFactory.setLogPerformance(Boolean.valueOf(
					props.getProperty(PropertySupport.PREFIX_SITE + SiteProperties.LOG_JDBC_PERFORMANCE, "false")));
			registerBean("datasource", DataSourceFactory.class, () -> dataSourceFactory);
		} else {
			register(ApplicationConfig.class);
		}
		if (null != appNGApplication) {
			register(appNGApplication);
		}

		Application application = siteApplication.getApplication();
		String id = siteApplication.getSite().getName() + "_" + application.getName();
		setId(id);
		setDisplayName(getClass().getName() + "-" + id);
		setClassLoader(classLoader);
		if (application.isPrivileged()) {
			setParent(parent);
			this.site = siteApplication.getSite();
		}
	}

	/**
	 * Tries to retrieve the bean of the required type, returning {@code null} instead of throwing a
	 * {@link BeansException} if such a bean does not exist.
	 * 
	 * @param requiredType
	 *                     the type that the bean must match
	 * @return the bean, or {@code null} if no such bean exists
	 * @throws BeansException
	 *                        - <b>never thrown</b>, instead {@code null} is returned
	 */
	@Override
	public <T> T getBean(Class<T> requiredType) throws BeansException {
		try {
			return super.getBean(requiredType);
		} catch (BeansException e1) {
			LOGGER.info("bean '{}' not found in context", requiredType);
		}
		return null;
	}

	/**
	 * Returns the {@link Site} this context belongs to, only available if this context belongs to a core-
	 * {@link Application} .
	 * 
	 * @return the {@link Site}
	 */
	public Site getSite() {
		return site;
	}

}
