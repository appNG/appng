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
package org.appng.formtags;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.appng.forms.FormUpload;
import org.appng.forms.impl.FormUploadBean;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class RuleValidationTest {

	private static String SEP = File.separator;
	private static final String filePath = SEP + "src" + SEP + "test" + SEP + "resources" + SEP + "images" + SEP;
	static List<FormUpload> uploads;

	static {
		uploads = new ArrayList<>();
		String upload_1 = new File("").getAbsolutePath() + filePath + "Jellyfish.jpg";
		uploads.add(new FormUploadBean(new File(upload_1), "upload_1.jpg", "jpg", Arrays.asList("image/jpg"), 1024L));
		String upload_2 = new File("").getAbsolutePath() + filePath + "Lighthouse.jpg";
		uploads.add(new FormUploadBean(new File(upload_2), "upload_2.jpg", "jpg", Arrays.asList("image/jpg"), 1024L));
	}

	private TestRequest container;
	private RuleValidation ruleValidation;

	@Before
	public void setup() {
		Map<String, Object> sessionAttribues = new HashMap<>();
		Map<String, Object> session = new HashMap<>();
		session.put("SESSION", sessionAttribues);
		sessionAttribues.put("foobar", 5);
		container = new TestRequest(sessionAttribues);
		container.addParameter("foo", "foo");
		container.addParameter("bar", "123");
		container.addParameter("foobar", "123.45");
		container.addParameter("multifoo", "a", "b", "c", "d");
		container.addParameter("multibar");
		container.addUploads("upload", uploads);

		ruleValidation = new RuleValidation(container);
	}

	@Test
	public void testFileSizeMaxMB() {
		assertTrue("fileSizeMax(upload,'3.5MB')");
		assertFalse("fileSizeMax(upload,'0.1MB')");
	}

	@Test
	public void testFileSizeMaxKB() {
		assertTrue("fileSizeMax(upload,'850KB')");
		assertFalse("fileSizeMax(upload,'200KB')");
	}

	@Test
	public void testFileSizeMinMB() {
		assertTrue("fileSizeMin(upload,'0.5MB')");
		assertFalse("fileSizeMin(upload,'1.1MB')");
	}

	@Test
	public void testFileSizeMinKB() {
		assertTrue("fileSizeMin(upload,'500KB')");
		assertFalse("fileSizeMin(upload,'1100KB')");
	}

	@Test
	public void testFileSize() {
		assertTrue("fileSize(upload,'500KB','2.5MB')");
		assertFalse("fileSize(upload,'600KB','1.1MB')");
		assertFalse("fileSize(upload,'500KB','0.1MB')");
	}

	@Test
	public void testFileType() {
		assertTrue("fileType(upload,'jpg,gif,png')");
		assertFalse("fileType(upload,'tif,pdf')");
	}

	@Test
	public void testFileCount() {
		assertTrue("fileCount(upload, 1, 2)");
		assertFalse("fileCount(upload, 1, 0)");
		assertFalse("fileCount(upload, 3, 2)");
	}

	@Test
	public void testFileCountMin() {
		assertTrue("fileCountMin(upload, 1)");
		assertFalse("fileCountMin(upload, 5)");
		assertFalse("fileCountMin(upload, 3)");
	}

	@Test
	public void testFileCountMax() {
		assertTrue("fileCountMax(upload, 3)");
		assertFalse("fileCountMax(upload, 1)");
		assertFalse("fileCountMax(upload, 0)");
	}

	@Test
	public void testString() {
		assertTrue("string(foo)");
		assertTrue("string(bar)");
		assertFalse("string(foobar)");
		assertTrue("string('foobar')");
	}

	@Test
	public void testCaptcha() {
		assertFalse("captcha(bar, SESSION.foobar)");
		assertFalse("captcha(bar, SESSION['foobar'])");
		assertTrue("captcha(5, SESSION.foobar)");
		assertTrue("captcha(5, SESSION['foobar'])");
	}

	@Test
	public void testEmail() {
		assertTrue("email('mm@aiticon.de')");
		assertTrue("email('_mm@aiticon.de')");
		assertFalse("email('mm@aiticon')");
		assertFalse("email('@aiticon.de')");
	}

	@Test
	public void testEquals() {
		assertTrue("equals('a','a')");
		assertTrue("equals('b','b')");
		assertFalse("equals('a','A')");
		assertFalse("equals('a',' A')");
	}

	@Test
	public void testRegExp() {
		assertTrue("regExp('abc','[a-z]+')");
		assertFalse("regExp('abc','[a-z]{4,}')");
	}

	@Test
	public void testSize() {
		assertTrue("size(foo,3)");
		assertFalse("size(foo,4)");
		assertTrue("size(multifoo,4)");
		assertFalse("size(multifoo,5)");
		assertFalse("size(multibar,5)");
	}

	@Test
	public void testSizeMin() {
		assertTrue("sizeMin(foo,3)");
		assertFalse("sizeMin(foo,4)");
		assertTrue("sizeMin(multifoo,4)");
		assertFalse("sizeMin(multifoo,5)");
		assertFalse("sizeMin(multibar,5)");
	}

	@Test
	public void testSizeMax() {
		assertTrue("sizeMax(foo,4)");
		assertFalse("sizeMax(foo,2)");
		assertTrue("sizeMax(multifoo,4)");
		assertFalse("sizeMax(multifoo,3)");
		assertTrue("sizeMax(multibar,3)");
	}

	@Test
	public void testSizeMinMax() {
		assertTrue("sizeMinMax(foo,1,3)");
		assertFalse("sizeMinMax(foo,4,5)");
		assertTrue("sizeMinMax(multifoo,1,4)");
		assertFalse("sizeMinMax(multifoo,1,3)");
		assertFalse("sizeMinMax(multifoo,5,7)");
		assertFalse("sizeMinMax(multibar,1,3)");
		assertFalse("sizeMinMax(multibar,5,7)");
	}

	@Test
	public void testNumber() {
		assertFalse("number(foo)");
		assertTrue("number(bar)");
	}

	@Test
	public void testNumberFractionDigits() {
		assertFalse("numberFractionDigits(foobar,1,1)");
		assertFalse("numberFractionDigits(foobar,1,2)");
		assertFalse("numberFractionDigits(foobar,2,1)");
		assertFalse("numberFractionDigits(foobar,2,2)");
		assertFalse("numberFractionDigits(foobar,3,1)");
		assertTrue("numberFractionDigits(foobar,3,2)");
		assertTrue("numberFractionDigits(foobar,3,3)");
		assertTrue("numberFractionDigits(foobar,3,4)");
		assertTrue("numberFractionDigits(foobar,4,3)");
		assertTrue("numberFractionDigits(foobar,4,4)");
	}

	private void assertTrue(String expression) {
		boolean result = ruleValidation.validate("${" + expression + "}");
		Assert.assertTrue(expression, result);
	}

	private void assertFalse(String expression) {
		boolean result = ruleValidation.validate("${" + expression + "}");
		Assert.assertFalse(expression, result);
	}
}
