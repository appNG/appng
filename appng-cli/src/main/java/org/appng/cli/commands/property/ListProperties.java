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

import org.apache.commons.lang3.StringUtils;
import org.appng.api.BusinessException;
import org.appng.api.model.Property;
import org.appng.cli.CliEnvironment;
import org.appng.cli.ExecutableCliCommand;
import org.appng.cli.commands.CommandList;
import org.appng.cli.prettytable.TableConstants;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Lists the available sites.<br/>
 * 
 * <pre>
 * Usage: appng list-properties [options]
 *   Options:
 *     -a
 *        List only properties of the specified application.
 *     -c
 *        Returns only changed properties.
 *        Default: false
 *     -f
 *        Returns only properties containing the filter expression in their name or
 *        value (case-insensitive).
 *     -s
 *        List only properties of the specified site.
 *     -t
 *        Prints tab-separated values instead of a table.
 *        Default: false
 *     -v
 *        Verbose output.
 *        Default: false
 * </pre>
 * 
 * @author Matthias Herlitzius
 */
@Parameters(commandDescription = "Lists the available properties.")
public class ListProperties extends CommandList implements ExecutableCliCommand {

	@Parameter(names = "-s", required = false, description = "List only properties of the specified site.")
	private String siteName;

	@Parameter(names = "-a", required = false, description = "List only properties of the specified application.")
	private String applicationName;

	@Parameter(names = "-f", required = false, description = "Returns only properties containing the filter expression in their name or value (case-insensitive).")
	private String filterExpression;

	@Parameter(names = "-c", required = false, description = "Returns only changed properties.")
	private boolean changedValues = false;

	public ListProperties() {

	}

	ListProperties(String siteName) {
		this.siteName = siteName;
	}

	public void execute(CliEnvironment cle) throws BusinessException {

		Iterable<? extends Property> properties = cle.getCoreService().getPropertiesList(siteName, applicationName);

		if (null != properties) {

			prettyTable.addColumn(TableConstants.NAME);
			prettyTable.addColumn(TableConstants.VALUE);
			prettyTable.addColumn(TableConstants.DEFAULTVALUE, true);
			prettyTable.addColumn(TableConstants.CHANGED, true);

			for (Property property : properties) {
				if (printRow(property, changedValues, filterExpression)) {
					prettyTable.addRow(property.getName(), property.getString(), property.getDefaultString(),
							hasChangedDefaultValue(property));
				}
			}

			cle.setResult(renderTable());

		}

	}

	protected boolean printRow(Property property, boolean showOnlyChangedValues, String filterValue) {
		boolean hasChangedDefaultValue = hasChangedDefaultValue(property);
		boolean doFilter = doFilter(property, filterValue);
		return (showOnlyChangedValues && hasChangedDefaultValue && doFilter) || (!showOnlyChangedValues && doFilter);
	}

	private boolean doFilter(Property property, String filterValue) {
		boolean hasFilter = StringUtils.isNotBlank(filterValue);
		boolean filterMatch = StringUtils.containsIgnoreCase(property.getName(), filterValue)
				|| StringUtils.containsIgnoreCase(property.getString(), filterValue);
		return !hasFilter || filterMatch;
	}

	private boolean hasChangedDefaultValue(Property property) {
		return !StringUtils.equals(property.getString(), property.getDefaultString());
	}

}
