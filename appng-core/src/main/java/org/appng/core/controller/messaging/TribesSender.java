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

import org.apache.catalina.tribes.ByteMessage;
import org.apache.catalina.tribes.Channel;
import org.apache.catalina.tribes.ChannelException;
import org.apache.catalina.tribes.Member;
import org.apache.commons.lang3.StringUtils;
import org.appng.api.messaging.Event;
import org.appng.api.messaging.Sender;
import org.appng.api.messaging.Serializer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TribesSender implements Sender {

	private Channel channel;
	private Serializer eventSerializer;

	public TribesSender configure(Serializer eventSerializer) {
		this.eventSerializer = eventSerializer;
		return this;
	}

	public TribesSender(Channel channel) {
		this.channel = channel;
	}

	public boolean send(Event event) {
		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			eventSerializer.serialize(out, event);
			Member[] members = channel.getMembers();
			channel.send(members, new ByteMessage(out.toByteArray()), Channel.SEND_OPTIONS_BYTE_MESSAGE);
			LOGGER.info("sending {} to {} via {}", event, StringUtils.join(members), channel);
			return true;
		} catch (ChannelException | IOException e) {
			LOGGER.error(String.format("error sending %s", event), e);
		}
		return false;
	}

}
