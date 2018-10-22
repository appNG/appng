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
package org.appng.core.controller.rest;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.appng.api.InvalidConfigurationException;
import org.appng.api.ProcessingException;
import org.appng.api.rest.model.Action;
import org.appng.api.support.RequestSupportImpl;
import org.appng.core.controller.rest.RestPostProcessor.RestAction;
import org.appng.testsupport.validation.WritingJsonValidator;
import org.appng.xml.MarshallService;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

public class RestActionTest extends RestOperationTest {

	@Test
	public void testGetAction() throws Exception {
		runTest("action-get", getStream("action-get.xml"), true);
	}

	@Test
	public void testGetActionChildFields() throws Exception {
		runTest("action-get-childfields", getStream("action-get-childfields.xml"), true);
	}

	@Test
	public void testPostActionChildFields() throws Exception {
		runTest("action-post-childfields", getStream("action-post-childfields.xml"), false);
	}

	@Test
	public void testPostAction() throws Exception {
		runTest("action-post", getStream("action-get.xml"), false);
	}

	protected void runTest(String actionId, InputStream originalData, boolean istGet)
			throws InvalidConfigurationException, ProcessingException, JAXBException, IOException {
		RequestSupportImpl requestSupport = new RequestSupportImpl();
		requestSupport.setEnvironment(environment);

		org.appng.xml.platform.Action originalAction = MarshallService.getMarshallService().unmarshall(originalData,
				org.appng.xml.platform.Action.class);
		Mockito.when(appconfig.getAction("", actionId)).thenReturn(originalAction);

		org.appng.xml.platform.Action processedAction = MarshallService.getMarshallService()
				.unmarshall(getStream(actionId + ".xml"), org.appng.xml.platform.Action.class);
		Mockito.when(application.processAction(Mockito.eq(servletResponse), Mockito.eq(false), Mockito.any(),
				Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(processedAction);

		Map<String, String> pathVariables = new HashMap<String, String>();
		if (istGet) {
			ResponseEntity<Action> action = new RestAction(site, application, request, messageSource, true)
					.getAction("", actionId, pathVariables, environment, servletRequest, servletResponse);
			WritingJsonValidator.validate(action.getBody(), "rest/" + actionId + "-result.json");
		} else {
			InputStream resource = getStream(actionId + ".json");
			Action input = Jackson2ObjectMapperBuilder.json().build().readValue(resource, Action.class);
			ResponseEntity<Action> action = new RestAction(site, application, request, messageSource, true)
					.performAction("", actionId, pathVariables, input, environment, servletRequest, servletResponse);

			WritingJsonValidator.validate(action.getBody(), "rest/" + actionId + "-result.json");
		}
	}

	public InputStream getStream(String resource) {
		return getClass().getClassLoader().getResourceAsStream("rest/" + resource);
	}

}
