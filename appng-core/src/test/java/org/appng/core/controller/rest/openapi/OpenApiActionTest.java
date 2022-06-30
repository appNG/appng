/*
 * Copyright 2011-2022 the original author or authors.
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
package org.appng.core.controller.rest.openapi;

import java.util.HashMap;
import java.util.Map;

import org.appng.openapi.model.Action;
import org.appng.openapi.model.Datasource;
import org.appng.testsupport.validation.WritingJsonValidator;
import org.junit.Test;
import org.springframework.http.ResponseEntity;

public class OpenApiActionTest extends OpenApiTest {

	@Test
	public void testAction() throws Exception {
		OpenApiAction openApiAction = new OpenApiAction(site, applicationProvider, request, messageSource, true) {
		};
		Map<String, String> pathVariables = new HashMap<>();
		pathVariables.put("form_action", "create");
		pathVariables.put("action", "create");
		ResponseEntity<Action> action = openApiAction.getAction("events", "create", pathVariables, environment,
				servletRequest, servletResponse);
		WritingJsonValidator.validate(action, "rest/openapi/action.json");

		servletRequest.addParameter("form_action", "create");
		servletRequest.addParameter("action", "create");
		ResponseEntity<Action> validated = openApiAction.performActionMultiPart("events", "create", environment,
				servletRequest, servletResponse);
		WritingJsonValidator.validate(validated, "rest/openapi/action-validate.json");

		servletRequest.addParameter("name", "super new name");
		ResponseEntity<Action> performed = openApiAction.performActionMultiPart("events", "create", environment,
				servletRequest, servletResponse);
		WritingJsonValidator.validate(performed, "rest/openapi/action-performed.json");

	}

	@Test
	public void testDataSource() throws Exception {
		OpenApiDataSource openApiDatasource = new OpenApiDataSource(site, applicationProvider, request, messageSource,
				true) {
		};
		ResponseEntity<Datasource> datasource = openApiDatasource.getDataSource("entities", null, environment,
				servletRequest, servletResponse);
		WritingJsonValidator.validate(datasource, "rest/openapi/datasource.json");
	}

}
