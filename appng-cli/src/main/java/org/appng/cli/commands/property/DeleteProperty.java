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
package org.appng.cli.commands.property;

import org.appng.api.BusinessException;
import org.appng.cli.CliEnvironment;
import org.appng.cli.ExecutableCliCommand;
import org.appng.core.domain.PropertyImpl;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Deletes a property.<br/>
 * 
 * <pre>
 * Usage: appng delete-property [options]
 *   Options:
 *   * -n
 *        The property name.
 * </pre>
 * 
 * @author Matthias Herlitzius
 * 
 */
@Parameters(commandDescription = "Deletes a property.")
public class DeleteProperty implements ExecutableCliCommand {

	@Parameter(names = "-n", required = true, description = "The property name.")
	private String name;

	public void execute(CliEnvironment cle) throws BusinessException {

		PropertyImpl prop = cle.getCoreService().getProperty(name);

		if (null != prop) {
			cle.getCoreService().deleteProperty(prop);
		} else {
			throw new BusinessException("No such property: " + name);
		}

	}

}
