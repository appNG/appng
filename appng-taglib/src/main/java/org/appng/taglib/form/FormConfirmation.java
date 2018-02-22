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
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.appng.api.Environment;
import org.appng.api.ParameterSupport;
import org.appng.api.model.Application;
import org.appng.api.support.ParameterSupportBase;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.core.domain.SiteImpl;
import org.appng.core.model.ApplicationProvider;
import org.appng.formtags.FormConfirmation.FormConfirmationMode;
import org.appng.formtags.FormProcessProvider;
import org.appng.taglib.MultiSiteSupport;
import org.appng.taglib.ParameterOwner;

/**
 * This class represents the part of a form that is used to confirm and process the form input data. Usually something
 * like "Thank you for your message" is displayed. The form data is being processed by a
 * {@link org.appng.formtags.FormProcessProvider} or a {@link org.appng.api.FormProcessProvider}. See {@link Form} for
 * more details.
 * </p>
 * <b>Attributes:</b>
 * <ul>
 * <li>application - the name of the {@link Application} that contains the {@link org.appng.api.FormProcessProvider}
 * </li>
 * <li>method - the name of the bean implementing {@link org.appng.api.FormProcessProvider}</li>
 * <li>mode - the display mode of the tag's body content. The default is {@code submitted}, meaning the body content is
 * only being displayed if the form has been submitted. If the body content should always be displayed, use
 * {@code always}.</li>
 * </ul>
 * 
 * @author Matthias Herlitzius
 * @author Matthias Müller
 * 
 * @see Form
 */
public class FormConfirmation extends BodyTagSupport implements ParameterOwner {

	private String application;
	private String method;
	private Map<String, String> tagletAttributes;

	public FormConfirmation() {
	}

	@Override
	public int doStartTag() throws JspException {
		if (getForm() == null) {
			throw new JspTagException("<formConfirmation> can only be used inside <form>!");
		}
		getForm().setFormConfirmation(this);
		tagletAttributes = new HashMap<String, String>();

		switch (getWrappedFormConfirmation().getMode()) {

		case SUBMITTED:
			if (!getForm().getWrappedForm().isSubmitted() || getForm().getWrappedForm().hasErrors()) {
				return SKIP_BODY;
			}
		default:
			break;
		}
		return EVAL_BODY_BUFFERED;

	}

	public Form getForm() {
		Form form = (Form) findAncestorWithClass(this, Form.class);
		return form;
	}

	@Override
	public int doAfterBody() throws JspException {
		writeBodyContent(FormConfirmationMode.ALWAYS);
		if (getForm().getWrappedForm().isSubmitted() && !getForm().getWrappedForm().hasErrors()) {
			writeBodyContent(FormConfirmationMode.SUBMITTED);
			doProcess();
		}
		return super.doAfterBody();
	}

	protected void doProcess() throws JspException {
		org.appng.formtags.Form wrappedForm = getForm().getWrappedForm();
		MultiSiteSupport multiSiteSupport = new MultiSiteSupport();
		Environment env = DefaultEnvironment.get(pageContext);
		HttpServletRequest servletRequest = (HttpServletRequest) pageContext.getRequest();
		multiSiteSupport.process(env, application, method, servletRequest);
		SiteImpl callingSite = multiSiteSupport.getCallingSite();
		ApplicationProvider applicationProvider = multiSiteSupport.getApplicationProvider();
		ParameterSupport parameterSupport = new ParameterSupportBase("#\\[", "\\]",
				wrappedForm.getRequest().getParameters()) {
		};

		Map<String, Object> parameters = new HashMap<String, Object>();
		for (String paramName : tagletAttributes.keySet()) {
			parameters.put(paramName, parameterSupport.replaceParameters(tagletAttributes.get(paramName)));
		}

		Object formProcessProvider = applicationProvider.getBean(method);
		if (null == formProcessProvider) {
			throw new JspException(
					"no FormProcessProvider '" + method + "' for application '" + getApplication() + "'!");
		}
		JspWriter writer = getBodyContent().getEnclosingWriter();
		if (formProcessProvider instanceof org.appng.api.FormProcessProvider) {
			((org.appng.api.FormProcessProvider) formProcessProvider).onFormSuccess(env, callingSite,
					applicationProvider, writer, wrappedForm, parameters);
		} else if (formProcessProvider instanceof FormProcessProvider) {
			wrappedForm.addFormProcessProvider((FormProcessProvider) formProcessProvider);
		}
		wrappedForm.runProcessProviders(writer, parameters);
	}

	protected void writeBodyContent(FormConfirmationMode mode) throws JspException {
		if (getForm().getWrappedForm().getFormConfirmation().getMode().equals(mode)) {
			try {
				getBodyContent().getEnclosingWriter().write(getBodyContent().getString());
			} catch (IOException e) {
				throw new JspException(e);
			}
		}
	}

	@Override
	public int doEndTag() throws JspException {
		tagletAttributes.clear();
		application = null;
		method = null;
		tagletAttributes = null;
		return super.doEndTag();
	}

	@Override
	public void release() {
		super.release();
	}

	public void addParameter(String name, String value) {
		tagletAttributes.put(name, value);
	}

	org.appng.formtags.FormConfirmation getWrappedFormConfirmation() {
		return getForm().getWrappedForm().getFormConfirmation();
	}

	public String getApplication() {
		return application;
	}

	public void setApplication(String application) {
		this.application = application;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public void setMode(String mode) {
		getWrappedFormConfirmation().setMode(mode);
	}
}
