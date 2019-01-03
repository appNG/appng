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
package org.appng.api;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

import org.appng.api.FileUpload.Unit;
import org.junit.Assert;
import org.junit.Test;

public class FileUploadTest {

	private long byteSize = (long) (2.46d * FileUpload.FACTOR * FileUpload.FACTOR);
	private NumberFormat numberFormat = new DecimalFormat("0.0# ", new DecimalFormatSymbols(Locale.ENGLISH));

	@Test
	public void testFormat1() {
		Assert.assertEquals("2.46 MB", Unit.format(Unit.MB, byteSize, numberFormat));
	}

	@Test
	public void testFormat2() {
		Assert.assertEquals("2519.04 KB", Unit.KB.format(byteSize, numberFormat));
	}

}
