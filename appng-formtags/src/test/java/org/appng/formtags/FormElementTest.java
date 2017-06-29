/*
 * Copyright 2011-2017 the original author or authors.
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

import java.util.List;

import org.appng.formtags.FormElement.InputTag;
import org.appng.formtags.FormElement.InputType;
import org.junit.Assert;
import org.junit.Test;

public class FormElementTest {

	private String name = "foobar";
	private String original = "<input type=\"{type}\" name=\"" + name + "\" value=\"{value}\" />";
	private String error = "<input type=\"{type}\" name=\"" + name + "\" value=\"{value}\" class=\"error\" />";

	@Test
	public void testRadioOk() {
		runOK(InputType.RADIO, "12345", "12345", "checked=\"checked\"");
	}

	@Test
	public void testCheckboxSelected() {
		runOK(InputType.CHECKBOX, "12345", "12345", "checked=\"checked\"");
	}

	@Test
	public void testCheckboxNotSelected() {
		FormElement formElement = getFormElement("", "<input type=\"checkbox\" name=\"foobar\" value=\"12345\" />");
		String content = formElement.processContent();
		String tag = "<input type=\"checkbox\" name=\"foobar\" value=\"12345\" class=\"error\" />";
		Assert.assertEquals(tag, content);
		Assert.assertEquals("12345", formElement.getValue());
		Assert.assertFalse(formElement.isValid());
	}

	@Test
	public void testTextOk() {
		runOK(InputType.TEXT, "12345", "12345", null);
	}

	@Test
	public void testTextWithAmpersand() {
		runOK(InputType.TEXT, "&gt;", "&amp;gt;", null);
	}

	@Test
	public void testTextError() {
		runError(InputType.TEXT, "123456");
	}

	@Test
	public void testTextareaOK() {
		String expectedValue = "12345";
		String expectedContent = "<textarea name=\"foobar\">" + expectedValue + "</textarea>";
		FormElement formElement = getFormElement(expectedValue, expectedContent);
		formElement.setInputTag(InputTag.TEXTAREA);
		String content = formElement.processContent();
		String value = formElement.getValue();
		Assert.assertNull(value);
		Assert.assertEquals(expectedValue, formElement.getRequestValues().get(0));
		Assert.assertEquals(expectedContent, content);
	}

	@Test
	public void testErrorElementMandatory() {
		String expectedValue = "";
		String expectedContent = "<input value=\"\" class=\"error\" type=\"text\" name=\"email\" />";
		FormElement formElement = getFormElement(expectedValue, expectedContent + "<div id=\"error\"/>");
		formElement.setMandatory(true);
		formElement.setErrorElementId("error");
		formElement.setMandatoryMessage("e-mail required");
		String content = formElement.processContent();
		String value = formElement.getValue();
		Assert.assertNull(value);
		Assert.assertEquals(expectedContent + "<div id=\"error\"><span>e-mail required</span></div>", content);
	}

	@Test
	public void testErrorElement() {
		this.name = "email";
		String expectedValue = "foobar";
		String expectedContent = "<input value=\"foobar\" class=\"error\" type=\"text\" name=\"email\" />";
		FormElement formElement = getFormElement(expectedValue, expectedContent + "<div id=\"error\"/>");
		formElement.setMandatory(true);
		formElement.setErrorElementId("error");
		formElement.setRule("email");
		formElement.setErrorMessage("Not a valid e-mail address");
		formElement.setMandatoryMessage("e-mail required");
		String content = formElement.processContent();
		String value = formElement.getValue();
		Assert.assertEquals(expectedValue, value);
		Assert.assertEquals(expectedContent + "<div id=\"error\"><span>Not a valid e-mail address</span></div>",
				content);
	}

	@Test
	public void testTextareaError() {

	}

	@Test
	public void testSelectOK() {
		FormGroup formGroup = getFormGroup("b");

		List<FormElement> elements = formGroup.getElements();
		for (FormElement element : elements) {
			element.processContent();
		}
		String content0 = elements.get(0).getContent();
		Assert.assertEquals("<option value=\"\">--please select--</option>", content0);
		String content1 = elements.get(1).getContent();
		Assert.assertEquals("<option value=\"a\">A</option>", content1);
		String content2 = elements.get(2).getContent();
		Assert.assertEquals("<option selected=\"selected\" value=\"b\">B</option>", content2);
		String content3 = elements.get(3).getContent();
		Assert.assertEquals("<option value=\"c\">C</option>", content3);

		String content = "<selection name=\"selection\">" + content0 + content1 + content2 + content3
				+ "</selection><div id=\"error\"/>";
		formGroup.setContent(content);
		formGroup.processContent();

		String expected = "<selection name=\"selection\">" + content0 + content1 + content2 + content3
				+ "</selection><div id=\"error\"></div>";

		Assert.assertEquals(expected, formGroup.getContent());
	}

	@Test
	public void testSelectError() {
		FormGroup formGroup = getFormGroup("");

		List<FormElement> elements = formGroup.getElements();
		for (FormElement element : elements) {
			element.processContent();
		}

		String content0 = elements.get(0).getContent();
		Assert.assertEquals("<option selected=\"selected\" value=\"\">--please select--</option>", content0);
		String content1 = elements.get(1).getContent();
		Assert.assertEquals("<option value=\"a\">A</option>", content1);
		String content2 = elements.get(2).getContent();
		Assert.assertEquals("<option value=\"b\">B</option>", content2);
		String content3 = elements.get(3).getContent();
		Assert.assertEquals("<option value=\"c\">C</option>", content3);

		String content = "<selection name=\"selection\">" + content0 + content1 + content2 + content3
				+ "</selection><div id=\"error\"/>";
		formGroup.setContent(content);
		formGroup.processContent();

		String expected = "<selection name=\"selection\" class=\"error\">" + content0 + content1 + content2 + content3
				+ "</selection><div id=\"error\"><span>" + formGroup.getMandatoryMessage() + "</span></div>";
		Assert.assertEquals(expected, formGroup.getContent());
	}

	private FormGroup getFormGroup(String value) {
		Form form = new Form();
		FormData formData = new FormData(form);
		FormGroup formGroup = new FormGroup(formData);
		formGroup.setMandatory(true);
		formGroup.setMandatoryMessage("make a selection");
		formGroup.setMultiple(false);
		formGroup.setErrorClass("error");
		formGroup.setErrorMessage("errormessage");
		formGroup.setStyleClass("style");
		formGroup.setErrorElementId("error");
		formGroup.setName("selection");

		FormElement element0 = formGroup.addFormElement();
		element0.setName(formGroup.getName());
		element0.setContent("<option value=\"\">--please select--</option>");
		FormElement element1 = formGroup.addFormElement();
		element1.setName(formGroup.getName());
		element1.setContent("<option value=\"a\">A</option>");
		FormElement element2 = formGroup.addFormElement();
		element2.setName(formGroup.getName());
		element2.setContent("<option value=\"b\">B</option>");
		FormElement element3 = formGroup.addFormElement();
		element3.setName(formGroup.getName());
		element3.setContent("<option value=\"c\">C</option>");

		String content = "<selection name=\"selection\">" + element0.getContent() + element1.getContent()
				+ element2.getContent() + element3.getContent() + "</selection><div id=\"error\"/>";

		formGroup.setContent(content);

		TestRequest testRequest = new TestRequest();
		testRequest.addParameter("selection", value);
		form.setRequest(testRequest);
		form.setSubmitted(true);
		return formGroup;
	}

	@Test
	public void testFileOK() {

	}

	@Test
	public void testFileError() {

	}

	private void runOK(InputType inputType, String value, String expectedValue, String additionalAttribute) {
		FormElement formElement = getFormElement(value, inputType);
		String content = formElement.processContent();
		String tag = getOriginal(inputType, expectedValue);
		if (null != additionalAttribute) {
			tag = tag.replace("<input", "<input " + additionalAttribute);
		}
		Assert.assertEquals(tag, content);
		Assert.assertEquals(expectedValue, formElement.getValue());
	}

	private void runError(InputType inputType, String value) {
		FormElement formElement = getFormElement(value, inputType);
		String content = formElement.processContent();
		String tag = getError(inputType, value);
		Assert.assertEquals(tag, content);
	}

	private String getOriginal(InputType type, String value) {
		return original.replaceAll("\\{type\\}", type.name().toLowerCase()).replaceAll("\\{value\\}", value);
	}

	private String getError(InputType type, String value) {
		return error.replaceAll("\\{type\\}", type.name().toLowerCase()).replaceAll("\\{value\\}", value);
	}

	private FormElement getFormElement(String value, InputType type) {
		return getFormElement(value, getOriginal(type, value));
	}

	private FormElement getFormElement(String value, String conent) {
		Form form = new Form();
		FormElement formElement = form.getFormData().addFormElement();
		formElement.setContent(conent);
		formElement.setErrorClass("error");
		formElement.setErrorMessage("maximum 5 chars");
		formElement.setForwardValidation(false);
		formElement.setMandatory(true);
		formElement.setRule("sizeMax(5)");
		TestRequest container = new TestRequest();
		container.addParameter(name, value);
		form.setRequest(container);
		form.setSubmitted(true);
		return formElement;
	}

}
