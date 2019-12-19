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
package org.appng.testsupport.persistence;

import javax.sql.DataSource;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * A {@link Configuration} that adds a {@link DriverManagerDataSource} using a HSQL in memory database. The name of the
 * database can be set using the property {@code database} which defaults to {@code hsql-testdb}.
 * 
 * @author Matthias Müller
 */
@Configuration
public class ApplicationConfigDataSource {

	@Bean
	@Primary
	public FactoryBean<DataSource> datasource(@Value("${database:hsql-testdb}") String database) {
		return new FactoryBean<DataSource>() {

			public DataSource getObject() throws Exception {
				return new DriverManagerDataSource("jdbc:hsqldb:mem:" + database);
			}

			public Class<?> getObjectType() {
				return DataSource.class;
			}
		};
	}

}
