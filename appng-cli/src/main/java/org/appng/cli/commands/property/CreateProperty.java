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
package org.appng.cli.commands.property;

import org.appng.api.BusinessException;
import org.appng.cli.CliEnvironment;
import org.appng.cli.ExecutableCliCommand;
import org.appng.cli.validators.FileExists;
import org.appng.core.domain.PropertyImpl;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Creates a property.<br/>
 * 
 * <pre>
 * Usage: appng create-property [options]
 *   Options:
 *     -a
 *        The application id.
 *     -c
 *        The name of the file containing the clob value. Mutually exclusive with -v.
 *   * -n
 *        The property name.
 *     -s
 *        The site id.
 *     -v
 *        The property value. Mutually exclusive with -c.
 * </pre>
 * 
 * @author Matthias Herlitzius
 * 
 */
@Parameters(commandDescription = "Creates a property.")
public class CreateProperty implements ExecutableCliCommand {

	@Parameter(names = "-s", required = false, description = "The site id.")
	private Integer siteId;

	@Parameter(names = "-a", required = false, description = "The application id.")
	private Integer applicationId;

	@Parameter(names = "-n", required = true, description = "The property name.")
	private String name;

	@Parameter(names = "-v", required = false, description = "The property value. Mutually exclusive with -c.")
	private String value;

	@Parameter(names = "-c", required = false, description = "The name of the file containing the clob value. Mutually exclusive with -v.", validateWith = FileExists.class)
	private String clob;

	public CreateProperty() {

	}

	CreateProperty(Integer siteId, Integer applicationId, String name, String value, String clob) {
		this.siteId = siteId;
		this.applicationId = applicationId;
		this.name = name;
		this.value = value;
		this.clob = clob;
	}

	public void execute(CliEnvironment cle) throws BusinessException {
		PropertyHelper.validate(value, clob);
		PropertyImpl property = new PropertyImpl(name, null);
		PropertyHelper.update(property, value, clob, true);
		cle.getCoreService().createProperty(siteId, applicationId, property);
	}

}
