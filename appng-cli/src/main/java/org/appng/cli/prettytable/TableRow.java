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
package org.appng.cli.prettytable;

import java.util.ArrayList;
import java.util.List;

/**
 * A table row for a {@link PrettyTable}.
 * 
 * @author Matthias Herlitzius
 */
public class TableRow {

	private final List<String> values = new ArrayList<>();

	TableRow(List<TableColumn> columns, Object[] values) {
		for (int i = 0; i < values.length; i++) {
			String value = String.valueOf(values[i]);
			this.values.add(value);
			columns.get(i).setCellWidth(value.length());
		}
	}

	public StringBuilder render(List<TableColumn> columns, boolean withBorders, boolean beVerbose) {

		StringBuilder builder = new StringBuilder();

		for (int i = 0; i < values.size(); i++) {

			TableColumn tableColumn = columns.get(i);
			boolean isVerbose = tableColumn.isVerbose();

			if (!isVerbose || beVerbose) {
				if (withBorders) {
					builder.append(PrettyTable.bordered(values.get(i), tableColumn.getCellWidth()));
				} else {
					builder.append(PrettyTable.tabbed(values.get(i)));
					if (i == columns.size() - 1) {
						builder.deleteCharAt(builder.length() - 1);
					}
				}
			}

		}

		return builder;
	}

	public String getValueAt(int index) {
		return values.get(index);
	}

}
