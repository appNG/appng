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

import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.appng.forms.Request;

import lombok.extern.slf4j.Slf4j;

/**
 * This class represents a HTML form. A {@link Form} consists of two parts:
 * <ol>
 * <li>the {@link FormData}, wrapping the various {@link FormElement}s</li>
 * <li>the {@link FormConfirmation}, responsible for what happens after the form has been submitted.
 * </ol>
 * A Form can habe multiple {@link FormProcessProvider}s which are processing the form inputs.
 * 
 * @author Matthias Müller
 * 
 * @see FormData
 * @see FormElement
 * @see FormConfirmation
 * @see FormProcessProvider
 */
@Slf4j
public class Form {

	private Request request;

	private boolean submitted;

	private FormData formData;

	private FormConfirmation formConfirmation;

	private Boolean restoreRequestData;

	private List<FormProcessProvider> formProcessProviders;

	/**
	 * Constructs a new {@link Form}
	 */
	public Form() {
		this.formData = new FormData(this);
		this.formConfirmation = new FormConfirmation(this);
		this.formProcessProviders = new ArrayList<>();
	}

	/**
	 * Enables logging for this {@link Form} by adding a {@link LogFormData} to the form's {@link FormProcessProvider}s.
	 */
	public void enableLogging() {
		addFormProcessProvider(new LogFormData(LOGGER));
	}

	/**
	 * Adds a {@link FormProcessProvider} to this {@link Form}.
	 * 
	 * @param formProcessProvider
	 *            the {@link FormProcessProvider} to add
	 * @return {@code true} if adding was successful, {@code false} otherwise
	 */
	public boolean addFormProcessProvider(FormProcessProvider formProcessProvider) {
		return this.formProcessProviders.add(formProcessProvider);
	}

	/**
	 * Removes a {@link FormProcessProvider} to this {@link Form}.
	 * 
	 * @param formProcessProvider
	 *            the {@link FormProcessProvider} to remove
	 * @return {@code true} if removing was successful, {@code false} otherwise
	 */
	public boolean removeFormProcessProvider(FormProcessProvider formProcessProvider) {
		return this.formProcessProviders.remove(formProcessProvider);
	}

	/**
	 * Checks whether this {@link Form} has been submitted.
	 * 
	 * @return {@code true} if this {@link Form} has been submitted, {@code false} otherwise
	 */
	public boolean isSubmitted() {
		return submitted;
	}

	/**
	 * Sets whether or not this {@link Form} has been submitted.
	 * 
	 * @param submitted
	 */
	public void setSubmitted(boolean submitted) {
		this.submitted = submitted;
	}

	/**
	 * Convenient method for checking whethers this {@link Form} has validation errors, delegating to
	 * {@link FormData#hasErrors()}.
	 * 
	 * @return {@code true} if this {@link Form} has validation errors, {@code false} otherwise
	 */
	public boolean hasErrors() {
		return getFormData().hasErrors();
	}

	/**
	 * Returns the {@link Request} for this {@link Form}.
	 * 
	 * @return the {@link Request}
	 */
	public Request getRequest() {
		return request;
	}

	/**
	 * Sets the {@link Request} for this form.
	 * 
	 * @param request
	 *            the {@link Request}
	 */
	public void setRequest(Request request) {
		this.request = request;
	}

	/**
	 * Returns the {@link RuleValidation} for this {@link Form}.
	 * 
	 * @return the {@link RuleValidation}
	 */
	public RuleValidation getRuleValidation() {
		return new RuleValidation(getRequest());
	}

	// TODO is this even used anywhere?
	public Boolean getRestoreRequestData() {
		return restoreRequestData;
	}

	public void setRestoreRequestData(Boolean restoreRequestData) {
		this.restoreRequestData = restoreRequestData;
	}

	/**
	 * Returns the {@link FormData} for this {@link Form}.
	 * 
	 * @return the {@link FormData}
	 */
	public FormData getFormData() {
		return formData;
	}

	/**
	 * Sets the {@link FormData} for this {@link Form}.
	 * 
	 * @param formData
	 *            the {@link FormData}
	 */
	public void setFormData(FormData formData) {
		this.formData = formData;
	}

	/**
	 * Returns the {@link FormConfirmation} for this {@link Form}.
	 * 
	 * @return the {@link FormConfirmation}
	 */
	public FormConfirmation getFormConfirmation() {
		return formConfirmation;
	}

	/**
	 * Sets the {@link FormConfirmation} for this {@link Form}.
	 * 
	 * @param formConfirmation
	 *            the {@link FormConfirmation}
	 */
	public void setFormConfirmation(FormConfirmation formConfirmation) {
		this.formConfirmation = formConfirmation;
	}

	/**
	 * Executes the {@link FormProcessProvider} previously added by calling
	 * {@link #addFormProcessProvider(FormProcessProvider)}. Thus means,
	 * {@link FormProcessProvider#onFormSuccess(Writer, Form, Map)} is being executed with the given {@link Writer} and
	 * the given property {@link Map}.
	 * 
	 * @param writer
	 *            a {@link Writer}
	 * @param properties
	 *            the property {@link Map}
	 * 
	 * @see FormProcessProvider#onFormSuccess(Writer, Form, Map)
	 */
	public void runProcessProviders(Writer writer, Map<String, Object> properties) {
		if (isSubmitted()) {
			for (FormProcessProvider provider : formProcessProviders) {
				provider.onFormSuccess(writer, this, properties);
			}
		}
	}

}
