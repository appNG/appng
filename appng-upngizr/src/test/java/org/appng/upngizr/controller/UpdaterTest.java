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
package org.appng.upngizr.controller;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipException;

import org.apache.catalina.Container;
import org.apache.catalina.Host;
import org.appng.upngizr.controller.Updater.Status;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.client.RestClientException;

public class UpdaterTest {

	private String target;

	@BeforeClass
	public static void setup() {
		File appNGizer = new File("target/webapps/appNGizer");
		appNGizer.mkdirs();
		UpNGizr.appNGHome = new File("target/webapps/ROOT").getAbsolutePath();
		UpNGizr.appNGizerHome = appNGizer.getAbsolutePath();
	}

	@Test
	public void testUpdateAppNG() throws Exception {
		target = UpNGizr.appNGHome;
		Resource resource = getResource("appng-application");
		new Updater(new MockServletContext()).updateAppNG(resource, target);
		assertFolderNotEmpty("WEB-INF");
		assertFolderNotEmpty("WEB-INF/classes");
		Assert.assertFalse(new File(target, "WEB-INF/conf").exists());
		assertFolderNotEmpty("WEB-INF/lib");
	}

	@Test
	public void testUpdateAppNGizer() throws Exception {
		target = UpNGizr.appNGizerHome;
		Resource resource = getResource("appng-appngizer");
		new Updater(new MockServletContext()).updateAppNGizer(resource, target);
		assertFolderNotEmpty("WEB-INF");
		File metaInf = assertFolderNotEmpty("META-INF");
		Assert.assertTrue(new File(metaInf, "MANIFEST.MF").exists());
		assertFolderNotEmpty("WEB-INF/classes/org/appng/appngizer/model/");
		assertFolderNotEmpty("WEB-INF/classes/org/appng/appngizer/controller/");
		assertFolderNotEmpty("WEB-INF/lib");
	}

	@Test
	public void testUpdate() throws Exception {
		MockServletContext context = new MockServletContext();
		Host host = Mockito.mock(Host.class);
		context.setAttribute(UpNGizr.HOST, host);
		Container container = Mockito.mock(Container.class);
		Mockito.when(host.findChild(Mockito.anyString())).thenReturn(container);
		Updater doNotDownload = new Updater(context) {
			protected void updateAppNG(Resource resource, String appNGHome)
					throws IOException, ZipException, FileNotFoundException {
			}

			protected void updateAppNGizer(Resource resource, String appNGizerHome)
					throws RestClientException, IOException {
			}
		};

		ResponseEntity<Status> status = doNotDownload.getStatus();
		Assert.assertEquals(0d, status.getBody().getCompleted(), 0d);
		Assert.assertFalse(status.getBody().isDone());
		MockHttpServletRequest request = new MockHttpServletRequest(context);
		ResponseEntity<String> updated = doNotDownload.updateAppng("1.17.0", "myTarget", request);
		Assert.assertEquals(HttpStatus.OK, updated.getStatusCode());

		status = doNotDownload.getStatus();
		Assert.assertEquals(100d, status.getBody().getCompleted(), 0d);
		Assert.assertTrue(status.getBody().isDone());
		Assert.assertTrue(status.getBody().getTaskName().contains("<a href=\"myTarget\">"));
		Mockito.verify(host, Mockito.times(1)).findChild("");
		Mockito.verify(host, Mockito.times(1)).findChild("/" + UpNGizr.APPNGIZER);
		Mockito.verify(container, Mockito.times(2)).stop();
		Mockito.verify(container, Mockito.times(2)).start();
	}

	@Test
	public void testBlockedIP() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRemoteAddr("foobar.org");
		ResponseEntity<String> updated = new Updater(request.getServletContext()).updateAppng("1.17.0", null, request);
		Assert.assertEquals(HttpStatus.FORBIDDEN, updated.getStatusCode());
	}

	@Test
	public void testCheckVersionAvailable() throws IOException {
		ResponseEntity<Void> checkVersionAvailable = new Updater(new MockServletContext())
				.checkVersionAvailable("1.17.0", new MockHttpServletRequest());
		Assert.assertEquals(HttpStatus.OK, checkVersionAvailable.getStatusCode());
	}

	@Test
	public void testCheckVersionNotAvailable() throws IOException {
		ResponseEntity<Void> checkVersionAvailable = new Updater(new MockServletContext())
				.checkVersionAvailable("0.8.15", new MockHttpServletRequest());
		Assert.assertEquals(HttpStatus.NOT_FOUND, checkVersionAvailable.getStatusCode());
	}

	@Test
	public void testGetStartPage() throws IOException, URISyntaxException {
		ResponseEntity<String> startPage = new Updater(new MockServletContext()).getStartPage("1.17.0", "myTarget",
				new MockHttpServletRequest());
		Assert.assertEquals(HttpStatus.OK, startPage.getStatusCode());
		List<String> lines = Files.readAllLines(new File("src/test/resources/startpage.html").toPath());
		List<String> actualLines = Arrays.asList(startPage.getBody().split(System.lineSeparator()));
		int size = lines.size();
		Assert.assertEquals(size, actualLines.size());
		for (int i = 0; i < size; i++) {
			Assert.assertEquals("error in line " + (i + 1), lines.get(i), actualLines.get(i));
		}
	}

	@Test
	public void testGetStartPageWithLocalHostName() throws IOException, URISyntaxException {
		MockServletContext context = new MockServletContext();
		context.setInitParameter("useFQDN", "true");
		ResponseEntity<String> startPage = new Updater(context).getStartPage("1.17.0", "myTarget",
				new MockHttpServletRequest());
		Assert.assertEquals(HttpStatus.OK, startPage.getStatusCode());
		Assert.assertTrue(startPage.getBody().contains(InetAddress.getLocalHost().getCanonicalHostName()));
	}

	protected Resource getResource(String artifactName) {
		File[] files = new File("../" + artifactName + "/target").listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.startsWith(artifactName) && name.endsWith(".war");
			}
		});
		Assume.assumeTrue(files.length == 1);
		return new FileSystemResource(files[0]);
	}

	private File assertFolderNotEmpty(String path) {
		File folder = new File(target, path);
		Assert.assertTrue(folder.getAbsolutePath() + " does not exist!", folder.exists());
		Assert.assertTrue(folder.getAbsolutePath() + " is empty!", folder.listFiles().length > 0);
		return folder;
	}

}
