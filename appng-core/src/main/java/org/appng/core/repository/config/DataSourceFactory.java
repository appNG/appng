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
package org.appng.core.repository.config;

import javax.sql.DataSource;

import org.appng.core.domain.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;

/**
 * 
 * A {@link FactoryBean} for {@link DataSource}s, using a {@link DatasourceConfigurer}.
 * 
 * @author Matthias Müller
 * 
 */
public class DataSourceFactory implements FactoryBean<DataSource>, DatasourceConfigurer {

	private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceFactory.class);
	private DatasourceConfigurer configurer;

	private String configurerClass;
	private boolean logPerformance = false;

	public DataSourceFactory() {

	}

	public DataSourceFactory(DatasourceConfigurer configurer) {
		this.configurer = configurer;
	}

	public Class<?> getObjectType() {
		return DataSource.class;
	}

	public boolean isSingleton() {
		return true;
	}

	private DatasourceConfigurer initConfigurer() {
		try {
			Class<?> loadClass = Thread.currentThread().getContextClassLoader().loadClass(configurerClass);
			this.configurer = (DatasourceConfigurer) loadClass.newInstance();
			this.configurer.setLogPerformance(logPerformance);
		} catch (Exception e) {
			LOGGER.error("error creating instance of '" + configurerClass + "' ", e);
		}
		return configurer;
	}

	public DataSource getObject() throws Exception {
		return getDataSource();
	}

	public void configure(DatabaseConnection connection) {
		if (null != connection) {
			initConfigurer();
			configurer.configure(connection);
		}
	}

	public void destroy() {
		if (null != configurer) {
			configurer.destroy();
		}
	}

	public DataSource getDataSource() {
		return configurer.getDataSource();
	}

	public String getConfigurerClass() {
		return configurerClass;
	}

	public void setConfigurerClass(String configurerClass) {
		this.configurerClass = configurerClass;
	}

	public boolean isLogPerformance() {
		return logPerformance;
	}

	public void setLogPerformance(boolean logPerformance) {
		this.logPerformance = logPerformance;
	}

}
