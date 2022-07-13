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
import org.appng.testsupport.validation.WritingJsonValidator;
import org.junit.Test;
import org.springframework.http.ResponseEntity;

public class OpenApiActionTest extends OpenApiTest {

	@Test
	public void testAction() throws Exception {
		OpenApiAction openApiAction = new OpenApiAction(site, applicationProvider, request, messageSource) {
		};
		Map<String, String> pathVariables = new HashMap<>();
		pathVariables.put("action", "create");
		ResponseEntity<Action> action = openApiAction.getAction("events", "create", environment, servletRequest,
				servletResponse, pathVariables);
		WritingJsonValidator.validate(action, "rest/openapi/action.json");

		servletRequest.addParameter("form_action", "create");
		servletRequest.addParameter("action", "create");
		ResponseEntity<Action> validated = openApiAction.performActionMultiPart("events", "create", environment,
				servletRequest, servletResponse, pathVariables);
		WritingJsonValidator.validate(validated, "rest/openapi/action-validate.json");

		servletRequest.addParameter("name", "super new name");
		ResponseEntity<Action> performed = openApiAction.performActionMultiPart("events", "create", environment,
				servletRequest, servletResponse, pathVariables);
		WritingJsonValidator.validate(performed, "rest/openapi/action-performed.json");

	}

}
