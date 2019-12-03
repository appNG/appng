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

/**
 * A {@link DatasourceConfigurer} is responsible for configuring and providing a {@link javax.sql.DataSource} based upon
 * a {@link DatabaseConnection}. For performance reasons, some kind of connection-pooling should be applied.
 * 
 * @author Matthias Müller
 */
public interface DatasourceConfigurer {

	/**
	 * the JMX-domain to register the configurer instance at
	 */
	String JMX_DOMAIN = "org.appng.repository.config";

	/**
	 * Configures the instance using the settings of the given {@link DatabaseConnection}.
	 * 
	 * @param connection
	 *                   a {@link DatabaseConnection}
	 */
	void configure(DatabaseConnection connection);

	/**
	 * Destroys the instance.
	 */
	void destroy();

	/**
	 * Returns a {@link javax.sql.DataSource}
	 * 
	 * @return the {@link javax.sql.DataSource}
	 */
	DataSource getDataSource();

	/**
	 * Whether or not JDBC performance logger should be used
	 */
	void setLogPerformance(boolean logPerformance);

	/**
	 * Sets the connection timeout in milliseconds
	 * 
	 * @param connectionTimeout
	 *                          the timeout
	 */
	void setConnectionTimeout(int connectionTimeout);
	
	/**
	 * Sets the validation timeout in milliseconds
	 * 
	 * @param validationTimeout
	 *                          the timeout
	 */
	void setValidationTimeout(int validationTimeout);


}
