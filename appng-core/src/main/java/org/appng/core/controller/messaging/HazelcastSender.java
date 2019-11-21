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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.appng.api.messaging.Event;
import org.appng.api.messaging.Sender;
import org.appng.api.messaging.Serializer;
import org.slf4j.Logger;

import com.hazelcast.core.HazelcastInstance;

import lombok.extern.slf4j.Slf4j;

/**
 * A {@link Sender} that uses a reliable topic for sending {@link Event}s.
 * 
 * @author Matthias Müller
 *
 * @see HazelcastInstance#getReliableTopic(String)
 */
@Slf4j
public class HazelcastSender extends HazelcastBase implements Sender {

	public HazelcastSender(HazelcastInstance instance) {
		super(instance);
	}

	public HazelcastSender configure(Serializer serializer) {
		this.serializer = serializer;
		return this;
	}

	public boolean send(Event event) {
		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			serializer.serialize(out, event);
			instance.getReliableTopic(getTopicName()).publish(out.toByteArray());
			LOGGER.debug("Successfully published event {} to instance {}", event, instance.getName());
			return true;
		} catch (IOException e) {
			logger().error(String.format("error while sending event %s", event), e);
		}
		return false;
	}

	protected Logger logger() {
		return LOGGER;
	}
}
