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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.appng.tools.poi.Borders.BorderBuilder;

/**
 * A utility-class to support easier creation of a {@link Workbook}
 * 
 * @author Matthias Müller, 2012
 */
public class WorkBookHelper {

	private static final int ADD_COL_WIDTH = 512;
	private static final int POI_COLUMN_MAXWITH = 65280;
	private Workbook wb;
	private CreationHelper creationHelper;
	private Map<Integer, Integer> maxRowPerSheet = new HashMap<>();
	private Font hyperlinkFont;
	private Font headerFont;
	private FontBuilder fontbuilder;
	private CellStyle defaultCellStyle;
	private CellStyle headerCellStyle;

	public WorkBookHelper(Workbook wb) {
		this.wb = wb;
		this.creationHelper = wb.getCreationHelper();
	}

	public Sheet createSheet() {
		return wb.createSheet();
	}

	public Cell addCell(Row row, int column) {
		return addCell(row, column, getDefaultCellstyle());
	}

	public Cell addCell(Row row, int column, CellStyle cellStyle) {
		Cell cell = row.getCell(column);
		if (null == cell) {
			cell = row.createCell(column);
			cell.setCellStyle(cellStyle);
		}
		return cell;
	}

	public Cell addCell(Row row, int column, String value) {
		return addCell(row, column, value, getDefaultCellstyle());
	}

	public Cell addCell(Row row, int column, String value, CellStyle cellStyle) {
		Cell cell = addCell(row, column, cellStyle);
		cell.setCellValue(value);
		return cell;
	}

	public Cell addHeaderCell(Row row, int column, String value) {
		return addCell(row, column, value, getHeaderCellStyle(), getHeaderFont(), (Borders) null);
	}

	public Cell addCell(Row row, int column, String value, Font font) {
		return addCell(row, column, value, getDefaultCellstyle(), font, (Borders) null);
	}

	public Cell addCell(Row row, int column, String value, Border border) {
		return addCell(row, column, value, getDefaultCellstyle(), null, border);
	}

	public Cell addCell(Row row, int column, String value, Borders borders) {
		return addCell(row, column, value, getDefaultCellstyle(), null, borders);
	}

	public Cell addCell(Row row, int column, String value, CellStyle cellStyle, Font font, Border border) {
		Cell cell = addCell(row, column, value, cellStyle);
		if (null != font) {
			cell.getCellStyle().setFont(font);
		}
		if (null != border) {
			border.applyTo(cell.getCellStyle());
		}
		return cell;
	}

	public Cell addCell(Row row, int column, String value, CellStyle cellStyle, Font font, Borders borders) {
		CellStyle usedCellStyle;
		if (null != borders || null != font) {
			usedCellStyle = wb.createCellStyle();
			usedCellStyle.cloneStyleFrom(cellStyle);
		} else {
			usedCellStyle = cellStyle;
		}

		Cell cell = addCell(row, column, value, usedCellStyle);

		if (null != font) {
			usedCellStyle.setFont(font);
		}
		if (null != borders) {
			borders.applyTo(usedCellStyle);
		}
		return cell;
	}

	public Cell addHyperLinkCell(Row row, int column, String value, String address) {
		Cell cell = addCell(row, column, value, getHyperLinkFont());
		Hyperlink link = creationHelper.createHyperlink(HyperlinkType.URL);
		link.setAddress(address);
		cell.setHyperlink(link);
		return cell;
	}

	private Font getHyperLinkFont() {
		if (null == hyperlinkFont) {
			hyperlinkFont = font().underline().color(Color.BLUE).build();
		}
		return hyperlinkFont;
	}

	private Font getHeaderFont() {
		if (null == headerFont) {
			headerFont = font().bold().build();
		}
		return headerFont;
	}

	public void borderColumns(Sheet sheet, Border border, int... columns) {
		borderColumns(sheet, 0, border, columns);
	}

	public void borderColumns(Sheet sheet, int startRow, Border border, int... columns) {
		for (int column : columns) {
			int rows = sheet.getLastRowNum();
			for (int rowNum = startRow; rowNum <= rows; rowNum++) {
				Row row = getRow(sheet, rowNum);
				Cell cell = addCell(row, column);
				border.applyTo(cell.getCellStyle());
			}
		}
	}

	public void borderRow(Row row, Border border, int endColumnsIndex) {
		borderRow(row, border, 0, endColumnsIndex);
	}

	public void borderRow(Row row, Border border, int beginColumnIndex, int endColumnsIndex) {
		for (int i = beginColumnIndex; i < endColumnsIndex; i++) {
			Cell cell = addCell(row, i);
			CellStyle cellStyle = cell.getCellStyle();
			border.applyTo(cellStyle);
		}
	}

