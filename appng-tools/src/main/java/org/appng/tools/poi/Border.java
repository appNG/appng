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
package org.appng.tools.poi;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;

public class Border {

	public static final Border BOTTOM_THICK = new Border(BorderLocation.BOTTOM, BorderStyle.THICK);
	public static final Border BOTTOM_THIN = new Border(BorderLocation.BOTTOM, BorderStyle.THIN);
	public static final Border BOTTOM_DASHED = new Border(BorderLocation.BOTTOM, BorderStyle.DASHED);
	public static final Border BOTTOM_DOUBLE = new Border(BorderLocation.BOTTOM, BorderStyle.DOUBLE);
	public static final Border BOTTOM_DOTTED = new Border(BorderLocation.BOTTOM, BorderStyle.DOTTED);

	public static final Border TOP_THICK = new Border(BorderLocation.TOP, BorderStyle.THICK);
	public static final Border TOP_THIN = new Border(BorderLocation.TOP, BorderStyle.THIN);
	public static final Border TOP_DASHED = new Border(BorderLocation.TOP, BorderStyle.DASHED);
	public static final Border TOP_DOUBLE = new Border(BorderLocation.TOP, BorderStyle.DOUBLE);
	public static final Border TOP_DOTTED = new Border(BorderLocation.TOP, BorderStyle.DOTTED);

	public static final Border RIGHT_THICK = new Border(BorderLocation.RIGHT, BorderStyle.THICK);
	public static final Border RIGHT_THIN = new Border(BorderLocation.RIGHT, BorderStyle.THIN);
	public static final Border RIGHT_DASHED = new Border(BorderLocation.RIGHT, BorderStyle.DASHED);
	public static final Border RIGHT_DOUBLE = new Border(BorderLocation.RIGHT, BorderStyle.DOUBLE);
	public static final Border RIGHT_DOTTED = new Border(BorderLocation.RIGHT, BorderStyle.DOTTED);

	public static final Border LEFT_THICK = new Border(BorderLocation.LEFT, BorderStyle.THICK);
	public static final Border LEFT_THIN = new Border(BorderLocation.LEFT, BorderStyle.THIN);
	public static final Border LEFT_DASHED = new Border(BorderLocation.LEFT, BorderStyle.DASHED);
	public static final Border LEFT_DOUBLE = new Border(BorderLocation.LEFT, BorderStyle.DOUBLE);
	public static final Border LEFT_DOTTED = new Border(BorderLocation.LEFT, BorderStyle.DOTTED);

	private final BorderLocation location;
	private final BorderStyle style;

	public Border(BorderLocation location, BorderStyle style) {
		this.location = location;
		this.style = style;
	}

	public void applyTo(CellStyle cellStyle) {
		switch (location) {
		case BOTTOM:
			cellStyle.setBorderBottom(style);
			break;
		case TOP:
			cellStyle.setBorderTop(style);
			break;
		case LEFT:
			cellStyle.setBorderLeft(style);
			break;
		case RIGHT:
			cellStyle.setBorderRight(style);
			break;
		}
	}

	public void applyTo(Cell cell) {
		applyTo(cell.getCellStyle());
	}

	public enum BorderLocation {
		BOTTOM, LEFT, RIGHT, TOP;
	}

	public String toString() {
		return location.name() + ": " + style.name();
	}
}
