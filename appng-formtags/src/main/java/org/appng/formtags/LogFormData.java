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
package org.appng.formtags;

import java.io.Writer;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

public class LogFormData implements FormProcessProvider {

	private Logger log;

	public LogFormData(Logger log) {
		this.log = log;
	}

	public void onFormSuccess(Writer writer, Form form, Map<String, Object> properties) {
		List<FormElement> elements = form.getFormData().getElements();
		for (FormElement element : elements) {
			log.debug(element.toString());
		}

	}

}
