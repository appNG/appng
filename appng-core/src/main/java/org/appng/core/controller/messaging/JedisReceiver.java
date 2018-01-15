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
package org.appng.core.controller.messaging;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.BusinessException;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.messaging.Event;
import org.appng.api.messaging.EventHandler;
import org.appng.api.messaging.EventRegistry;
import org.appng.api.messaging.Receiver;
import org.appng.api.messaging.Sender;
import org.appng.api.messaging.Serializer;
import org.appng.api.model.Site;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.Jedis;

/**
 * Message receiver implementing {@link Receiver} to use a redis database with its build-in publish/subscribe function
 * as message broker. Following platform properties are needed (default value in brackets):
 * <ul>
 * <li>{@code redisMessagingHost} (localhost): Host of the redis server</li>
 * <li>{@code redisMessagingPort} (6379): Port of the redis server</li>
 * <li>{@code redisMessagingPassword} (): Password of the redis server</li>
 * <li>{@code redisMessagingTimeout} (): Timeout is optional. If not defined, Redis default is used</li>
 * <li>{@code redisMessagingChannel} (appng-messaging): Channel where all cluster nodes should publish and subscribe. Be
 * aware that this name must be different among different clusters using the same Redis server</li>
 * </ul>
 * 
 * @author Claus Stuemke, aiticon GmbH, 2015
 *
 */
public class JedisReceiver extends JedisBase implements Receiver, Runnable {

	private static final Logger LOGGER = LoggerFactory.getLogger(JedisReceiver.class);
	private EventRegistry eventRegistry = new EventRegistry();
	private Jedis jedis;

	public Receiver configure(Serializer eventDeserializer) {
		this.eventSerializer = eventDeserializer;
		initialize();
		return this;
	}

	public Sender createSender() {
		return new JedisSender().configure(eventSerializer);
	}

	public void runWith(ExecutorService executorService) {
		executorService.submit(this);
	}

	@Override
	public void run() {
		jedis = getJedis();
		BinaryJedisPubSub pubSub = new BinaryJedisPubSub() {

			public void onMessage(byte[] channel, byte[] message) {
				Event event = eventSerializer.deserialize(message);
				if (null != event) {
					try {
						LOGGER.debug("Received event {}", event);
						onEvent(eventSerializer.getSite(event.getSiteName()), event);
					} catch (Exception e) {
						LOGGER.error("error while performing event " + event, e);
					}
				} else {
					LOGGER.debug("could not read event {}", message);
				}
			}
		};
		jedis.subscribe(pubSub, this.channel.getBytes());
	}

	void onEvent(Site site, Event event) throws InvalidConfigurationException, BusinessException {
		String currentNode = eventSerializer.getNodeId();
		String originNode = event.getNodeId();
		LOGGER.debug("current node: {}, originNode node: {}", currentNode, originNode);
		boolean sameNode = StringUtils.equals(currentNode, originNode);
		if (!sameNode) {
			LOGGER.info("about to execute {} ", event);
			for (EventHandler<Event> eventHandler : eventRegistry.getHandlers(event)) {
				eventHandler.onEvent(event, eventSerializer.getEnvironment(), site);
			}
		} else {
			LOGGER.debug("message is from myself and can be ignored");
		}
	}

	public void registerHandler(EventHandler<?> handler) {
		eventRegistry.register(handler);
	}

	public void setDefaultHandler(EventHandler<?> defaultHandler) {
		eventRegistry.setDefaultHandler(defaultHandler);
	}

	/**
	 * @return the host
	 */
	public String getHost() {
		return host;
	}

	/**
	 * @param host
	 *            the host to set
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * @return the port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * @param port
	 *            the port to set
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * @return the timeout
	 */
	public int getTimeout() {
		return timeout;
	}

	/**
	 * @param timeout
	 *            the timeout to set
	 */
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public void close() throws IOException {
		jedis.close();
	}

}
