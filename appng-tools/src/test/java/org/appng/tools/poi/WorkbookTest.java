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
package org.appng.tools.poi;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Test;

public class WorkbookTest {

	public static void main(String[] args) throws Exception {
		build(new XSSFWorkbook(), "out.xlsx");
		build(new HSSFWorkbook(), "out.xls");
	}

	@Test
	public void testUseDefaultCellStyle() throws IOException {
		WorkBookHelper wbh1 = new WorkBookHelper(new XSSFWorkbook());
		Sheet sheet1 = wbh1.createSheet();
		WorkBookHelper wbh2 = new WorkBookHelper(new XSSFWorkbook());
		Sheet sheet2 = wbh2.createSheet();

		for (int i = 0; i < 64001; i++) {
			wbh1.addCell(sheet1.createRow(i), 0, String.valueOf(i), wbh1.getDefaultCellstyle());
			wbh2.addCell(sheet2.createRow(i), 0, String.valueOf(i));
		}

	}

	// result should look like src/test/resources/workbook.png
	protected static void build(Workbook workbook, String fileName) throws FileNotFoundException, IOException {
		WorkBookHelper workBookHelper = new WorkBookHelper(workbook);
		Sheet sheet = workBookHelper.createSheet();
		Row row = sheet.createRow(0);

		// 0,0: col0 with default style
		workBookHelper.addCell(row, 0, "col0", workBookHelper.getDefaultCellstyle());

		// 0,1: col1 borders(l,r,t,b): (dashed, dash-dot-dot, medium-dash-dot-dot, slanted-dash-dot)
		Borders borders1 = new Borders(BorderStyle.DASHED, BorderStyle.DASH_DOT_DOT, BorderStyle.MEDIUM_DASH_DOT_DOT,
				BorderStyle.SLANTED_DASH_DOT);
		workBookHelper.addCell(row, 1, "col1", borders1);

		// 0,2: col2 Tahoma 12, bold, italic, double underlined, striked out, #CCC
		Font font1 = workBookHelper.font().bold().italic().underlineDouble().strikeout().color("#CCC").size((short) 12)
				.name("Tahoma").build();
		workBookHelper.addCell(row, 2, "col2", font1);

		// 0,3: col3 Courier New, #B40F66, borders (l,r): (dashed, hair)
		Font font2 = workBookHelper.font().color("#B40F66").name("Courier New").build();
		Borders borders2 = workBookHelper.borders().left(BorderStyle.DASHED).right(BorderStyle.HAIR).build();
		workBookHelper.addCell(row, 3, "col3", workBookHelper.getDefaultCellstyle(), font2, borders2);

		workBookHelper.autoSizeAllColumns(sheet, 3);
		FileOutputStream out = new FileOutputStream(new File(fileName));

		CellRangeAddress cellRangeAddress = CellRangeAddress.valueOf("C3:F9");
		// thick outer borders for C3:F9
		Borders borders = new Borders(BorderStyle.THICK, BorderStyle.THICK, BorderStyle.THICK, BorderStyle.THICK);
		workBookHelper.applyOuterBorders(sheet, cellRangeAddress, borders);
		// thick outer borders for D3:D9
		workBookHelper.applyOuterBorders(sheet, CellRangeAddress.valueOf("D3:D9"), borders);

		// thin inner borders for C3:F9
		Borders innerBorders = new Borders(BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN);
		workBookHelper.applyBorders(sheet, cellRangeAddress, innerBorders);

		workbook.write(out);
		out.close();
	}
}
