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
package org.appng.cli.prettyTable;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;
import org.appng.api.BusinessException;
import org.appng.cli.prettytable.PrettyTable;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class PrettyTableTest {

	private PrettyTable prettyTable;

	@Before
	public void setup() {
		prettyTable = new PrettyTable();
		prettyTable.addColumn("A");
		prettyTable.addColumn("B");
		prettyTable.addColumn("C");
	}

	@Test
	public void testTabbedEmpty() throws BusinessException {
		String table = prettyTable.render(true, true);
		Assert.assertEquals("\nA\tB\tC\n", table);
	}

	@Test
	public void testTabbed() throws BusinessException {
		prettyTable.addRow(1, null, 3);
		String table = prettyTable.render(true, true);
		Assert.assertEquals("\nA\tB\tC\n1\tnull\t3\n", table);
	}

	@Test
	public void testTable() throws BusinessException, IOException {
		prettyTable.addRow(1, null, 3);
		prettyTable.addRow("a", "b", null);
		String table = prettyTable.render(false, true);
		URL resource = getClass().getClassLoader().getResource("PrettyTableTest.txt");
		String expected = FileUtils.readFileToString(new File(resource.getFile()), Charset.defaultCharset());
		Assert.assertEquals(expected, table);
	}

	@Test
	public void testTableEmpty() throws BusinessException, IOException {
		String table = prettyTable.render(false, true);
		URL resource = getClass().getClassLoader().getResource("PrettyTableTest-empty.txt");
		String expected = FileUtils.readFileToString(new File(resource.getFile()), Charset.defaultCharset());
		Assert.assertEquals(expected, table);
	}
}
