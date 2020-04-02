/*
 * Copyright 2011-2020 the original author or authors.
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
import org.apache.poi.ss.usermodel.CellStyle;
import org.appng.tools.poi.Border.BorderLocation;

public class Borders {

	public static class BorderBuilder {

		private Borders borders;

		public BorderBuilder() {
			this.borders = new Borders();
		}

		public BorderBuilder left(BorderStyle style) {
			borders.left(style);
			return this;
		}

		public BorderBuilder right(BorderStyle style) {
			borders.right(style);
			return this;
		}

		public BorderBuilder top(BorderStyle style) {
			borders.top(style);
			return this;
		}

		public BorderBuilder bottom(BorderStyle style) {
			borders.bottom(style);
			return this;
		}

		public Borders build() {
			return borders;
		}
	}

	private Border left;
	private Border right;
	private Border top;
	private Border bottom;

	public Borders() {

	}

	public Borders(BorderStyle left, BorderStyle right, BorderStyle top, BorderStyle bottom) {
		left(left);
		right(right);
		top(top);
		bottom(bottom);
	}

	public Border left() {
		return left;
	}

	public Border right() {
		return right;
	}

	public Border top() {
		return top;
	}

	public Border bottom() {
		return bottom;
	}

	public void applyTo(CellStyle cellStyle) {
		applyBorder(cellStyle, left);
		applyBorder(cellStyle, right);
		applyBorder(cellStyle, top);
		applyBorder(cellStyle, bottom);
	}

	private void applyBorder(CellStyle cellStyle, Border border) {
		if (null != border) {
			border.applyTo(cellStyle);
		}
	}

	private Border getBorder(BorderLocation location, BorderStyle style) {
		if (null != style) {
			return new Border(location, style);
		}
		return new Border(location, BorderStyle.NONE);
	}

	public Borders left(BorderStyle style) {
		this.left = getBorder(BorderLocation.LEFT, style);
		return this;
	}

	public Borders right(BorderStyle style) {
		this.right = getBorder(BorderLocation.RIGHT, style);
		return this;
	}

	public Borders top(BorderStyle style) {
		this.top = getBorder(BorderLocation.TOP, style);
		return this;
	}

	public Borders bottom(BorderStyle style) {
		this.bottom = getBorder(BorderLocation.BOTTOM, style);
		return this;
	}

	public String toString() {
		return left + ", " + right + ", " + top + ", " + bottom;
	}
}
