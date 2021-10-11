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
package org.appng.api;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.appng.api.model.Application;
import org.appng.api.model.Resources;
import org.appng.api.support.ApplicationConfigProviderImpl;
import org.appng.api.support.ApplicationResourceHolder;
import org.appng.api.support.ConfigValidationError;
import org.appng.api.support.ConfigValidator;
import org.appng.xml.MarshallService;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class ConfigValidatorTest {

	@Mock
	private Application application;

	@Test
	public void test() throws Exception {
		MockitoAnnotations.initMocks(this);
		Mockito.when(application.isFileBased()).thenReturn(true);
		File targetFolder = new File("target/temp");
		ClassLoader classLoader = getClass().getClassLoader();
		File applicationFolder = new File(classLoader.getResource("application").toURI());
		Resources applicationResources = new ApplicationResourceHolder(application,
				MarshallService.getApplicationMarshallService(), applicationFolder, targetFolder);
		MarshallService marshallService = MarshallService.getMarshallService();
		ApplicationConfigProvider applicationConfigProvider = new ApplicationConfigProviderImpl(marshallService,
				"application", applicationResources, false);

		ConfigValidator configValidator = new ConfigValidator(applicationConfigProvider, false, false);
		configValidator.setWithDetailedErrors(true);
		configValidator.validate("application");
		configValidator.validateMetaData(new URLClassLoader(new URL[0]));
		Collection<String> errors = configValidator.getErrors();
		List<String> sorted = new ArrayList<>(errors);
		Collections.sort(sorted);
//		System.out.println("");
//		for (String e : sorted) {
//			System.out.println(e);
//		}
		InputStream expected = classLoader.getResourceAsStream("configvalidator.txt");
		List<String> expectedErrors = IOUtils.readLines(expected, Charset.defaultCharset());
		Assert.assertEquals(70, expectedErrors.size());
		for (int i = 0; i < expectedErrors.size(); i++) {
			Assert.assertEquals("error in line " + (i + 1), expectedErrors.get(i), sorted.get(i));
		}

		List<ConfigValidationError> detaildErrors = configValidator.getDetaildErrors();
		sorted = new ArrayList<>();
		for (ConfigValidationError error : detaildErrors) {
			Assert.assertNotNull(error.getLine());
			sorted.add(error.toString());
		}
		Collections.sort(sorted);
//		System.out.println("");
//		for (String e : sorted) {
//			System.out.println(e);
//		}
		Assert.assertEquals(51, detaildErrors.size());

		InputStream expectedDetails = classLoader.getResourceAsStream("configvalidatorDetails.txt");
		List<String> expectedDetailErrors = IOUtils.readLines(expectedDetails, Charset.defaultCharset());
		for (int i = 0; i < expectedDetailErrors.size(); i++) {
			Assert.assertEquals("error in line " + (i + 1), expectedDetailErrors.get(i), sorted.get(i));
		}
	}
}
