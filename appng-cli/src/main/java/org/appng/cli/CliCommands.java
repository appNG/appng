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
package org.appng.cli;

import java.util.HashMap;
import java.util.Map;

import org.appng.api.BusinessException;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

/**
 * A container class for registering/retrieving {@link ExecutableCliCommand}s by name.
 * 
 * @author Matthias Herlitzius
 * 
 */
public class CliCommands {

	private Map<String, ExecutableCliCommand> commands = new HashMap<>();
	private JCommander jc;

	public CliCommands(JCommander jc) {
		this.jc = jc;
	}

	/**
	 * Registers the given {@link ExecutableCliCommand} under the given name.
	 * 
	 * @param cliName
	 *            the name used for registering
	 * @param command
	 *            the {@link ExecutableCliCommand} to register
	 */
	public void add(String cliName, ExecutableCliCommand command) {
		commands.put(cliName, command);
		jc.addCommand(cliName, command);
	}

	/**
	 * Retrieves the {@link ExecutableCliCommand} registered under the given name.
	 * 
	 * @param parsedCommand
	 *            the name of the registered {@link ExecutableCliCommand}
	 * @return the non-{@code null} {@link ExecutableCliCommand}
	 * @throws ParameterException
	 *             if no {@link ExecutableCliCommand} was registered under the given name
	 */
	public ExecutableCliCommand getCommand(String parsedCommand) throws ParameterException {
		if (null == parsedCommand) {
			return new ExecutableCliCommand() {
				public void execute(CliEnvironment cle) throws BusinessException {
					// nothing to do.
				}
			};
		} else if (commands.containsKey(parsedCommand)) {
			return commands.get(parsedCommand);
		}
		throw new ParameterException("unknown command: " + parsedCommand);
	}

}
