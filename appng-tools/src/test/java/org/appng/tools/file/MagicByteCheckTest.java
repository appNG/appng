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
package org.appng.tools.file;

import java.io.File;
import java.net.URL;

import org.apache.commons.io.FilenameUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * TODO insert description
 * 
 * @author Claus Stümke, aiticon GmbH, 2016
 *
 */
public class MagicByteCheckTest {

	@Test
	@Ignore
	public void testFilesOnLocalMachine() {
		File directory = new File("/home/cstuemke/Documents");
		checkDir(directory, true);
	}

	@Test
	public void testWrongExtension() {
		URL resource = MagicByteCheckTest.class.getClassLoader().getResource("images/wrong_extension");
		File dir = new File(resource.getPath());
		checkDir(dir, false);
	}

	@Test
	public void testRightExtension() {
		URL resource = MagicByteCheckTest.class.getClassLoader().getResource("images/right_extension");
		File dir = new File(resource.getPath());
		checkDir(dir, true);
	}

	private void checkDir(File directory, boolean expected) {
		for (File f : directory.listFiles()) {
			if (!f.isDirectory()) {
				boolean result = MagicByteCheck.compareFileExtensionWithMagicBytes(f);
				boolean matches = expected == result;
				if (!matches) {
					String ext = MagicByteCheck.getExtensionByMagicBytes(f);
					String message = String.format("expected type '%s' for %s, but was '%s'",
							FilenameUtils.getExtension(f.getName()), f.getName(), ext);
					Assert.fail(message);
				}
			}
		}
	}
}
