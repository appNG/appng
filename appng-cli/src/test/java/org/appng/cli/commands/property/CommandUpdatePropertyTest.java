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
package org.appng.cli.commands.property;

import org.appng.api.Platform;
import org.appng.cli.ExecutableCliCommand;
import org.appng.cli.commands.AbstractCommandTest;
import org.appng.core.domain.PropertyImpl;
import org.appng.core.service.PropertySupport;
import org.junit.Assert;

/**
 * Test for {@link UpdateProperty}.
 * 
 * @author Matthias Müller
 * 
 */
public class CommandUpdatePropertyTest extends AbstractCommandTest {

	private static final String DEV_MODE = PropertySupport.PREFIX_PLATFORM + Platform.Property.DEV_MODE;
	
	public ExecutableCliCommand getCommand() {
		return new UpdateProperty(DEV_MODE, "true", null);
	}
	

	public void validate() {
		PropertyImpl property = cliEnv.getCoreService().getProperty(DEV_MODE);
		Assert.assertEquals("true", property.getActualString());
	}

}
