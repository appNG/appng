/*
 * Copyright 2011-2018 the original author or authors.
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
import java.io.StringWriter;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTag;

import org.apache.jasper.runtime.BodyContentImpl;
import org.appng.formtags.FormConfirmation.FormConfirmationMode;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockJspWriter;

public class FormConfirmationTest {

	@Test
	public void testSubmittedOk() throws JspException, IOException {
		runTest(true, true, BodyTag.EVAL_BODY_BUFFERED, FormConfirmationMode.SUBMITTED, true);
	}

	@Test
	public void testSubmittedWithErrors() throws JspException, IOException {
		runTest(false, true, BodyTag.SKIP_BODY, FormConfirmationMode.SUBMITTED, false);
	}

	@Test
	public void testNotSubmitted() throws JspException, IOException {
		runTest(true, false, BodyTag.SKIP_BODY, FormConfirmationMode.SUBMITTED, false);
	}

	private void runTest(boolean valid, boolean submitted, int returnCode, FormConfirmationMode mode,
			boolean withContent) throws JspException, IOException {
		FormConfirmation formConfirmation = new FormConfirmation() {
			@Override
			protected void doProcess() throws JspException {
			}
		};
		StringWriter contentWriter = new StringWriter();
		String content = "<p>This is content!</p>";
		if (withContent) {
			JspWriter enclosingWriter = new MockJspWriter(contentWriter);
			BodyContentImpl bodyContent = new BodyContentImpl(enclosingWriter);
			bodyContent.write(content);
			formConfirmation.setBodyContent(bodyContent);
		}

		Form form = new Form();
		formConfirmation.setParent(form);
		form.getWrappedForm().getFormData().addFormElement().setValid(valid);
		form.getWrappedForm().getFormConfirmation().setMode(mode);
		form.getWrappedForm().setSubmitted(submitted);
		int doStartTag = formConfirmation.doStartTag();
		Assert.assertEquals(returnCode, doStartTag);

		formConfirmation.doAfterBody();
		if (withContent) {
			Assert.assertEquals(content, contentWriter.toString());
		}
	}
}
