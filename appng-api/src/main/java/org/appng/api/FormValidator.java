/*
 * Copyright 2011-2020 the original author or authors.
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

import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.Message;

/**
 * Interface that can be implemented by an {@link ActionProvider} or the formBean that is used by an {@link ActionProvider}
 * . A {@link FormValidator} is used for contextual validation, e.g. to verify that one value is greater than the other
 * or that two given passwords match. For all other basic validations, use <a
 * href="https://jcp.org/en/jsr/detail?id=380">JSR-380 (Bean Validtion 2.0)</a> annotations.<br/>
 * The {@code validate()}-method is being called before the {@link ActionProvider}s {@code perform}-method. Only if
 * there are no errors (see {@link FieldProcessor#hasErrors()}), the {@link ActionProvider} is being actually called.
 * 
 * @author Matthias Müller
 * 
 * @see ActionProvider#perform(Site, Application, Environment, Options, Request, Object, FieldProcessor)
 * @see FieldProcessor#hasErrors()
 */
public interface FormValidator {

	/**
	 * This method validates the given {@link Request} and eventually adds some {@link Message}s to the
	 * {@link FieldProcessor} using one of its {@code addErrorMessage()}-methods.
	 * 
	 * @param site
	 *            the current {@link Site}
	 * @param application
	 *            the current {@link Application}
	 * @param environment
	 *            the current {@link Environment}
	 * @param options
	 *            the {@link Options} for this {@link DataProvider}
	 * @param request
	 *            the current {@link Request}
	 * @param fieldProcessor
	 *            the {@link FieldProcessor} containing all readable {@link FieldDef}initions for the DataProvider
	 * 
	 * @see FieldProcessor#addErrorMessage(String)
	 * @see FieldProcessor#addErrorMessage(FieldDef, String)
	 */
	void validate(Site site, Application application, Environment environment, Options options, Request request,
			FieldProcessor fieldProcessor);

}
