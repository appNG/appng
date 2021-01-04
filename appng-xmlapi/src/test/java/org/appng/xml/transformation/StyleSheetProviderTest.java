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
package org.appng.xml.transformation;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * tests {@link StyleSheetProvider}
 * 
 * @author Matthias Müller
 * 
 */
public class StyleSheetProviderTest {
	private StyleSheetProvider ssProvider;

	@Before
	public void setup() throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		File file = new File("src/test/resources/xsl/platform.xsl");
		InputStream masterXsl = new FileInputStream(file);
		ssProvider = new StyleSheetProvider();
		ssProvider.setDocumentBuilderFactory(dbf);
		ssProvider.setTransformerFactory(transformerFactory);
		ssProvider.init();
		ssProvider.setMasterSource(masterXsl, file.getParent());
	}

	@Test
	public void testStyleSheetProvider() throws Exception {
		ClassLoader classLoader = getClass().getClassLoader();
		ssProvider.setName("container");
		ssProvider.setInsertBefore("include-4.xsl");
		addStyleSheet("include-1.xsl");
		addStyleSheet("include-2.xsl");
		addStyleSheet("include-3.xsl");
		File expected = new File(classLoader.getResource("xsl/result.xsl").toURI()).getAbsoluteFile();
		File result = new File("target/xsl/result-2.xsl");
		FileUtils.writeByteArrayToFile(result, ssProvider.getStyleSheet(true));
		List<String> lines1 = FileUtils.readLines(expected, "UTF-8");
		List<String> lines2 = FileUtils.readLines(result, "UTF-8");
		Assert.assertEquals(lines1.size(), lines2.size());
		for (int i = 0; i < lines1.size(); i++) {
			Assert.assertEquals(lines1.get(i).trim(), lines2.get(i).trim());
		}
	}

	private void addStyleSheet(String name) throws Exception {
		File file = new File(getClass().getClassLoader().getResource("xsl/" + name).toURI());
		InputStream styleSheet = new FileInputStream(file);
		ssProvider.addStyleSheet(styleSheet, name);
	}

}
