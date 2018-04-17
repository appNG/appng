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
package org.appng.upngizr.controller;

import java.io.File;
import java.io.FilenameFilter;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockServletContext;

public class UpdaterTest {

	private File target;

	@Test
	public void testUpdateAppNG() throws Exception {
		target = new File("target/appNG");
		Resource resource = getResource("appng-application");
		new Updater(new MockServletContext()).updateAppNG(resource, target.getAbsolutePath());
		assertFolderNotEmpty("WEB-INF");
		assertFolderNotEmpty("WEB-INF/classes");
		assertFolderNotEmpty("WEB-INF/conf");
		assertFolderNotEmpty("WEB-INF/lib");
	}

	@Test
	public void testUpdateAppNGizer() throws Exception {
		target = new File("target/appNGizer");
		target.mkdirs();
		Resource resource = getResource("appng-appngizer");
		new Updater(new MockServletContext()).updateAppNGizer(resource, target.getAbsolutePath());
		assertFolderNotEmpty("WEB-INF");
		assertFolderNotEmpty("WEB-INF/classes/org/appng/appngizer/model/");
		assertFolderNotEmpty("WEB-INF/classes/org/appng/appngizer/controller/");
		assertFolderNotEmpty("WEB-INF/lib");
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

	private void assertFolderNotEmpty(String path) {
		File folder = new File(target, path);
		Assert.assertTrue(folder.getAbsolutePath() + " does not exist!", folder.exists());
		Assert.assertTrue(folder.getAbsolutePath() + " is empty!", folder.listFiles().length > 0);
	}

}
