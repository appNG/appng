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
package org.appng.tools.poi;

import java.awt.Color;

import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFPalette;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;

public class FontBuilder {

	private Workbook workbook;
	private short colorIdx = 55;
	private Color color;
	private short size;
	private String name;
	private boolean italic;
	private boolean strikeout;
	private boolean bold;
	private byte underline;

	public FontBuilder(Workbook workbook) {
		this.workbook = workbook;
		reset();
	}

	public FontBuilder color(String hexcode) {
		Color color = Color.decode(hexcode);
		return color(color);
	}

	public FontBuilder color(Color color) {
		this.color = color;
		return this;
	}

	public FontBuilder size(short size) {
		this.size = size;
		return this;
	}

	public FontBuilder name(String name) {
		this.name = name;
		return this;
	}

	public FontBuilder italic() {
		this.italic = true;
		return this;
	}

	public FontBuilder strikeout() {
		this.strikeout = true;
		return this;
	}

	public FontBuilder bold() {
		this.bold = true;
		return this;
	}

	public FontBuilder underline() {
		this.underline = Font.U_SINGLE;
		return this;
	}

	public FontBuilder underlineDouble() {
		this.underline = Font.U_DOUBLE;
		return this;
	}

	protected void setColor(Font font) {
		if (font instanceof XSSFFont) {
			XSSFFont xssfFont = (XSSFFont) font;
			XSSFColor colorInternal = new XSSFColor(color, null);
			xssfFont.setColor(colorInternal);
		} else if (font instanceof HSSFFont) {
			HSSFFont hssfFont = (HSSFFont) font;
			HSSFPalette customPalette = ((HSSFWorkbook) workbook).getCustomPalette();
			byte red = (byte) color.getRed();
			byte green = (byte) color.getGreen();
			byte blue = (byte) color.getBlue();
			HSSFColor colorInternal = customPalette.findColor(red, green, blue);
			if (null == colorInternal) {
				if (colorIdx < 0) {
					throw new IllegalArgumentException("no more free color index");
				}
				customPalette.setColorAtIndex((short) colorIdx--, red, green, blue);
				colorInternal = customPalette.findColor(red, green, blue);
			}
			hssfFont.setColor(colorInternal.getIndex());
		}
	}

	public Font build() {
		Font font = workbook.createFont();
		if (size > 0) {
			font.setFontHeightInPoints(size);
		}
		font.setBold(bold);
		font.setItalic(italic);
		font.setStrikeout(strikeout);
		if (underline > 0) {
			font.setUnderline(underline);
		}
		if (null != name) {
			font.setFontName(name);
		}
		if (null != color) {
			setColor(font);
		}

		return font;
	}

	public void reset() {
		color = null;
		size = -1;
		name = null;
		italic = false;
		strikeout = false;
		bold = false;
		underline = -1;
	}

}
