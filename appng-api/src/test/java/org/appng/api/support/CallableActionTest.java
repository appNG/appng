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
package org.appng.api.support;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import org.appng.api.ActionProvider;
import org.appng.api.ApplicationConfigProvider;
import org.appng.api.DataContainer;
import org.appng.api.DataProvider;
import org.appng.api.Environment;
import org.appng.api.FieldProcessor;
import org.appng.api.PermissionProcessor;
import org.appng.api.ProcessingException;
import org.appng.api.Scope;
import org.appng.api.Session;
import org.appng.api.model.Application;
import org.appng.api.model.Properties;
import org.appng.api.model.Site;
import org.appng.api.model.Subject;
import org.appng.api.support.validation.DefaultValidationProvider;
import org.appng.xml.platform.Action;
import org.appng.xml.platform.ActionRef;
import org.appng.xml.platform.ApplicationConfig;
import org.appng.xml.platform.ApplicationRootConfig;
import org.appng.xml.platform.Bean;
import org.appng.xml.platform.Condition;
import org.appng.xml.platform.DataConfig;
import org.appng.xml.platform.Datasource;
import org.appng.xml.platform.DatasourceRef;
import org.appng.xml.platform.Event;
import org.appng.xml.platform.Message;
import org.appng.xml.platform.MessageType;
import org.appng.xml.platform.Messages;
import org.appng.xml.platform.MetaData;
import org.appng.xml.platform.Params;
import org.junit.Assert;
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
 */
public class CallableActionTest {

	private static final String MY_ACTION = "myAction";
	private static final String MY_ACTIONREF = "actionRef";
	private static final String MY_DATASOURCE = "myDatasource";
	private static final String MY_EVENT = "myEvent";
	private static final String TEST_BEAN = "testBean";

	@Mock
	private Action action;

	@Mock
	private ActionProvider<Object> actionProvider;

	@Mock
	private DataProvider dataProvider;

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
	private Properties properties;

	@Mock
	private ApplicationConfigProvider applicationConfigProvider;

	@Mock
	public org.appng.forms.Request request;

	@Mock
	public Subject subject;

	@Mock
	public Site site;

	@Test
	public void testPerformException() {
		ApplicationRequest applicationRequest = initApplication(false);

		// force exception
		Mockito.when(config.getLinkpanel()).thenReturn(null);

		try {
			CallableAction action = new CallableAction(site, application, applicationRequest, actionRef);
			action.perform();
			fail("ProcessingException must occur!");
		} catch (ProcessingException e) {
			String message = e.getMessage();
			assertTrue(message.matches("error performing action 'myAction' of event 'myEvent', ID: \\d{6,12}"));
		}
	}

	@Test
	public void testPerformWithErrorFromDataProvider() throws ProcessingException {
		ApplicationRequest applicationRequest = initApplication(true);

		AtomicReference<Messages> envMessages = new AtomicReference<Messages>(new Messages());
		AtomicReference<Messages> actionMessages = new AtomicReference<Messages>(new Messages());
		mockMessages(envMessages, actionMessages, false);

		Mockito.doAnswer(i -> {
			FieldProcessor fp = i.getArgumentAt(5, FieldProcessor.class);
			DataContainer dataContainer = new DataContainer(fp);
			dataContainer.setItem(new Object());
			fp.addErrorMessage("Error!");
			return dataContainer;
		}).when(dataProvider).getData(Mockito.eq(site), Mockito.eq(application), Mockito.eq(environment), Mockito.any(),
				Mockito.eq(applicationRequest), Mockito.any());

		Mockito.when(environment.removeAttribute(Scope.SESSION, Session.Environment.MESSAGES))
				.thenReturn(new Messages());
		CallableAction action = new CallableAction(site, application, applicationRequest, actionRef);
		action.perform();
		Assert.assertNotNull(envMessages.get());
		Assert.assertNotNull(actionMessages.get());
		Message message = actionMessages.get().getMessageList().get(0);
		Assert.assertEquals("Error!", message.getContent());
		Assert.assertEquals(MessageType.ERROR, message.getClazz());

	}

