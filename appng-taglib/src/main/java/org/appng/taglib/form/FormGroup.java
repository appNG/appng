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
package org.appng.taglib.form;

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTagSupport;

/**
 * An {@code <appNG:formGroup>} can contain multiple {@code <appNG:formElement>}s. The elements of a group are
 * implicitly enclosed with a {@code <div>} container whose {@code style} attribute can be set using the
 * {@code styleClass} attribute of the taglet.
 * <p/>
 * <b>Attributes:</b><br/>
 * All of the following attributes are optional.
 * <ul>
 * <li>name - the name of the input element(s) this group refers to</li>
 * <li>mandatory - set to {@code true} if the field is mandatory</li>
 * <li>mandatoryMessage - the message to be displayed when no value has been entered for a mandatory field</li>
 * <li>errorMessage - the error message to be displayed when validation fails</li>
 * <li>errorClass - the CSS class to add to enclosing {@code <div>} and the element(s) with the given name when validation fails</li>
 * <li>errorElementId - the id of a nested element to append a {@code <span>} with the error message</li>
 * <li>styleClass -a CSS class for the enclosing {@code <div>}</li>
 * </ul>
 * <b>Usage:</b>
 * 
 * <pre>
 * Favorite number:&lt;br/>
 * &lt;appNG:formGroup name="select" styleClass="group" mandatory="true" mandatoryMessage="Please make your selection!" errorClass="error" errorElementId="numberError">
 *   &lt;select name="select">
 *     &lt;appNG:formElement>
 *       &lt;option value="42">42&lt;/option>
 *     &lt;/appNG:formElement>
 *     &lt;appNG:formElement>
 *       &lt;option value="0815">0815&lt;/option>
 *     &lt;/appNG:formElement>
 *     &lt;appNG:formElement>
 *       &lt;option value="1701">1701&lt;/option>
 *     &lt;/appNG:formElement>
 *   &lt;/select>
 * &lt;/appNG:formGroup>
 * &lt;div id="numberError">&lt;/div>
 * </pre>
 * 
 * <b>Output (before submitting):</b>
 * 
 * <pre>Favorite number:&lt;br/>
 * &lt;div class="group">
 *   &lt;select name="select">
 *     &lt;option value="42">42&lt;/option>
 *     &lt;option value="0815">0815&lt;/option>
 *     &lt;option value="1701">1701&lt;/option>
 *   &lt;/select>
 *   &lt;div id="numberError">&lt;/div>
 * &lt;/div>
 * </pre>
 * 
 * <b>Output (after submitting, nothing selected):</b>
 * 
 * <pre>Favorite number:&lt;br/>
 * &lt;div class="group error">
 *   &lt;select name="select" class="error">
 *     &lt;option value="42">42&lt;/option>
 *     &lt;option value="0815">0815&lt;/option>
 *     &lt;option value="1701">1701&lt;/option>
 *   &lt;/select>
 *  &lt;div id="numberError">&lt;span>Please make your selection!&lt;/span>&lt;/div>
 * &lt;/div>
 * </pre>
 * 
 * @author Matthias Müller
 *
 * @see FormElement
 * @see Form
 */
public class FormGroup extends BodyTagSupport {

	private org.appng.formtags.FormGroup wrappedFormGroup;

	@Override
	public int doStartTag() throws JspException {
		if (getFormData() == null) {
			throw new JspTagException("a <formGroup> can only be used inside <formData>!");
		}
		return EVAL_BODY_BUFFERED;
	}

	@Override
	public int doAfterBody() throws JspException {
		BodyContent body = getBodyContent();
		if (body != null) {
			JspWriter jspWriter = body.getEnclosingWriter();
			try {
				StringBuilder out = new StringBuilder();
				out.append("<div class=\"");
				if (null != getStyleClass()) {
					out.append(getStyleClass());
				}
				boolean hasSubmittedErros = getWrappedFormGroup().getForm().isSubmitted()
						&& getWrappedFormGroup().hasErrors();
				if (hasSubmittedErros && null != getErrorClass()) {
					out.append(" " + getErrorClass());
				}
				out.append("\" >");
				out.append(body.getString());
				out.append("</div>");

				if (hasSubmittedErros) {
					getWrappedFormGroup().setContent(out.toString());
					getWrappedFormGroup().processContent();
					String content = getWrappedFormGroup().getContent();
					jspWriter.write(content);
				} else {
					jspWriter.write(out.toString());
				}
			} catch (IOException ioe) {
				throw new JspException(ioe);
			}
		}
		wrappedFormGroup = null;
		return SKIP_BODY;
	}

	@Override
	public void release() {
		wrappedFormGroup = null;
		super.release();
	}

	protected org.appng.formtags.FormGroup getWrappedFormGroup() {
		if (null == wrappedFormGroup) {
			wrappedFormGroup = getFormData().getWrappedFormData().addFormGroup();
		}
		return wrappedFormGroup;
	}

	public FormData getFormData() {
		return (FormData) findAncestorWithClass(this, FormData.class);
	}

	public String getErrorClass() {
		return getWrappedFormGroup().getErrorClass();
	}

	public void setErrorClass(String errorClass) {
		getWrappedFormGroup().setErrorClass(errorClass);
	}

	public String getStyleClass() {
		return getWrappedFormGroup().getStyleClass();
	}

	public void setStyleClass(String styleClass) {
		getWrappedFormGroup().setStyleClass(styleClass);
	}

	public String getName() {
		return getWrappedFormGroup().getName();
	}

	public void setName(String name) {
		getWrappedFormGroup().setName(name);
	}

	public boolean isMandatory() {
		return getWrappedFormGroup().isMandatory();
	}

	public void setMandatory(boolean mandatory) {
		getWrappedFormGroup().setMandatory(mandatory);
	}

	public boolean isMultiple() {
		return wrappedFormGroup.isMultiple();
	}

	public void setMultiple(boolean multiple) {
		wrappedFormGroup.setMultiple(multiple);
	}

	public String getMandatoryMessage() {
		return wrappedFormGroup.getMandatoryMessage();
	}

	public void setMandatoryMessage(String mandatoryMessage) {
		wrappedFormGroup.setMandatoryMessage(mandatoryMessage);
	}

	public String getErrorElementId() {
		return wrappedFormGroup.getErrorElementId();
	}

	public void setErrorElementId(String errorElementId) {
		wrappedFormGroup.setErrorElementId(errorElementId);
	}

	public String getErrorMessage() {
		return wrappedFormGroup.getErrorMessage();
	}

	public void setErrorMessage(String errorMessage) {
		wrappedFormGroup.setErrorMessage(errorMessage);
	}

}
