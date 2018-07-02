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
package org.appng.standalone;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link ApplicationStartup}.
 * 
 * @author Claus Stümke, 2017
 *
 */
public class ApplicationStartupTest {

	@Test
	public void testReplaceInFile() throws Exception {
		ClassLoader classLoader = ApplicationStartupTest.class.getClassLoader();
		File original = new File(classLoader.getResource("xml/sourceConfig.xml").toURI());
		File copy = new File("src/test/resources/xml/copyConfig.xml");
		File replacementSource = new File(classLoader.getResource("txt/replacement.txt").toURI());
		FileUtils.deleteQuietly(copy);
		FileUtils.copyFile(original, copy);
		Charset utf8 = StandardCharsets.UTF_8;
		String replacement = IOUtils.toString(new FileInputStream(replacementSource), utf8);
		System.out.println("Replacement String: " + replacement);
		ApplicationStartup.replaceInFile(copy.getAbsolutePath(), "${replaceMe}", replacement);
		String newContent = IOUtils.toString(new FileInputStream(copy), utf8);
		Assert.assertEquals("C:\\Foo\\Bar\\Bla\\Blubb", newContent);
	}

}
