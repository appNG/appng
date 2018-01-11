/*
 * Copyright 2011-2018 the original author or authors.
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
package org.appng.cli.commands;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test for {@link CommandBatch}.
 * 
 * @author Matthias Herlitzius
 * 
 */
public class CommandBatchTest {

	private CommandBatch batch = new CommandBatch();
	private Map<String, String> variables = batch.getVariables();

	@Test
	public void testComment() {
		Assert.assertArrayEquals(new String[0], batch.parseLine("#My first comment"));
		Assert.assertArrayEquals(new String[0], batch.parseLine("# AnotherOne"));
	}

	@Test
	public void testVariables() {
		checkVariables("def ABC = abc", "ABC", "abc");
		checkVariables("def A = b", "A", "b");
		checkVariables("def AB = ab", "AB", "ab");
		checkVariables("def C= d", "C", "d");
		checkVariables("def E =f", "E", "f");
		checkVariables("def G=h", "G", "h");
		checkVariables("def ADMIN_SUBJECT = administrator", "ADMIN_SUBJECT", "administrator");
		checkVariables("def REPOSITORY= Local Repository", "REPOSITORY", "\"Local Repository\"");
		checkVariables("def ADMIN_GROUP =Administrator", "ADMIN_GROUP", "Administrator");
		checkVariables("def AUTH_APP=appng-authentication", "AUTH_APP", "appng-authentication");
	}

	@Test
	public void testSysEnvVariables() {
		String osName = System.getProperty("os.name");
		boolean isLinux = StringUtils.containsIgnoreCase(osName, "linux");
		boolean isWindows = StringUtils.containsIgnoreCase(osName, "windows");
		if (isLinux) {
			Set<String> validResults = new HashSet<String>(Arrays.asList("en_US.UTF-8", "de_DE.UTF-8"));
			Assert.assertArrayEquals(new String[0], batch.parseLine("def LANG = ${systemEnv['LANG']}"));
			Assert.assertTrue(validResults.contains(variables.get("LANG")));
		} else if (isWindows) {
			Assert.assertArrayEquals(new String[0], batch.parseLine("def SYSTEMROOT = ${systemEnv['SystemRoot']}"));
			Assert.assertTrue(StringUtils.equalsIgnoreCase(variables.get("SYSTEMROOT"), "c:\\windows"));
		} else {
			Assert.fail("No test found for Operating System: " + osName);
		}
	}

	@Test
	public void testCommandParsing() {
		Assert.assertArrayEquals(new String[0], batch.parseLine("# a comment"));
		Assert.assertArrayEquals(new String[0], batch.parseLine("def foo = bar"));
		Assert.assertArrayEquals(new String[0], batch.parseLine(""));
		Assert.assertArrayEquals(new String[] { "-i", "-f", "-g", "-h" }, batch.parseLine("-i -f -g -h"));
		Assert.assertArrayEquals(new String[] { "-k", "chunk", "with", "space", "\"in double-quotes\"" },
				batch.parseLine("-k chunk with space \"in double-quotes\""));
		Assert.assertArrayEquals(new String[] { "-initdatabase" }, batch.parseLine("-initdatabase"));
		Assert.assertArrayEquals(
				new String[] { "create-site", "-n", "manager", "-h", "localhost", "-d", "http://localhost:8080", "-e" },
				batch.parseLine("create-site -n manager -h localhost -d http://localhost:8080 -e"));
		Assert.assertArrayEquals(new String[] { "-r", "\"L R\"", "-f" }, batch.parseLine("-r \"L R\" -f"));
		Assert.assertArrayEquals(
				new String[] { "import-application", "-n", "appng-authentication", "-v", "1.0-SNAPSHOT", "-r",
						"\"Local Repository\"", "-f" },
				batch.parseLine(
						"import-application -n appng-authentication -v 1.0-SNAPSHOT -r \"Local Repository\" -f"));
	}

	@Test
	public void testCommandVariables() {
		checkVariables("def DEPLOY = -f", "DEPLOY", "-f");
		checkVariables("def MANAGER_APP =appng-manager ", "MANAGER_APP", "appng-manager");
		checkVariables("def MANAGER_VERSION = 1.1-SNAPSHOT", "MANAGER_VERSION", "1.1-SNAPSHOT");
		checkVariables("def REPOSITORY=\"Local Repository\"", "REPOSITORY", "\"Local Repository\"");
		checkVariables("def ADMIN_GROUP=Administrator", "ADMIN_GROUP", "Administrator");
		checkVariables("def ADMIN_SUBJECT=admin", "ADMIN_GROUP", "Administrator");
		checkVariables("def ROLE_NAME=Platform Administrator", "ROLE_NAME", "\"Platform Administrator\"");

		Assert.assertArrayEquals(new String[] { "test", "-f" }, batch.parseLine("test ${DEPLOY}"));
		Assert.assertArrayEquals(
				new String[] { "import-application", "-n", "appng-manager", "-v", "1.1-SNAPSHOT", "-r",
						"\"Local Repository\"", "-c", "-f" },
				batch.parseLine(
						"import-application -n ${MANAGER_APP} -v ${MANAGER_VERSION} -r ${REPOSITORY} -c ${DEPLOY}"));
		Assert.assertArrayEquals(
				new String[] { "add-applicationrole", "-g", "Administrator", "-p", "appng-manager", "-r",
						"\"Platform Administrator\"" },
				batch.parseLine("add-applicationrole -g ${ADMIN_GROUP} -p ${MANAGER_APP} -r ${ROLE_NAME}"));
		Assert.assertArrayEquals(
				new String[] { "create-subject", "-u", "admin", "-p", "tester", "-n", "\"appNG Administrator\"", "-l",
						"en", "-e", "info@aiticon.de" },
				batch.parseLine(
						"create-subject -u ${ADMIN_SUBJECT} -p tester -n \"appNG Administrator\" -l en -e info@aiticon.de"));
	}

	@Test
	public void testCommandUndeclaredVariables() {
		Assert.assertArrayEquals(new String[] { "foo", "$BAR" }, batch.parseLine("foo $BAR"));
		Assert.assertArrayEquals(
				new String[] { "prefix", "$UNDECLARED_VAR", "suffix", "plus", "\"quotes with space\"" },
				batch.parseLine("prefix $UNDECLARED_VAR suffix plus 'quotes with space'"));
	}

	@Test
	public void testQuotes() {
		Assert.assertArrayEquals(new String[] { "\"foo\"", "\"bar\"" }, batch.parseLine("\"foo\" \"bar\""));
		Assert.assertArrayEquals(new String[] { "\"foo\"", "further", "parameters" },
				batch.parseLine("\"foo\" further parameters"));
		Assert.assertArrayEquals(new String[] { "\"foo\"", "\"bar\"" }, batch.parseLine("'foo' 'bar'"));
		Assert.assertArrayEquals(new String[] { "\"foo\"", "further", "parameters" },
				batch.parseLine("'foo' further parameters"));
	}

	public void checkVariables(String expression, String variableName, String expected) {
		Assert.assertArrayEquals(new String[0], batch.parseLine(expression));
		Assert.assertEquals(expected, variables.get(variableName));
	}

}
