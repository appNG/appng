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
package org.appng.persistence.dialect;

import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.dialect.MySQL5Dialect;

/**
 * A {@link MySQL5Dialect} which converts varchar-fields with a length of >=1024 to mysql-type 'text'
 * 
 * @author Matthias Müller
 * 
 */
public class MySqlTextDialect extends MySQL5Dialect {

	private static final String TEXT_TYPE = "text";

	private static final long MAX_LENGTH = 1024;

	@Override
	public String getTypeName(int code, long length, int precision, int scale) throws HibernateException {
		if (Types.VARCHAR == code && length >= MAX_LENGTH) {
			return TEXT_TYPE;
		}
		return super.getTypeName(code, length, precision, scale);
	}

}
