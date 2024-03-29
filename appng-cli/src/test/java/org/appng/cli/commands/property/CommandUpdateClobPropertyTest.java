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
package org.appng.cli.commands.property;

import java.io.File;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.appng.api.SiteProperties;
import org.appng.api.model.Property;
import org.appng.cli.ExecutableCliCommand;
import org.appng.cli.commands.AbstractCommandTest;
import org.appng.cli.commands.site.CommandCreateSiteTest;
import org.appng.core.domain.PropertyImpl;
import org.junit.Assert;

/**
 * Test for {@link UpdateProperty}.
 * 
 * @author Matthias Müller
 */
public class CommandUpdateClobPropertyTest extends AbstractCommandTest {

	private static final String FILE = "target/test-classes/clob.properties";
	private static final String PROP = "platform.site.appng." + SiteProperties.CACHE_EXCEPTIONS;
	private String expected;

	public ExecutableCliCommand getCommand() {
		ExecutableCliCommand command = new CommandCreateSiteTest().getCommand();
		try {
			command.execute(cliEnv);
			expected = FileUtils.readFileToString(new File(FILE), StandardCharsets.UTF_8);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return new UpdateProperty(PROP, null, FILE);
	}

	public void validate() {
		PropertyImpl property = cliEnv.getCoreService().getProperty(PROP);
		Assert.assertEquals(expected, property.getClob());
		Assert.assertEquals(Property.Type.MULTILINE, property.getType());
	}

}
