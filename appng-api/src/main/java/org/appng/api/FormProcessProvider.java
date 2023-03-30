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

import java.io.Writer;
import java.util.Map;

import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.formtags.Form;

/**
 * When using the appNG-Taglibrary to define a HTLM-form inside a JSP, a {@link FormProcessProvider} is used to handle
 * the submitted data. It can write to a database, send an email or whatever else you want to do with the form data.
 * <br/>
 * Consider using a {@link FormDataBinder} for binding {@link Form} parameters to a target object.
 * 
 * @author Matthias Müller
 * 
 * @see FormDataBinder
 */
public interface FormProcessProvider {

	/**
	 * This method is being called after a Form has been successfully submitted.
	 * 
	 * @param environment
	 *                    the current {@link Environment}
	 * @param site
	 *                    the current {@link Site}
	 * @param application
	 *                    the current {@link Application}
	 * @param writer
	 *                    the {@link Writer} of the calling JSP-page to write the response to
	 * @param form
	 *                    the {@link Form} that was submitted
	 * @param properties
	 *                    the properties which where used to configure the form
	 */
	void onFormSuccess(Environment environment, Site site, Application application, Writer writer, Form form,
			Map<String, Object> properties);

}
