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

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.appng.formtags.FormData.FormDataMode;

import lombok.extern.slf4j.Slf4j;

/**
 * This class represents a web Form.
 * <p>
 * The {@code <appNG:formData>} wraps an HTML {@code <form>} and contains several input fields, that in turn are wrapped
 * by {@code <appNG:formElement>}s. See {@link Form} for details.
 * 
 * @author Matthias Herlitzius
 * @author Matthias Müller
 * 
 * @see Form
 * @see FormElement
 * @see FormGroup
 */
@Slf4j
public class FormData extends BodyTagSupport {

	private static final String FORM_CLOSE = "</form>";

	public FormData() {

	}

	@Override
	public int doStartTag() throws JspException {
		if (getForm() == null) {
			throw new JspTagException("<formData> can only be used inside <form>!");
		}
		getForm().setFormData(this);

		return EVAL_BODY_BUFFERED;
	}

	@Override
	public int doAfterBody() {
		if (!getForm().getWrappedForm().hasErrors() && getForm().getWrappedForm().isSubmitted()
				&& getWrappedFormData().getMode().equals(FormDataMode.NOT_SUBMITTED)) {
			return SKIP_BODY;
		}
		BodyContent body = getBodyContent();
		if (body != null) {
			try {
				JspWriter out = body.getEnclosingWriter();
				StringBuilder content = new StringBuilder(body.getString());
				content.insert(content.indexOf(FORM_CLOSE), getHiddenField(Form.IS_SUBMITTED, Form.TRUE));
				if (getForm().hasName()) {
					content.insert(content.indexOf(FORM_CLOSE), getHiddenField(Form.FORM_NAME, getForm().getName()));
				}
				out.print(content.toString());
			} catch (IOException ioe) {
				LOGGER.error("error while writing body", ioe);
			}
		}
		return SKIP_BODY;
	}

	private String getHiddenField(String name, String value) {
		return "<input type=\"hidden\" name=\"" + name + "\" value=\"" + value + "\"/>";
	}

	public Form getForm() {
		Form form = (Form) findAncestorWithClass(this, Form.class);
		return form;
	}

	protected org.appng.formtags.FormData getWrappedFormData() {
		return getForm().getWrappedForm().getFormData();
	}

	public void setMode(String mode) {
		getWrappedFormData().setMode(mode);
	}

}
