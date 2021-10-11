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
package org.appng.persistence.hibernate;

import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.HibernateException;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitBasicColumnNameSource;
import org.hibernate.boot.model.naming.ImplicitEntityNameSource;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.springframework.data.util.ParsingUtils;
import org.springframework.util.StringUtils;

/**
 * An {@link ImplicitNamingStrategy} that uses snake case for table and column names.
 */
public class SnakeCaseNamingStrategy extends ImplicitNamingStrategyJpaCompliantImpl {

	@Override
	public Identifier determinePrimaryTableName(ImplicitEntityNameSource source) {
		if (source == null) {
			throw new HibernateException("Entity naming information was not provided.");
		}
		String tableName = transformEntityName(source.getEntityNaming());
		if (tableName == null) {
			throw new HibernateException("Could not determine primary table name for entity");
		}
		return toSnakeCase(tableName);
	}

	@Override
	public Identifier determineBasicColumnName(ImplicitBasicColumnNameSource source) {
		return toSnakeCase(source.getAttributePath().getProperty());
	}

	protected Identifier toSnakeCase(String name) {
		return Identifier.toIdentifier(stringToSnakeCase(name));
	}

	protected String stringToSnakeCase(String name) {
		List<String> parts = ParsingUtils.splitCamelCaseToLower(name).stream().filter(StringUtils::hasText)
				.collect(Collectors.toList());
		return StringUtils.collectionToDelimitedString(parts, "_");
	}

}