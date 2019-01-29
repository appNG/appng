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
package org.appng.api;

import java.util.List;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.validation.DataBinder;

/**
 * A {@link DataBinder} that uses a {@link Request} to bind its values to the target object.<br/>
 * This class is especially useful inside a {@link Webservice}, as shown below:
 * 
 * <pre>
byte[] processRequest(Site site, Application application, Environment environment, Request request)
        throws BusinessException;
    RequestDataBinder<Person> requestDataBinder = new RequestDataBinder<Person>(new Person(), request);
    Person person = requestDataBinder.bind();
    // proceed with person
}
 * </pre>
 * 
 * Also note the possibility to set a custom {@link ConversionService} for the binder.
 * 
 * @author Matthias Müller
 *
 * @param <T>
 *            The type to bind the data to.
 */
public class RequestDataBinder<T> extends DataBinder {

	private Request request;

	/**
	 * Constructs a new {@link RequestDataBinder} using a {@link DefaultConversionService}
	 * 
	 * @param target
	 *            the target object
	 * @param request
	 *            the {@link Request}
	 */
	public RequestDataBinder(T target, Request request) {
		this(target, request, new DefaultConversionService());
	}

	/**
	 * Constructs a new {@link RequestDataBinder} using the given {@link ConversionService}
	 * 
	 * @param target
	 *            the target object
	 * @param request
	 *            the {@link Request}
	 * @param conversionService
	 *            the {@link ConversionService} to use
	 */
	public RequestDataBinder(T target, Request request, ConversionService conversionService) {
		super(target);
		this.request = request;
		setConversionService(conversionService);
	}

	protected RequestDataBinder(T target) {
		super(target);
	}

	/**
	 * Performs the actual binding. Therefore {@link Request#getParameterNames()} and {@link Request#getFormUploads()}
	 * is used.
	 * 
	 * @return the object where the binding has been applied to
	 */
	@SuppressWarnings("unchecked")
	public T bind() {
		MutablePropertyValues mpvs = new MutablePropertyValues();
		for (String name : request.getParameterNames()) {
			addValue(mpvs, name, request.getParameterList(name));
		}
		for (String name : request.getFormUploads().keySet()) {
			addValue(mpvs, name, request.getFormUploads(name));
		}
		doBind(mpvs);
		return (T) getTarget();
	}

	protected void addValue(MutablePropertyValues mpvs, String name, List<?> values) {
		if (isAllowed(name)) {
			if (values.size() == 1) {
				mpvs.addPropertyValue(name, values.get(0));
			} else {
				mpvs.addPropertyValue(name, values);
			}
		}
	}

}
