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
package org.appng.tools.ui;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

public class StringNormalizerTest {

	@Test
	public void test() {
		String string = "äÄöÖüÜßñéôëïçèÕдвойной-верхнийáóúñéí°^!\"§$%&/()=?´{[]}\\`+-*%,:;<>#~'";
		Assert.assertEquals("aeAeoeOeueUessneoeiceOдвоиноиверхнииaounei", StringNormalizer.normalize(string));
	}
	
	@Test
	public void testStripNonPrintableCharacter() {
		for (int c = 0; c < 32; c++) {
			if (c != 9 && c != 10 && c != 13) {
				String s = Character.toString((char) c);
				Assert.assertEquals("", StringNormalizer.removeNonPrintableCharacters(s));
			}
		}
		int[] allowedCtrlChar = { 9, 10, 13 };
		for (int c : allowedCtrlChar) {
			String s = Character.toString((char) c);
			Assert.assertEquals(s, StringNormalizer.removeNonPrintableCharacters(s));
		}
		for (int c = 32; c < 127; c++) {
			String s = Character.toString((char) c);
			Assert.assertEquals(s, StringNormalizer.removeNonPrintableCharacters(s));
		}
		for (int c = 127; c < 160; c++) {
			String s = Character.toString((char) c);
			Assert.assertEquals(StringUtils.EMPTY, StringNormalizer.removeNonPrintableCharacters(s));
		}
		for (int c = 160; c < 65535; c++) {
			String s = Character.toString((char) c);
			Assert.assertEquals(s, StringNormalizer.removeNonPrintableCharacters(s));
		}
	}

}
