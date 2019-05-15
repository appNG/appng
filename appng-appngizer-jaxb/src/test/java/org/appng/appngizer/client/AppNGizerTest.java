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
package org.appng.appngizer.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.appng.appngizer.model.xml.Home;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.web.client.HttpClientErrorException;

@Ignore("Run locally")
public class AppNGizerTest {

	static AppNGizer appNGizer;
	static String host = "http://localhost:8080";
	static String sharedSecret = "Vu5w1HkkIcYGaGZXG9KJhFZcYQCkWVwLE3vnaVY5eRA=";

	@BeforeClass
	public static void setup() {
		appNGizer = new AppNGizer(host, sharedSecret);
		Home login = appNGizer.login();
		Assert.assertNotNull(login);
	}

	@Test(expected = HttpClientErrorException.class)
	public void testUploadPackage() throws Exception {
		appNGizer.uploadPackage("local", new File("pom.xml"));
	}

	@Test
	public void testWriteAndReadPlatformYaml() throws Exception {
		File config = new File("target/yaml/platform.yaml");
		FileOutputStream out = new FileOutputStream(config);
		AppNGizer.YamlConfig.readPlatformProperties(appNGizer, out, false);
		AppNGizer.YamlConfig.writePlatformProperties(appNGizer, new FileInputStream(config));
	}

	@Test
	public void testWriteAndReadSiteYaml() throws Exception {
		File config = new File("target/yaml/manager.yaml");
		FileOutputStream out = new FileOutputStream(config);
		AppNGizer.YamlConfig.readSiteProperties(appNGizer, "manager", out, false);
		AppNGizer.YamlConfig.writeSiteProperties(appNGizer, "manager", new FileInputStream(config));
	}

	@Test
	public void testWriteAndReadApplicationYaml() throws Exception {
		File config = new File("target/yaml/appng-authentication.yaml");
		FileOutputStream out = new FileOutputStream(config);
		AppNGizer.YamlConfig.readSiteApplicationProperties(appNGizer, "manager", "appng-authentication", out, false);
		AppNGizer.YamlConfig.writeSiteApplicationProperties(appNGizer, "manager", "appng-authentication",
				new FileInputStream(config));
	}

}
