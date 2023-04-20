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
package org.appng.core.controller.messaging;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import org.appng.api.Environment;
import org.appng.api.Platform;
import org.appng.api.Scope;
import org.appng.api.messaging.Event;
import org.appng.api.messaging.EventHandler;
import org.appng.api.messaging.EventRegistry;
import org.appng.api.messaging.Receiver;
import org.appng.api.messaging.Sender;
import org.appng.api.messaging.Serializer;
import org.appng.core.service.HazelcastConfigurer;
import org.slf4j.Logger;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.topic.ITopic;
import com.hazelcast.topic.Message;
import com.hazelcast.topic.MessageListener;

import lombok.extern.slf4j.Slf4j;

/**
 * A {@link Receiver} that uses a
 * <a href= "https://docs.hazelcast.com/hazelcast/5.2/data-structures/topic">Topic</a> for sending
 * {@link Event}s. Following platform properties are supported (default value in brackets):
 * <ul>
 * <li>{@code hazelcastTopicName} (appng-messaging): Name of the topic</li>
 * </ul>
 * 
 * @author Matthias Müller
 *
 * @see HazelcastInstance#getReliableTopic(String)
 */
@Slf4j
public class HazelcastReceiver extends HazelcastBase implements Receiver, MessageListener<byte[]> {

	private EventRegistry eventRegistry = new EventRegistry();
	private UUID listenerId;

	public Receiver configure(Serializer serializer) {
		this.serializer = serializer;
		Environment env = serializer.getEnvironment();
		instance = HazelcastConfigurer.getInstance(
				env.getAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG), serializer.getNodeId());
		return this;
	}

	public Sender createSender() {
		return new HazelcastSender(instance).configure(serializer);
	}

	public void runWith(ExecutorService executorService) {
		ITopic<byte[]> topic = getTopic();
		this.listenerId = topic.addMessageListener(this);
		LOGGER.info("Listening to topic {} on {} with id {}", topic.getName(), instance, listenerId);
	}

	@Override
	public void close() throws IOException {
		getTopic().removeMessageListener(listenerId);
		String appngVersion = serializer.getEnvironment().getAttribute(Scope.PLATFORM,
				Platform.Environment.APPNG_VERSION);
		if (null == appngVersion) {
			instance.shutdown();
		}
	}

	public void onMessage(Message<byte[]> message) {
		Messaging.handleEvent(LOGGER, eventRegistry, serializer, message.getMessageObject());
	}

	protected Logger logger() {
		return LOGGER;
	}

	public void registerHandler(EventHandler<?> handler) {
		eventRegistry.register(handler);
	}

	public void setDefaultHandler(EventHandler<?> defaultHandler) {
		eventRegistry.setDefaultHandler(defaultHandler);
	}
}
