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
package org.appng.tools.os;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class CommandTest {

	@Test
	public void testDirectoryListing() {
		StringConsumer outputConsumer = new StringConsumer();
		if (OperatingSystem.isWindows()) {
			Command.execute("dir /b /on", outputConsumer, null);
		} else {
			Command.execute("ls", outputConsumer, null);
		}
		List<String> result = outputConsumer.getResult();
		if (null != result) {
			Assert.assertEquals(Arrays.asList("pom.xml", "src", "target"), result);
		}
	}

	@Test
	public void testWrongOs() {
		int result = 0;
		if (OperatingSystem.isWindows()) {
			result = Command.execute(OperatingSystem.LINUX, "dummy", null, null);
		} else {
			result = Command.execute(OperatingSystem.WINDOWS, "dummy", null, null);
		}
		Assert.assertEquals(Command.WRONG_OS, result);
	}
}
