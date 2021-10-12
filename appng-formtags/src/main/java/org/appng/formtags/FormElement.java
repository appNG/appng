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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.appng.forms.FormUpload;
import org.appng.forms.Request;
import org.appng.tools.markup.XHTML;

/**
 * A {@link FormElement} represents one of the following HTML elements:
 * <ul>
 * <li>{@code <input>}</li>
 * <li>{@code <textarea>}</li>
 * <li>{@code <select>}</li>
 * <li>{@code <option>}</li>
 * </ul>
 * To process a {@link FormElement}, two steps are necessary:
 * <ol>
 * <li>set the text content of the {@link FormElement} by calling {@link #setContent(String)}
 * <li>calling {@link #processContent()}
 * </ol>
 * 
 * @author Matthias Müller
 */
public class FormElement extends FormElementOwner implements ErrorAware {

	private static final String ATTR_TYPE = "type";
	private static final String ATTR_NAME = "name";
	private static final String ATTR_VALUE = "value";
	private static final String ATTR_CHECKED = "checked";
	private static final String ATTR_SELECTED = "selected";

	/**
	 * Defines the possible types for an HTML {@code <input>}-element
	 * 
	 * @author Matthias Müller
	 */
	public enum InputType {
		/** {@code <input type="radio" />} */
		RADIO,
		/** {@code <input type="file" />} */
		FILE,
		/** {@code <input type="checkbox" />} */
		CHECKBOX,
		/** {@code <input type="hidden" />} */
		HIDDEN,
		/** {@code <input type="password" />} */
		PASSWORD,
		/** {@code <input type="text" />} */
		TEXT;
	}

	/**
	 * Defines the possible HTML Tags that a {@link FormElement} can represent.
	 * 
	 * @author Matthias Müller
	 */
	public enum InputTag {
		/**
		 * {@code <textarea />}
		 */
		TEXTAREA,
		/**
		 * {@code <select />}
		 */
		SELECT,
		/**
		 * {@code <option />}, only valid within a {@code <select />}
		 */
		OPTION,
		/**
		 * {@code <input type="..."/>}
		 * 
		 * @see InputType
		 */
		INPUT;
	}

	private boolean mandatory;

	private String mandatoryMessage;

	private String errorElementId;

	private boolean forwardValidation;

	private String rule;

	private String errorClass;

	private String errorMessage;

	private List<String> requestValues;

	private String value;

	private String content;

	private String name;

	private FormData formData;

	private List<FormUpload> formUploads;

	private boolean valid = true;

	private InputType inputType;

	private InputTag inputTag;

	FormElement(FormData formData) {
		super(formData.getForm());
		setFormData(formData);
		this.requestValues = new ArrayList<>();
		this.elements.add(this);
	}

	FormElement(FormGroup formGroup) {
		this(formGroup.getFormData());
	}

	public String processContent() {
		inputType = null;
		inputTag = null;
		String name = XHTML.getAttr(content, ATTR_NAME);
		if (StringUtils.isNotBlank(name)) {
			setName(name);
		}

		String tag = XHTML.getTag(content);
		if (null != tag) {
			inputTag = InputTag.valueOf(tag.toUpperCase());
			String type = XHTML.getAttr(content, ATTR_TYPE);

			switch (inputTag) {
			case INPUT:
				if (null != type) {
					inputType = InputType.valueOf(type.toUpperCase());
				}
				handleInput();
				break;

			case OPTION:
				setValue(XHTML.getAttr(content, ATTR_VALUE));
				handleOption();
				break;

			case TEXTAREA:
				handleTextArea();
				break;

			default:
				break;
			}
		}
		if (getForm().isSubmitted()) {
			if (!hasValue()) {
				setValid(!isMandatory());
			} else {
				processRule();
			}
		}

		setContent(ErrorAppender.appendError(this));

		List<String> parameterList = getForm().getRequest().getParameterList(getName());
		setRequestValues(parameterList);
		return getContent();
	}

	@Override
	public boolean hasValue() {
		Request request = getForm().getRequest();
		List<String> parameterList = request.getParameterList(getName());
		List<FormUpload> formUpload = request.getFormUploads(getName());
		int numParameters = parameterList.size();
		boolean emptyValue = (numParameters == 0 || numParameters == 1 && "".equals(parameterList.get(0)))
				&& formUpload.isEmpty();
		return !emptyValue;
	}

	private void processRule() {
		if (null != rule) {
			if (RuleValidation.SHORT_RULES.contains(rule)) {
				rule += "(" + getName() + ")";
			} else {
				rule = rule.replaceFirst("\\(", "(" + getName() + ",");
			}
			boolean validationResult = getForm().getRuleValidation().validate("${" + rule + "}");
			setValid(validationResult);
		}
	}

	private void handleTextArea() {
		if (getForm().isSubmitted()) {
			String parameter = getForm().getRequest().getParameter(getName());
			String escapedContent = escapeHtml(parameter, true);
			setContent(XHTML.setBody(content, escapedContent));
			// setValue(escapedContent);
		} else {
			String body = XHTML.getBody(inputTag.toString().toLowerCase(), content);
			if (null != body) {
				String escapedBody = escapeHtml(body);
				content = XHTML.setBody(content, escapedBody);
			}
		}
	}

