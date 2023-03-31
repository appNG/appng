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

import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBException;

import org.appng.api.model.Application;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;

@Lazy
@RestController
class OpenApiMetrics {

	private Application application;

	@Autowired
	public OpenApiMetrics(Application application) throws JAXBException {
		this.application = application;
	}

	@GetMapping(path = "/openapi/metrics")
	public void metrics(HttpServletResponse response) throws IOException {
		CollectorRegistry registry = application.getBean(CollectorRegistry.class);
		if (null != registry) {
			response.setContentType(TextFormat.CONTENT_TYPE_OPENMETRICS_100);
			TextFormat.writeOpenMetrics100(response.getWriter(), registry.metricFamilySamples());
			return;
		}
		response.setStatus(HttpStatus.NOT_IMPLEMENTED.value());
	}

}