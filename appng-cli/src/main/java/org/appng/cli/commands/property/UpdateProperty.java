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

import org.appng.api.BusinessException;
import org.appng.cli.CliEnvironment;
import org.appng.cli.ExecutableCliCommand;
import org.appng.cli.validators.FileExists;
import org.appng.core.domain.PropertyImpl;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Updates a property.<br/>
 * 
 * <pre>
 * Usage: appng update-property [options]
 *   Options:
 *     -c
 *        The name of the file containing the clob value. Mutually exclusive with -v.
 *   * -n
 *        The property name.
 *     -v
 *        The property value. Mutually exclusive with -c.
 * </pre>
 * 
 * @author Matthias Herlitzius
 * 
 */
@Parameters(commandDescription = "Updates a property.")
public class UpdateProperty implements ExecutableCliCommand {

	@Parameter(names = "-n", required = true, description = "The property name.")
	private String name;

	@Parameter(names = "-v", required = false, description = "The property value. Mutually exclusive with -c.")
	private String value;

	@Parameter(names = "-c", required = false, description = "The name of the file containing the clob value. Mutually exclusive with -v.", validateWith = FileExists.class)
	private String clob;

	public UpdateProperty() {
	}

	UpdateProperty(String name, String value, String clob) {
		this.name = name;
		this.value = value;
		this.clob = clob;
	}

	public void execute(CliEnvironment cle) throws BusinessException {
		PropertyHelper.validate(value, clob);
		PropertyImpl property = cle.getCoreService().getProperty(name);
		if (null != property) {
			PropertyHelper.update(property, value, clob, false);
			cle.getCoreService().saveProperty(property);
		} else {
			throw new BusinessException("No such property: " + name);
		}

	}
}
