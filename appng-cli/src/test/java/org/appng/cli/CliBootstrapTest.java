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
package org.appng.cli;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

/**
 * Test for {@link CliBootstrap}.
 * 
 * @author Matthias Herlitzius
 */
public class CliBootstrapTest {

	public static final String TARGET = "target";
	public static final String BOOTSTRAP_ROOT = "/bootstrapRoot";
	private static final String WEBINF_BIN = "/WEB-INF/bin";
	private static final String APPNG_ROOT = "appng-root";

	private static PathTool path;

	@Mock
	protected CliBootstrapEnvironment cliBootstrapEnvironment;

	@BeforeClass
	public static void setUpBeforeClass() {
		path = new PathTool(TARGET + BOOTSTRAP_ROOT);
		try {
			FileUtils.forceMkdir(path.getFile(APPNG_ROOT + WEBINF_BIN));
		} catch (IOException e) {
			Assert.fail(e.getMessage());
		}

	}

	@AfterClass
	public static void tearDownAfterClass() {
		FileUtils.deleteQuietly(path.getBasePath());
	}

	@Before
	public void setUp() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	public void testAppngHome() {
		File result = path.getFile(APPNG_ROOT);
		Mockito.when(cliBootstrapEnvironment.getFileFromEnv(CliBootstrap.APPNG_HOME)).thenReturn(result);
		Assert.assertEquals(result, CliBootstrap.getPlatformRootPath(cliBootstrapEnvironment));
	}

	@Test
	public void testAppngHomeUndefined() {
		try {
			CliBootstrap.getPlatformRootPath(cliBootstrapEnvironment);
			Assert.fail("IllegalArgumentException expected, but no exception has been thrown.");
		} catch (IllegalArgumentException e) {
			Assert.assertEquals("APPNG_HOME is not defined!", e.getMessage());
		}

	}

	@Test
	public void testAppngHomeInvalid() {
		File result = path.getFile("invalid-path");
		Mockito.when(cliBootstrapEnvironment.getFileFromEnv(CliBootstrap.APPNG_HOME)).thenReturn(result);
		try {
			CliBootstrap.getPlatformRootPath(cliBootstrapEnvironment);
			Assert.fail("IllegalArgumentException expected, but no exception has been thrown.");
		} catch (IllegalArgumentException e) {
			Assert.assertEquals("The path specified in APPNG_HOME does not exist: " + result, e.getMessage());
		}

	}

	private static class PathTool {

		private final File basePath;

		private PathTool(String basePath) {
			String workingDir = new File("").getAbsolutePath();
			this.basePath = new File(workingDir, basePath);
		}

		private File getFile(String path) {
			return new File(basePath, path);
		}

		private File getBasePath() {
			return basePath;
		}

	}

}
