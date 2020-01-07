/*
 * Copyright 2011-2020 the original author or authors.
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
package org.appng.xml;

import java.io.File;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

public class ApplicationPropertyConstantCreatorTest {

	@Test
	public void test() throws Exception {
		String outFile = "src/test/resources/xml/application.xml";
		ApplicationPropertyConstantCreator
				.main(new String[] { outFile, "org.appng.xml.ApplicationProperty", "target/tmp", "PROP_" });
		String actual = FileUtils.readFileToString(new File("target/tmp/org/appng/xml/ApplicationProperty.java"),
				StandardCharsets.UTF_8);
		String expected = FileUtils.readFileToString(new File("src/test/resources/ApplicationProperty.java"),
				StandardCharsets.UTF_8);
		Assert.assertEquals(expected, actual);
	}
}
