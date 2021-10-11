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
package org.appng.api.support;

import java.io.File;
import java.io.IOException;

import org.appng.api.InvalidConfigurationException;
import org.appng.api.model.Application;
import org.appng.api.model.Resource;
import org.appng.api.model.ResourceType;
import org.appng.xml.MarshallService;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class ApplicationResourceHolderTest {

	@Test
	public void test() throws InvalidConfigurationException, IOException {
		Application app = Mockito.mock(Application.class);
		Mockito.when(app.isFileBased()).thenReturn(true);
		try (ApplicationResourceHolder arh = new ApplicationResourceHolder(app, Mockito.mock(MarshallService.class),
				new File("src/test/resources/application"), new File("target/cache/"))) {
			arh.dumpToCache(ResourceType.APPLICATION);
			File cachedFile = new File("target/cache/application.xml").getAbsoluteFile();
			Assert.assertTrue(cachedFile.exists());
			Resource resource = arh.getResources(ResourceType.APPLICATION).iterator().next();
			Assert.assertEquals(ResourceType.APPLICATION_XML_NAME, resource.getName());
			Assert.assertEquals(cachedFile, resource.getCachedFile());
		}
	}
}
