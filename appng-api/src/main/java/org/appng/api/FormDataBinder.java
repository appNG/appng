/*
 * Copyright 2011-2023 the original author or authors.
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
package org.appng.api;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.appng.forms.Request;
import org.appng.formtags.Form;
import org.appng.formtags.FormElement;
import org.appng.formtags.FormElement.InputType;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.validation.DataBinder;

/**
 * A {@link DataBinder} that uses a {@link Form} to bind its values to the target object. Additional parameters that are
 * not part of the form can also be used using {@link #setBindAdditionalParams(boolean)}. Also, some additional
 * properties can be used by calling {@link #setExternalParams(Map)}.<br/>
 * This class is especially useful inside a {@link FormProcessProvider}, as shown below:
 * 
 * <pre>
 * public void onFormSuccess(Environment environment, Site site, Application application, Writer writer, Form form,
 * 		Map<String, Object> properties) {
 * 	FormDataBinder<Person> formDataBinder = new FormDataBinder<Person>(new Person(), form);
 * 	formDataBinder.setExternalParams(properties);
 * 	Person person = formDataBinder.bind();
 * 	// proceed with person
 * }
 * </pre>
 * 
 * Also note the possibility to set a custom {@link ConversionService} for the binder.
 * 
 * @author Matthias Müller
 *
 * @param <T>
 *            The type to bind the data to.
 */
public class FormDataBinder<T> extends RequestDataBinder<T> {

	private Form form;
	private boolean bindAdditionalParams;
	private Map<String, Object> externalParams;

	/**
	 * Constructs a new {@link FormDataBinder} using a {@link DefaultConversionService}
	 * 
	 * @param target
	 *               the target object
	 * @param form
	 *               the {@link Form}
	 */
	public FormDataBinder(T target, Form form) {
		this(target, form, new DefaultConversionService());
	}

	/**
	 * * Constructs a new {@link FormDataBinder} using the given {@link ConversionService}
	 * 
	 * @param target
	 *                          the target object
	 * @param form
	 *                          the {@link Form}
	 * @param conversionService
	 *                          the {@link ConversionService} to use
	 */
	public FormDataBinder(T target, Form form, ConversionService conversionService) {
		super(target);
		this.form = form;
		setConversionService(conversionService);
	}

	/**
	 * Performs the actual binding. Therefore, all {@link FormElement}s are retrieved from the {@link Form}. Also,
	 * additional and external parameters might be used.
	 * 
	 * @return the object where the binding has been applied to
	 * 
	 * @see #setBindAdditionalParams(boolean)
	 * @see #setExternalParams(Map)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public T bind() {
		MutablePropertyValues mpvs = new MutablePropertyValues();
		Request request = form.getRequest();
		Set<String> names = new HashSet<>(request.getParameterNames());
		for (FormElement formElement : form.getFormData().getElements()) {
			String name = formElement.getName();
			names.remove(name);
			if (InputType.FILE.equals(formElement.getInputType())) {
				addValue(mpvs, name, formElement.getFormUploads());
			} else {
				addValue(mpvs, name, formElement.getRequestValues());
			}
		}
		if (bindAdditionalParams) {
			for (String name : names) {
				addValue(mpvs, name, request.getParameterList(name));
			}
		}
		if (null != externalParams) {
			for (String externalParam : externalParams.keySet()) {
				mpvs.addPropertyValue(externalParam, externalParams.get(externalParam));
			}
		}
		doBind(mpvs);
		return (T) getTarget();
	}

	public boolean isBindAdditionalParams() {
		return bindAdditionalParams;
	}

	/**
	 * Whether to use all request parameters for binding, including those that are not defined through a
	 * {@link FormElement} of the given {@link Form}
	 * 
	 * @param bindAdditionalParams
	 *                             whether to use all request parameters for binding
	 */
	public void setBindAdditionalParams(boolean bindAdditionalParams) {
		this.bindAdditionalParams = bindAdditionalParams;
	}

	/**
	 * Sets some additional external parameters used for binding
	 * 
	 * @param externalParams
	 *                       the external parameters
	 */
	public void setExternalParams(Map<String, Object> externalParams) {
		this.externalParams = externalParams;
	}

	public Map<String, Object> getExternalParams() {
		return externalParams;
	}

}
