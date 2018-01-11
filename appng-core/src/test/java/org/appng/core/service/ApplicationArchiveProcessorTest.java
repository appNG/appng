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
package org.appng.core.service;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.time.DateUtils;
import org.appng.api.model.Resource;
import org.appng.api.model.ResourceType;
import org.appng.core.domain.ApplicationImpl;
import org.appng.core.domain.PackageArchiveTest;
import org.appng.core.model.PackageArchive;
import org.junit.Assert;
import org.junit.Test;

public class ApplicationArchiveProcessorTest {

	@Test
	public void test() throws ParseException, IOException {
		ApplicationImpl application = new ApplicationImpl();
		application.setName(PackageArchiveTest.NAME);
		application.setVersion(DateUtils.parseDate(PackageArchiveTest.TIMESTAMP, "yyyy-MM-dd-HHmm"));
		PackageArchive applicationArchive = PackageArchiveTest.getPackageArchive();
		ApplicationArchiveProcessor processor = new ApplicationArchiveProcessor(application);
		List<Resource> resources = applicationArchive.processZipFile(processor);
		List<String> expectedNames = new ArrayList<String>(Arrays.asList("datasources.xml", "events.xml", "master.xml",
				"page.xml", "plugin.xml", "messages-demo.properties", "mssql/V1.0_script.sql", "mysql/V1.0_script.sql",
				ResourceType.BEANS_XML_NAME, ResourceType.APPLICATION_XML_NAME));
		List<ResourceType> expectedTypes = new ArrayList<ResourceType>(Arrays.asList(ResourceType.XML,
				ResourceType.XML, ResourceType.XML, ResourceType.XML, ResourceType.XML, ResourceType.DICTIONARY,
				ResourceType.SQL, ResourceType.SQL, ResourceType.BEANS_XML, ResourceType.APPLICATION));
		for (Resource applicationResource : resources) {
			System.out.println(applicationResource.getName());
			int idx = expectedNames.indexOf(applicationResource.getName());
			expectedNames.remove(idx);
			Assert.assertEquals(expectedTypes.remove(idx), applicationResource.getResourceType());
		}
		Assert.assertTrue(applicationArchive.isValid());
		Assert.assertTrue(expectedNames.isEmpty());
		Assert.assertTrue(expectedTypes.isEmpty());
	}

}
