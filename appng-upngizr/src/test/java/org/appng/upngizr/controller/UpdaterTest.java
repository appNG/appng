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
		File[] files = new File("../appng-application/target").listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.startsWith("appng-application-") && name.endsWith(".war");
			}
		});
		Assume.assumeTrue(files.length == 1);
		Resource resource = new FileSystemResource(files[0]);
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
		File[] files = new File("../appng-appngizer/target").listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.startsWith("appng-appngizer-") && name.endsWith(".war");
			}
		});
		Assume.assumeTrue(files.length == 1);
		Resource resource = new FileSystemResource(files[0]);
		new Updater(new MockServletContext()).updateAppNGizer(resource, target.getAbsolutePath());
		assertFolderNotEmpty("WEB-INF");
		assertFolderNotEmpty("WEB-INF/classes/org/appng/appngizer/model/");
		assertFolderNotEmpty("WEB-INF/classes/org/appng/appngizer/controller/");
		assertFolderNotEmpty("WEB-INF/lib");
	}

	private void assertFolderNotEmpty(String path) {
		File folder = new File(target, path);
		Assert.assertTrue(folder.getAbsolutePath() + " does not exist!", folder.exists());
		Assert.assertTrue(folder.getAbsolutePath() + " is empty!", folder.listFiles().length > 0);
	}

}
