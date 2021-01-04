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
package org.appng.persistence.hibernate.dialect;

import java.sql.Types;

import org.hibernate.dialect.Dialect;
import org.junit.Assert;
import org.junit.Test;

public class DialectUTF8Test {

	@Test
	public void testMariaDB103() {
		runTest(new MariaDB103DialectUTF8());
	}

	@Test
	public void testMySql57() {
		runTest(new MySql57DialectUTF8());
	}

	@Test
	public void testMySql8() {
		runTest(new MySql8DialectUTF8());
	}

	private void runTest(Dialect dialect) {
		Assert.assertEquals("varchar(255)", dialect.getTypeName(Types.VARCHAR, 255, -1, -1));
		Assert.assertEquals("varchar(1000)", dialect.getTypeName(Types.VARCHAR, 1000, -1, -1));
		Assert.assertEquals("text", dialect.getTypeName(Types.VARCHAR, 1001, -1, -1));
		Assert.assertEquals("text", dialect.getTypeName(Types.VARCHAR, 16383, -1, -1));
		Assert.assertEquals("mediumtext", dialect.getTypeName(Types.VARCHAR, 16384, -1, -1));
		Assert.assertEquals("mediumtext", dialect.getTypeName(Types.VARCHAR, 4194303, -1, -1));
		Assert.assertEquals("longtext", dialect.getTypeName(Types.VARCHAR, 4194304, -1, -1));
	}
}