	public Row getRow(Sheet sheet, int row) {
		Row dataRow = sheet.getRow(row);
		if (null == dataRow) {
			dataRow = sheet.createRow(row);
			maxRowPerSheet.put(sheet.hashCode(), row);
		}
		return dataRow;
	}

	public byte[] getData() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		wb.write(out);
		return out.toByteArray();
	}

	public void setColumnSpan(Sheet sheet, Cell cell, int span) {
		int row = cell.getRowIndex();
		int column = cell.getColumnIndex();
		sheet.addMergedRegion(new CellRangeAddress(row, row, column, column + span - 1));
	}

	public void setAutoFilter(Sheet sheet, int startRow, int columns) {
		CellRangeAddress range = new CellRangeAddress(startRow, getMaxRow(sheet), 0, columns);
		sheet.setAutoFilter(range);
	}

	public void setRowSpan(Sheet sheet, Cell cell, int span) {
		int row = cell.getRowIndex();
		int column = cell.getColumnIndex();
		sheet.addMergedRegion(new CellRangeAddress(row, row + span - 1, column, column));
	}

	public void setColumnAndRowSpan(Sheet sheet, Cell cell, int colSpan, int rowSpan) {
		int row = cell.getRowIndex();
		int column = cell.getColumnIndex();
		sheet.addMergedRegion(new CellRangeAddress(row, row + rowSpan - 1, column, column + colSpan - 1));
	}

	public void autoSizeAllColumns(Sheet sheet, int columns) {
		for (int col = 0; col < columns; col++) {
			autoSizeColumn(sheet, col);
		}
	}

	public void autoSizeColumns(Sheet sheet, int... columns) {
		for (int col : columns) {
			autoSizeColumn(sheet, col);
		}
	}

	private void autoSizeColumn(Sheet sheet, int col) {
		sheet.autoSizeColumn(col);
		int oldWidth = sheet.getColumnWidth(col);
		int newWidth = Math.min(POI_COLUMN_MAXWITH, oldWidth + ADD_COL_WIDTH);
		sheet.setColumnWidth(col, newWidth);
	}

	public int getMaxRow(Sheet sheet) {
		return maxRowPerSheet.get(sheet.hashCode());
	}

	public FontBuilder font() {
		if (null == fontbuilder) {
			fontbuilder = new FontBuilder(wb);
		} else {
			fontbuilder.reset();
		}
		return fontbuilder;
	}

	public BorderBuilder borders() {
		return new BorderBuilder();
	}

	private void applyBorders(Sheet sheet, CellRangeAddress cellRangeAddress, Borders borders, boolean outerOnly) {
		int firstRow = cellRangeAddress.getFirstRow();
		int lastRow = cellRangeAddress.getLastRow();
		int firstColumn = cellRangeAddress.getFirstColumn();
		int lastColumn = cellRangeAddress.getLastColumn();
		for (int row = firstRow; row <= lastRow; row++) {
			Row actualRow = getRow(sheet, row);
			for (int col = firstColumn; col <= lastColumn; col++) {
				Cell cell = addCell(actualRow, col);
				CellStyle newCellStyle = wb.createCellStyle();
				newCellStyle.cloneStyleFrom(cell.getCellStyle());
				cell.setCellStyle(newCellStyle);
				if (!outerOnly || col == firstColumn) {
					borders.left().applyTo(cell.getCellStyle());
				}
				if (!outerOnly || col == lastColumn) {
					borders.right().applyTo(cell.getCellStyle());
				}
				if (!outerOnly || row == firstRow) {
					borders.top().applyTo(cell.getCellStyle());
				}
				if (!outerOnly || row == lastRow) {
					borders.bottom().applyTo(cell.getCellStyle());
				}
			}
		}
	}

	public void applyBorders(Sheet sheet, CellRangeAddress cellRangeAddress, Borders borders) {
		applyBorders(sheet, cellRangeAddress, borders, false);
	}

	public void applyOuterBorders(Sheet sheet, CellRangeAddress cellRangeAddress, Borders borders) {
		applyBorders(sheet, cellRangeAddress, borders, true);
	}

	public CellStyle getDefaultCellstyle() {
		if (null == defaultCellStyle) {
			defaultCellStyle = wb.createCellStyle();
		}
		return defaultCellStyle;
	}

	public void setDefaultCellstyle(CellStyle defaultCellstyle) {
		this.defaultCellStyle = defaultCellstyle;
	}

	public CellStyle getHeaderCellStyle() {
		if (null == headerCellStyle) {
			headerCellStyle = wb.createCellStyle();
		}
		return headerCellStyle;
	}

	public void setHeaderCellstyle(CellStyle headerCellstyle) {
		this.headerCellStyle = headerCellstyle;
	}

}