	@Test
	public void testPerformWithErrorFromAction() throws ProcessingException {
		ApplicationRequest applicationRequest = initApplication(true);

		AtomicReference<Messages> envMessages = new AtomicReference<Messages>(new Messages());
		AtomicReference<Messages> actionMessages = new AtomicReference<Messages>(new Messages());
		mockMessages(envMessages, actionMessages, true);

		Mockito.doAnswer(i -> {
			FieldProcessor fp = i.getArgumentAt(5, FieldProcessor.class);
			DataContainer dataContainer = new DataContainer(fp);
			dataContainer.setItem(new Object());
			return dataContainer;
		}).when(dataProvider).getData(Mockito.eq(site), Mockito.eq(application), Mockito.eq(environment), Mockito.any(),
				Mockito.eq(applicationRequest), Mockito.any());

		Mockito.doAnswer(i -> {
			FieldProcessor fp = i.getArgumentAt(6, FieldProcessor.class);
			fp.addErrorMessage("BOOOOM!");
			return null;
		}).when(actionProvider).perform(Mockito.eq(site), Mockito.eq(application), Mockito.eq(environment),
				Mockito.any(), Mockito.eq(applicationRequest), Mockito.any(), Mockito.any());

		CallableAction action = new CallableAction(site, application, applicationRequest, actionRef);
		action.perform();
		Assert.assertNull(envMessages.get());
		Assert.assertNotNull(actionMessages.get());
		List<Message> messageList = actionMessages.get().getMessageList();
		Assert.assertEquals("BOOOOM!", messageList.get(0).getContent());
		Assert.assertEquals(MessageType.ERROR, messageList.get(0).getClazz());

	}

	@Test
	public void testPerform() throws ProcessingException {
		ApplicationRequest applicationRequest = initApplication(true);

		AtomicReference<Messages> envMessages = new AtomicReference<Messages>(new Messages());
		AtomicReference<Messages> actionMessages = new AtomicReference<Messages>(new Messages());
		mockMessages(envMessages, actionMessages, true);

		Mockito.doAnswer(i -> {
			FieldProcessor fp = i.getArgumentAt(5, FieldProcessor.class);
			DataContainer dataContainer = new DataContainer(fp);
			dataContainer.setItem(new Object());
			fp.addOkMessage("Done!");
			return dataContainer;
		}).when(dataProvider).getData(Mockito.eq(site), Mockito.eq(application), Mockito.eq(environment), Mockito.any(),
				Mockito.eq(applicationRequest), Mockito.any());

		Mockito.doAnswer(i -> {
			FieldProcessor fp = i.getArgumentAt(6, FieldProcessor.class);
			fp.addOkMessage("ACTION!");
			return null;
		}).when(actionProvider).perform(Mockito.eq(site), Mockito.eq(application), Mockito.eq(environment),
				Mockito.any(), Mockito.eq(applicationRequest), Mockito.any(), Mockito.any());
		Mockito.when(actionRef.getMode()).thenReturn("awesome");
		CallableAction action = new CallableAction(site, application, applicationRequest, actionRef);
		action.perform();

		Assert.assertNull(envMessages.get());

		Assert.assertNotNull(actionMessages.get());
		List<Message> messageList = actionMessages.get().getMessageList();

		Message fromAction = messageList.get(0);
		Assert.assertEquals("ACTION!", fromAction.getContent());
		Assert.assertEquals(MessageType.OK, fromAction.getClazz());

		Message fromDataSource = messageList.get(1);
		Assert.assertEquals("Done!", fromDataSource.getContent());
		Assert.assertEquals(MessageType.OK, fromDataSource.getClazz());

		Mockito.verify(action.getAction()).setMode(actionRef.getMode());
	}

	public void mockMessages(AtomicReference<Messages> envMessages, AtomicReference<Messages> actionMessages,
			boolean returnRemoved) {
		Mockito.when(environment.getAttribute(Scope.SESSION, Session.Environment.MESSAGES))
				.thenReturn(envMessages.get());
		Mockito.doAnswer(i -> {
			envMessages.set(i.getArgumentAt(2, Messages.class));
			return null;
		}).when(environment).setAttribute(Mockito.eq(Scope.SESSION), Mockito.eq(Session.Environment.MESSAGES),
				Mockito.any());

		Mockito.doAnswer(i -> {
			Messages messages = envMessages.get();
			envMessages.set(null);
			return returnRemoved ? messages : null;
		}).when(environment).removeAttribute(Scope.SESSION, Session.Environment.MESSAGES);

		Mockito.when(environment.getAttribute(Scope.SESSION, Session.Environment.MESSAGES))
				.thenReturn(envMessages.get());

		Mockito.doAnswer(i -> {
			actionMessages.set(i.getArgumentAt(0, Messages.class));
			return null;
		}).when(action).setMessages(Mockito.any());

		Mockito.doAnswer(i -> actionMessages.get()).when(action).getMessages();
	}

