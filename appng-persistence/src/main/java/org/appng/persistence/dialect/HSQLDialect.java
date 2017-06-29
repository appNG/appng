/*
 * Copyright 2011-2017 the original author or authors.
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

/**
 * A {@link HSQLDialect} which converts varchar-fields with a length of >=1024 to hsql-type 'LONGVARCHAR'
 * 
 * @author Matthias Müller
 * 
 */
public class HSQLDialect extends org.hibernate.dialect.HSQLDialect {

	private static final String LONGVARCHAR = "LONGVARCHAR";

	private static final long MAX_LENGHT = 1024;

	@Override
	public String getTypeName(int code, long length, int precision, int scale) throws HibernateException {
		if (Types.VARCHAR == code && length >= MAX_LENGHT) {
			return LONGVARCHAR;
		}
		return super.getTypeName(code, length, precision, scale);
	}

}
