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
package org.appng.cli.prettytable;

/**
 * A table column for a {@link PrettyTable}.
 * 
 * @author Matthias Herlitzius
 */
class TableColumn {

	private final String name;
	private final boolean isVerbose;
	private final int index;
	private int columnWidth;

	/**
	 * Creates a new {@code TableColumn}
	 * 
	 * @param name
	 *                  the name of the column
	 * @param isVerbose
	 *                  whether this column should only be displayed in verbose mode
	 * @param index
	 *                  the index of this column
	 */
	TableColumn(String name, boolean isVerbose, int index) {
		this.name = name;
		this.isVerbose = isVerbose;
		this.index = index;
		this.columnWidth = name.length();
	}

	String getName() {
		return name;
	}

	boolean isVerbose() {
		return isVerbose;
	}

	int getIndex() {
		return index;
	}

	void setCellWidth(int cellWidth) {
		if (cellWidth > columnWidth) {
			columnWidth = cellWidth;
		}
	}

	public int getCellWidth() {
		return columnWidth;
	}

	public String render(boolean withBorders, boolean beVerbose) {
		if (!isVerbose || beVerbose) {
			if (withBorders) {
				return PrettyTable.bordered(name, columnWidth);
			} else {
				return PrettyTable.tabbed(name);
			}
		} else {
			return null;
		}
	}

}