	public ApplicationRequest initApplication(boolean withDataSource) {
		MockitoAnnotations.initMocks(this);

		permissionProcessor = new DefaultPermissionProcessor(subject, site, application);
		ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
		messageSource.setBasename("testmessages");
		RequestSupportImpl requestSupport = new RequestSupportImpl();
		requestSupport.setMessageSource(messageSource);
		requestSupport.setEnvironment(environment);
		ApplicationRequest applicationRequest = new ApplicationRequest(request,
				new DummyPermissionProcessor(subject, site, application), requestSupport);
		applicationRequest.setApplicationConfig(applicationConfigProvider);
		applicationRequest.setValidationProvider(new DefaultValidationProvider());

		Mockito.when(config.getParams()).thenReturn(new Params());
		Mockito.when(config.getLinkpanel()).thenReturn(new ArrayList<>());
		MetaData metaData = new MetaData();
		metaData.setBindClass(Object.class.getName());
		Mockito.when(config.getMetaData()).thenReturn(metaData);
		Mockito.when(action.getConfig()).thenReturn(config);
		Mockito.when(action.getBean()).thenReturn(bean);
		Mockito.when(action.getId()).thenReturn(MY_ACTION);
		Condition condition = new Condition();
		condition.setExpression("${APP.foo < 42.01}");
		Mockito.when(action.getCondition()).thenReturn(condition);
		Mockito.when(actionRef.getEventId()).thenReturn(MY_EVENT);
		Mockito.when(actionRef.getId()).thenReturn(MY_ACTIONREF);
		Mockito.when(actionRef.getParams()).thenReturn(new Params());

		if (withDataSource) {
			Mockito.when(action.getDatasource()).thenReturn(datasourceRef);
			Mockito.when(datasourceRef.getId()).thenReturn(MY_DATASOURCE);
			Mockito.when(datasourceRef.getParams()).thenReturn(new Params());
			Mockito.when(datasource.getConfig()).thenReturn(config);
			Mockito.when(datasource.getBean()).thenReturn(bean);
			Mockito.when(application.getBean(TEST_BEAN, DataProvider.class)).thenReturn(dataProvider);
			Mockito.when(applicationConfigProvider.getDatasource(MY_DATASOURCE)).thenReturn(datasource);
		}
		Mockito.when(bean.getId()).thenReturn(TEST_BEAN);

		Mockito.when(environment.getLocale()).thenReturn(Locale.GERMAN);
		Mockito.when(event.getConfig()).thenReturn(config);
		Mockito.when(event.getId()).thenReturn(MY_EVENT);
		Mockito.when(application.getBean(TEST_BEAN, ActionProvider.class)).thenReturn(actionProvider);
		Mockito.when(site.getProperties()).thenReturn(properties);
		Mockito.when(application.getProperties()).thenReturn(properties);
		java.util.Properties props = new java.util.Properties();
		props.put("foo", 42d);
		Mockito.when(properties.getPlainProperties()).thenReturn(props);
		Mockito.when(application.getBean(MessageSource.class)).thenReturn(messageSource);
		Mockito.when(applicationConfigProvider.getAction(MY_EVENT, MY_ACTIONREF)).thenReturn(action);
		Mockito.when(applicationConfigProvider.getEvent(MY_EVENT)).thenReturn(event);
		ApplicationRootConfig applicationRootConfig = new ApplicationRootConfig();
		applicationRootConfig.setConfig(new ApplicationConfig());
		Mockito.when(applicationConfigProvider.getApplicationRootConfig()).thenReturn(applicationRootConfig);
		return applicationRequest;
	}

	@Test
	public void testHiddenAction() throws ProcessingException {
		CallableAction callableAction = getCallableAction(false, null);
		callableAction.perform(true);
		Assert.assertNull(callableAction.getAction().getMessages());
		Assert.assertNull(callableAction.getAction().getOnSuccess());
	}

	@Test
	public void testForward() throws ProcessingException {
		CallableAction callableAction = getCallableAction(true, "foo");
		callableAction.perform(false);
		Assert.assertNull(callableAction.getAction().getMessages());
		Assert.assertEquals("/prefix/foo", callableAction.getAction().getOnSuccess());
	}

	@Test
	public void testNoForward() throws ProcessingException {
		CallableAction callableAction = getCallableAction(false, null);
		callableAction.perform(false);
		Assert.assertNull(callableAction.getAction().getMessages());
		Assert.assertNull(callableAction.getAction().getOnSuccess());
	}

	private CallableAction getCallableAction(boolean forward, String onSuccess) {
		Action action = new Action();
		action.setOnSuccess(onSuccess);
		ElementHelper elementHelper = new ElementHelper(null, null, null, null) {
			@Override
			public Messages removeMessages() {
				Messages messages = new Messages();
				Message message = new Message();
				message.setClazz(MessageType.ERROR);
				message.setContent("Foo!");
				messages.getMessageList().add(message);
				return messages;
			}

			@Override
			public String getOutputPrefix() {
				return "/prefix/";
			}
		};

		Site site = Mockito.mock(Site.class);
		ApplicationRequest req = Mockito.mock(ApplicationRequest.class);
		CallableAction callableAction = new CallableAction(site, req, elementHelper) {
			@Override
			public boolean doExecute() {
				return true;
			}

			@Override
			public boolean doForward() {
				return forward;
			}

			@Override
			public Action getAction() {
				return action;
			}

			@Override
			public String getOnSuccess() {
				return onSuccess;
			}

			@Override
			protected boolean retrieveData(boolean setBeanNull) throws ProcessingException {
				return true;
			}
		};

		return callableAction;
	}
}
