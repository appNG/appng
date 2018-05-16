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
import java.util.Locale;
import java.util.TimeZone;

import javax.xml.bind.JAXBException;

import org.appng.api.Environment;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.ProcessingException;
import org.appng.api.model.Site;
import org.appng.api.model.Subject;
import org.appng.api.rest.model.Action;
import org.appng.api.support.RequestSupportImpl;
import org.appng.core.controller.rest.RestPostProcessor.RestAction;
import org.appng.core.model.ApplicationProvider;
import org.appng.testsupport.validation.WritingJsonValidator;
import org.appng.xml.MarshallService;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class RestActionTest {

	static {
		WritingJsonValidator.writeJson = true;
	}

	@Test
	public void testGetAction() throws Exception {
		runTest("action-get", true);
	}

	@Test
	public void testPostAction() throws Exception {
		runTest("action-post", false);
	}

	@Test
	public void testPostActionValidationError() throws Exception {
		runTest("action-post-validation", false);
	}

	protected void runTest(String actionId, boolean istGet)
			throws InvalidConfigurationException, ProcessingException, JAXBException, IOException {
		Environment environment = Mockito.mock(Environment.class);
		Site site = Mockito.mock(Site.class);
		ApplicationProvider application = Mockito.mock(ApplicationProvider.class);
		RequestSupportImpl requestSupport = new RequestSupportImpl();
		requestSupport.setEnvironment(environment);
		Subject subject = Mockito.mock(Subject.class);
		MessageSource messageSource = Mockito.mock(MessageSource.class);
		MockHttpServletResponse servletResponse = new MockHttpServletResponse();
		MockHttpServletRequest servletRequest = new MockHttpServletRequest();

		Mockito.when(environment.getSubject()).thenReturn(subject);
		Mockito.when(environment.getLocale()).thenReturn(Locale.GERMANY);
		Mockito.when(environment.getTimeZone()).thenReturn(TimeZone.getDefault());
		ClassLoader classLoader = getClass().getClassLoader();
		InputStream is = classLoader.getResourceAsStream("rest/action-get.xml");
		Mockito.when(application.processAction(Mockito.eq(servletResponse), Mockito.eq(false), Mockito.any(),
				Mockito.any(), Mockito.any(), Mockito.any()))
				.thenReturn(MarshallService.getMarshallService().unmarshall(is, org.appng.xml.platform.Action.class));

		if (istGet) {
			ResponseEntity<Action> action = new RestAction(messageSource).getAction("", actionId, site, application,
					environment, servletRequest, servletResponse);
			WritingJsonValidator.validate(action.getBody(), "rest/" +actionId + "-result.json");
		} else {
			InputStream resource = classLoader.getResourceAsStream("rest/" +actionId + ".json");
			Action input = Jackson2ObjectMapperBuilder.json().build().readValue(resource, Action.class);
			ResponseEntity<Action> action = new RestAction(messageSource).performAction("", actionId, input, site,
					application, environment, servletRequest, servletResponse);
			WritingJsonValidator.validate(action.getBody(), "rest/" +actionId + "-result.json");
		}
	}

}
