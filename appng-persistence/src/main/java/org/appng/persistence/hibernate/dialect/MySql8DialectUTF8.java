/*
 * Copyright 2011-2023 the original author or authors.
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
package org.appng.persistence.hibernate.dialect;

import java.sql.Types;

import org.hibernate.dialect.MySQL8Dialect;

/**
 * A {@link MySQL8Dialect} which converts varchar-fields ({@link Types#VARCHAR}) with
 * <ul>
 * <li>a length of <=1000 to type {@code varchar}
 * <li>a length of <=16383 to type {@code text}
 * <li>a length of <=4194303 to type {@code mediumtext}
 * <li>a length of >4194303 to type {@code longtext}
 * </ul>
 * These values assume <a href="https://dev.mysql.com/doc/refman/8.0/en/charset-unicode-utf8mb4.html">utf8mb4</a>
 * character encoding, where each character consumes up to 4 bytes.<br/>
 * 
 * @author Matthias Müller
 */
public class MySql8DialectUTF8 extends MySQL8Dialect {

	@Override
	protected void registerVarcharTypes() {
		registerColumnType(Types.VARCHAR, "longtext");
		registerColumnType(Types.VARCHAR, 4194303, "mediumtext");
		registerColumnType(Types.VARCHAR, 16383, "text");
		registerColumnType(Types.VARCHAR, 1000, "varchar($l)");
	}
}
