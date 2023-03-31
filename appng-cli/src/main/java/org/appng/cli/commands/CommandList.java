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
package org.appng.cli.commands;

import org.appng.cli.ExecutableCliCommand;
import org.appng.cli.prettytable.PrettyTable;

import com.beust.jcommander.Parameter;

/**
 * Base class for {@link ExecutableCliCommand}s that list something.
 * 
 * @author Matthias Herlitzius
 */
public abstract class CommandList {

	protected PrettyTable prettyTable = new PrettyTable();

	@Parameter(names = "-v", required = false, description = "Verbose output.")
	protected boolean beVerbose = false;

	@Parameter(names = "-t", required = false, description = "Prints tab-separated values instead of a table.")
	protected boolean tabbedValues = false;

	protected String renderTable() {
		return prettyTable.render(tabbedValues, beVerbose);
	}

	public PrettyTable getPrettyTable() {
		return prettyTable;
	}

}
