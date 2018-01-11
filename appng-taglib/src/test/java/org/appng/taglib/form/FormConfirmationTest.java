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

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTag;

import org.appng.formtags.FormConfirmation.FormConfirmationMode;
import org.junit.Assert;
import org.junit.Test;

public class FormConfirmationTest {

	@Test
	public void testSubmittedOk() throws JspException {
		runTest(true, true, BodyTag.EVAL_BODY_BUFFERED);
	}

	@Test
	public void testSubmittedWithErrors() throws JspException {
		runTest(false, true, BodyTag.SKIP_BODY);
	}

	@Test
	public void testNotSubmitted() throws JspException {
		runTest(true, false, BodyTag.SKIP_BODY);
	}

	private void runTest(boolean valid, boolean submitted, int returnCode) throws JspException {
		FormConfirmation formConfirmation = new FormConfirmation();
		Form form = new Form();
		formConfirmation.setParent(form);
		form.getWrappedForm().getFormData().addFormElement().setValid(valid);
		form.getWrappedForm().getFormConfirmation().setMode(FormConfirmationMode.SUBMITTED);
		form.getWrappedForm().setSubmitted(submitted);
		int doStartTag = formConfirmation.doStartTag();
		Assert.assertEquals(returnCode, doStartTag);
	}
}
