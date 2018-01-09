/*
 * Copyright 2011-2017 the original author or authors.
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
package org.appng.api.support;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Locale;

import org.appng.api.ActionProvider;
import org.appng.api.ApplicationConfigProvider;
import org.appng.api.Environment;
import org.appng.api.PermissionProcessor;
import org.appng.api.ProcessingException;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.api.model.Subject;
import org.appng.xml.platform.Action;
import org.appng.xml.platform.ActionRef;
import org.appng.xml.platform.ApplicationConfig;
import org.appng.xml.platform.ApplicationRootConfig;
import org.appng.xml.platform.Bean;
import org.appng.xml.platform.DataConfig;
import org.appng.xml.platform.Datasource;
import org.appng.xml.platform.DatasourceRef;
import org.appng.xml.platform.Event;
import org.appng.xml.platform.Params;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * Test for {@link CallableAction}.
 * 
 * @author Matthias Herlitzius
 * 
 */
public class CallableActionTest {

	private static final String MY_ACTION = "myAction";
	private static final String MY_ACTIONREF = "actionRef";
	private static final String MY_EVENT = "myEvent";
	private static final String TEST_BEAN = "testBean";

	@Mock
	private Action action;

	@Mock
	private ActionProvider<Object> actionProvider;

	@Mock
	private ActionRef actionRef;

	@Mock
	private Bean bean;

	@Mock
	private DataConfig config;

	@Mock
	private Datasource datasource;

	@Mock
	private DatasourceRef datasourceRef;

	@Mock
	private Environment environment;

	@Mock
	private Event event;

	@Mock
	private PermissionProcessor permissionProcessor;

	@Mock
	private Application application;

	@Mock
	private ApplicationConfigProvider applicationConfigProvider;

	@Mock
	public org.appng.forms.Request request;

	@Test
	public void testPerformException() {
		MockitoAnnotations.initMocks(this);

		Subject subject = Mockito.mock(Subject.class);
		Site site = Mockito.mock(Site.class);
		permissionProcessor = new DefaultPermissionProcessor(subject, site, application);
		ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
		messageSource.setBasename("testmessages");
		RequestSupportImpl requestSupport = new RequestSupportImpl();
		requestSupport.setMessageSource(messageSource);
		requestSupport.setEnvironment(environment);
		ApplicationRequest applicationRequest = new ApplicationRequest(request, new DummyPermissionProcessor(subject, site,
				application), requestSupport);
		applicationRequest.setApplicationConfig(applicationConfigProvider);

		Mockito.when(action.getConfig()).thenReturn(config);
		Mockito.when(action.getBean()).thenReturn(bean);
		Mockito.when(action.getId()).thenReturn(MY_ACTION);
		Mockito.when(actionRef.getEventId()).thenReturn(MY_EVENT);
		Mockito.when(actionRef.getId()).thenReturn(MY_ACTIONREF);
		Mockito.when(actionRef.getParams()).thenReturn(new Params());
		Mockito.when(bean.getId()).thenReturn(TEST_BEAN);
		Mockito.when(config.getParams()).thenReturn(new Params());
		Mockito.when(environment.getLocale()).thenReturn(Locale.GERMAN);
		Mockito.when(event.getConfig()).thenReturn(config);
		Mockito.when(event.getId()).thenReturn(MY_EVENT);
		Mockito.when(application.getBean(TEST_BEAN, ActionProvider.class)).thenReturn(actionProvider);
		Mockito.when(application.getBean(MessageSource.class)).thenReturn(messageSource);
		Mockito.when(applicationConfigProvider.getAction(MY_EVENT, MY_ACTIONREF)).thenReturn(action);
		Mockito.when(applicationConfigProvider.getEvent(MY_EVENT)).thenReturn(event);
		ApplicationRootConfig applicationRootConfig = new ApplicationRootConfig();
		applicationRootConfig.setConfig(new ApplicationConfig());
		Mockito.when(applicationConfigProvider.getApplicationRootConfig()).thenReturn(applicationRootConfig);

		// force exception
		Mockito.when(config.getLinkpanel()).thenReturn(null);

		try {
			CallableAction action = new CallableAction(site, application, applicationRequest, actionRef);
			action.perform();
			fail();
		} catch (ProcessingException e) {
			String message = e.getMessage();
			assertTrue(message.matches("error performing action 'myAction' of event 'myEvent', ID: \\d{6,12}"));
		}
	}
}
