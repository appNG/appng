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
package org.appng.taglib.form;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.lang3.StringUtils;
import org.appng.forms.Request;
import org.appng.taglib.TagletAdapter;

/**
 * This class represents a web Form.
 * <p/>
 * It must contain a {@code <appNG:formData>} and a {@code <appNG:formConfirmation>}.
 * <p/>
 * <b>Usage:</b>
 * 
 * <pre>
 * &lt;appNG:form>
 *   &lt;appNG:formData mode="not_submitted">
 *     &lt;form action="" method="post" enctype="multipart/form-data" >
 *       &lt;!-- &lt;appNG:formElement>s go here -->
 *       &lt;input type="submit" />
 *     &lt;/form>
 *   &lt;/appNG:formData>
 *   &lt;appNG:formConfirmation application="appng-webutils" method="debugProvider" mode="submitted">
 *     &lt;appNG:param name="foo">bar&lt;/appNG:param>
 *     &lt;appNG:param name="jin">fizz&lt;/appNG:param>
 *   &lt;/appNG:formConfirmation>
 * &lt;/appNG:form>
 * </pre>
 * 
 * As you can see, the {@code <appNG:formConfirmation>} can be parameterized using the syntax {@code 
 * <appNG:param name="name">value</appNG:param>}. For the value, you can access request parameters using the syntax
 * {@code #[param]}, for example {@code <appNG:param name="receiver">#[email]</appNG:param>}.
 * 
 * @author Matthias Herlitzius
 * 
 * @see FormElement
 * @see FormGroup
 * @see FormConfirmation
 */
public class Form extends TagSupport {

	static final String IS_SUBMITTED = "isSubmitted";
	static final String FORM_NAME = "formName";
	static final String TRUE = "true";
	private org.appng.formtags.Form wrappedForm;

	private FormConfirmation formConfirmation;
	private FormData formData;
	private String name;

	public Form() {

	}

	public int doAfterBody() throws JspException {
		return super.doAfterBody();
	}

	public int doStartTag() throws JspException {
		Request request = TagletAdapter.getRequest(pageContext);
		getWrappedForm().enableLogging();
		boolean isSubmitted = TRUE.equalsIgnoreCase(request.getParameter(IS_SUBMITTED));
		boolean isCurrentForm = !hasName() || getName().equals(request.getParameter(FORM_NAME));
		getWrappedForm().setSubmitted(isSubmitted && isCurrentForm);
		getWrappedForm().setRequest(request);

		return EVAL_BODY_INCLUDE;
	}

	public int doEndTag() throws JspException {
		wrappedForm = null;
		name = null;
		return super.doEndTag();
	}

	public org.appng.formtags.Form getWrappedForm() {
		if (null == wrappedForm) {
			wrappedForm = new org.appng.formtags.Form();
		}
		return wrappedForm;
	}

	private FormConfirmation getFormConfirmation() {
		return formConfirmation;
	}

	public void setFormConfirmation(FormConfirmation formConfirmation) {
		this.formConfirmation = formConfirmation;
	}

	private FormData getFormData() {
		return formData;
	}

	public void setFormData(FormData formData) {
		this.formData = formData;
	}

	public void setRestoreRequestData(Boolean restoreRequestData) {
		getWrappedForm().setRestoreRequestData(restoreRequestData);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean hasName() {
		return StringUtils.isNotBlank(name);
	}

}
