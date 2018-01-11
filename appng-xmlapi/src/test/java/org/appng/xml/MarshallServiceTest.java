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
package org.appng.xml;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.apache.commons.io.FileUtils;
import org.appng.xml.MarshallService.AppNGSchema;
import org.appng.xml.platform.Platform;
import org.junit.Assert;
import org.junit.Test;

public class MarshallServiceTest {

	private static final String EMPTY_PLATFORM = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><platform xmlns=\"http://www.appng.org/schema/platform\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.appng.org/schema/platform http://www.appng.org/schema/platform/appng-platform.xsd\"/>";

	@Test
	public void testRoundTrip() throws Exception {
		MarshallService marshallService = init(true, true, true);
		InputStream isOriginal = MarshallServiceTest.class.getClassLoader().getResourceAsStream(
				"xml/platform-original.xml");
		Object masterOriginal = marshallService.unmarshall(isOriginal);

		InputStream isControl = MarshallServiceTest.class.getClassLoader().getResourceAsStream("xml/platform.xml");
		Object masterControl = marshallService.unmarshall(isControl);
		String controllXml = marshallService.marshal(masterControl);

		String resultXml = marshallService.marshal(masterOriginal);
		Assert.assertEquals(controllXml, resultXml);
	}

	@Test(expected = JAXBException.class)
	public void testMarshallException() throws Exception {
		MarshallService marshallService = init(true, true, true);
		marshallService.marshal(new Platform());
	}

	@Test
	public void testMarshallLogError() throws Exception {
		MarshallService marshallService = init(true, false, false, false);
		String result = marshallService.marshal(new Platform());
		Assert.assertEquals(EMPTY_PLATFORM, result);
	}

	@Test(expected = JAXBException.class)
	public void testUnmarshallException() throws Exception {
		MarshallService marshallService = init(true, true, true);
		ClassLoader classLoader = MarshallServiceTest.class.getClassLoader();
		File file = new File(classLoader.getResource("xml/platform-error.xml").toURI());
		String string = FileUtils.readFileToString(file, "UTF-8");
		marshallService.unmarshall(string);
	}

	@Test
	public void testUnmarshallLogError() throws Exception {
		MarshallService marshallService = init(true, false, false);
		ClassLoader classLoader = MarshallServiceTest.class.getClassLoader();
		File file = new File(classLoader.getResource("xml/platform-error.xml").toURI());
		String string = FileUtils.readFileToString(file, "UTF-8");
		Platform master = (Platform) marshallService.unmarshall(string);
		Assert.assertNotNull(master.getConfig());
		Assert.assertNotNull(master.getNavigation());
		Assert.assertNotNull(master.getSubject());
		Assert.assertNull(master.getContent());
	}

	private MarshallService init(boolean useSchema, boolean throwMarshallingError, boolean throwUnmarshallingError)
			throws JAXBException, TransformerFactoryConfigurationError {
		return init(useSchema, true, throwMarshallingError, throwUnmarshallingError);
	}

	private MarshallService init(boolean useSchema, boolean prettyPrint, boolean throwMarshallingError,
			boolean throwUnmarshallingError) throws JAXBException, TransformerFactoryConfigurationError {
		MarshallService marshallService = new MarshallService();
		marshallService.setSchema(AppNGSchema.PLATFORM);
		marshallService.setPrettyPrint(prettyPrint);
		marshallService.setUseSchema(useSchema);
		marshallService.setSchemaLocation("http://www.appng.org/schema/platform/appng-platform.xsd");
		marshallService.setDocumentBuilderFactory(DocumentBuilderFactory.newInstance());
		marshallService.setTransformerFactory(TransformerFactory.newInstance());
		marshallService.setCdataElements(Arrays.asList("title", "description", "label"));
		marshallService.setThrowMarshallingError(throwMarshallingError);
		marshallService.setThrowUnmarshallingError(throwUnmarshallingError);
		marshallService.init();
		return marshallService;
	}
}
