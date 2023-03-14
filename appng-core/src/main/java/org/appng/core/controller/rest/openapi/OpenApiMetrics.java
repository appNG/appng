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

import org.appng.api.Request;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.slf4j.Logger;
import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
abstract class OpenApiMetrics extends OpenApiOperation {

	public OpenApiMetrics(Site site, Application application, Request request, MessageSource messageSource) throws JAXBException {
		super(site, application, request, messageSource);
	}

	@GetMapping(path = "/openapi/metrics}")
	public void metrics(HttpServletResponse response) throws IOException {
		TextFormat.write004(response.getWriter(), CollectorRegistry.defaultRegistry.metricFamilySamples());
	}

	@Override
	Logger getLogger() {
		return LOGGER;
	}
}