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
package org.appng.cli.prettytable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.appng.api.BusinessException;

/**
 * Provides the ability to print some pretty formatted tables to the console.
 * 
 * @author Matthias Herlitzius
 * 
 */
public class PrettyTable {

	private static final int CELL_MARGIN = 1;
	private static final String NL = "\n";
	private static final String TAB = "\t";
	private static final char SPACE = ' ';
	private static final String VERTICAL_BORDER = "|";
	private static final char DOUBLE_LINE = '=';
	private static final char SINGLE_LINE = '-';

	private List<TableColumn> columns = new ArrayList<>();
	private List<TableRow> rows = new ArrayList<>();
	private Map<String, TableColumn> columnMap = new HashMap<>();
	private int tableWidth = 0;

	public PrettyTable() {
	}

	public void addColumn(String name) {
		addColumn(name, false);
	}

	public void addColumn(String name, boolean isVerbose) {
		TableColumn col = new TableColumn(name, isVerbose, columns.size());
		columns.add(col);
		columnMap.put(name, col);
	}

	public void addRow(Object... values) throws BusinessException {
		if (values.length == columns.size()) {
			rows.add(new TableRow(columns, values));
		} else {
			throw new BusinessException("Number of header columns (" + columns.size()
					+ ") is not equal to number of row columns (" + values.length + ")");
		}
	}

	public String render(boolean tabbedValues, boolean beVerbose) {
		boolean withBorders = !tabbedValues;
		tableWidth = 0;
		StringBuilder builder = new StringBuilder();
		builder = (withBorders) ? builder.append(PrettyTable.VERTICAL_BORDER) : builder;

		int noOfColumns = 0;

		for (TableColumn column : columns) {
			String col = column.render(withBorders, beVerbose);
			builder = (null != col) ? builder.append(col) : builder;
			if (!column.isVerbose() || beVerbose) {
				tableWidth += column.getCellWidth();
				noOfColumns++;
			}
			if (!withBorders && column.equals(columns.get(columns.size() - 1))) {
				builder.deleteCharAt(builder.length() - 1);
			}
		}

		tableWidth += ((noOfColumns + 1) * PrettyTable.VERTICAL_BORDER.length()) + (CELL_MARGIN * noOfColumns * 2);

		builder = builder.insert(0, newLine(withBorders, DOUBLE_LINE));
		builder.append(newLine(withBorders, DOUBLE_LINE));

		for (TableRow row : rows) {
			builder = (withBorders) ? builder.append(PrettyTable.VERTICAL_BORDER) : builder;
			builder.append(row.render(columns, withBorders, beVerbose));
			builder.append(newLine(withBorders, SINGLE_LINE));
		}

		return builder.toString();

	}

	private StringBuilder newLine(boolean withBorders, char c) {
		if (withBorders) {
			return newLine(c);
		} else {
			return newLine();
		}
	}

	private StringBuilder newLine(char lineChar) {
		return newLine().append(addSpace(tableWidth, lineChar)).append(NL);
	}

	private StringBuilder newLine() {
		return new StringBuilder().append(NL);
	}

	static String bordered(String n, int columnWidth) {
		int space = columnWidth - n.length();
		return addSpace(CELL_MARGIN, SPACE) + n + addSpace(space, ' ') + addSpace(CELL_MARGIN, SPACE)
				+ PrettyTable.VERTICAL_BORDER;
	}

	static String tabbed(String n) {
		return n + PrettyTable.TAB;
	}

	private static StringBuilder addSpace(int space, char c) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < space; i++) {
			builder.append(c);
		}
		return builder;
	}

	public List<TableRow> getRows() {
		return Collections.unmodifiableList(rows);
	}

	public int getColumnIndex(String id) {
		return columns.indexOf(columnMap.get(id));
	}

}
