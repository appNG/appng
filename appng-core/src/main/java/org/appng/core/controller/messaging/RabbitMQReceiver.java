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

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.BusinessException;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.messaging.Event;
import org.appng.api.messaging.EventHandler;
import org.appng.api.messaging.EventRegistry;
import org.appng.api.messaging.Receiver;
import org.appng.api.messaging.Serializer;
import org.appng.api.model.Site;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AMQP.Queue.DeclareOk;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import net.jodah.lyra.ConnectionOptions;

/**
 * Message receiver implementing {@link Receiver} to use a RabbitMQ message broker. Following platform properties are
 * needed (default value in brackets):
 * <ul>
 * <li>rabbitMQHost (localhost): Host of the RabbitMQ server</li>
 * <li>rabbitMQPort (5672): Port of the RabbitMQ server</li>
 * <li>rabbitMQUser (guest): Username</li>
 * <li>rabbitMQPassword (guest): Password</li>
 * <li>rabbitMQExchange (appng-messaging): Name of the exchange where the receiver binds its messaging queue on. Be
 * aware that this name must be different among different clusters using the same RabbitMQ server</li>
 * </ul>
 * 
 * @author Claus Stümke, aiticon GmbH, 2015
 *
 */
public class RabbitMQReceiver extends RabbitMQBase implements Receiver {

	private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMQReceiver.class);
	private EventRegistry eventRegistry = new EventRegistry();
	private Connection connection;

	public Receiver configure(Serializer eventSerializer) {
		this.eventSerializer = eventSerializer;
		initialize();
		return this;
	}

	public RabbitMQSender createSender() {
		RabbitMQSender sender = new RabbitMQSender();
		sender.configure(eventSerializer);
		return sender;
	}

	public void runWith(ExecutorService executorService) {

		try {
			ThreadFactory threadFactory = new ThreadFactoryBuilder().setDaemon(true)
					.setNameFormat("appng-messaging-rabbitmq").build();
			factory.setThreadFactory(threadFactory);
			ConnectionOptions connectionOptions = new ConnectionOptions(factory);
			connectionOptions.withConsumerExecutor(executorService);
			connection = getConnection(connectionOptions);
			Channel channel = connection.createChannel();
			DeclareOk queueDeclare;
			channel.exchangeDeclare(this.exchange, EXCHANGE_TYPE_FANOUT);
			String nodeId = eventSerializer.getNodeId();
			if (null != nodeId) {
				// create a queue with a name containing the node id
				String queueName = "appngNode_" + nodeId + "_queue";
				queueDeclare = channel.queueDeclare(queueName, false, true, true, null);
			} else {
				// no node id, queue name doesn't matter
				queueDeclare = channel.queueDeclare();
			}
			channel.queueBind(queueDeclare.getQueue(), this.exchange, "");

			Consumer consumer = new DefaultConsumer(channel) {
				@Override
				public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
						byte[] body) throws IOException {
					Event event = eventSerializer.deserialize(body);
					if (null != event) {
						try {
							onEvent(eventSerializer.getSite(event.getSiteName()), event);
						} catch (Exception e) {
							LOGGER.error("Error while executing event " + event, e);
						}
					} else {
						LOGGER.debug("could not read event");
					}
				}
			};
			channel.basicConsume(queueDeclare.getQueue(), true, consumer);
		} catch (Exception e) {
			LOGGER.error("Error starting messaging receiver!");
		}

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

	public void close() throws IOException {
		connection.close();
	}

}
