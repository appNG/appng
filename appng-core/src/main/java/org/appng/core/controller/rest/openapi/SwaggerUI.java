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

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * A controller serving Swagger-UI resources
 */
@RestController
public class SwaggerUI {
	private static final String BASE_PATH = "org/appng/api/rest";
	private static final String YAML_SPEC = "appng-openapi.yaml";

	@GetMapping(path = "/openapi/swagger-ui/{path:.+}")
	public ResponseEntity<byte[]> serveResource(@PathVariable String path) throws IOException {
		ClassPathResource resource;
		if (path.equals(YAML_SPEC)) {
			resource = new ClassPathResource(BASE_PATH + "/" + YAML_SPEC);
		} else {
			resource = new ClassPathResource(BASE_PATH + "/swagger-ui/" + path);
		}
		if (resource.exists()) {
			MultiValueMap<String, String> headers = new LinkedMultiValueMap<String, String>();
			if (path.endsWith(".js")) {
				headers.add(HttpHeaders.CONTENT_TYPE, "text/javascript");
			}
			byte[] data = IOUtils.toByteArray(resource.getInputStream());
			return new ResponseEntity<>(data, headers, HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.NOT_FOUND);
	}

}
