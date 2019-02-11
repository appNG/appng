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
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.appng.api.messaging.Event;
import org.appng.api.messaging.Sender;
import org.appng.api.messaging.Serializer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MulticastSender extends MessageHandler implements Sender {

	private static final String APPNG_MESSAGING_DISABLED = "appng.messaging.disabled";
	public static final String APPNG_MESSAGING_BIND_ADR = "appng.messaging.bind_adr";
	private Serializer eventSerializer;

	public MulticastSender(String address, Integer port) {
		super(address, port);
	}

	public MulticastSender configure(Serializer eventSerializer) {
		this.eventSerializer = eventSerializer;
		return this;
	}

	public boolean send(Event event) {
		String groupAddress = getGroupAddress();
		Integer groupPort = getGroupPort();
		if (!Boolean.getBoolean(APPNG_MESSAGING_DISABLED)) {
			try {
				String nodeAddress = System.getProperty(APPNG_MESSAGING_BIND_ADR);
				InetAddress inetAddress;
				if (null == nodeAddress) {
					inetAddress = InetAddress.getLocalHost();
				} else {
					inetAddress = InetAddress.getByName(nodeAddress);
					LOGGER.debug("{}={}", APPNG_MESSAGING_BIND_ADR, nodeAddress);
				}
				try (DatagramSocket socket = new DatagramSocket(new InetSocketAddress(inetAddress, 0))) {
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					eventSerializer.serialize(out, event);
					InetAddress address = InetAddress.getByName(groupAddress);
					DatagramPacket outPacket = new DatagramPacket(out.toByteArray(), out.size(), address, groupPort);
					socket.send(outPacket);
					LOGGER.info("sending {} to {}:{} via {}", event, groupAddress, groupPort, inetAddress);
					return true;
				}
			} catch (IOException e) {
				LOGGER.error(String.format("error while sending event %s", event), e);
			}
		} else {
			LOGGER.info("sending is disabled via '{}'", APPNG_MESSAGING_DISABLED);
		}
		return false;
	}

}
