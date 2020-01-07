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
package org.appng.core.service;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;

import org.appng.api.support.SiteClassLoader;
import org.junit.Assert;
import org.junit.Test;

public class SiteClassLoaderBuilderTest {

	@Test
	public void test() throws MalformedURLException {
		InitializerService.SiteClassLoaderBuilder siteClassLoaderBuilder = new InitializerService.SiteClassLoaderBuilder();
		Path jarPath = new File("test.jar").toPath();
		siteClassLoaderBuilder.addJar(jarPath, "test");
		String origin = siteClassLoaderBuilder.addJar(jarPath, "foo");
		Assert.assertEquals("test", origin);

		SiteClassLoader siteClassloader = siteClassLoaderBuilder.build(getClass().getClassLoader(), "localhost");

		URL[] urls = siteClassloader.getURLs();
		Assert.assertEquals(1, urls.length);
		Assert.assertEquals(jarPath.toUri().toURL(), urls[0]);
	}

}
