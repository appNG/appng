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
package org.appng.core.controller.rest.openapi;

import org.appng.api.Scope;
import org.appng.api.Session;
import org.appng.openapi.model.PageDefinition;
import org.appng.testsupport.validation.WritingJsonValidator;
import org.junit.Test;
import org.springframework.http.ResponseEntity;

public class OpenApiPageTest extends OpenApiTest {

	@Test
	public void testPage() throws Exception {
		environment.removeAttribute(Scope.SESSION, Session.Environment.MESSAGES);
		OpenApiPage openApiPage = new OpenApiPage(site, applicationProvider, request, messageSource) {
		};
		ResponseEntity<PageDefinition> page = openApiPage.getPage(PAGE_ID, null, environment, servletRequest,
				servletResponse);
		WritingJsonValidator.validate(page, "rest/openapi/page.json");
	}

}
