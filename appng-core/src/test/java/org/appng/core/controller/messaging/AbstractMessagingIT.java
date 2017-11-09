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
package org.appng.core.controller.messaging;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.appng.api.BusinessException;
import org.appng.api.Environment;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.Platform;
import org.appng.api.Scope;
import org.appng.api.messaging.Event;
import org.appng.api.messaging.EventHandler;
import org.appng.api.messaging.Messaging;
import org.appng.api.messaging.Receiver;
import org.appng.api.messaging.Sender;
import org.appng.api.model.Properties;
import org.appng.api.model.Site;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.BeanWrapperImpl;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * Abstract base class for integration tests of messaging {@link Sender}s and {@link Receiver}s
 * 
 * @author Matthias Müller
 *
 */
public abstract class AbstractMessagingIT {

	@Mock
	Properties props;

	Class<? extends Sender> senderClass;
	Class<? extends Receiver> receiverClass;

	public AbstractMessagingIT(Class<? extends Sender> senderClass, Class<? extends Receiver> receiverClass) {
		this.senderClass = senderClass;
		this.receiverClass = receiverClass;
	}

	@Test(timeout = 5000)
	public void test() throws Exception {
		MockitoAnnotations.initMocks(this);
		test(true);
	}

	public void test(boolean configure) throws Exception {
		Environment env = Mockito.mock(Environment.class);
		ThreadFactoryBuilder tfb = new ThreadFactoryBuilder();
		ThreadFactory threadFactory = tfb.setDaemon(true).setNameFormat("appng-messaging").build();
		ExecutorService executor = Executors.newSingleThreadExecutor(threadFactory);

		Mockito.when(props.getBoolean(Platform.Property.MESSAGING_ENABLED)).thenReturn(true);
		Mockito.when(props.getString(Platform.Property.MESSAGING_RECEIVER)).thenReturn(receiverClass.getName());

		if (configure) {
			configureMessaging();
		}

		Mockito.when(env.getAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG)).thenReturn(props);
		Mockito.when(env.getAttribute(Scope.PLATFORM, Platform.Environment.SITES))
				.thenReturn(new HashMap<String, Site>());

		final BeanWrapperImpl wrapper = new BeanWrapperImpl();

		ProcessedHandler processedHandler = new ProcessedHandler();
		Mockito.doAnswer(new Answer<Void>() {
			public Void answer(InvocationOnMock invocation) throws Throwable {
				wrapper.setBeanInstance(invocation.getArgumentAt(2, receiverClass));
				return null;
			}
		}).when(env).setAttribute(Mockito.eq(Scope.PLATFORM), Mockito.eq(Platform.Environment.MESSAGE_RECEIVER),
				Mockito.any(receiverClass));

		Sender sender = Messaging.createMessageSender(env, executor, "test", processedHandler, null);
		Mockito.verify(env).setAttribute(Mockito.eq(Scope.PLATFORM), Mockito.eq(Platform.Environment.MESSAGE_RECEIVER),
				Mockito.any(receiverClass));
		Mockito.verify(env).setAttribute(Mockito.eq(Scope.PLATFORM), Mockito.eq(Platform.Environment.MESSAGE_SENDER),
				Mockito.any(senderClass));
		Assert.assertTrue(sender.send(new MessagingTest()));

		while (!processedHandler.processed) {
			Thread.sleep(1000);
		}
		Assert.assertTrue(processedHandler.processed);
		Mockito.when(env.getAttribute(Scope.PLATFORM, Platform.Environment.MESSAGE_SENDER)).thenReturn(sender);
		Mockito.when(env.getAttribute(Scope.PLATFORM, Platform.Environment.MESSAGE_RECEIVER))
				.thenReturn(wrapper.getWrappedInstance());
		Messaging.shutdown(env);
	}

	protected abstract void configureMessaging();

	protected void mockDefaultString(String name, String defaultValue) {
		Mockito.when(props.getString(name, defaultValue)).thenReturn(defaultValue);
	}

	protected void mockDefaultInteger(String name, Integer defaultValue) {
		Mockito.when(props.getInteger(name, defaultValue)).thenReturn(defaultValue);
	}

	class ProcessedHandler implements EventHandler<Event> {

		private boolean processed;

		public void onEvent(Event event, Environment environment, org.appng.api.model.Site site)
				throws InvalidConfigurationException, BusinessException {
			processed = true;
		}

		public Class<Event> getEventClass() {
			return Event.class;
		}

	}
}
