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
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class defines a Form element.
 * <p/>
 * The {@code <appNG:formElement>} is used as a wrapper for standard HTML form input fields, which are
 * <ul>
 * <li>{@code <input>}</li>
 * <li>{@code <textarea>}</li>
 * <li>{@code <select>}</li>
 * <li>{@code <option>}</li>
 * </ul>
 * <p/>
 * <b>Attributes:</b><br/>
 * All of the following attributes are optional.
 * <ul>
 * <li>mandatory - set to {@code true} if the field is mandatory</li>
 * <li>mandatoryMessage - the message to be displayed when no value has been entered for a mandatory field</li>
 * <li>errorMessage - the error message to be displayed when validation fails</li>
 * <li>errorClass - the CSS class to add to the input field when validation fails</li>
 * <li>errorElementId - the id of an element to append a {@code <span>} with the error message</li>
 * <li>rule - a validation rule for the input field</li>
 * <li>desc - a description for the input field</li>
 * </ul>
 * <p/>
 * <b>Usage:</b>
 * 
 * <pre>
 * &lt;appNG:formElement errorClass="error" rule="email" mandatory="true" errorElementId="emailError"
 * 	mandatoryMessage="E-mail is mandatory!" errorMessage="Not a valid e-mail!">
 *   &lt;input type="text" name="email" value=""/>
 *   &lt;div id="emailError">&lt/div>
 * &lt;/appNG:formElement>
 * </pre>
 * 
 * <b>Output (no value given):</b>
 * 
 * <pre>
 * &lt;input class="error" type="text" name="email" value=""/>
 * &lt;div id="emailError">&lt;span>E-mail is mandatory!&lt;/span>&lt;/div>
 * </pre>
 * 
 * <b>Output (invalid value given):</b>
 * 
 * <pre>
 * &lt;input class="error" type="text" name="email" value=""/>
 * &lt;div id="emailError">&lt;span>Not a valid e-mail!&lt;/span>&lt/div>
 * </pre>
 * 
 * <b>Rules:</b><br/>
 * Here's a list of the possible values for the {@code rule} attribute
 * <table>
 * <tr>
 * <th>name</th>
 * <th>description</th>
 * <th>example</th>
 * </tr>
 * <tr>
 * <td>string</td>
 * <td>only word characters ([a-zA-Z_0-9] allowed)</td>
 * <td>{@code rule="string"}</td>
 * </tr>
 * <tr>
 * <td>email</td>
 * <td>must be a valid email address</td>
 * <td>{@code rule="email"}</td>
 * </tr>
 * <tr>
 * <td>equals</td>
 * <td>must be equal to another field or value</td>
 * <td>{@code rule="equals('foo')}<br/>{@code rule="equals(anotherfield)}"</td>
 * </tr>
 * <tr>
 * <td>regExp</td>
 * <td>must match the given regular expression</td>
 * <td>{@code rule="regExp('[A-F0-9]+')"}</td>
 * </tr>
 * <tr>
 * <td>number</td>
 * <td>must be a number</td>
 * <td>{@code rule="number"}</td>
 * </tr>
 * <tr>
 * <td>numberFractionDigits</td>
 * <td>must be a number with up to x digits, and y fractional digits</td>
 * <td>{@code rule="number(2,4)"}</td>
 * </tr>
 * <tr>
 * <td>size</td>
 * <td>must have an exact length of x</td>
 * <td>{@code rule="size(3)"}</td>
 * </tr>
 * <tr>
 * <td>sizeMin</td>
 * <td>must have a minimum length of x</td>
 * <td>{@code rule="sizeMin(3)"}</td>
 * </tr>
 * <tr>
 * <td>sizeMax</td>
 * <td>must have a maximum length of x</td>
 * <td>{@code rule="sizeMax(3)"}</td>
 * </tr>
 * <tr>
 * <td>sizeMinMax</td>
 * <td>must have a minimum length of x and a maximum length of y</td>
 * <td>{@code rule="sizeMinMax(3,5)"}</td>
 * </tr>
 * <tr>
 * <td>fileType</td>
 * <td>must have one of the comma-separated types<br/>
 * ({@code <input type="file">} only)</td>
 * <td>{@code rule="fileType('tif,pdf')"}</td>
 * </tr>
 * <tr>
 * <td>fileSizeMin</td>
 * <td>must have a minimum size of x MB/KB<br/>
 * ({@code <input type="file">} only)</td>
 * <td>{@code rule="fileSizeMin('0.5MB')"}</td>
 * </tr>
 * <tr>
 * <td>fileSizeMax</td>
 * <td>must have a maximum size of x MB/KB<br/>
 * ({@code <input type="file">} only)</td>
 * <td>{@code rule="fileSizeMax('5.0MB')"}</td>
 * </tr>
 * <tr>
 * <td>fileSize</td>
 * <td>must have a size between x and y MB/KB<br/>
 * ({@code <input type="file">} only)</td>
 * <td>{@code rule="fileSize('500KB','5.0MB')"}</td>
 * </tr>
 * <tr>
 * <td>fileCount</td>
 * <td>between x and y files must have been selected<br/>
 * ({@code <input type="file" multiple="true">} only)</td>
 * <td>{@code rule="fileCount(1,10)"}</td>
 * </tr>
 * <tr>
 * <td>fileCountMin</td>
 * <td>at least x files must have been selected<br/>
 * ({@code <input type="file" multiple="true">} only)</td>
 * <td>{@code rule="fileCountMin(5)"}</td>
 * </tr>
 * <tr>
 * <td>fileCountMax</td>
 * <td>at most x files must have been selected<br/>
 * ({@code <input type="file" multiple="true">} only)</td>
 * <td>{@code rule="fileCountMax(5)"}</td>
 * </tr>
 * <tr>
 * <td>captcha</td>
 * <td>Must match a captcha value. The result of the captcha is<br/>
 * stored in the variable SESSION['SESSION']['captcha'], where the first<br/>
 * SESSION means the HTTP Session, ['SESSION'] the name of an attribute<br/>
 * within the HTTP session. Since this attribute is also a map, you can<br/>
 * use ['captcha'] to retrieve the result.</td>
 * <td>{@code rule="captcha(SESSION['SESSION']['captcha'])}</td>
 * </tr>
 * </table>
 * 
 * @author Matthias Herlitzius
 * @author Matthias Müller
 */
public class FormElement extends BodyTagSupport {

	private static Logger log = LoggerFactory.getLogger(FormElement.class);
	protected org.appng.formtags.FormElement wrappedFormElement;

	private String desc;

	public FormElement() {

	}

	@Override
	public int doStartTag() throws JspException {
		if (getFormData() == null) {
			throw new JspTagException("a <formElement> can only be used inside <formData>!");
		}
		return EVAL_BODY_BUFFERED;
	}

	@Override
	public int doEndTag() throws JspException {
		wrappedFormElement = null;
		return super.doEndTag();
	}

	@Override
	public int doAfterBody() {
		try {
			String elementContent = getBodyContent().getString();
			getWrappedFormElement().setContent(elementContent);
			getWrappedFormElement().setFormData(getFormData().getWrappedFormData());
			String content = processContent();
			getBodyContent().getEnclosingWriter().print(content);
		} catch (IOException ioe) {
			log.error("error while writing to JspWriter", ioe);
		}
		return SKIP_BODY;
	}

	public String processContent() {
		return getWrappedFormElement().processContent();
	}

	public boolean isMandatory() {
		return getWrappedFormElement().isMandatory();
	}

	public void setMandatory(boolean mandatory) {
		getWrappedFormElement().setMandatory(mandatory);
	}

	public String getRule() {
		return getWrappedFormElement().getRule();
	}

	public void setRule(String rule) {
		getWrappedFormElement().setRule(rule);
	}

	public String getErrorClass() {
		return getWrappedFormElement().getErrorClass();
	}

	public void setErrorClass(String errorClass) {
		getWrappedFormElement().setErrorClass(errorClass);
	}

	public boolean isForwardValidation() {
		return getWrappedFormElement().isForwardValidation();
	}

	public void setForwardValidation(boolean forwardValidation) {
		getWrappedFormElement().setForwardValidation(forwardValidation);
	}

	public String getContent() {
		return getWrappedFormElement().getContent();
	}

	public void setContent(String content) {
		getWrappedFormElement().setContent(content);
	}

	public String getName() {
		return getWrappedFormElement().getName();
	}

	public void setName(String name) {
		getWrappedFormElement().setName(name);
	}

	public String getErrorMessage() {
		return getWrappedFormElement().getErrorMessage();
	}

	public void setErrorMessage(String errorMessage) {
		getWrappedFormElement().setErrorMessage(errorMessage);
	}

	public String getMandatoryMessage() {
		return wrappedFormElement.getMandatoryMessage();
	}

	public void setMandatoryMessage(String mandatoryMessage) {
		wrappedFormElement.setMandatoryMessage(mandatoryMessage);
	}

	public String getErrorElementId() {
		return wrappedFormElement.getErrorElementId();
	}

	public void setErrorElementId(String errorElementId) {
		wrappedFormElement.setErrorElementId(errorElementId);
	}

	protected org.appng.formtags.FormElement getWrappedFormElement() {
		if (null == wrappedFormElement) {
			FormGroup formGroup = getFormGroup();
			if (null == formGroup) {
				wrappedFormElement = getFormData().getWrappedFormData().addFormElement();
			} else {
				org.appng.formtags.FormGroup wrappedFormGroup = formGroup.getWrappedFormGroup();
				String name = wrappedFormGroup.getName();
				wrappedFormElement = wrappedFormGroup.addFormElement();
				wrappedFormElement.setName(name);
				wrappedFormElement.setMandatory(wrappedFormGroup.isMandatory());
			}
		}
		return wrappedFormElement;
	}

	@Override
	public void release() {
		wrappedFormElement = null;
		super.release();
	}

	public FormData getFormData() {
		return (FormData) findAncestorWithClass(this, FormData.class);
	}

	public FormGroup getFormGroup() {
		return (FormGroup) findAncestorWithClass(this, FormGroup.class);
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

}
