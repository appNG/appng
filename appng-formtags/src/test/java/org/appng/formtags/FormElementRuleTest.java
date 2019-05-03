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
package org.appng.formtags;

import org.junit.Assert;
import org.junit.Test;

public class FormElementRuleTest {

	private static final String ERROR_DIV = "<div id=\"error\"><span>errormessage</span></div>";

	@Test
	public void testRegExp() {
		runTest("regExp('[0-9]{5}')", "1234");
	}

	@Test
	public void testString() {
		runTest(RuleValidation.STRING, "+++");
	}

	@Test
	public void testEmail() {
		runTest(RuleValidation.EMAIL, "abc");
	}

	@Test
	public void testEquals() {
		runTestOK(RuleValidation.EQUALS + "('abc')", "abc");
		runTestOK(RuleValidation.EQUALS + "(field)", "abc");
		runTest(RuleValidation.EQUALS + "('abc')", "abcd");
	}

	@Test
	public void testNumber() {
		runTest(RuleValidation.NUMBER, "abc");
	}

	@Test
	public void testNumberFractionDigits() {
		runTest(RuleValidation.NUMBER_FRACTION_DIGITS + "(2,2)", "12.345");
	}

	@Test
	public void testSize() {
		runTest(RuleValidation.SIZE, "abc");
		runSizeTest(RuleValidation.SIZE + "(2)", true);
		runSizeTest(RuleValidation.SIZE + "(3)", false);
	}

	@Test
	public void testSizeMax() {
		runTest(RuleValidation.SIZE_MAX + "(3)", "abcd");
		runSizeTest(RuleValidation.SIZE_MAX + "(3)", true);
		runSizeTest(RuleValidation.SIZE_MAX + "(1)", false);
	}

	@Test
	public void testSizeMin() {
		runTest(RuleValidation.SIZE_MIN + "(3)", "ab");
		runSizeTest(RuleValidation.SIZE_MIN + "(2)", true);
		runSizeTest(RuleValidation.SIZE_MIN + "(3)", false);
	}

	@Test
	public void testSizeMinMax() {
		runTest(RuleValidation.SIZE_MIN_MAX + "(3,5)", "ab");
		runTest(RuleValidation.SIZE_MIN_MAX + "(3,5)", "abcdef");
		runTestOK(RuleValidation.SIZE_MIN_MAX + "(1,2)", "ab");
		runSizeTest(RuleValidation.SIZE_MIN_MAX + "(1,2)", true);
		runSizeTest(RuleValidation.SIZE_MIN_MAX + "(3,5)", false);
		runSizeTest(RuleValidation.SIZE_MIN_MAX + "(0,1)", false);
	}

	private void runSizeTest(String rule, boolean valid) {
		FormElement formElement = getFormElement("checkbox", "multivalued", rule, "ab", "cd");
		formElement.processContent();
		Assert.assertTrue(valid == formElement.isValid());
	}

	@Test
	public void testCaptcha() {
		runTest(RuleValidation.CAPTCHA + "('field')", "ab");
	}

	@Test
	public void testFileCount() {
		runFileTest(RuleValidation.FILE_COUNT + "(3,5)");
	}

	@Test
	public void testFileCountMax() {
		runFileTest(RuleValidation.FILE_COUNT_MAX + "(1)");
	}

	@Test
	public void testFileCountMin() {
		runFileTest(RuleValidation.FILE_COUNT_MIN + "(5)");
	}

	@Test
	public void testFileSize() {
		runFileTest(RuleValidation.FILE_SIZE + "(3,5)");
	}

	@Test
	public void testFileSizeMax() {
		runFileTest(RuleValidation.FILE_SIZE_MAX + "('1KB')");
	}

	@Test
	public void testFileSizeMin() {
		runFileTest(RuleValidation.FILE_SIZE_MIN + "('2MB')");
	}

	@Test
	public void testFileType() {
		runFileTest(RuleValidation.FILE_TYPE + "('gif')");
	}

	private void runFileTest(String rule) {
		FormElement formElement = getFormElement("file", "upload", rule);
		formElement.processContent();
		Assert.assertFalse(formElement.isValid());
		String expected = "<input value=\"\" type=\"file\" name=\"upload\" class=\"error\" />" + ERROR_DIV;
		Assert.assertEquals(expected, formElement.getContent());
	}

	private void runTestOK(String rule, String value) {
		FormElement formElement = getFormElement(rule, value);
		formElement.processContent();
		Assert.assertTrue(formElement.isValid());
		String expected = "<input value=\"" + value + "\" type=\"text\" name=\"field\" /><div id=\"error\"></div>";
		Assert.assertEquals(expected, formElement.getContent());
	}

	private void runTest(String rule, String value) {
		FormElement formElement = getFormElement(rule, value);
		formElement.processContent();
		Assert.assertFalse(formElement.isValid());
		String expected = "<input value=\"" + value + "\" type=\"text\" name=\"field\" class=\"error\" />" + ERROR_DIV;
		Assert.assertEquals(expected, formElement.getContent());
	}

	private FormElement getFormElement(String rule, String value) {
		return getFormElement("text", "field", rule, value);
	}

	private FormElement getFormElement(String type, String name, String rule, String... values) {
		Form form = new Form();
		FormElement formElement = form.getFormData().addFormElement();
		formElement.setContent("<input type=\"" + type + "\" name=\"" + name + "\" /><div id=\"error\"/>");
		formElement.setErrorClass("error");
		formElement.setErrorMessage("errormessage");
		formElement.setErrorElementId("error");
		formElement.setForwardValidation(false);
		formElement.setMandatory(true);
		formElement.setRule(rule);

		TestRequest container = new TestRequest();
		if (null != values) {
			container.addParameter(name, values);
		}
		container.addUploads("upload", RuleValidationTest.uploads);

		form.setRequest(container);
		form.setSubmitted(true);
		return formElement;
	}
}