	private void handleOption() {
		removeAttribute(ATTR_NAME);
		if (getForm().isSubmitted()) {
			List<String> parameterList = getForm().getRequest().getParameterList(getName());
			String unescaped = unescapeHtml(getValue());
			if (parameterList.contains(unescaped)) {
				setAttribute(ATTR_SELECTED, ATTR_SELECTED);
			} else {
				removeAttribute(ATTR_SELECTED);
			}
		}
	}

	private void handleFile() {
		if (getForm().isSubmitted()) {
			List<FormUpload> formUploads = getForm().getRequest().getFormUploads(getName());
			setFormUploads(formUploads);
		}
	}

	private void handleInput() {
		setValue(XHTML.getAttr(content, ATTR_VALUE));
		if (getForm().isSubmitted()) {
			switch (inputType) {

			case FILE:
				handleFile();
				break;
			case CHECKBOX:
				handleCheckbox();
				break;
			case RADIO:
				handleRadio();
				break;
			default:
				String parameter = getForm().getRequest().getParameter(getName());
				setValue(escapeHtml(parameter, true));
				break;
			}

		}

		setAttribute(ATTR_VALUE, getValue() == null ? "" : getValue());
	}

	private void handleCheckbox() {
		if (getForm().isSubmitted()) {
			List<String> parameterList = getForm().getRequest().getParameterList(getName());
			String unescaped = unescapeHtml(getValue());
			if (parameterList.contains(unescaped)) {
				setAttribute(ATTR_CHECKED, ATTR_CHECKED);
			} else {
				removeAttribute(ATTR_CHECKED);
			}
		}
	}

	private void removeAttribute(String attribute) {
		content = XHTML.removeAttr(content, attribute);
	}

	private void handleRadio() {
		if (getForm().isSubmitted()) {
			String parameter = getForm().getRequest().getParameter(getName());
			if (getValue().equals(escapeHtml(parameter))) {
				setAttribute(ATTR_CHECKED, ATTR_CHECKED);
			}
		}
	}

	private void setAttribute(String attribute, String value) {
		content = XHTML.setAttr(content, attribute, value);
	}

	public boolean isMandatory() {
		return mandatory;
	}

	public void setMandatory(boolean mandatory) {
		this.mandatory = mandatory;
	}

	public String getRule() {
		return rule;
	}

	public void setRule(String rule) {
		this.rule = rule;
	}

	public String getErrorClass() {
		return errorClass;
	}

	public void setErrorClass(String errorClass) {
		this.errorClass = errorClass;
	}

	public boolean isForwardValidation() {
		return forwardValidation;
	}

	public void setForwardValidation(boolean forwardValidation) {
		this.forwardValidation = forwardValidation;
	}

	private String escapeHtml(String string) {
		return escapeHtml(string, false);
	}

	private String escapeHtml(String string, boolean escapeFirst) {
		String value = string;
		if (escapeFirst) {
			value = StringEscapeUtils.escapeHtml4(string);
		}
		value = StringEscapeUtils.unescapeHtml4(value);
		String escapedValue = StringEscapeUtils.escapeHtml4(value);
		// XXX HACK..is there a better solution?
		if (StringUtils.isNotBlank(escapedValue)) {
			escapedValue = escapedValue.replaceAll("\\$", "&#36;");
			escapedValue = escapedValue.replaceAll("\\\\", "&#92;");
		}
		return escapedValue;
	}

	private String unescapeHtml(String string) {
		return StringEscapeUtils.unescapeHtml4(string);
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = escapeHtml(value);
	}

	public List<String> getRequestValues() {
		return Collections.unmodifiableList(requestValues);
	}

	private void setRequestValues(List<String> requestValues) {
		this.requestValues = requestValues;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public FormData getFormData() {
		return formData;
	}

	public void setFormData(FormData formData) {
		this.formData = formData;
	}

	public List<FormUpload> getFormUploads() {
		return formUploads;
	}

	public void setFormUploads(List<FormUpload> formUploads) {
		this.formUploads = formUploads;
	}

	public boolean isValid() {
		return valid;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}

	public InputTag getInputTag() {
		return inputTag;
	}

	public void setInputTag(InputTag inputTag) {
		this.inputTag = inputTag;
	}

	public InputType getInputType() {
		return inputType;
	}

	public void setInputType(InputType inputType) {
		this.inputType = inputType;
	}

	public String getMandatoryMessage() {
		return mandatoryMessage;
	}

	public void setMandatoryMessage(String mandatoryMessage) {
		this.mandatoryMessage = mandatoryMessage;
	}

	public String getErrorElementId() {
		return errorElementId;
	}

	public void setErrorElementId(String errorElementId) {
		this.errorElementId = errorElementId;
	}

	@Override
	public String toString() {
		if (InputType.FILE.equals(getInputType())) {
			return name + ": " + StringUtils.join(getFormUploads(), ", ");
		} else {
			return name + ": " + StringUtils.join(getRequestValues(), ", ");
		}
	}

	@Override
	FormElement addFormElement() {
		throw new UnsupportedOperationException("can not add a FormElement to FormElement");
	}

}
