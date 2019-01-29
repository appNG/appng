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

import java.util.Arrays;
import java.util.List;

public class FormGroup extends FormElementOwner implements ErrorAware {

	private String styleClass;
	private String errorClass;
	private String errorMessage;
	private String errorElementId;
	private String name;
	private boolean mandatory;
	private String mandatoryMessage;
	private boolean multiple;
	private FormData formData;
	private String content;

	FormGroup(FormData formData) {
		super(formData.getForm());
		this.formData = formData;
		this.multiple = true;
	}

	public FormData getFormData() {
		return formData;
	}

	public void setFormData(FormData formData) {
		this.formData = formData;
	}

	public boolean isValid() {
		return !hasErrors();
	}

	public String processContent() {
		setContent(ErrorAppender.appendError(this));
		return content;
	}

	public boolean hasValue() {
		for (FormElement element : getElements()) {
			if (element.hasValue()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean hasErrors() {
		if (super.hasErrors()) {
			return true;
		}
		if (isMandatory()) {
			return !hasValue();
		}
		return false;
	}

	@Override
	public FormElement addFormElement() {
		FormElement formElement = new FormElement(this);
		this.elements.add(formElement);
		return formElement;
	}

	public String getErrorClass() {
		return errorClass;
	}

	public void setErrorClass(String errorClass) {
		this.errorClass = errorClass;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isMandatory() {
		return mandatory;
	}

	public void setMandatory(boolean mandatory) {
		this.mandatory = mandatory;
	}

	public String getStyleClass() {
		return styleClass;
	}

	public void setStyleClass(String styleClass) {
		this.styleClass = styleClass;
	}

	@Override
	public List<FormElement> getElements() {
		if (!isMultiple() || elements.size() == 0) {
			return super.getElements();
		} else {
			return Arrays.asList(elements.get(0));
		}
	}

	public boolean isMultiple() {
		return multiple;
	}

	public void setMultiple(boolean multiple) {
		this.multiple = multiple;
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

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

}
