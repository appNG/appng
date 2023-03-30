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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.appng.api.messaging.Event;
import org.appng.api.messaging.Sender;
import org.appng.api.messaging.Serializer;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;

/**
 * Message sender implementing {@link Sender} to use a redis database with its build-in publish/subscribe function as
 * message broker. See {@link JedisReceiver} for configuration details.
 * 
 * @author Claus Stuemke, aiticon GmbH, 2015
 * 
 * @see JedisReceiver
 */
@Slf4j
public class JedisSender extends JedisBase implements Sender {

	public Sender configure(Serializer eventDeserializer) {
		this.eventSerializer = eventDeserializer;
		this.initialize();
		return this;
	}

	public boolean send(Event event) {
		Jedis jedis = getJedis();
		ByteArrayOutputStream outMessage = new ByteArrayOutputStream();
		try {
			eventSerializer.serialize(outMessage, event);
			jedis.publish(channel.getBytes(), outMessage.toByteArray());
			LOGGER.debug("Successfully published event {}", event);
			return true;
		} catch (IOException e) {
			LOGGER.error(String.format("error while sending event %s", event), e);
		} finally {
			jedis.close();
		}
		return false;
	}
}
