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
package org.appng.tools.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class PropertyConstantCreatorTest {

	@Test
	public void test() throws IOException {
		String className = "org.appng.tools.file.Constants";
		String path = className.replace('.', '/') + ".java";
		String target = "target/constants/";

		PropertyConstantCreator.main(new String[] { "src/test/resources/props.properties", className, target });
		List<String> expected = Files.readAllLines(new File("src/test/resources/" + path.substring(path.lastIndexOf('/'))).toPath());
		List<String> actual = Files.readAllLines(new File(target + path).toPath());

		Assert.assertEquals(expected, actual);
	}

}
