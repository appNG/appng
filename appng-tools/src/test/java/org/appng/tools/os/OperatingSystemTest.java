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
package org.appng.tools.os;

import org.junit.Assert;
import org.junit.Test;

public class OperatingSystemTest {

	@Test
	public void testOsDetection() {
		Assert.assertEquals(OperatingSystem.LINUX, OperatingSystem.detect("Debian GNU/Linux Stretch"));
		Assert.assertEquals(OperatingSystem.LINUX, OperatingSystem.detect("Unix"));
		Assert.assertEquals(OperatingSystem.LINUX, OperatingSystem.detect("Minix"));
		Assert.assertEquals(OperatingSystem.WINDOWS, OperatingSystem.detect("Microsoft Windows Server 2016"));
		Assert.assertEquals(OperatingSystem.MACOSX, OperatingSystem.detect("Mac OS X"));
		Assert.assertEquals(OperatingSystem.OTHER, OperatingSystem.detect("MS DOS 5.0"));
		Assert.assertEquals(OperatingSystem.OTHER, OperatingSystem.detect(""));
		Assert.assertEquals(OperatingSystem.OTHER, OperatingSystem.detect(null));

		OperatingSystem os = OperatingSystem.detect();
		if (OperatingSystem.isLinux()) {
			Assert.assertEquals(OperatingSystem.LINUX, os);
			Assert.assertTrue(OperatingSystem.isOs(OperatingSystem.LINUX));
			Assert.assertFalse(OperatingSystem.isOs(OperatingSystem.WINDOWS));
			Assert.assertFalse(OperatingSystem.isOs(OperatingSystem.MACOSX));
			Assert.assertFalse(OperatingSystem.isOs(OperatingSystem.OTHER));
		}
		if (OperatingSystem.isWindows()) {
			Assert.assertEquals(OperatingSystem.WINDOWS, os);
			Assert.assertFalse(OperatingSystem.isOs(OperatingSystem.LINUX));
			Assert.assertTrue(OperatingSystem.isOs(OperatingSystem.WINDOWS));
			Assert.assertFalse(OperatingSystem.isOs(OperatingSystem.MACOSX));
			Assert.assertFalse(OperatingSystem.isOs(OperatingSystem.OTHER));
		}
		if (OperatingSystem.isMac()) {
			Assert.assertEquals(OperatingSystem.MACOSX, os);
			Assert.assertFalse(OperatingSystem.isOs(OperatingSystem.LINUX));
			Assert.assertFalse(OperatingSystem.isOs(OperatingSystem.WINDOWS));
			Assert.assertTrue(OperatingSystem.isOs(OperatingSystem.MACOSX));
			Assert.assertFalse(OperatingSystem.isOs(OperatingSystem.OTHER));
		}
		if (OperatingSystem.isOther()) {
			Assert.assertEquals(OperatingSystem.OTHER, os);
			Assert.assertFalse(OperatingSystem.isOs(OperatingSystem.LINUX));
			Assert.assertFalse(OperatingSystem.isOs(OperatingSystem.WINDOWS));
			Assert.assertFalse(OperatingSystem.isOs(OperatingSystem.MACOSX));
			Assert.assertTrue(OperatingSystem.isOs(OperatingSystem.OTHER));
		}
	}

}
