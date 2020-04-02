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
package org.appng.appngizer.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import org.appng.appngizer.client.AppNGizerClient.Config.Format;
import org.appng.appngizer.client.AppNGizerClient.PropertyWrapper;
import org.appng.appngizer.client.AppNGizerClient.SiteConfig;
import org.appng.appngizer.model.xml.Home;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.web.client.HttpClientErrorException;

public class AppNGizerTest {

	static String host = "http://localhost:8080";
	static String sharedSecret = "Vu5w1HkkIcYGaGZXG9KJhFZcYQCkWVwLE3vnaVY5eRA=";

	private AppNGizer getAppNGizer() {
		AppNGizer appNGizer = new AppNGizer(host, sharedSecret);
		Home login = appNGizer.login();
		Assert.assertNotNull(login);
		return appNGizer;
	}

	@Test
	public void testEncode() {
		Assert.assertEquals("name%20with%20spaces", AppNGizer.encode("name with spaces"));
	}

	@Test
	public void testReadAndWriteSiteYaml() throws IOException {
		testSite("config/site.yaml", Format.YAML, "target/localhost.yaml");
	}

	@Test
	public void testReadAndWriteSiteJson() throws IOException {
		testSite("config/site.json", Format.JSON, "target/localhost.json");
	}

	private void testSite(String source, Format format, String output) throws IOException, FileNotFoundException {
		InputStream in = getClass().getClassLoader().getResourceAsStream(source);
		Map<String, SiteConfig> sites = AppNGizer.Config.readSite(in, format);
		Assert.assertEquals(1, sites.size());
		Assert.assertEquals("localhost", sites.keySet().iterator().next());
		SiteConfig localhost = sites.get("localhost");
		Assert.assertEquals(63, localhost.getProperties().size());
		File controlfile = new File(output);
		AppNGizer.Config.write("localhost", new FileOutputStream(controlfile), format, localhost);
		validate(source, controlfile);
	}

	@Test
	public void testReadAndWritePlatformYaml() throws Exception {
		testPlatform("config/platform.yaml", Format.YAML, "target/platform.yaml");
	}

	@Test
	public void testReadAndWritePlatformJson() throws Exception {
		testPlatform("config/platform.json", Format.JSON, "target/platform.json");
	}

	private void testPlatform(String source, Format format, String output) throws Exception {
		InputStream in = getClass().getClassLoader().getResourceAsStream(source);
		Map<String, PropertyWrapper> platform = AppNGizer.Config.read(in, format);
		Assert.assertEquals(1, platform.size());
		Assert.assertEquals("appNG", platform.keySet().iterator().next());
		PropertyWrapper config = platform.get("appNG");
		Assert.assertEquals(51, config.getProperties().size());
		File controlfile = new File(output);
		AppNGizer.Config.write("appNG", new FileOutputStream(controlfile), format, config);
		validate(source, controlfile);
	}

	private void validate(String source, File controlfile) throws IOException {
		List<String> expected = Files.readAllLines(controlfile.toPath());
		List<String> actual = Files
				.readAllLines(new File(getClass().getClassLoader().getResource(source).getPath()).toPath());
		Assert.assertEquals(expected, actual);
	}

	@Ignore("Run locally")
	@Test(expected = HttpClientErrorException.class)
	public void testUploadPackage() throws Exception {
		getAppNGizer().uploadPackage("local", new File("pom.xml"));
	}

	@Test
	@Ignore("Run locally")
	public void testWriteAndReadSiteYaml() throws Exception {
		File config = new File("target/yaml/manager.yaml");
		FileOutputStream out = new FileOutputStream(config);
		AppNGizer appNGizer = getAppNGizer();
		AppNGizer.Config.readSiteProperties(appNGizer, "manager", out, Format.YAML, false);
		AppNGizer.Config.writeSiteProperties(appNGizer, "manager", new FileInputStream(config), Format.YAML);
	}

}
