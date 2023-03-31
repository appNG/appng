/*
 * Copyright 2011-2023 the original author or authors.
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
package org.appng.core.domain;

import java.io.File;
import java.text.ParseException;

import org.appng.core.model.PackageArchive;
import org.junit.Assert;
import org.junit.Test;

public class PackageArchiveTest {

	private static final String ZIP = ".zip";
	private static final String LOCATION = "src/test/resources/zip";
	public static final String VERSION = "1.5.2";
	public static final String NAME = "demo-application";
	public static final String TIMESTAMP = "2012-11-27-1305";

	@Test
	public void testValidArchive() throws ParseException {
		PackageArchive applicationArchive = getPackageArchive();
		Assert.assertTrue(applicationArchive.isValid());
	}

	@Test
	public void testInvalidArchive() throws ParseException {
		PackageArchive applicationArchive = getInvalidArchive();
		Assert.assertFalse(applicationArchive.isValid());
	}

	@Test
	public void testArchiveNonStrict() throws ParseException {
		File applicationFile = new File(LOCATION, NAME + "-" + VERSION + "-" + TIMESTAMP + ZIP);
		Assert.assertTrue(new PackageArchiveImpl(applicationFile, false).isValid());

		applicationFile = new File(LOCATION, NAME + "-1.5.1" + ZIP);
		Assert.assertTrue(new PackageArchiveImpl(applicationFile, false).isValid());
	}

	public static PackageArchive getPackageArchive() {
		File applicationFile = new File(LOCATION, NAME + "-" + VERSION + "-" + TIMESTAMP + ZIP);
		return new PackageArchiveImpl(applicationFile, true);
	}

	public static PackageArchive getInvalidArchive() {
		File applicationFile = new File(LOCATION, NAME + "-" + "1.5.3" + "-" + TIMESTAMP + ZIP);
		return new PackageArchiveImpl(applicationFile, true);
	}

}
