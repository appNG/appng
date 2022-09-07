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

import org.appng.openapi.model.Datasource;
import org.appng.testapplication.TestEntities;
import org.appng.testsupport.validation.WritingJsonValidator;
import org.junit.Test;
import org.springframework.http.ResponseEntity;

public class OpenApiDataSourceTest extends OpenApiTest {

	@Test
	public void testDataSource() throws Exception {
		TestEntities.reset();
		OpenApiDataSource openApiDatasource = new OpenApiDataSource(site, applicationProvider, request, messageSource) {
		};
		servletRequest.setRequestURI("/entities;param3=5");
		ResponseEntity<Datasource> datasource = openApiDatasource.getDataSource("entities", environment, servletRequest,
				servletResponse);
		WritingJsonValidator.validate(datasource, "rest/openapi/datasource.json");
	}

	@Test
	public void testSingleDataSource() throws Exception {
		TestEntities.reset();
		OpenApiDataSource openApiDatasource = new OpenApiDataSource(site, applicationProvider, request, messageSource) {
		};
		servletRequest.setRequestURI("/entity;entityId=1");
		ResponseEntity<Datasource> datasource = openApiDatasource.getDataSource("entity", environment, servletRequest,
				servletResponse);
		WritingJsonValidator.validate(datasource, "rest/openapi/datasource-single.json");
	}

}
