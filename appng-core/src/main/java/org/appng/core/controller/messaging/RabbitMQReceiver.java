/*
 * Copyright 2011-2019 the original author or authors.
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
import org.appng.api.messaging.Event;
import org.appng.api.messaging.EventHandler;
import org.appng.api.messaging.EventRegistry;
import org.appng.api.messaging.Receiver;
import org.appng.api.messaging.Serializer;
import org.appng.api.model.Site;
import org.slf4j.Logger;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AMQP.Queue.DeclareOk;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import lombok.extern.slf4j.Slf4j;

/**
 * Message receiver implementing {@link Receiver} to use a RabbitMQ message broker. Following platform properties are
 * needed (default value in brackets):
 * <ul>
 * <li>{@code rabbitMQAdresses} (localhost:5672): A comma separated list of &lt;host&gt;:&lt;port&gt; for RabbitMQ
 * server(s)</li>
 * <li>{@code rabbitMQUser} (guest): Username</li>
 * <li>{@code rabbitMQPassword} (guest): Password</li>
 * <li>{@code rabbitMQExchange} (appng-messaging): Name of the exchange where the receiver binds its messaging queue on.
 * Be aware that this name must be different among different clusters using the same RabbitMQ server</li>
 * <li>{@code rabbitMQAutoDeleteQueue} (true): If the queue to create should be marked as autodelete.</li>
 * <li>{@code rabbitMQDurableQueue} (false): If the queue to create should be marked as durable.</li>
 * <li>{@code rabbitMQExclusiveQueue} (true): If the queue to create should be marked as exclusive.</li>
 * </ul>
 * 
 * @author Claus Stümke, aiticon GmbH, 2015
 * @author Matthias Müller
 */
@Slf4j
public class RabbitMQReceiver extends RabbitMQBase implements Receiver {

	private static final String RABBIT_MQ_AUTO_DELETE_QUEUE = "rabbitMQAutoDeleteQueue";
	private static final String RABBIT_MQ_DURABLE_QUEUE = "rabbitMQDurableQueue";
	private static final String RABBIT_MQ_EXCLUSIVE_QUEUE = "rabbitMQExclusiveQueue";
	private EventRegistry eventRegistry = new EventRegistry();
	private DeclareOk queueDeclare;

	public Receiver configure(Serializer eventSerializer) {
		this.eventSerializer = eventSerializer;
		initialize("appng-rabbitmq-receiver-%d");
		return this;
	}

	@SuppressWarnings("resource")
	public RabbitMQSender createSender() {
		return new RabbitMQSender().configure(eventSerializer);
	}

	public void runWith(ExecutorService executorService) {
		try {
			channel.exchangeDeclarePassive(exchange);
			log().info("Exchange {} already exists.", exchange);
		} catch (IOException e) {
			try {
				log().info("Declaring exchange '{}' (type: {})", exchange, BuiltinExchangeType.FANOUT);
				channel = connection.createChannel();
				channel.exchangeDeclare(exchange, BuiltinExchangeType.FANOUT, true);
			} catch (IOException e1) {
				throw new IllegalStateException("Error declaring exchange.", e1);
			}
		}

		String nodeId = eventSerializer.getNodeId();
		String queueName = (null == nodeId) ? StringUtils.EMPTY : String.format("%s@%s", nodeId, exchange);

		try {
			queueDeclare = channel.queueDeclarePassive(queueName);
			log().info("Queue {} already exists.", queueName);
		} catch (IOException e) {
			try {
				boolean durable = eventSerializer.getPlatformConfig().getBoolean(RABBIT_MQ_DURABLE_QUEUE, false);
				boolean exclusive = eventSerializer.getPlatformConfig().getBoolean(RABBIT_MQ_EXCLUSIVE_QUEUE, true);
				boolean autoDelete = eventSerializer.getPlatformConfig().getBoolean(RABBIT_MQ_AUTO_DELETE_QUEUE, true);
				log().info("Declaring queue '{}'.", queueName);
				channel = connection.createChannel();
				queueDeclare = channel.queueDeclare(queueName, durable, exclusive, autoDelete, null);
			} catch (IOException e1) {
				throw new IllegalStateException("Error declaring queue.", e);
			}
		}
		try {
			String queue = queueDeclare.getQueue();
			log().info("Binding queue '{}' to exchange {}", queue, exchange);
			channel.queueBind(queue, exchange, "");

			EventConsumer consumer = new EventConsumer(channel);
			log().info("Consuming queue '{}' with {}", queue, consumer);
			channel.basicConsume(queue, true, consumer);
		} catch (IOException e) {
			throw new IllegalStateException("Error binding queue to exchange.", e);
		}
	}

	class EventConsumer extends DefaultConsumer {

		public EventConsumer(Channel channel) {
			super(channel);
		}

		@Override
		public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
				throws IOException {
			Event event = eventSerializer.deserialize(body);
			if (null != event) {
				try {
					Site site = eventSerializer.getSite(event.getSiteName());
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

				} catch (Exception e) {
					LOGGER.error(String.format("Error while executing event %s", event), e);
				}
			} else {
				LOGGER.debug("could not read event");
			}
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + " (" + channel + ")";
		}
	}

	public void registerHandler(EventHandler<?> handler) {
		eventRegistry.register(handler);
	}

	public void setDefaultHandler(EventHandler<?> defaultHandler) {
		eventRegistry.setDefaultHandler(defaultHandler);
	}

	Logger log() {
		return LOGGER;
	}

}
