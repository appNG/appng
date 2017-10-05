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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.appng.api.messaging.Event;
import org.appng.api.messaging.Sender;
import org.appng.api.messaging.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

import net.jodah.lyra.ConnectionOptions;

/**
 * Message sender implementing {@link Sender} to use a RabbitMQ message broker. Following platform properties are needed
 * (default value in brackets):
 * <ul>
 * <li>rabbitMQHost (localhost): Host of the RabbitMQ server</li>
 * <li>rabbitMQPort (5672): Port of the RabbitMQ server</li>
 * <li>rabbitMQUser (guest): Username</li>
 * <li>rabbitMQPassword (guest): Password</li>
 * <li>rabbitMQExchange (appng-messaging): Name of the exchange where the messages are send to. Be aware that this name
 * must be the same for all nodes within a cluster and must be different among different clusters using the same
 * RabbitMQ server</li> </u>
 * 
 * @author Claus Stümke, aiticon GmbH, 2015
 *
 */
public class RabbitMQSender extends RabbitMQBase implements Sender {

	private static final String APPNG_MESSAGING_DISABLED = "appng.messaging.disabled";
	private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMQSender.class);

	@Override
	public RabbitMQSender configure(Serializer eventDeserializer) {
		this.eventSerializer = eventDeserializer;
		initialize();
		return this;
	}

	@Override
	public boolean send(Event event) {
		if (!"true".equals(System.getProperty(APPNG_MESSAGING_DISABLED))) {
			Connection connection = null;
			Channel channel = null;
			try {
				connection = getConnection(new ConnectionOptions(factory));
				channel = connection.createChannel();
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				eventSerializer.serialize(out, event);
				LOGGER.info("sending {} to rabbitMQ host {} exchange {}", event, this.host, this.exchange);
				channel.exchangeDeclare(this.exchange, EXCHANGE_TYPE_FANOUT);
				channel.basicPublish(this.exchange, "", null, out.toByteArray());
				channel.close();
				connection.close();
				return true;
			} catch (IOException | TimeoutException e) {
				LOGGER.error("error while sending event " + event, e);
			}
		}
		return false;
	}
}
